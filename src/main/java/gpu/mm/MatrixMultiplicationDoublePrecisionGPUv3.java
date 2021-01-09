package gpu.mm;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class MatrixMultiplicationDoublePrecisionGPUv3 extends Kernel implements MatrixMultiplicationDoublePrecision {

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

	// Warning boolean represented as an int because of limitation of aparapi
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
	public MatrixMultiplicationDoublePrecisionGPUv3(double[] matrixA, int matrixAWidth, int matrixAHeight,
			boolean transposeMatrixA, double[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
		this.matrixA = matrixA;
		this.matrixB = matrixB;

		this.matrixAHeight = !transposeMatrixA ? matrixAHeight : matrixAWidth;
		this.matrixAWidth = !transposeMatrixA ? matrixAWidth : matrixAHeight;
		this.matrixBHeight = !transposeMatrixB ? matrixBHeight : matrixBWidth;
		this.matrixBWidth = !transposeMatrixB ? matrixBWidth : matrixBHeight;

		// Warning boolean represented as an int because of limitation of aparapi
		this.isMatrixATranspose = transposeMatrixA ? 1 : 0;
		// Warning boolean represented as an int because of limitation of aparapi
		this.isMatrixBTranspose = transposeMatrixB ? 1 : 0;

		if (this.matrixAWidth != this.matrixBHeight) {
			throw new IllegalArgumentException(String.format(
					"Incompatible matrix width and height A.width=%d while B.height=%d", matrixAWidth, matrixBHeight));
		}

		this.matrixRWidth = this.matrixBWidth;
		this.matrixRHeight = this.matrixAHeight;

		this.matrixR = new double[this.matrixRWidth * this.matrixRHeight];
	}

	private int tileSize = 2;
	private final int MAX_TILE_SIZE = 24;
	private int numTiles;

	public void execute() {
		// this.setExecutionMode(EXECUTION_MODE.GPU);
		
		this.setExplicit(true);

		this.put(matrixA);
		this.put(matrixB);

		while (this.matrixRWidth % (tileSize * 2) == 0 && this.matrixRHeight % (tileSize * 2) == 0
				&& (tileSize * 2) <= MAX_TILE_SIZE
				&& (tileSize * 2 * tileSize * 2) <= this.getTargetDevice().getMaxWorkGroupSize()
				&& (matrixAHeight / (tileSize * 2) > 0) && (matrixAWidth / (tileSize * 2) > 0)
				&& (matrixBWidth / (tileSize * 2) > 0) && (matrixBHeight / (tileSize * 2) > 0)) {
			tileSize *= 2;
		}
		this.numTiles = matrixBHeight / tileSize;
		System.out.println(String.format("Tile size=%d # of tiles=%d", tileSize, numTiles));
		execute(Range.create2D(this.matrixRWidth, this.matrixRHeight, tileSize, tileSize));

	}

	@Local
	double[] subA = new double[MAX_TILE_SIZE * MAX_TILE_SIZE];
	@Local
	double[] subB = new double[MAX_TILE_SIZE * MAX_TILE_SIZE];

	@Override
	public void run() {

		int col = getLocalId(0);
		int row = getLocalId(1);

		int globalCol = tileSize * getGroupId(0) + col;
		int globalRow = tileSize * getGroupId(1) + row;
		// Because we are tiling the matrix we might overflow because of the tiling size
		if (globalRow > matrixRHeight || globalCol > matrixRWidth) {
			return;
		}

		double value = 0;

		for (int tile = 0; tile < numTiles; tile++) {

			int tileOffset = tileSize * tile;
			int columnOffsetFromTile = tileOffset + col;
			int rowOffsetFromTile = tileOffset + row;

			// isMatrixATranspose == 1 because we cannot use boolean as variable type on the
			// GPU
			int subMatrixPos = col + row * tileSize;
			subA[subMatrixPos] = isMatrixATranspose == 0 ? matrixA[(globalRow * matrixAWidth) + columnOffsetFromTile]
					: matrixA[(columnOffsetFromTile * matrixAWidth) + globalRow];
			subB[subMatrixPos] = isMatrixBTranspose == 0 ? matrixB[globalCol + (rowOffsetFromTile * matrixBWidth)]
					: matrixB[rowOffsetFromTile + (globalCol * matrixBWidth)];

			localBarrier();
			// Iterate for every column of matrix B
			for (int tileNr = 0; tileNr < tileSize; tileNr++) {
				double a = subA[row * tileSize + tileNr];
				double b = subB[tileNr * tileSize + col];
				value += a * b;

//				if (globalCol == 0 && globalRow == 0) {
//					System.out.println(String.format(" %.0f %.0f ", a, b));
//				}

			}
			localBarrier();
		}

		this.matrixR[globalRow * this.matrixRHeight + globalCol] = value;

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
		this.get(matrixR);
		for (int y = 0; y < matrixHeight; y++) {
			for (int x = 0; x < matrixWidth; x++) {
				System.out.print(String.format("%10.0f  ", get(matrix, matrixWidth, matrixHeight, x, y, transpose)));
			}
			System.out.println();
		}
	}
}
