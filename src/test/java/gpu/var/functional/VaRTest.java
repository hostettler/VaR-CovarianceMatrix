package gpu.var.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import gpu.var.ValueAtRisk;
import gpu.var.ValueAtRiskData;
import gpu.var.ValueAtRiskGPU;
import gpu.var.ValueAtRiskJava;

class VaRTest {

	static final int INSTRUMENT_NUMBERS = 4096;
	static final int OBSERVATIONS_HISTORY = 512;

	protected static final double instrumentsValueHistory[] = new double[INSTRUMENT_NUMBERS * OBSERVATIONS_HISTORY];
	protected static final double instrumentWeight[] = new double[INSTRUMENT_NUMBERS];

	@Test
	void testCompare16x8() {
		int numberOfInstruments = 16;
		int numberOfObservations = 8;
		ValueAtRisk var = testVar(numberOfInstruments, numberOfObservations);
		assertEquals(0.0094845, var.getPortfolioStandardDeviation(), 0.0000001);
		assertEquals(0.0049068, var.getWeightedAverageReturns(), 0.00001);
	}

	@Test
	void testCompare16x512() {
		int numberOfInstruments = 16;
		int numberOfObservations = 512;
		ValueAtRisk var = testVar(numberOfInstruments, numberOfObservations);

		assertEquals(0.0113788, var.getPortfolioStandardDeviation(), 0.0000001);
		assertEquals(-0.0001798, var.getWeightedAverageReturns(), 0.00001);
	}
	
	@Test
	void testCompare512x512() {
		int numberOfInstruments = 512;
		int numberOfObservations = 512;
		ValueAtRisk var = testVar(numberOfInstruments, numberOfObservations);
		
		assertEquals(1_000_000f, var.getPortfolioValue());
		assertEquals(0.0116131, var.getPortfolioStandardDeviation(), 0.0000001);
		assertEquals(-0.0001628, var.getWeightedAverageReturns(), 0.00001);
	
	}

	protected ValueAtRisk testVar(int numberOfInstruments, int numberOfObservations) {
		ValueAtRiskData.initTest(instrumentsValueHistory, instrumentWeight, numberOfInstruments, numberOfObservations);
		ValueAtRisk varGPU = new ValueAtRiskGPU(instrumentsValueHistory, instrumentWeight, 1_000_000f,
				numberOfInstruments, numberOfObservations, false);
		
		ValueAtRisk varJava = new ValueAtRiskJava(instrumentsValueHistory, instrumentWeight, 1_000_000f,
				numberOfInstruments, numberOfObservations, false);

		varGPU.execute();
		varJava.execute();
		assertEquals(varGPU.getWeightedAverageReturns(), varJava.getWeightedAverageReturns(), 0.00001);
		assertEquals(varGPU.getPortfolioStandardDeviation(), varJava.getPortfolioStandardDeviation(), 0.00001);
		
		return varJava;
	}

}
