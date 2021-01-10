package gpu.var.performance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gpu.var.ValueAtRisk;
import gpu.var.ValueAtRiskData;

public abstract class AbstractValueAtRiskTest {

	static final int INSTRUMENT_NUMBERS = 4096;
	static final int OBSERVATIONS_HISTORY = 512;
	protected static final double instrumentsValueHistory[] = new double[INSTRUMENT_NUMBERS * OBSERVATIONS_HISTORY];
	protected static final double instrumentWeight[] = new double[INSTRUMENT_NUMBERS];

	protected ValueAtRisk var;

	@BeforeAll
	public void setup() {
		ValueAtRiskData.initTest(instrumentsValueHistory, instrumentWeight, getNumberOfInstruments(),
				getNumberOfObservations());
	}

	public AbstractValueAtRiskTest() {
		super();
	}

	@Test
	public void testComputVar() {
		this.var = getValueAtRiskCalculatorInstance();
		Long time = System.currentTimeMillis();
		this.var.execute();
		System.out.println(String.format("\nTime: %d ms", System.currentTimeMillis() - time));

		assertResults();

	}

	protected abstract ValueAtRisk getValueAtRiskCalculatorInstance();

	protected abstract int getNumberOfInstruments();

	protected abstract int getNumberOfObservations();

	protected abstract void assertResults();
}