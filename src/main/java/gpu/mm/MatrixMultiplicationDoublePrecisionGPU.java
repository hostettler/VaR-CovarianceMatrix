package gpu.mm;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class MatrixMultiplicationDoublePrecisionGPU extends Kernel implements MatrixMultiplicationDoublePrecision {

	// input
	private final double[] matrixA;
	// input
	private final double[] matrixB;
	// output
	private final double[] matrixR;

	private final int matrixAWidth;
	private final int matrixAHeight;
	private final int isMatrixATranspose;

	private final int matrixBWidth;
	private final int matrixBHeight;
	
	//Warning boolean represented as an int because of limitation of aparapi
	private final int isMatrixBTranspose;

	private final int matrixRWidth;
	private final int matrixRHeight;

	/**
	 * Matrix with M lines and N Colunms are expected to be 1-D encoded following
	 * the schema
	 * 
	 * L0C0 L0C1 L0C2 .... L0CN L1C0 L1C1 L1C2 .... L0CN L2C0 L2C1 L2C2 .... L0CN
	 * ... LMC0 LMC1 LMC2 .... LMCN
	 * 
	 * 
	 * @param matrixA
	 * @param matrixAWidth
	 * @param matrixAHeight
	 * @param transposeMatrixA
	 * @param matrixB
	 * @param matrixBWidth
	 * @param matrixBHeight
	 * @param transposeMatrixB
	 */
	public MatrixMultiplicationDoublePrecisionGPU(double[] matrixA, int matrixAWidth, int matrixAHeight, boolean transposeMatrixA,
			double[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
		this.matrixA = matrixA;
		this.matrixB = matrixB;

		this.matrixAHeight = !transposeMatrixA ? matrixAHeight : matrixAWidth;
		this.matrixAWidth = !transposeMatrixA ? matrixAWidth : matrixAHeight;
		this.matrixBHeight = !transposeMatrixB ? matrixBHeight : matrixBWidth;
		this.matrixBWidth = !transposeMatrixB ? matrixBWidth : matrixBHeight;
		
		//Warning boolean represented as an int because of limitation of aparapi
		this.isMatrixATranspose = transposeMatrixA ? 1 : 0;
		//Warning boolean represented as an int because of limitation of aparapi
		this.isMatrixBTranspose = transposeMatrixB ? 1 : 0;

		if (this.matrixAWidth != this.matrixBHeight) {
			throw new IllegalArgumentException(String.format(
					"Incompatible matrix width and height A.width=%d while B.height=%d", matrixAWidth, matrixBHeight));
		}

		this.matrixRWidth = this.matrixBWidth;
		this.matrixRHeight = this.matrixAHeight;

		this.matrixR = new double[this.matrixRWidth * this.matrixRHeight];
	}

	public void execute() {
		this.setExplicit(true);
		this.put(matrixA);
		this.put(matrixB);
		execute(Range.create(this.matrixRWidth));

	}

	@Override
	public void run() {
		
		int index = getGlobalId();
		computeMatrixColumn(index);
	}

	private void computeMatrixColumn(int resultColumnIndex) {
		// Iterate for every line of matrixA
		for (int lineInA = 0; lineInA < this.matrixAHeight; lineInA++) {
			double value = 0;
			// Iterate for every column of matrix B
			for (int columnInB = 0; columnInB < this.matrixBHeight; columnInB++) {
				//isMatrixATranspose == 1 because we cannot use boolean as variable type on the GPU
				double a = get(matrixA, matrixAWidth, matrixAHeight, columnInB, lineInA, isMatrixATranspose == 1);
				double b = get(matrixB, matrixBWidth, matrixBHeight, resultColumnIndex, columnInB, isMatrixBTranspose == 1);
				value += a * b;
			}
			this.matrixR[lineInA * this.matrixRWidth + resultColumnIndex] = value;
		}
	}

	private double get(double[] matrix, int matrixWidth, int matrixHeight, int column, int line, boolean transpose) {
		return transpose ? matrix[column * matrixHeight + line] : matrix[line * matrixWidth + column];
	}

	public double[] getResult() {
		this.get(matrixR);
		return this.matrixR;
	}

	public int getResultWidth() {
		return this.matrixRWidth;
	}

	public int getResultHeight() {
		return this.matrixRHeight;
	}

	public double getMatrixAXY(int x, int y) {
		return get(matrixA, matrixAWidth, matrixAHeight, x, y, isMatrixATranspose == 1);
	}

	public double getMatrixBXY(int x, int y) {
		return get(matrixB, matrixBWidth, matrixBHeight, x, y, isMatrixBTranspose == 1);
	}

	public double getMatrixRXY(int x, int y) {
		this.get(matrixR);
		return get(matrixR, matrixRWidth, matrixRHeight, x, y, false);
	}

	public void printMatrixA() {
		printMatrix(matrixA, matrixAWidth, matrixAHeight, isMatrixATranspose == 1);
	}

	public void printMatrixB() {
		printMatrix(matrixB, matrixBWidth, matrixBHeight, isMatrixBTranspose == 1);
	}

	public void printMatrixR() {
		printMatrix(matrixR, matrixRWidth, matrixRHeight, false);
	}

	private void printMatrix(double matrix[], int matrixWidth, int matrixHeight, boolean transpose) {
		System.out.println(transpose);
		for (int y = 0; y < matrixHeight; y++) {
			for (int x = 0; x < matrixWidth; x++) {
				System.out.print(String.format("%10.0f  ", get(matrix, matrixWidth, matrixHeight, x, y, transpose)));
			}
			System.out.println();
		}
	}
}
