package gpu;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class MatrixMultiplicationDoublePrecisionGPUv4 extends Kernel implements MatrixMultiplicationDoublePrecision {

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
	public MatrixMultiplicationDoublePrecisionGPUv4(double[] matrixA, int matrixAWidth, int matrixAHeight,
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
	private final int MAX_TILE_SIZE = 64;
	private final int MAX_COLUMN_TILE_SIZE = 8;
	private int columnTileSize = 4;
	private int rts = tileSize / columnTileSize;
	private int numTiles;

	public void execute() {
		// this.setExecutionMode(EXECUTION_MODE.GPU);

		this.setExplicit(true);

		this.put(matrixA);
		this.put(matrixB);

		while (this.matrixRWidth % (tileSize * 2) == 0 && this.matrixRHeight % (tileSize * 2) == 0
				&& (tileSize * 2) <= MAX_TILE_SIZE
				&& (tileSize * 2 * tileSize * 2) <= this.getTargetDevice().getMaxWorkGroupSize()) {
			tileSize *= 2;
		}

		this.numTiles = this.matrixAWidth / this.tileSize;
		this.rts = this.tileSize / columnTileSize;

		System.out.println(String.format("TileSize=%d ColumnTileSize=%d rts=%d numTiles=%d", this.tileSize,
				this.columnTileSize, this.rts, this.numTiles));
		execute(Range.create2D(this.matrixRWidth / columnTileSize, this.matrixRHeight, this.rts, tileSize));

	}

	@Local
	double[] subA = new double[MAX_TILE_SIZE * MAX_TILE_SIZE];
	@Local
	double[] subB = new double[MAX_TILE_SIZE * MAX_TILE_SIZE];

	@Override
	public void run() {

		int col = getLocalId(0);
		int row = getLocalId(1);

		int globalCol = tileSize * getGroupId(0) + col * this.columnTileSize;
		int globalRow = tileSize * getGroupId(1) + row;
		// Because we are tiling the matrix we might overflow because of the tiling size

		double value_0 = 0;
		double value_1 = 0;
		double value_2 = 0;
		double value_3 = 0;
		double value_4 = 0;
		double value_5 = 0;
		double value_6 = 0;
		double value_7 = 0;

		for (int tile = 0; tile < numTiles; tile++) {

			int tileOffset = tileSize * tile;
			int columnOffsetFromTile = tileOffset + col * columnTileSize;
			int rowOffsetFromTile = tileOffset + row;

			// isMatrixATranspose == 1 because we cannot use boolean as variable type on the
			// GPU
			int subMatrixPos = col * columnTileSize + row * tileSize;
			int off1 = (globalCol) + (rowOffsetFromTile * matrixBWidth);
			int off2 = (globalRow * matrixAWidth) + columnOffsetFromTile;

			for (int i = 0; i < columnTileSize; i++) {
				subA[subMatrixPos] = matrixA[off2++];
				subB[subMatrixPos++] = matrixB[off1++];
			}

			localBarrier();

//			if (getGroupId(0) == 0 && getGroupId(1) == 0 && getLocalId(0) == 0 && getLocalId(1) == 0) {
////
//
//				System.out.println("##################################");
//				for (int j = 0; j < tileSize; j++) {
//					for (int i = 0; i < tileSize; i++) {
//						System.out.print(String.format(" %3.0f ", subA[j * tileSize + i]));
//					}
//					System.out.print("    --------       ");
//					for (int i = 0; i < tileSize; i++) {
//						System.out.print(String.format(" %3.0f ", subB[j * tileSize + i]));
//					}
//					System.out.println();
//				}
//				System.out.println("##################################");
//			}

			// Iterate for every column of matrix B
			for (int tileNr = 0; tileNr < tileSize; tileNr++) {
				// Can save columnTileSize-1 fma operations
				double a = subA[row * tileSize + tileNr];

				int offset = tileNr * tileSize + col;
				value_0 += a * subB[offset++];
				value_1 += a * subB[offset++];
				value_2 += a * subB[offset++];
				value_3 += a * subB[offset++];
				value_4 += a * subB[offset++];
				value_5 += a * subB[offset++];
				value_6 += a * subB[offset++];
				value_7 += a * subB[offset];

			}
			localBarrier();
		}
		int offset = globalRow * this.matrixRHeight + (globalCol);
		this.matrixR[offset++] = value_0;
		this.matrixR[offset++] = value_1;
		this.matrixR[offset++] = value_2;
		this.matrixR[offset++] = value_3;
		this.matrixR[offset++] = value_4;
		this.matrixR[offset++] = value_5;
		this.matrixR[offset++] = value_6;
		this.matrixR[offset] = value_7;

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
		this.get(matrixR);
		for (int y = 0; y < matrixHeight; y++) {
			for (int x = 0; x < matrixWidth; x++) {
				System.out.print(String.format("%10.0f  ", get(matrix, matrixWidth, matrixHeight, x, y, transpose)));
			}
			System.out.println();
		}
	}
}
