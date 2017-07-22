package electricity;

public class MathUtil {

	public static final double BlockResistance = 1000000000; // 1GOhm
	public static final double PassResistance = 0.001; // 1mOhm

	public static void solveMatrix(double[][] eq) {
		int m = eq.length;
		for(int i = 0; i < m; i++) {
			double[] row = eq[i];
			int j = i;
			while(row[i] == 0)
				if (++j >= m) break;
				else row = eq[j];
			if (j != i) {
				if (j >= m) continue; //variable can't be solved
				eq[j] = eq[i];
				eq[i] = row;
			}
			int n = row.length;
			double y = row[i];
			if(y != 1.0)
				for (int k = i + 1; k < n; k++)
					row[k] /= y;
			for (int k = 0; k < i; k++) {
				double[] row2 = eq[k];
				double x = row2[i];
				if (x != 0)
					for (int l = i + 1; l < n; l++)
						row2[l] -= row[l] * x;
			}
			for (int k = j + 1; k < m; k++) {
				double[] row2 = eq[k];
				double x = row2[i];
				if (x != 0)
					for (int l = i + 1; l < n; l++)
						row2[l] -= row[l] * x;
			}
		}
	}

}
