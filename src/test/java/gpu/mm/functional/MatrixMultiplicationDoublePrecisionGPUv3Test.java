package gpu.mm.functional;

import gpu.mm.MatrixMultiplicationDoublePrecision;
import gpu.mm.MatrixMultiplicationDoublePrecisionGPUv3;

public class MatrixMultiplicationDoublePrecisionGPUv3Test extends AbstractMatrixMultiplicationDoublePrecisionTest {

	@Override
	protected MatrixMultiplicationDoublePrecision getInstance(double[] matrixA, int matrixAWidth, int matrixAHeight,
			boolean transposeMatrixA, double[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
		return new MatrixMultiplicationDoublePrecisionGPUv3(matrixA, matrixAWidth, matrixAHeight, transposeMatrixA, matrixB,
				matrixBWidth, matrixBHeight, transposeMatrixB);
	}
	
	@Override
	protected void setCardinalities() {
		this.matrixHeight = 10;
		this.matrixWidth = 4;
		this.squareMatrixSize = 10;		
	}
}
