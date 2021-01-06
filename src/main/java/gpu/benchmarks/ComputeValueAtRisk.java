package gpu.benchmarks;

import gpu.var.ValueAtRisk;
import gpu.var.ValueAtRiskData;
import gpu.var.ValueAtRiskGPU;
import gpu.var.ValueAtRiskJava;

public class ComputeValueAtRisk {

	private static String mode = "BOTH";
	private static int nb_instruments = 256;
	private static int nb_observations = 512;
	private static int NB_RUNS = 3;
	
	public static void main(String[] args) {

		if (args.length < 2) {
			System.out.println("Proper Usage is:  java -jar gpu.jar <# of instruments> <# of runs> [GPU|JAVA|BOTH]");
			System.exit(0);
		} else {
			nb_instruments = Integer.valueOf(args[0]);
			NB_RUNS = Integer.valueOf(args[1]);
		}
		
		if (args.length == 3) {
			mode = args[2];
		}

		benchmarkVar();
	
	}

	private static void benchmarkVar() {
		double[] instruments = new double[nb_instruments * nb_observations];
		double[] weights = new double[nb_instruments];
		ValueAtRiskData.initTest(instruments, weights, nb_instruments,
				nb_observations);
		
		System.out.println(String.format("Value At Risk for %d instruments and %d observations", nb_instruments, nb_observations));

		Long totalTime;
		
		if (mode.equals("JAVA") || mode.equals("BOTH")) {
			totalTime = 0l;
			ValueAtRisk var;
			var = new ValueAtRiskJava(instruments, weights, 1_000_000f, nb_instruments,
					nb_observations, false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				var.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;

			}
			System.out.println(String.format("\nJava Based VaR Average %d ms", (totalTime / NB_RUNS)));
		}
		
		if (mode.equals("GPU") || mode.equals("BOTH")) {
			totalTime = 0l;
			ValueAtRisk var;
			var = new ValueAtRiskGPU(instruments, weights, 1_000_000f, nb_instruments,
					nb_observations, false);
			for (int i = 0; i < NB_RUNS; i++) {
				Long time = System.currentTimeMillis();
				var.execute();
				time = System.currentTimeMillis() - time;
				System.out.print(".");
				totalTime += time;

			}
			System.out.println(String.format("\nGPU Based VaR Average %d ms", (totalTime / NB_RUNS)));
		}
	}

}
