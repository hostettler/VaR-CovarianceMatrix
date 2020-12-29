package gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ValueAtRiskJava {

	private static final int BLOCK_SIZE = 250;

	// input
	private final float[] instrumentsValueHistory;
	// input
	private final float[] instrumentsWeight;
	// output
	private final float[] varianceCovarienceMatrix;

	private final float[] instrumentsExcessReturns;
	// [0] Average price, [1] Average return
	private final float[] instrumentsStatistics;
	private final float[] weightedCovariance;

	private float[] weightedAverageReturnsBuffer;
	private float[] portfolioStandardDeviationBuffer;

	private int numberOfInstruments;
	private int numberOfObservations;

	private final float[] zstats = new float[] { /* 90% */-1.28155f, /* 95% */-1.64485f, /* 97.5% */-1.95996f,
			/* 99% */-2.32635f };

	private float weightedAverageReturns = 0;
	private float portfolioStandardDeviation = 0;

	private float[] VaRPercent = new float[zstats.length];
	private float[] VaRValue = new float[zstats.length];

	private float portfolioValue;
	private int subrange;

	public ValueAtRiskJava(float[] instrumentsValueHistory, float[] instrumentsWeight, float portfolioValue,
			int numberOfInstruments, int numberOfObservations) {
		this.numberOfInstruments = numberOfInstruments;
		this.instrumentsWeight = instrumentsWeight;
		this.numberOfObservations = numberOfObservations;
		this.instrumentsValueHistory = instrumentsValueHistory;
		this.instrumentsExcessReturns = new float[numberOfInstruments * numberOfObservations];
		this.instrumentsStatistics = new float[numberOfInstruments * 2];
		this.varianceCovarienceMatrix = new float[numberOfInstruments * numberOfInstruments];
		this.weightedCovariance = new float[numberOfInstruments];
		this.portfolioValue = portfolioValue;
		this.subrange = (this.numberOfInstruments / BLOCK_SIZE)  + 1;
		this.weightedAverageReturnsBuffer = new float[subrange];
		this.portfolioStandardDeviationBuffer = new float[subrange];

	}

	public float[] getVarianceCovarianceMatrix() {
		return varianceCovarienceMatrix;
	}

	public void execute() {

		try {

			ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
					Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());

			List<Callable<Integer>> step1Tasks = new ArrayList<>();
			for (int i = 0; i < this.numberOfInstruments; i++) {
				final int index = i;
				Callable<Integer> step1 = () -> {
					calculateExcessReturns(index);
					return 0;
				};
				step1Tasks.add(step1);
			}
			executorService.invokeAll(step1Tasks);

			List<Callable<?>> step2Tasks = new ArrayList<>();
			for (int i = 0; i < this.numberOfInstruments; i++) {
				final int index = i;
				Callable<?> step1 = () -> {
					computeWeightedAverageReturns(index);
					buildVarianceCovarianceMatrix(index);
					return 0;
				};
				step2Tasks.add(step1);
			}
			executorService.invokeAll(step2Tasks);

			List<Callable<?>> step3Tasks = new ArrayList<>();
			for (int i = 0; i < this.numberOfInstruments; i++) {
				final int index = i;
				Callable<?> step1 = () -> {
					computeWeightedCovarianceMatrix(index);
					return 0;
				};
				step3Tasks.add(step1);
			}
			executorService.invokeAll(step3Tasks);
			
			List<Callable<?>> step4Tasks = new ArrayList<>();
			for (int i = 0; i < this.numberOfInstruments; i++) {
				final int index = i;
				Callable<?> step1 = () -> {
					computeStandardDeviation(index);
					return 0;
				};
				step4Tasks.add(step1);
			}
			executorService.invokeAll(step4Tasks);

			executorService.shutdown();

			for (int i = 0; i < subrange; i++) {
				weightedAverageReturns += weightedAverageReturnsBuffer[i];
			}

			for (int i = 0; i < subrange; i++) {
				portfolioStandardDeviation += portfolioStandardDeviationBuffer[i];
			}
			portfolioStandardDeviation = (float) Math.sqrt((double) portfolioStandardDeviation);

			System.out.println(String.format("Weighted Average Return (%%) = %.5f", weightedAverageReturns * 100));
			System.out.println(
					String.format("Portfolio Standard Deviation (%%) = %.5f", portfolioStandardDeviation * 100));

			for (int i = 0; i < zstats.length; i++) {
				VaRPercent[i] = -(weightedAverageReturns + zstats[i] * portfolioStandardDeviation);
				VaRValue[i] = portfolioValue * VaRPercent[i];
				System.out.println(
						String.format("VCV VaR (%%) = %.2f     VCV VaR ($) = %.2f", VaRPercent[i] * 100, VaRValue[i]));
			}

			// Optional

//			System.out.println("Variance-Covariance Matrix");
//			for (int l = 0; l < 5; l++) {
//				for (int c = 0; c < TestGPU.INSTRUMENT_NUMBERS; c++) {
//					System.out.print(String.format("%f ", varianceCovarienceMatrix[l * this.numberOfInstruments + c]));
//				}
//				System.out.println();
//			}
//
//			System.out.println("Weighted Covariance");
//			for (int l = 0; l < TestGPU.INSTRUMENT_NUMBERS; l++) {
//				System.out.print(String.format("%f ", weightedCovariance[l]));
//			}

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Compute the weighted average return from the instruments statistics
	 */
	private void computeWeightedAverageReturns(int index) {
		if (index % BLOCK_SIZE != 0) {
			return;
		}
		int range = index == 0 ? (this.numberOfInstruments < BLOCK_SIZE ? this.numberOfInstruments : BLOCK_SIZE)
				: this.numberOfInstruments - ((BLOCK_SIZE / index) * BLOCK_SIZE);
		int start = BLOCK_SIZE * (index / BLOCK_SIZE);
		float weightedReturns = 0;
		for (int i = start; i < start + range; i++) {
			weightedReturns += instrumentsStatistics[i * 2 + 1] * instrumentsWeight[i];
		}
		weightedAverageReturnsBuffer[index / BLOCK_SIZE] = weightedReturns;
	}

	/**
	 * Compute the weighted covariance matrix from the variance covariance matrix
	 */
	private void computeWeightedCovarianceMatrix(int instrumentId) {
		float weightedCovarianceOfInst = 0;
		for (int i = 0; i < this.numberOfInstruments; i++) {
			weightedCovarianceOfInst += varianceCovarienceMatrix[instrumentId * this.numberOfInstruments + i]
					* instrumentsWeight[i];
		}
		weightedCovariance[instrumentId] = weightedCovarianceOfInst;
	}

	private void computeStandardDeviation(int index) {
		if (index % BLOCK_SIZE != 0) {
			return;
		}
		int range = index == 0 ? (this.numberOfInstruments < BLOCK_SIZE ? this.numberOfInstruments : BLOCK_SIZE)
				: this.numberOfInstruments - ((BLOCK_SIZE / index) * BLOCK_SIZE);
		int start = BLOCK_SIZE * (index / BLOCK_SIZE);
		float portfolioDeviation = 0;
		for (int i = start; i < start + range; i++) {
			portfolioDeviation += (weightedCovariance[i] * instrumentsWeight[i]);
		}
		portfolioStandardDeviationBuffer[index / BLOCK_SIZE] = portfolioDeviation;
	}

	/**
	 * Calculate the excess returns based on the instrument history
	 * 
	 * @param instrumentId
	 */
	private void calculateExcessReturns(int instrumentId) {
		float averageReturn = 0;
		float average = instrumentsValueHistory[instrumentId * numberOfObservations + 0];
		for (int j = 1; j < numberOfObservations; j++) {
			float returnValue = ((instrumentsValueHistory[instrumentId * numberOfObservations + j]
					/ instrumentsValueHistory[instrumentId * numberOfObservations + j - 1]) - 1);
			instrumentsExcessReturns[instrumentId * (numberOfObservations - 1) + j - 1] = returnValue;
			averageReturn += returnValue;
			average += instrumentsValueHistory[instrumentId * numberOfObservations + j];
		}

		instrumentsStatistics[instrumentId * 2 + 0] = average / (numberOfObservations);
		instrumentsStatistics[instrumentId * 2 + 1] = averageReturn / (numberOfObservations - 1);

		for (int j = 0; j < numberOfObservations - 1; j++) {
			instrumentsExcessReturns[instrumentId * (numberOfObservations - 1)
					+ j] = instrumentsExcessReturns[instrumentId * (numberOfObservations - 1) + j]
							- instrumentsStatistics[instrumentId * 2 + 1];
		}
	}

	/**
	 * Calculate a covariance matrix, based on the instruments excess returns
	 * 
	 * @param instrumentId
	 */
	private void buildVarianceCovarianceMatrix(int instrumentId) {
		for (int c = 0; c < numberOfInstruments; c++) {
			float covariance = 0;
			for (int k = 0; k < numberOfObservations - 1; k++) {
				covariance += instrumentsExcessReturns[instrumentId * (numberOfObservations-1) + k]
						* instrumentsExcessReturns[c * (numberOfObservations-1) + k];
			}
			varianceCovarienceMatrix[instrumentId * numberOfInstruments + c] = covariance / (numberOfObservations - 1);
		}

	}

	public float getWeightedAverageReturns() {
		return weightedAverageReturns;
	}

	public float[] getWeightedCovariance() {
		return weightedCovariance;
	}
}
