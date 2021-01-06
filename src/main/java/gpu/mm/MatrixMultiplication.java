package gpu.mm;

public interface MatrixMultiplication {

	void execute();

	float[] getResult();

	int getResultWidth();

	int getResultHeight();

	float getMatrixAXY(int x, int y);

	float getMatrixBXY(int x, int y);

	float getMatrixRXY(int x, int y);

	void printMatrixA();

	void printMatrixB();

	void printMatrixR();

}