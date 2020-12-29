package gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractMassiveMatrixMultiplicationDoublePrecisionTest {

	int squareMatrixSize;
	double[] squareMatrixA;
	double[] squareMatrixB;

	int matrixWidth;
	int matrixHeight;
	double[] matrixC;
	double[] matrixD;

	@BeforeAll
	public void setup() {
		setCardinalities();
		squareMatrixA = new double[getSquareMatrixSize() * getSquareMatrixSize()];
		squareMatrixB = new double[getSquareMatrixSize() * getSquareMatrixSize()];
		init(squareMatrixA, getSquareMatrixSize(), getSquareMatrixSize());
		init(squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize());

		matrixC = new double[getMatrixWidth() * getMatrixHeight()];
		matrixD = new double[getMatrixHeight() * getMatrixWidth()];
		init(matrixC, getMatrixWidth(), getMatrixHeight());
		init(matrixD, getMatrixHeight(), getMatrixWidth());

	}

	protected abstract void setCardinalities();

	private int getMatrixHeight() {
		return this.matrixHeight;
	}

	private int getMatrixWidth() {
		return this.matrixWidth;
	}

	protected int getSquareMatrixSize() {
		return this.squareMatrixSize;
	}

	@Test
	public void testMultiplicationAxB() {
		MatrixMultiplicationDoublePrecision mm;

		mm = getInstance(squareMatrixA, getSquareMatrixSize(), getSquareMatrixSize(), false, squareMatrixB,
				getSquareMatrixSize(), getSquareMatrixSize(), false);
		long time = System.currentTimeMillis();
		mm.execute();
		time = System.currentTimeMillis() - time;
		System.out.println(String.format("R=AxB R=%dx%d Time=%d ms", mm.getResultWidth(), mm.getResultHeight(), time));
//mm.printMatrixR();
		assertEquals(checkValue(0, 0, getSquareMatrixSize()), mm.getMatrixRXY(0, 0));
		assertEquals(checkValue(0, getSquareMatrixSize() - 1, getSquareMatrixSize()),
				mm.getMatrixRXY(0, getSquareMatrixSize() - 1));
		assertEquals(checkValue(getSquareMatrixSize() - 1, 0, getSquareMatrixSize()),
				mm.getMatrixRXY(getSquareMatrixSize() - 1, 0));
		assertEquals(checkValue(getSquareMatrixSize() - 1, getSquareMatrixSize() - 1, getSquareMatrixSize()),
				mm.getMatrixRXY(getSquareMatrixSize() - 1, getSquareMatrixSize() - 1));
	}

	@Test
	public void testMultiplicationTAxB() {
		MatrixMultiplicationDoublePrecision mm;

		mm = getInstance(squareMatrixA, getSquareMatrixSize(), getSquareMatrixSize(), true, squareMatrixB,
				getSquareMatrixSize(), getSquareMatrixSize(), false);

		long time = System.currentTimeMillis();
		mm.execute();
		time = System.currentTimeMillis() - time;

		System.out.println(String.format("R=T(A)xB %dx%d Time=%d ms", mm.getResultWidth(), mm.getResultHeight(), time));
	}

	@Test
	public void testMultiplicationAxTB() {
		MatrixMultiplicationDoublePrecision mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), false, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), true);

		long time = System.currentTimeMillis();
		mm.execute();
		time = System.currentTimeMillis() - time;

		System.out.println(String.format("R=AxT(B) %dx%d Time=%d ms", mm.getResultWidth(), mm.getResultHeight(), time));
	}

	@Test
	public void testMultiplicationTAxTB() {
		MatrixMultiplicationDoublePrecision mm;

		mm = getInstance(squareMatrixA, getSquareMatrixSize(), getSquareMatrixSize(), true, squareMatrixB,
				getSquareMatrixSize(), getSquareMatrixSize(), true);

		long time = System.currentTimeMillis();
		mm.execute();
		time = System.currentTimeMillis() - time;

		System.out.println(
				String.format("R=T(A)xT(B) %dx%d Time=%d ms", mm.getResultWidth(), mm.getResultHeight(), time));
	}

	@Test
	public void testMultiplicationCxD() {
		MatrixMultiplicationDoublePrecision mm;

		mm = getInstance(matrixC, getMatrixWidth(), getMatrixHeight(), false, matrixD, getMatrixHeight(),
				getMatrixWidth(), false);

		long time = System.currentTimeMillis();
		mm.execute();
		time = System.currentTimeMillis() - time;
		System.out.println(String.format("R=CxD R=%dx%d Time=%d ms", mm.getResultWidth(), mm.getResultHeight(), time));
	}

	protected abstract MatrixMultiplicationDoublePrecision getInstance(double[] matrixA, int matrixAWidth,
			int matrixAHeight, boolean transposeMatrixA, double[] matrixB, int matrixBWidth, int matrixBHeight,
			boolean transposeMatrixB);

	private static void init(double[] matrix, int width, int height) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				matrix[y * width + x] = y * width + x + 1;
			}
		}
	}

	private double checkValue(int x, int y, int matrixWidth) {
		return checkValue(x, y, matrixWidth, 0);
	}

	private double checkValue(int x, int y, int matrixWidth, int delta) {
		int col = x;
		int row = y;

		int width = matrixWidth;

		double sum = 0;
		for (int i = 0; i < width; i++) {

			double a = (width * row) + i + 1;
			double b = ((width * i) + 1 + col);
			sum += a * b;
		}
		return sum;
	}
}
