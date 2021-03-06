package gpu.mm;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class MatrixMultiplicationGPUv3 extends Kernel implements MatrixMultiplication {

	// input
	private final float[] matrixA;
	// input
	private final float[] matrixB;
	// output
	private final float[] matrixR;

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
	public MatrixMultiplicationGPUv3(float[] matrixA, int matrixAWidth, int matrixAHeight,
			boolean transposeMatrixA, float[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
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

		this.matrixR = new float[this.matrixRWidth * this.matrixRHeight];
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
				&& (tileSize * 2) <= MAX_TILE_SIZE && (tileSize*2*tileSize*2) <= this.getTargetDevice().getMaxWorkGroupSize() ) {
			tileSize *= 2;
		}
		this.numTiles = matrixAWidth / tileSize;
		
		execute(Range.create2D(this.matrixRWidth, this.matrixRHeight, tileSize, tileSize));

	}

	@Local
	float[] subA = new float[MAX_TILE_SIZE * MAX_TILE_SIZE];
	@Local
	float[] subB = new float[MAX_TILE_SIZE * MAX_TILE_SIZE];

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

		float value = 0;

		for (int tile = 0; tile < numTiles; tile++) {

			int tileOffset = tileSize * tile;
			int columnOffsetFromTile = tileOffset + col;
			int rowOffsetFromTile = tileOffset + row;

			// isMatrixATranspose == 1 because we cannot use boolean as variable type on the
			// GPU
			int subMatrixPos= col + row * tileSize;
			subA[subMatrixPos] = isMatrixATranspose == 0
					? matrixA[(globalRow * matrixAWidth) + columnOffsetFromTile]
					: matrixA[(columnOffsetFromTile * matrixAWidth) + globalRow];
			subB[subMatrixPos] = isMatrixBTranspose == 0
					? matrixB[globalCol + (rowOffsetFromTile * matrixBWidth)]
					: matrixB[rowOffsetFromTile + (globalCol * matrixBWidth)];

			localBarrier();
			// Iterate for every column of matrix B
			for (int tileNr = 0; tileNr < tileSize; tileNr++) {
				float a = subA[row * tileSize + tileNr];
				float b = subB[tileNr * tileSize + col];
				value += a * b;
			}
			localBarrier();
		}

		this.matrixR[globalRow * this.matrixRHeight + globalCol] = value;

	}

	private float get(float[] matrix, int matrixWidth, int matrixHeight, int column, int line, boolean transpose) {
		return transpose ? matrix[column * matrixHeight + line] : matrix[line * matrixWidth + column];
	}

	public float[] getResult() {
		this.get(matrixR);
		return this.matrixR;
	}

	public int getResultWidth() {
		return this.matrixRWidth;
	}

	public int getResultHeight() {
		return this.matrixRHeight;
	}

	public float getMatrixAXY(int x, int y) {
		return get(matrixA, matrixAWidth, matrixAHeight, x, y, isMatrixATranspose == 1);
	}

	public float getMatrixBXY(int x, int y) {
		return get(matrixB, matrixBWidth, matrixBHeight, x, y, isMatrixBTranspose == 1);
	}

	public float getMatrixRXY(int x, int y) {
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

	private void printMatrix(float matrix[], int matrixWidth, int matrixHeight, boolean transpose) {
		System.out.println(transpose);
		for (int y = 0; y < matrixHeight; y++) {
			for (int x = 0; x < matrixWidth; x++) {
				System.out.print(String.format("%10.0f  ", get(matrix, matrixWidth, matrixHeight, x, y, transpose)));
			}
			System.out.println();
		}
	}
}
