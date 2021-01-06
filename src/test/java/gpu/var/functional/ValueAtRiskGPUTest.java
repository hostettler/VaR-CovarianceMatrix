package gpu.var.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import gpu.var.ValueAtRisk;
import gpu.var.ValueAtRiskGPU;

@TestInstance(Lifecycle.PER_CLASS)
public class ValueAtRiskGPUTest extends AbstractValueAtRiskTest {
	protected ValueAtRisk getValueAtRiskCalculatorInstance() {
		System.out.println("\n**********************************************************");
		System.out.println(String.format("Compute Value at Risk on a %d instruments with %d observations using GPU",
				getNumberOfInstruments(), getNumberOfObservations()));

		return new ValueAtRiskGPU(instrumentsValueHistory, instrumentWeight, 1_000_000f, getNumberOfInstruments(),
				getNumberOfObservations(), true);
	}

	@Override
	protected int getNumberOfInstruments() {
		return 16;
	}

	@Override
	protected int getNumberOfObservations() {
		return 8;
	}

	@Override
	protected void assertResults() {
		assertEquals(1_000_000f, var.getPortfolioValue());
		assertEquals(0.00725828, var.getPortfolioStandardDeviation(), 0.0000001);
		assertEquals(0.00344621, var.getWeightedAverageReturns(), 0.00001);

		assertEquals(0.00585564, var.getVaRPercent()[ValueAtRisk.PERCENTILES._90P.ordinal()], 0.0001);
		assertEquals(0.00849258, var.getVaRPercent()[ValueAtRisk.PERCENTILES._95P.ordinal()], 0.0001);
		assertEquals(0.01077974, var.getVaRPercent()[ValueAtRisk.PERCENTILES._97_5P.ordinal()], 0.0001);
		assertEquals(0.01343910, var.getVaRPercent()[ValueAtRisk.PERCENTILES._99P.ordinal()], 0.0001);

		assertEquals(0.00012481, var.getVarianceCovarianceMatrix(0, 0), 0.000001);
		assertEquals(-0.0000042, var.getVarianceCovarianceMatrix(0, 1), 0.000001);
		assertEquals(0.00001132, var.getVarianceCovarianceMatrix(0, 2), 0.000001);
		assertEquals(0.00012471, var.getVarianceCovarianceMatrix(4, 4), 0.000001);					
	}
}
