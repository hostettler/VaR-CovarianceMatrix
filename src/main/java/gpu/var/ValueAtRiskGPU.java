package gpu.var;

public class ValueAtRiskGPU extends AbstractValueAtRisk implements ValueAtRisk {

	public ValueAtRiskGPU(double[] instrumentsValueHistory, double[] instrumentsWeight, double portfolioValue,
			int numberOfInstruments, int numberOfObservations, boolean debug) {
		super(instrumentsValueHistory, instrumentsWeight, portfolioValue, 
				numberOfInstruments, numberOfObservations, debug);

	}


	@Override
	public void execute() {
		ValueAtRiskKernel kernel = new ValueAtRiskKernel(instrumentsValueHistory, numberOfInstruments, numberOfObservations, instrumentsWeight,
				varianceCovarienceMatrix, weightedCovariance, debug);
		kernel.execute();
		this.portfolioStandardDeviation = kernel.getPortfolioStandardDeviation();
		this.weightedAverageReturns = kernel.getWeightedAverageReturns();
		computeVar();
		printSummary();
	}
}
