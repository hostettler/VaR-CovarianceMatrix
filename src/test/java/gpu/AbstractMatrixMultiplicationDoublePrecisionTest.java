package gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractMatrixMultiplicationDoublePrecisionTest {

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
		init(squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), 100);

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
	public void testMultiplicationCompatibilities() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			getInstance(squareMatrixA, getSquareMatrixSize(),
					getSquareMatrixSize(), false, matrixD, getMatrixHeight(), getMatrixWidth(), false);
		});
		assertEquals("Incompatible matrix width and height A.width=10 while B.height=4", exception.getMessage());
		
		getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), false, matrixD, getMatrixHeight(), getMatrixWidth(), true);
		
		getInstance(matrixC, getMatrixWidth(),
				getMatrixHeight(), false, matrixD, getMatrixHeight(), getMatrixWidth(), false);
	}

	@Test
	public void testMultiplicationAxB() {
		MatrixMultiplicationDoublePrecision mm;
		
		mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), false, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), false);
		mm.printMatrixA();
		mm.printMatrixB();
		
		mm.execute();
		
		mm.printMatrixR();
		assertEquals(8855, mm.getMatrixRXY(0, 0));
		assertEquals(9350, mm.getMatrixRXY(9, 0));
		assertEquals(140255, mm.getMatrixRXY(0, 9));
		assertEquals(148850, mm.getMatrixRXY(9, 9));
		assertEquals(23920, mm.getMatrixRXY(3, 1));
		assertEquals(97765, mm.getMatrixRXY(2, 6));
		assertEquals(70440, mm.getMatrixRXY(7, 4));
		assertEquals(145030, mm.getMatrixRXY(5, 9));	
		
	}
	
	

	@Test
	public void testMultiplicationTAxB() {
		MatrixMultiplicationDoublePrecision mm;
		
		mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), true, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), false);
		mm.execute();
		assertEquals(75410, mm.getMatrixRXY(0, 0));
		assertEquals(79550, mm.getMatrixRXY(9, 0));
		assertEquals(88550, mm.getMatrixRXY(0, 9));
		assertEquals(93500, mm.getMatrixRXY(9, 9));
		assertEquals(78280, mm.getMatrixRXY(3, 1));
		assertEquals(85210, mm.getMatrixRXY(2, 6));
		assertEquals(84750, mm.getMatrixRXY(7, 4));
		assertEquals(91300, mm.getMatrixRXY(5, 9));
	}

	@Test
	public void testMultiplicationAxTB() {
		MatrixMultiplicationDoublePrecision mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), false, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), true);
		mm.execute();
		
		assertEquals(5885, mm.getMatrixRXY(0, 0));
		assertEquals(10835, mm.getMatrixRXY(9, 0));
		assertEquals(100835, mm.getMatrixRXY(0, 9));
		assertEquals(186785, mm.getMatrixRXY(9, 9));
		assertEquals(21085, mm.getMatrixRXY(3, 1));
		assertEquals(82285, mm.getMatrixRXY(2, 6));
		assertEquals(79935, mm.getMatrixRXY(7, 4));
		assertEquals(148585, mm.getMatrixRXY(5, 9));
	}

	@Test
	public void testMultiplicationTAxTB() {
		MatrixMultiplicationDoublePrecision mm;
		
		mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), true, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), true);	
		mm.execute();
		assertEquals(49355, mm.getMatrixRXY(0, 0));
		assertEquals(90755, mm.getMatrixRXY(9, 0));
		assertEquals(58850, mm.getMatrixRXY(0, 9));
		assertEquals(108350, mm.getMatrixRXY(9, 9));
		assertEquals(64510, mm.getMatrixRXY(3, 1));
		assertEquals(66085, mm.getMatrixRXY(2, 6));
		assertEquals(88575, mm.getMatrixRXY(7, 4));
		assertEquals(86350, mm.getMatrixRXY(5, 9));	
	}

	@Test
	public void testMultiplicationCxD() {
		MatrixMultiplicationDoublePrecision mm;
		
		mm = getInstance(matrixC, getMatrixWidth(),
				getMatrixHeight(), false, matrixD, getMatrixHeight(), getMatrixWidth(), false);	
		mm.execute();
		assertEquals(210, mm.getMatrixRXY(0, 0));
		assertEquals(300, mm.getMatrixRXY(9, 0));
		assertEquals(2514, mm.getMatrixRXY(0, 9));
		assertEquals(3900, mm.getMatrixRXY(9, 9));
		assertEquals(544, mm.getMatrixRXY(3, 1));
		assertEquals(1958, mm.getMatrixRXY(2, 6));
		assertEquals(1752, mm.getMatrixRXY(7, 4));
		assertEquals(3284, mm.getMatrixRXY(5, 9));	
		

	}

	
	protected abstract MatrixMultiplicationDoublePrecision getInstance(double[] matrixA, int matrixAWidth, int matrixAHeight, boolean transposeMatrixA,
			double[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB);

	private static void init(double[] matrix, int width, int height) {
		init(matrix, width, height, 0);
	}
	private static void init(double[] matrix, int width, int height, int delta) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				matrix[y * width + x] = y * width + x + 1 + delta;
			}
		}
	}
}
