package gpu.var;

public interface ValueAtRisk {

	public static enum PERCENTILES {
		_90P(90, -1.28155f), _95P(95, -1.64485f), _97_5P(97.5, -1.95996f), _99P(99, -2.32635f);

		double percentage;
		double zstat;

		private PERCENTILES(double percentage, double zstat) {
			this.percentage = percentage;
			this.zstat = zstat;
		}

	}

	double getPortfolioStandardDeviation();

	double[] getVaRPercent();

	double[] getVaRValue();

	double getPortfolioValue();

	double getVarianceCovarianceMatrix(int x, int y);

	void execute();

	double getWeightedAverageReturns();

	double[] getWeightedCovariance();

}