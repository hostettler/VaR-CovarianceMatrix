package gpu;

public interface MatrixMultiplicationDoublePrecision {

	void execute();

	double[] getResult();

	int getResultWidth();

	int getResultHeight();

	double getMatrixAXY(int x, int y);

	double getMatrixBXY(int x, int y);

	double getMatrixRXY(int x, int y);

	void printMatrixA();

	void printMatrixB();

	void printMatrixR();

}