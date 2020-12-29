package gpu;

public class ComputeMatrixMultiplication {

	private static int MATRIX_SIZE = 256;
	private static int NB_RUNS = 3;
	private static String mode = "BOTH";

	public static void main(String[] args) {

		if (args.length < 2) {
			System.out.println("Proper Usage is:  java -jar gpu.jar <matrix-size> <# of runs> [GPU|JAVA|BOTH]");
			System.exit(0);
		} else {
			MATRIX_SIZE = Integer.valueOf(args[0]);
			NB_RUNS = Integer.valueOf(args[1]);
		}
		
		if (args.length == 3) {
			mode = args[2];
		}

		benchmarkSinglePrecision();
		benchmarkDoublePrecision();
	}

	private static void benchmarkSinglePrecision() {
		float[] matrixA = new float[MATRIX_SIZE * MATRIX_SIZE];
		float[] matrixB = new float[MATRIX_SIZE * MATRIX_SIZE];

		init(matrixA, MATRIX_SIZE, MATRIX_SIZE);
		init(matrixB, MATRIX_SIZE, MATRIX_SIZE);

		System.out.println(String.format("Matrix Multiplication Single Precision %dx%d", MATRIX_SIZE, MATRIX_SIZE));

		MatrixMultiplication mm;
		Long totalTime;
		
		if (mode.equals("JAVA") || mode.equals("BOTH")) {
			totalTime = 0l;
			mm = new MatrixMultiplicationJava(matrixA, MATRIX_SIZE, MATRIX_SIZE, false, matrixB, MATRIX_SIZE, MATRIX_SIZE,
					false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				mm.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;
				assert (checkValue(0, 0, MATRIX_SIZE) == mm.getMatrixRXY(0, 0));
				assert (checkValue(0, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(0, MATRIX_SIZE - 1));
				assert (checkValue(MATRIX_SIZE - 1, 0, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1, 0));
				assert (checkValue(MATRIX_SIZE - 1, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1,
						MATRIX_SIZE - 1));
			}
			System.out.println(String.format("\nJava Based Multiplication Average %d ms", (totalTime / NB_RUNS)));
		}
		
		
		if (mode.equals("GPU") || mode.equals("BOTH")) {
			totalTime = 0l;
			mm = new MatrixMultiplicationGPU(matrixA, MATRIX_SIZE, MATRIX_SIZE, false, matrixB, MATRIX_SIZE, MATRIX_SIZE,
					false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				mm.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;
				assert (checkValue(0, 0, MATRIX_SIZE) == mm.getMatrixRXY(0, 0));
				assert (checkValue(0, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(0, MATRIX_SIZE - 1));
				assert (checkValue(MATRIX_SIZE - 1, 0, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1, 0));
				assert (checkValue(MATRIX_SIZE - 1, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1,
						MATRIX_SIZE - 1));
			}
			System.out.println(String.format("\nGPUv1 Based Multiplication Average %d ms", (totalTime / NB_RUNS)));

			
			totalTime = 0l;
			mm = new MatrixMultiplicationGPUv3(matrixA, MATRIX_SIZE, MATRIX_SIZE, false, matrixB, MATRIX_SIZE, MATRIX_SIZE,
					false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				mm.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;
				assert (checkValue(0, 0, MATRIX_SIZE) == mm.getMatrixRXY(0, 0));
				assert (checkValue(0, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(0, MATRIX_SIZE - 1));
				assert (checkValue(MATRIX_SIZE - 1, 0, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1, 0));
				assert (checkValue(MATRIX_SIZE - 1, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1,
						MATRIX_SIZE - 1));
			}
			System.out.println(String.format("\nGPUv3 Based Multiplication Average %d ms", (totalTime / NB_RUNS)));
		}
	}

	
	private static void benchmarkDoublePrecision() {
		double[] matrixA = new double[MATRIX_SIZE * MATRIX_SIZE];
		double[] matrixB = new double[MATRIX_SIZE * MATRIX_SIZE];

		initDouble(matrixA, MATRIX_SIZE, MATRIX_SIZE);
		initDouble(matrixB, MATRIX_SIZE, MATRIX_SIZE);

		System.out.println(String.format("Matrix Multiplication Double Precision %dx%d", MATRIX_SIZE, MATRIX_SIZE));

		MatrixMultiplicationDoublePrecision mm;
		Long totalTime;
		
		if (mode.equals("JAVA") || mode.equals("BOTH")) {
			totalTime = 0l;
			mm = new MatrixMultiplicationDoublePrecisionJava(matrixA, MATRIX_SIZE, MATRIX_SIZE, false, matrixB, MATRIX_SIZE, MATRIX_SIZE,
					false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				mm.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;
				assert (checkValue(0, 0, MATRIX_SIZE) == mm.getMatrixRXY(0, 0));
				assert (checkValue(0, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(0, MATRIX_SIZE - 1));
				assert (checkValue(MATRIX_SIZE - 1, 0, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1, 0));
				assert (checkValue(MATRIX_SIZE - 1, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1,
						MATRIX_SIZE - 1));
			}
			System.out.println(String.format("\nJava Based Multiplication Average %d ms", (totalTime / NB_RUNS)));
		}
		
		
		if (mode.equals("GPU") || mode.equals("BOTH")) {
			totalTime = 0l;
			mm = new MatrixMultiplicationDoublePrecisionGPU(matrixA, MATRIX_SIZE, MATRIX_SIZE, false, matrixB, MATRIX_SIZE, MATRIX_SIZE,
					false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				mm.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;
				assert (checkValue(0, 0, MATRIX_SIZE) == mm.getMatrixRXY(0, 0));
				assert (checkValue(0, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(0, MATRIX_SIZE - 1));
				assert (checkValue(MATRIX_SIZE - 1, 0, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1, 0));
				assert (checkValue(MATRIX_SIZE - 1, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1,
						MATRIX_SIZE - 1));
			}
			System.out.println(String.format("\nGPUv1 Based Multiplication Average %d ms", (totalTime / NB_RUNS)));

			
			totalTime = 0l;
			mm = new MatrixMultiplicationDoublePrecisionGPUv3(matrixA, MATRIX_SIZE, MATRIX_SIZE, false, matrixB, MATRIX_SIZE, MATRIX_SIZE,
					false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				mm.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;
				assert (checkValue(0, 0, MATRIX_SIZE) == mm.getMatrixRXY(0, 0));
				assert (checkValue(0, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(0, MATRIX_SIZE - 1));
				assert (checkValue(MATRIX_SIZE - 1, 0, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1, 0));
				assert (checkValue(MATRIX_SIZE - 1, MATRIX_SIZE - 1, MATRIX_SIZE) == mm.getMatrixRXY(MATRIX_SIZE - 1,
						MATRIX_SIZE - 1));
			}
			System.out.println(String.format("\nGPUv3 Based Multiplication Average %d ms", (totalTime / NB_RUNS)));
		}
	}

	
	private static void initDouble(double[] matrix, int width, int height) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				matrix[y * width + x] = y * width + x + 1;
			}
		}		
	}

	private static void init(float[] matrix, int width, int height) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				matrix[y * width + x] = y * width + x + 1;
			}
		}
	}

	private static double checkValue(int x, int y, int matrixWidth) {
		return checkValue(x, y, matrixWidth, 0);
	}

	private static double checkValue(int x, int y, int matrixWidth, int delta) {
		int col = x;
		int row = y;

		int width = matrixWidth;

		double sum = 0;
		for (int i = 0; i < width; i++) {

			double a = (width * row) + i + 1;
			double b = ((width * i) + 1 + col);
			sum += a * b;
		}
		return sum;
	}
}
