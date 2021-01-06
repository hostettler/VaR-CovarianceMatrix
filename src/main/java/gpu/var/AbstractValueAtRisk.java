package gpu.var;

public abstract class AbstractValueAtRisk implements ValueAtRisk {
	
	protected boolean debug;
	protected int numberOfInstruments;
	protected int numberOfObservations;
	protected double[] varianceCovarienceMatrix;

	// [0] Average price, [1] Average return

	protected double[] weightedCovariance;
	protected double weightedAverageReturns = 0;
	protected double portfolioStandardDeviation = 0;

	protected double[] VaRPercent = new double[PERCENTILES.values().length];
	protected double[] VaRValue = new double[PERCENTILES.values().length];

	protected double portfolioValue;
	// input
	protected double[] instrumentsValueHistory;
	// input
	protected double[] instrumentsWeight;
	
	public AbstractValueAtRisk(double[] instrumentsValueHistory, double[] instrumentsWeight, double portfolioValue,
			int numberOfInstruments, int numberOfObservations, boolean debug) {
		this.numberOfInstruments = numberOfInstruments;
		this.instrumentsWeight = instrumentsWeight;
		this.numberOfObservations = numberOfObservations;
		this.instrumentsValueHistory = instrumentsValueHistory;

		this.varianceCovarienceMatrix = new double[numberOfInstruments * numberOfInstruments];
		this.weightedCovariance = new double[numberOfInstruments];
		this.portfolioValue = portfolioValue;
		this.debug = debug;
	}

	
	@Override
	public double getPortfolioStandardDeviation() {
		return portfolioStandardDeviation;
	}

	@Override
	public double[] getVaRPercent() {
		return VaRPercent;
	}

	@Override
	public double[] getVaRValue() {
		return VaRValue;
	}

	@Override
	public double getPortfolioValue() {
		return portfolioValue;
	}

	@Override
	public double getVarianceCovarianceMatrix(int x, int y) {
		return varianceCovarienceMatrix[x + y * this.numberOfInstruments];
	}

	@Override
	public double getWeightedAverageReturns() {
		return weightedAverageReturns;
	}

	@Override
	public double[] getWeightedCovariance() {
		return weightedCovariance;
	}

	protected void computeVar() {
		for (int i = 0; i < PERCENTILES.values().length; i++) {
			VaRPercent[i] = -(weightedAverageReturns + PERCENTILES.values()[i].zstat * portfolioStandardDeviation);
			VaRValue[i] = portfolioValue * VaRPercent[i];
		}
	}
	
	void printSummary() {
		System.out.println(String.format("Weighted Average Return (%%) = %.5f", weightedAverageReturns * 100));
		System.out.println(String.format("Portfolio Standard Deviation (%%) = %.5f", portfolioStandardDeviation * 100));

		for (int i = 0; i < PERCENTILES.values().length; i++) {
			System.out.println(String.format("VCV VaR (%.1f%%) = %.2f%%     VCV VaR (%.1f%%) = $%,.2f",
					PERCENTILES.values()[i].percentage, VaRPercent[i] * 100, PERCENTILES.values()[i].percentage,
					VaRValue[i]));
		}
	}
}