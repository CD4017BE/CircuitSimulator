package electricity.components;

import electricity.Component;
import electricity.Pin;

/**
 * Used to connect the pins of electric parts by setting them to equal voltage level and ensuring that in- and outgoing currents are equal.
 * @author CD4017BE
 */
public class Junction extends Component {

	public final Pin[] pins;

	/**
	 * creates a new pin junction
	 * @param circuit the circuit it belongs to
	 * @param pins the pins to connect with each other
	 */
	public Junction(Pin... pins) {
		this.pins = pins;
		for (Pin pin : pins)
			pin.U = this;
	}

	@Override
	public void setEquations(double[][] mat, int states) {
		double[] row = mat[id];
		for(Pin pin : pins)
			row[pin.id_I] = pin.dir ? 1.0 : -1.0;
	}

	@Override
	public int init() {
		for (Pin p : pins) p.Id_U = id;
		return 1;
	}

	public void replacePin(Pin original, Pin replacement) {
		for (int i = 0; i < pins.length; i++)
			if (pins[i] == original) {
				pins[i] = replacement;
				replacement.U = this;
				original.U = null;
				return;
			}
	}

	@Override
	public String toString() {
		String s = String.format("Id:%d Pins:[", id);
		for (Pin p : pins) s += String.format("%d=%s ", p.id_I, p.I.getClass().getSimpleName());
		return s + "]";
	}

	@Override
	public Pin[] getPins() {
		return pins;
	}

	@Override
	public void swapPin(Pin p) {}

}
