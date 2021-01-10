package gpu.var;

import com.aparapi.Kernel;
import com.aparapi.Range;

class ValueAtRiskKernel extends Kernel {

	private double[] innerInstrumentsValueHistory;

	private double[] instrumentsExcessReturns;

	private int numberOfInstruments;
	private int numberOfObservations;

	private double[] instrumentsWeight;
	private double[] varianceCovarienceMatrix;
	private double[] weightedCovariance;
	private double[] weightedAverageReturns;

	private double[] results;

	private double avgBuffer[];
	private double avgWeightedAverage[];
	private double avgVariance[];

	private double portfolioStandardDeviation = 0;
	private double weightedAverageReturn = 0;
	private int tileSize = 2;
	private final int MAX_TILE_SIZE = 8;
	private int numTiles;
	int step = 0;
	boolean debug;

	public ValueAtRiskKernel(double[] instrumentsValueHistory, int numberOfInstruments, int numberOfObservations,
			double[] instrumentsWeight, double[] varianceCovarienceMatrix, double[] weightedCovariance, boolean debug) {
		this.innerInstrumentsValueHistory = instrumentsValueHistory;
		this.numberOfInstruments = numberOfInstruments;
		this.numberOfObservations = numberOfObservations;
		this.instrumentsWeight = instrumentsWeight;
		this.varianceCovarienceMatrix = varianceCovarienceMatrix;

		this.instrumentsExcessReturns = new double[numberOfInstruments * (numberOfObservations)];
		this.weightedCovariance = new double[numberOfInstruments];
		this.weightedAverageReturns = new double[numberOfInstruments];

		while (this.numberOfInstruments % (tileSize * 2) == 0 && this.numberOfObservations % (tileSize * 2) == 0
				&& (tileSize * 2) <= MAX_TILE_SIZE
				&& (tileSize * 2 * tileSize * 2) <= this.getTargetDevice().getMaxWorkGroupSize()) {
			tileSize *= 2;
		}

		tileSize = 4;
		this.numTiles = numberOfObservations / tileSize;

		avgBuffer = new double[this.numberOfInstruments * ((this.numberOfObservations / tileSize) + 1)];

		avgWeightedAverage = new double[this.numberOfInstruments / tileSize + 1];
		avgVariance = new double[this.numberOfInstruments / tileSize + 1];

		results = new double[2];
		this.debug = debug;
	}

	@Local
	double[] subA = new double[MAX_TILE_SIZE * MAX_TILE_SIZE];
	@Local
	double[] subB = new double[MAX_TILE_SIZE * MAX_TILE_SIZE];

	public void execute() {
		this.setExplicit(true);

		this.put(this.innerInstrumentsValueHistory);
		this.put(this.instrumentsWeight);
		this.put(weightedAverageReturns);

		System.out.println(String.format("Starting the GPU kernel with tile size = %d", tileSize));
		executeStep(0, "compute returns",
				Range.create2D(numberOfInstruments, numberOfObservations, tileSize, tileSize));
		if (debug) {
			debugReturns();
		}
		executeStep(1, "compute average", Range.create2D(numberOfInstruments, numberOfObservations / tileSize));
		if (debug) {
			debugWeightedAverageReturns();
		}
		executeStep(2, "compute excess returns", Range.create2D(numberOfInstruments, numberOfObservations - 1, 1, 1));
		if (debug) {
			debugExcessReturns();
		}
		executeStep(3, "compute covariance matrix",
				Range.create2D(numberOfInstruments, numberOfInstruments, tileSize, tileSize));
		if (debug) {
			debugCovarianceMatrix();
		}
		executeStep(4, "compute Weighted Average Returns and Weighted Covariance", Range.create(numberOfInstruments));
		if (debug) {
			debugWeightedAverageAndWeightedCovariance();
		}
		executeStep(5, "compute Variance and Average Return", Range.create(numberOfInstruments / tileSize));

		this.get(results);
		this.portfolioStandardDeviation = sqrt(results[1]);
		this.weightedAverageReturn = results[0];

	}

	private void executeStep(int step, String description, Range range) {
		this.step = step;
		long time = System.currentTimeMillis();
		execute(range);
		time = System.currentTimeMillis() - time;
		System.out.println(String.format("Step %d (%s) took %,d ms", step, description, time));
	}

	protected void debugWeightedAverageAndWeightedCovariance() {
		this.get(weightedCovariance);
		this.get(weightedAverageReturns);
		for (int x = 0; x < this.numberOfInstruments; x++) {
			System.out.println(String.format("Weighted Avg Return=%+.12f Weighted Covariance=%+.12f",
					weightedAverageReturns[x], weightedCovariance[x]));
		}
		System.out.println();
	}

	protected void debugCovarianceMatrix() {
		this.get(varianceCovarienceMatrix);
		for (int y = 0; y < this.numberOfInstruments; y++) {
			for (int x = 0; x < this.numberOfInstruments; x++) {
				System.out.print(
						String.format("%+.12f     ", varianceCovarienceMatrix[x + y * (this.numberOfInstruments)]));
			}
			System.out.println();
		}
	}

	protected void debugExcessReturns() {
		this.get(instrumentsExcessReturns);
		for (int y = 0; y < this.numberOfObservations - 1; y++) {
			for (int x = 0; x < this.numberOfInstruments; x++) {
				System.out.print(
						String.format("%+.12f     ", instrumentsExcessReturns[x + y * (this.numberOfInstruments)]));
			}
			System.out.println();
		}
		System.out.println();
	}

	protected void debugWeightedAverageReturns() {
		this.get(weightedAverageReturns);
		for (int x = 0; x < this.numberOfInstruments; x++) {
			System.out.print(String.format("%+.12f     ", weightedAverageReturns[x]));
		}
		System.out.println();
	}

	protected void debugReturns() {
		this.get(instrumentsExcessReturns);
		for (int y = 0; y < this.numberOfObservations - 1; y++) {
			for (int x = 0; x < this.numberOfInstruments; x++) {
				System.out.print(
						String.format("%+.12f     ", instrumentsExcessReturns[x + y * (this.numberOfInstruments)]));
			}
			System.out.println();
		}
		System.out.println();
	}

	@Override
	public void run() {

		if (step == 0) {
			computeReturnMatrix();
		} else if (step == 1) {
			computeAvgReturn();
		} else if (step == 2) {
			computeExcessReturn();
		} else if (step == 3) {
			computeCovarianceMatrix();
		} else if (step == 4) {
			computeWeightedCovarianceAndAverageReturn();
		} else if (step == 5) {
			computeWeightedAverageReturnAndVariance();
		}

	}

	protected void computeWeightedCovarianceAndAverageReturn() {

		int y = getGlobalId();

		double weightedCovarianceValue = 0;
		for (int x = 0; x < numberOfInstruments; x++) {
			weightedCovarianceValue +=  this.varianceCovarienceMatrix[x + y * numberOfInstruments];
		}

		weightedAverageReturns[y] = this.weightedAverageReturns[y] * instrumentsWeight[y];
		this.weightedCovariance[y] = weightedCovarianceValue * instrumentsWeight[y] * instrumentsWeight[y];
		localBarrier();
	}

	protected void computeCovarianceMatrix() {
		int col = getLocalId(0);
		int row = getLocalId(1);

		int globalCol = tileSize * getGroupId(0) + col;
		int globalRow = tileSize * getGroupId(1) + row;
//		// Because we are tiling the matrix we might overflow because of the tiling size
		if (globalRow > numberOfInstruments - 1 || globalCol > numberOfInstruments - 1) {
			return;
		}

		double value = 0;

		for (int tile = 0; tile < numTiles; tile++) {
			int tileOffset = tileSize * tile;
			int columnOffsetFromTile = tileOffset + col;
			int rowOffsetFromTile = tileOffset + row;

			int subMatrixPos = col + row * tileSize;
			// transpose of excess return
			subA[subMatrixPos] = instrumentsExcessReturns[(columnOffsetFromTile * numberOfInstruments) + globalRow];
			subB[subMatrixPos] = instrumentsExcessReturns[globalCol + (rowOffsetFromTile * numberOfInstruments)];

			localBarrier();
			// Iterate for every column of matrix B
			for (int tileNr = 0; tileNr < tileSize; tileNr++) {
				double a = subA[row * tileSize + tileNr];
				double b = subB[tileNr * tileSize + col];
				value += a * b;
			}
			localBarrier();
		}

		this.varianceCovarienceMatrix[globalRow * this.numberOfInstruments + globalCol] = value
				/ (numberOfObservations - 1);
	}

	protected void computeExcessReturn() {
		int globalCol = getGlobalId(0);
		int globalRow = getGlobalId(1);

		// Because we are tiling the matrix we might overflow because of the tiling size
		if (globalRow > numberOfObservations || globalCol > numberOfInstruments) {
			return;
		}

		this.instrumentsExcessReturns[globalCol + globalRow * numberOfInstruments] -= weightedAverageReturns[globalCol];
		localBarrier();
	}

	protected void computeAvgReturn() {
		int returns = getGlobalId(1);
		if (returns >= numberOfObservations - 1) {
			return;
		}

		int instrumentId = getGlobalId(0);

		double returnsValue = 0;
		for (int i = 0; i < tileSize && (returns * tileSize + i) < (this.numberOfObservations - 1); i++) {
			double val = this.instrumentsExcessReturns[instrumentId + (returns * tileSize + i) * numberOfInstruments];
			returnsValue += val;
		}
		int o = instrumentId + (returns) * this.numberOfInstruments;

		avgBuffer[o] += returnsValue;
		localBarrier();

		if (returns == getGlobalSize(1) - 1) {
			double avg = 0;
			for (int i = 0; i < getGlobalSize(1); i++) {
				avg += avgBuffer[instrumentId + i * this.numberOfInstruments];
			}
			weightedAverageReturns[instrumentId] = avg / (this.numberOfObservations - 1);
		}
		localBarrier();
	}

	protected void computeWeightedAverageReturnAndVariance() {
		int instrumentId = getGlobalId(0);

		double sumVariance = 0;
		double sumAverage = 0;
		for (int i = 0; i < tileSize; i++) {
			sumVariance += this.weightedCovariance[i + instrumentId * tileSize];
			sumAverage += this.weightedAverageReturns[i + instrumentId * tileSize];

		}

		avgVariance[instrumentId] += sumVariance;
		avgWeightedAverage[instrumentId] += sumAverage;

		localBarrier();

		if (instrumentId == getGlobalSize(0) - 1) {
			sumAverage = 0;
			sumVariance = 0;
			for (int i = 0; i < getGlobalSize(0); i++) {
				sumAverage += avgWeightedAverage[i];
				sumVariance += avgVariance[i];

			}
			results[0] = sumAverage;
			results[1] = sumVariance;
		}
		localBarrier();
	}

	protected void computeReturnMatrix() {
		int col = getLocalId(0);
		int row = getLocalId(1);

		int globalCol = tileSize * getGroupId(0) + col;
		int globalRow = tileSize * getGroupId(1) + row;

		// Because we are tiling the matrix we might overflow because of the tiling size
		if (globalRow == 0 || globalRow > numberOfObservations || globalCol > numberOfInstruments) {
			return;
		}

		double excessReturn = (this.innerInstrumentsValueHistory[globalCol + (globalRow) * numberOfInstruments])
				/ (this.innerInstrumentsValueHistory[globalCol + (globalRow - 1) * numberOfInstruments]) - 1;
		this.instrumentsExcessReturns[globalCol + (globalRow - 1) * numberOfInstruments] = excessReturn;
		localBarrier();
	}

	public double getPortfolioStandardDeviation() {
		return portfolioStandardDeviation;
	}

	public double getWeightedAverageReturns() {
		return weightedAverageReturn;
	}
}