package gpu.var;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ValueAtRiskJava extends AbstractValueAtRisk implements ValueAtRisk {

	static final int BLOCK_SIZE = 250;

	private final double[] instrumentsExcessReturns;
	// [0] Average price, [1] Average return
	private final double[] instrumentsStatistics;
	private double[] weightedAverageReturnsBuffer;
	private double[] portfolioStandardDeviationBuffer;
	private int subrange;

	public ValueAtRiskJava(double[] instrumentsValueHistory, double[] instrumentsWeight, double portfolioValue,
			int numberOfInstruments, int numberOfObservations, boolean debug) {
		super(instrumentsValueHistory, instrumentsWeight, portfolioValue, numberOfInstruments, numberOfObservations,
				debug);
		this.instrumentsExcessReturns = new double[numberOfInstruments * numberOfObservations];
		this.instrumentsStatistics = new double[numberOfInstruments * 2];
		this.subrange = (this.numberOfInstruments / BLOCK_SIZE) + 1;
		this.weightedAverageReturnsBuffer = new double[subrange];
		this.portfolioStandardDeviationBuffer = new double[subrange];
	}

	@Override
	public void execute() {

		try {

			ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
					Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
						@Override
						public Thread newThread(Runnable r) {
							final Thread thread = new Thread(r);
							thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
								@Override
								public void uncaughtException(Thread t, Throwable e) {
									System.out.println("Uncaught Exception occurred on thread: " + t.getName());
									System.out.println("Exception message: " + e.getMessage());
								}
							});
							return thread;
						}
					});

			if (debug) {
				for (int y = 0; y < this.numberOfObservations; y++) {
					for (int x = 0; x < this.numberOfInstruments; x++) {
						System.out.print(String.format("%+.6f     ",
								instrumentsValueHistory[x + y * (this.numberOfInstruments)]));
					}
					System.out.println();
				}
				System.out.println();
			}
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

			if (debug) {
				for (int y = 0; y < this.numberOfObservations - 1; y++) {
					for (int x = 0; x < this.numberOfInstruments; x++) {
						System.out.print(
								String.format("%+.8f    ", instrumentsExcessReturns[y * numberOfInstruments + x]));
					}
					System.out.println();
				}
				System.out.println("********************************");
			}
			List<Callable<Integer>> step2Tasks = new ArrayList<>();
			for (int i = 0; i < this.numberOfInstruments; i++) {
				final int index = i;
				Callable<Integer> step1 = () -> {
					computeWeightedAverageReturns(index);
					buildVarianceCovarianceMatrix(index);
					return 0;
				};
				step2Tasks.add(step1);
			}
			executorService.invokeAll(step2Tasks);

			if (debug) {
				System.out.println("Covariance");
				for (int y = 0; y < this.numberOfInstruments; y++) {
					for (int x = 0; x < this.numberOfInstruments; x++) {
						System.out.print(String.format("%+.8f     ",
								varianceCovarienceMatrix[x + y * (this.numberOfInstruments)]));
					}
					System.out.println();
				}
			}
			List<Callable<Integer>> step3Tasks = new ArrayList<>();
			for (int i = 0; i < this.numberOfInstruments; i++) {
				final int index = i;
				Callable<Integer> step1 = () -> {
					computeWeightedCovarianceMatrix(index);
					return 0;
				};
				step3Tasks.add(step1);
			}
			executorService.invokeAll(step3Tasks);

			if (debug) {
				for (int y = 0; y < this.numberOfInstruments; y++) {
					System.out.println(String.format("%+.8f %+.8f %+.8f    ", instrumentsStatistics[y * 2 + 1],
							instrumentsWeight[y], weightedCovariance[y]));
				}
				System.out.println("********************************");
			}
			List<Callable<Integer>> step4Tasks = new ArrayList<>();
			for (int i = 0; i < this.numberOfInstruments; i++) {
				final int index = i;
				Callable<Integer> step1 = () -> {
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
			portfolioStandardDeviation = (double) Math.sqrt((double) portfolioStandardDeviation);

			computeVar();
			printSummary();

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
		double weightedReturns = 0;
		for (int i = start; i < start + range; i++) {
			weightedReturns += instrumentsStatistics[i * 2 + 1] * instrumentsWeight[i];
		}
		weightedAverageReturnsBuffer[index / BLOCK_SIZE] = weightedReturns;
	}

	/**
	 * Compute the weighted covariance matrix from the variance covariance matrix
	 */
	private void computeWeightedCovarianceMatrix(int instrumentId) {
		double weightedCovarianceOfInst = 0;
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
		double portfolioDeviation = 0;
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

		double averageReturn = 0;
		for (int j = 1; j < numberOfObservations; j++) {
			double returnValue = instrumentsValueHistory[instrumentId + numberOfInstruments * j]
					/ instrumentsValueHistory[instrumentId + numberOfInstruments * (j - 1)] - 1;
			instrumentsExcessReturns[instrumentId + (numberOfInstruments) * (j - 1)] = returnValue;

			averageReturn += returnValue;
		}
		instrumentsStatistics[instrumentId * 2 + 1] = averageReturn / (numberOfObservations - 1);

		for (int j = 0; j < numberOfObservations - 1; j++) {
			instrumentsExcessReturns[instrumentId
					+ j * (numberOfInstruments)] = instrumentsExcessReturns[instrumentId + numberOfInstruments * j]
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
			double covariance = 0;
			for (int k = 0; k < numberOfObservations - 1; k++) {
				double a = instrumentsExcessReturns[k * (numberOfInstruments) + c];
				double b = instrumentsExcessReturns[k * (numberOfInstruments) + instrumentId];
//				if (instrumentId == 0) {
//					System.out.println(String.format("%.5f * %.5f", a, b));
//				}
				covariance += a * b;
			}
			varianceCovarienceMatrix[instrumentId + numberOfInstruments * c] = covariance / (numberOfObservations - 1);
		}

	}
}
