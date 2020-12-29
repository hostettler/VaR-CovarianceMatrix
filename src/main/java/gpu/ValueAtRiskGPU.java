package gpu;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class ValueAtRiskGPU extends Kernel {

	// input
	private final float[] instrumentsValueHistory;
	// input
	private final float[] instrumentsWeight;

	private final float[] varianceCovarienceMatrix;
	private final float[] instrumentsExcessReturns;
	// [0] Average price, [1] Average return
	private final float[] instrumentsStatistics;
	private final float[] weightedCovariance;
	

	private float[] weightedAverageReturnsBuffer;
	private float[] portfolioStandardDeviationBuffer;

	private int numberOfInstruments;
	private int numberOfObservations;

	@Constant
	private final float[] zstats = new float[] { /* 90% */-1.28155f, /* 95% */-1.64485f, /* 97.5% */-1.95996f,
			/* 99% */-2.32635f };

	private float weightedAverageReturns = 0;
	private float portfolioStandardDeviation = 0;

	private float[] VaRPercent = new float[zstats.length];
	private float[] VaRValue = new float[zstats.length];

	private float portfolioValue;
	private Range range;
	public ValueAtRiskGPU(float[] instrumentsValueHistory, float[] instrumentsWeight, float portfolioValue,
			int numberOfInstruments, int numberOfObservations, boolean cpu) {
		this.numberOfInstruments = numberOfInstruments;
		this.instrumentsWeight = instrumentsWeight;
		this.numberOfObservations = numberOfObservations;
		this.instrumentsValueHistory = instrumentsValueHistory;
		this.instrumentsExcessReturns = new float[numberOfInstruments * numberOfObservations];
		this.varianceCovarienceMatrix = new float[numberOfInstruments * numberOfInstruments];
		this.instrumentsStatistics = new float[numberOfInstruments * 2];
		this.weightedCovariance = new float[numberOfInstruments];
		this.portfolioValue = portfolioValue;
		this.range = Range.create(this.numberOfInstruments);
		this.weightedAverageReturnsBuffer = new float[this.range.getNumGroups(0)];
		this.portfolioStandardDeviationBuffer = new float[this.range.getNumGroups(0)];

	}

	public ValueAtRiskGPU(float[] instrumentsValueHistory, float[] instrumentsWeight, float portfolioValue,
			int numberOfInstruments, int numberOfObservations) {
		this(instrumentsValueHistory, instrumentsWeight, portfolioValue, numberOfInstruments, numberOfObservations,
				true);
	}

	public float[] getVarianceCovarianceMatrix() {
		return varianceCovarienceMatrix;
	}

	public void execute() {
		this.setExecutionMode(EXECUTION_MODE.GPU);

		this.setExplicit(true);

		this.put(instrumentsValueHistory);
		execute(range);

		this.get(portfolioStandardDeviationBuffer);
		this.get(weightedAverageReturnsBuffer);

		for (int i = 0; i <this.range.getNumGroups(0); i++) {
			weightedAverageReturns += weightedAverageReturnsBuffer[i];
		}

		for (int i = 0; i < this.range.getNumGroups(0); i++) {
			portfolioStandardDeviation += portfolioStandardDeviationBuffer[i];
		}
		portfolioStandardDeviation = (float) Math.sqrt((double) portfolioStandardDeviation);

		System.out.println(String.format("Weighted Average Return (%%) = %.5f", weightedAverageReturns * 100));
		System.out.println(String.format("Portfolio Standard Deviation (%%) = %.5f", portfolioStandardDeviation * 100));

		for (int i = 0; i < zstats.length; i++) {
			VaRPercent[i] = -(weightedAverageReturns + zstats[i] * portfolioStandardDeviation);
			VaRValue[i] = portfolioValue * VaRPercent[i];
			System.out.println(
					String.format("VCV VaR (%%) = %.2f     VCV VaR ($) = %.2f", VaRPercent[i] * 100, VaRValue[i]));
		}

		// Optional
//		this.get(varianceCovarienceMatrix);
//		this.get(weightedCovariance);
//
//		System.out.println("Variance-Covariance Matrix");
//		for (int l = 0; l < 5; l++) {
//			for (int c = 0; c < TestGPU.INSTRUMENT_NUMBERS; c++) {
//				System.out.print(String.format("%f ", varianceCovarienceMatrix[l * this.numberOfInstruments + c]));
//			}
//			System.out.println();
//		}
//
//		System.out.println("Weighted Covariance");
//		for (int l = 0; l < TestGPU.INSTRUMENT_NUMBERS; l++) {
//			System.out.print(String.format("%f ", weightedCovariance[l]));
//		}

		System.out.println("Execution mode = " + this.getExecutionMode());
		System.out.println("Execution time = " + this.getAccumulatedExecutionTime());
		System.out.println("Conversion time = " + this.getConversionTime());
	}

	@Override
	public void run() {
		
		int instrumentId = getGlobalId();
		calculateExcessReturns(instrumentId);
		localBarrier();
		
		computeWeightedAverageReturns();		
		buildVarianceCovarianceMatrix(instrumentId);		
		localBarrier();
		
		computeWeightedCovarianceMatrix();
		localBarrier();
		computeStandardDeviation();
		localBarrier();
	}

	/**
	 * Compute the weighted average return from the instruments statistics
	 */
	private void computeWeightedAverageReturns() {
		if (getLocalId() != 0) {
			return;
		}
		int range = getLocalSize();
		int start = getGroupId() * getLocalSize();
		float weightedReturns = 0;
		for (int i = start; i < start + range; i++) {
			weightedReturns += instrumentsStatistics[i * 2 + 1] * instrumentsWeight[i];
		}
		weightedAverageReturnsBuffer[getGroupId()] = weightedReturns;
	}

	/**
	 * Compute the weighted covariance matrix from the variance covariance matrix
	 */
	private void computeWeightedCovarianceMatrix() {
		int instrumentId = getGlobalId();
		float weightedCovarianceOfInst = 0;
		for (int i = 0; i < this.numberOfInstruments; i++) {
			weightedCovarianceOfInst += varianceCovarienceMatrix[instrumentId * this.numberOfInstruments + i]
					* instrumentsWeight[i];
		}
		weightedCovariance[instrumentId] = weightedCovarianceOfInst;
	}

	private void computeStandardDeviation() {
		if (getLocalId() != 0) {
			return;
		}
		int range = getLocalSize();
		int start = getGroupId() * getLocalSize();
		float portfolioDeviation = 0;
		for (int i = start; i < start + range; i++) {
			portfolioDeviation += (weightedCovariance[i]  *  instrumentsWeight[i]);
		}
		portfolioStandardDeviationBuffer[getGroupId()] = portfolioDeviation;
	}

	/**
	 * Calculate the excess returns based on the instrument history
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
