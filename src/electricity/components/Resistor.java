package electricity.components;

import electricity.IResistorMergable;
import static electricity.IResistorMergable.*;

/**
 * Implements an electric resistor with fixed resistance between its pins
 * @author CD4017BE
 *
 */
public class Resistor extends BiPole implements IResistorMergable {

	public final double R;

	/**
	 * creates a new resistor
	 * @param circuit the circuit it belongs to
	 * @param R [Ohm] its electric resistance
	 */
	public Resistor(double R) {
		this.R = R;
	}

	@Override
	public void setEquations(double[][] mat, int states) {
		double[] row = mat[id];
		row[id] = R + Rc(A);
		row[A.Id_U] = -1.0;
		row[B.Id_U] = 1.0;
	}

	@Override
	public String toString() {
		return super.toString() + String.format(" R=%.3gOhm", R);
	}

	@Override
	public void updateResistor() {}

}
