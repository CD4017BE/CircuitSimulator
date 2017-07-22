package electricity.components;

import electricity.Pin;

/**
 * A type of Junction that sets all connected pins to 0V ground level.<br>
 * Although a circuit could have multiple of these, it's more efficient for calculation to connect all pins to the same ground.
 * @author CD4017BE
 */
public class Ground extends Junction {

	/**
	 * creates a new ground pin connection
	 * @param pins the pins to connect with ground
	 */
	public Ground(Pin... pins) {
		super(pins);
	}

	@Override
	public void setEquations(double[][] mat, int states) {
		mat[id][id] = 1.0;
	}

}
