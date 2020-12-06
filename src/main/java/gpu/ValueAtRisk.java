package gpu;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class ValueAtRisk extends Kernel {

	private float[][] instrumentsValueHistory;
	private float[][] instrumentsExcessReturns;
	private float[][] varianceCovarienceMatrix;
	private float[][] instrumentsStatistics;

	private int instrumentNumbers;
	private int observationsHistory;
	
	private Range range;
	private boolean cpu;

	public ValueAtRisk(float[][] instrumentsValueHistory, int numberOfInstruments, int numberOfObservations, boolean cpu) {
		this.instrumentNumbers = numberOfInstruments;
		this.observationsHistory = numberOfObservations;
		this.instrumentsValueHistory = instrumentsValueHistory;
		this.instrumentsExcessReturns = new float[numberOfInstruments][numberOfObservations];
		this.instrumentsStatistics = new float[numberOfInstruments][2];
		this.varianceCovarienceMatrix = new float[numberOfInstruments][numberOfInstruments];
		this.range = Range.create(this.instrumentNumbers);
		this.cpu = cpu;
		
	}
	
	public ValueAtRisk(float[][] instrumentsValueHistory, int numberOfInstruments, int numberOfObservations) {
		this(instrumentsValueHistory, numberOfInstruments, numberOfObservations, true);
	}

	public float[][] getVarianceCovarianceMatrix() {
		return varianceCovarienceMatrix;
	}

	public void execute() {
		this.setExecutionMode(cpu ? EXECUTION_MODE.JTP : EXECUTION_MODE.GPU);
		this.setExplicit(true);
		this.put(varianceCovarienceMatrix);
		this.put(instrumentsExcessReturns);
		this.put(instrumentsValueHistory);
		this.put(instrumentsStatistics);
		this.execute(range);
		
		this.get(varianceCovarienceMatrix);
		System.out.println("Execution mode = "+this.getExecutionMode());
		System.out.println("Execution time = "+this.getAccumulatedExecutionTime());
		System.out.println("Conversion time = "+this.getConversionTime());
	}

	@Override
	public void run() {
		int instrumentId = getGlobalId();
		calculateReturns(instrumentId);
		calculateExcessReturns(instrumentId);
		localBarrier();
		buildVarianceCovarianceMatrix(instrumentId);
	}

	private void calculateReturns(int instrumentId) {
		float averageReturn = 0;
		float average = instrumentsValueHistory[instrumentId][0];
		for (int j = 1; j < observationsHistory; j++) {
			float returnValue = ((instrumentsValueHistory[instrumentId][j]
					/ instrumentsValueHistory[instrumentId][j - 1]) - 1) * 100;
			instrumentsExcessReturns[instrumentId][j - 1] = returnValue;
			averageReturn += returnValue;
			average += instrumentsValueHistory[instrumentId][j];
		}

		instrumentsStatistics[instrumentId][0] = average / (observationsHistory);
		instrumentsStatistics[instrumentId][1] = averageReturn / (observationsHistory - 1);
	}

	private void calculateExcessReturns(int instrumentId) {
		for (int j = 0; j < observationsHistory - 1; j++) {
			instrumentsExcessReturns[instrumentId][j] = instrumentsExcessReturns[instrumentId][j]
					- instrumentsStatistics[instrumentId][1];
		}
	}

	private void buildVarianceCovarianceMatrix(int instrumentId) {
		for (int c = 0; c < instrumentNumbers; c++) {
			float covariance = 0;
			for (int k = 0; k < observationsHistory - 1; k++) {
				covariance += instrumentsExcessReturns[instrumentId][k]
						* instrumentsExcessReturns[c][k];
			}
			varianceCovarienceMatrix[instrumentId][c] = covariance / (observationsHistory - 1);
		}
	}

}
