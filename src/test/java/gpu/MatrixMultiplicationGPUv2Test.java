package gpu;

public class MatrixMultiplicationGPUv2Test extends AbstractMatrixMultiplicationTest {

	@Override
	protected MatrixMultiplication getInstance(float[] matrixA, int matrixAWidth, int matrixAHeight,
			boolean transposeMatrixA, float[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
		return new MatrixMultiplicationGPUv2(matrixA, matrixAWidth, matrixAHeight, transposeMatrixA, matrixB,
				matrixBWidth, matrixBHeight, transposeMatrixB);
	}
	
	@Override
	protected void setCardinalities() {
		this.matrixHeight = 10;
		this.matrixWidth = 4;
		this.squareMatrixSize = 10;		
	}
}
