package gpu.mm.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import gpu.mm.MatrixMultiplication;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractMatrixMultiplicationTest {

	int squareMatrixSize;
	float[] squareMatrixA;
	float[] squareMatrixB;

	int matrixWidth;
	int matrixHeight;
	float[] matrixC;
	float[] matrixD;

	@BeforeAll
	public void setup() {
		setCardinalities();
		squareMatrixA = new float[getSquareMatrixSize() * getSquareMatrixSize()];
		squareMatrixB = new float[getSquareMatrixSize() * getSquareMatrixSize()];
		init(squareMatrixA, getSquareMatrixSize(), getSquareMatrixSize());
		init(squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize());

		matrixC = new float[getMatrixWidth() * getMatrixHeight()];
		matrixD = new float[getMatrixHeight() * getMatrixWidth()];
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
		MatrixMultiplication mm;
		
		mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), false, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), false);
		mm.execute();
		
		assertEquals(3355, mm.getMatrixRXY(0, 0));
		assertEquals(44755, mm.getMatrixRXY(0, 9));
		assertEquals(3850, mm.getMatrixRXY(9, 0));
		assertEquals(53350, mm.getMatrixRXY(9, 9));
		assertEquals(8420, mm.getMatrixRXY(3, 1));
		assertEquals(32265, mm.getMatrixRXY(2, 6));
		assertEquals(24940, mm.getMatrixRXY(7, 4));
		assertEquals(49530, mm.getMatrixRXY(5, 9));	
		
	}
	
	

	@Test
	public void testMultiplicationTAxB() {
		MatrixMultiplication mm;
		
		mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), true, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), false);
		mm.execute();
		assertEquals(29410, mm.getMatrixRXY(0, 0));
		assertEquals(33550, mm.getMatrixRXY(9, 0));
		assertEquals(33550, mm.getMatrixRXY(0, 9));
		assertEquals(38500, mm.getMatrixRXY(9, 9));
		assertEquals(31280, mm.getMatrixRXY(3, 1));
		assertEquals(33210, mm.getMatrixRXY(2, 6));
		assertEquals(34750, mm.getMatrixRXY(7, 4));
		assertEquals(36300, mm.getMatrixRXY(5, 9));
	}

	@Test
	public void testMultiplicationAxTB() {
		MatrixMultiplication mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), false, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), true);
		mm.execute();
		assertEquals(385, mm.getMatrixRXY(0, 0));
		assertEquals(5335, mm.getMatrixRXY(9, 0));
		assertEquals(5335, mm.getMatrixRXY(0, 9));
		assertEquals(91285, mm.getMatrixRXY(9, 9));
		assertEquals(5585, mm.getMatrixRXY(3, 1));
		assertEquals(16785, mm.getMatrixRXY(2, 6));
		assertEquals(34435, mm.getMatrixRXY(7, 4));
		assertEquals(53085, mm.getMatrixRXY(5, 9));
	}

	@Test
	public void testMultiplicationTAxTB() {
		MatrixMultiplication mm;
		
		mm = getInstance(squareMatrixA, getSquareMatrixSize(),
				getSquareMatrixSize(), true, squareMatrixB, getSquareMatrixSize(), getSquareMatrixSize(), true);	
		mm.execute();
		assertEquals(3355, mm.getMatrixRXY(0, 0));
		assertEquals(44755, mm.getMatrixRXY(9, 0));
		assertEquals(3850, mm.getMatrixRXY(0, 9));
		assertEquals(53350, mm.getMatrixRXY(9, 9));
		assertEquals(17510, mm.getMatrixRXY(3, 1));
		assertEquals(14085, mm.getMatrixRXY(2, 6));
		assertEquals(38575, mm.getMatrixRXY(7, 4));
		assertEquals(31350, mm.getMatrixRXY(5, 9));	
	}

	@Test
	public void testMultiplicationCxD() {
		MatrixMultiplication mm;
		
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

	@Test
	public void testMultiplicationTCxC() {
		MatrixMultiplication mm;
		
		mm = getInstance(matrixC, 8,
				4, true, matrixC, 8,4, false);
		
		mm.execute();
		mm.printMatrixA();
		mm.printMatrixB();
		mm.printMatrixR();

		assertEquals(996, mm.getMatrixRXY(0, 0));
		assertEquals(1360,mm.getMatrixRXY(7, 0));
		assertEquals(1360,mm.getMatrixRXY(0, 7));
		assertEquals(1920,mm.getMatrixRXY(7, 7));

	}
	protected abstract MatrixMultiplication getInstance(float[] matrixA, int matrixAWidth, int matrixAHeight, boolean transposeMatrixA,
			float[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB);

	private static void init(float[] matrix, int width, int height) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				matrix[y * width + x] = y * width + x + 1;
			}
		}
	}
}
