package gpu.var.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import gpu.var.ValueAtRisk;
import gpu.var.ValueAtRiskJava;
import gpu.var.functional.AbstractValueAtRiskTest;

@TestInstance(Lifecycle.PER_CLASS)
public class MassiveValueAtRiskJavaTest extends AbstractValueAtRiskTest {

	protected ValueAtRisk getValueAtRiskCalculatorInstance() {
		System.out.println("\n**********************************************************");
		System.out.println(
				String.format("Compute Value at Risk on a %d instruments with %d observations using plain Java",
						getNumberOfInstruments(), getNumberOfObservations()));

		return new ValueAtRiskJava(instrumentsValueHistory, instrumentWeight, 1_000_000f, getNumberOfInstruments(),
				getNumberOfObservations(), false);
	}

	@Override
	protected int getNumberOfInstruments() {
		return 4096;
	}

	@Override
	protected int getNumberOfObservations() {
		return 512;
	}

	@Override
	protected void assertResults() {
		assertEquals(1_000_000f, var.getPortfolioValue());
		assertEquals(0.0116, var.getPortfolioStandardDeviation(), 0.0001);
		assertEquals(-0.0001624, var.getWeightedAverageReturns(), 0.000001);

		assertEquals(0.0150, var.getVaRPercent()[ValueAtRisk.PERCENTILES._90P.ordinal()], 0.0001);
		assertEquals(0.0193, var.getVaRPercent()[ValueAtRisk.PERCENTILES._95P.ordinal()], 0.0001);
		assertEquals(0.0229, var.getVaRPercent()[ValueAtRisk.PERCENTILES._97_5P.ordinal()], 0.0001);
		assertEquals(0.0272, var.getVaRPercent()[ValueAtRisk.PERCENTILES._99P.ordinal()], 0.0001);
	}
}
