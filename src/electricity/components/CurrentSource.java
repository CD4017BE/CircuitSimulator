package electricity.components;

import electricity.Parameter;

/**
 * Implements an electric component that provides a fixed electric current flowing from pin A to pin B
 * @author CD4017BE
 */
public class CurrentSource extends BiPole implements Parameter {

	int cid;
	double I;

	/**
	 * creates a new current source
	 * @param R [A] the current it provides
	 */
	public CurrentSource(double I) {
		this.I = I;
	}

	@Override
	public void setEquations(double[][] mat, int states) {
		double[] row = mat[id];
		row[cid + states] = 1;
		row[id] = 1;
	}

	@Override
	public void setValue(double[] vec) {
		vec[cid] = I;
	}

	@Override
	public int init() {
		this.cid = circuit.addPar(this);
		return super.init();
	}

	@Override
	public String toString() {
		return super.toString() + String.format(" I=%.3gA", I);
	}

}
