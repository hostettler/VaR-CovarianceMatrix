package gpu.mm.performance;

import gpu.mm.MatrixMultiplicationDoublePrecision;
import gpu.mm.MatrixMultiplicationDoublePrecisionGPU;

public class MassiveMatrixMultiplicationDoublePrecisionGPUTest extends AbstractMassiveMatrixMultiplicationDoublePrecisionTest {

	@Override
	protected MatrixMultiplicationDoublePrecision getInstance(double[] matrixA, int matrixAWidth, int matrixAHeight,
			boolean transposeMatrixA, double[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
		return new MatrixMultiplicationDoublePrecisionGPU(matrixA, matrixAWidth, matrixAHeight, transposeMatrixA, matrixB,
				matrixBWidth, matrixBHeight, transposeMatrixB);
	}
	
	@Override
	protected void setCardinalities() {
		this.matrixHeight = 1024;
		this.matrixWidth = 1024;
		this.squareMatrixSize = 2048;
	}
}
