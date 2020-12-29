package gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MatrixMultiplicationJava implements MatrixMultiplication {

	// input
	private final float[] matrixA;
	// input
	private final float[] matrixB;
	// output
	private final float[] matrixR;

	private final int matrixAWidth;
	private final int matrixAHeight;
	private final boolean isMatrixATranspose;

	private final int matrixBWidth;
	private final int matrixBHeight;
	private final boolean isMatrixBTranspose;

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
	public MatrixMultiplicationJava(float[] matrixA, int matrixAWidth, int matrixAHeight, boolean transposeMatrixA,
			float[] matrixB, int matrixBWidth, int matrixBHeight, boolean transposeMatrixB) {
		this.matrixA = matrixA;
		this.matrixB = matrixB;

		this.matrixAHeight = !transposeMatrixA ? matrixAHeight : matrixAWidth;
		this.matrixAWidth = !transposeMatrixA ? matrixAWidth : matrixAHeight;
		this.matrixBHeight = !transposeMatrixB ? matrixBHeight : matrixBWidth;
		this.matrixBWidth = !transposeMatrixB ? matrixBWidth : matrixBHeight;
		this.isMatrixATranspose = transposeMatrixA;
		this.isMatrixBTranspose = transposeMatrixB;

		if (this.matrixAWidth != this.matrixBHeight) {
			throw new IllegalArgumentException(String.format(
					"Incompatible matrix width and height A.width=%d while B.height=%d", matrixAWidth, matrixBHeight));
		}

		this.matrixRWidth = this.matrixBWidth;
		this.matrixRHeight = this.matrixAHeight;

		this.matrixR = new float[this.matrixRWidth * this.matrixRHeight];
	}

	@Override
	public void execute() {
		try {

			ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
					Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());

			List<Callable<Integer>> stepTasks = new ArrayList<>();
			for (int i = 0; i < this.matrixRWidth; i++) {
				final int index = i;
				Callable<Integer> step = () -> {
					computeMatrixColumn(index);
					return 0;
				};
				stepTasks.add(step);
			}
			executorService.invokeAll(stepTasks);

			executorService.shutdown();
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	private void computeMatrixColumn(int resultColumnIndex) {
		// Iterate for every line of matrixA
		for (int lineInA = 0; lineInA < this.matrixAHeight; lineInA++) {
			float value = 0;
			// Iterate for every column of matrix B
			for (int columnInB = 0; columnInB < this.matrixBHeight; columnInB++) {
				float a = get(matrixA, matrixAWidth, matrixAHeight, columnInB,  lineInA, isMatrixATranspose);
				float b = get(matrixB, matrixBWidth, matrixBHeight, resultColumnIndex, columnInB, isMatrixBTranspose);
//				if (resultColumnIndex == 0)
//					System.out.println(String.format("resultColumn=%d lineInA=%d columnInB=%d a=%.0f b=%.0f ", resultColumnIndex, lineInA,
//							columnInB, a, b));
				value += a * b;
			}
			this.matrixR[lineInA * this.matrixRWidth + resultColumnIndex] = value;
//			if (resultColumnIndex == 0)
//				System.out.println(value);
		}

	}

	private float get(float[] matrix, int matrixWidth, int matrixHeight, int column, int line, boolean transpose) {
		return transpose ? matrix[column * matrixHeight + line] : matrix[line * matrixWidth + column];
	}

	@Override
	public float[] getResult() {
		return this.matrixR;
	}

	@Override
	public int getResultWidth() {
		return this.matrixRWidth;
	}

	@Override
	public int getResultHeight() {
		return this.matrixRHeight;
	}

	@Override
	public float getMatrixAXY(int x, int y) {
		return get(matrixA, matrixAWidth, matrixAHeight, x, y, isMatrixATranspose);
	}
	@Override
	public float getMatrixBXY(int x, int y) {
		return get(matrixB, matrixBWidth, matrixBHeight, x, y, isMatrixBTranspose);
	}
	@Override
	public float getMatrixRXY(int x, int y) {
		return get(matrixR, matrixRWidth, matrixRHeight, x, y, false);
	}

	
	
	@Override
	public void printMatrixA() {
		printMatrix(matrixA, matrixAWidth, matrixAHeight, isMatrixATranspose);
	}

	@Override
	public void printMatrixB() {
		printMatrix(matrixB, matrixBWidth, matrixBHeight, isMatrixBTranspose);
	}

	@Override
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
