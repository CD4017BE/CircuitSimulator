package electricity;

import java.util.ArrayList;

import electricity.components.Resistor;

/**
 * This pin stores merged resistor components for {@link IResistorMergable}s.<br>These are automatically created by joining components together and will then replace the normal Pins.
 * @author CD4017BE
 */
public class CombinedPin extends Pin {

	public final ArrayList<Resistor> content = new ArrayList<Resistor>();
	public double R = 0;

	/**
	 * Creates a new CombinedPin as result of a connection between to component pins
	 * @param master pin of the {@link IResistorMergable} master component
	 * @param sub pin of the connected {@link Resistor} that will become a sub component
	 */
	CombinedPin(Pin master, Pin sub) {
		super(master.link);
		merge(sub);
	}

	/**
	 * Creates an empty CombinedPin to replace the given master pin
	 * @param master pin of the {@link IResistorMergable} master component
	 */
	CombinedPin(Pin master) {
		super(master.link);
	}

	/**
	 * Merges this pin with the given pin
	 * @param sub pin to merge with this pin. (must be from a {@link Resistor})
	 */
	public void merge(Pin sub) {
		Resistor r = (Resistor)sub.I;
		if (sub instanceof CombinedPin) {
			CombinedPin pin = (CombinedPin)sub;
			for (int i = pin.content.size() - 1; i >= 0; i--)
				addResistor(pin.content.get(i), sub);
			pin.content.clear();
		}
		addResistor(r, sub);
		sub = sub.link;
		if (sub.U != null) sub.U.replacePin(sub, this);
		if (sub instanceof CombinedPin) {
			CombinedPin pin = (CombinedPin)sub;
			for (Resistor res : pin.content)
				if (res.A != null) res.A = this;
				else res.B = this;
			content.addAll(pin.content);
			pin.content.clear();
			R += pin.R;
		}
	}

	/**
	 * Adds a resistor to the list of contained resistors
	 * @param r Resistor to add
	 * @param p resistor pin connected with this
	 */
	public void addResistor(Resistor r, Pin p) {
		if (r.A == p)	{r.A = null; r.B = this;}
		else 			{r.A = this; r.B = null;}
		content.add(r);
		R += r.R;
	}

}
