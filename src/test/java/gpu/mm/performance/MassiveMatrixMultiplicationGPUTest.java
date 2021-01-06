package gpu.mm.performance;

import gpu.mm.MatrixMultiplication;
import gpu.mm.MatrixMultiplicationGPU;

public class MassiveMatrixMultiplicationGPUTest extends AbstractMassiveMatrixMultiplicationTest {

	@Override
	protected MatrixMultiplication getInstance(float[] matrixA, int matrixAWidth, int matrixAHeight,
			boolean transposeMatrixA, float[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
		return new MatrixMultiplicationGPU(matrixA, matrixAWidth, matrixAHeight, transposeMatrixA, matrixB,
				matrixBWidth, matrixBHeight, transposeMatrixB);
	}
	
	@Override
	protected void setCardinalities() {
		this.matrixHeight = 1000;
		this.matrixWidth = 1000;
		this.squareMatrixSize = 2048;
	}
}
