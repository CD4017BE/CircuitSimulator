package electricity;

/**
 * This allows components to integrate connected resistors into their own calculation to reduce node count and this way improve performance.
 * @author CD4017BE
 */
public interface IResistorMergable {

	/**
	 * Called when resistors were added or removed.
	 * @see IResistorMergable.Rc
	 */
	public void updateResistor();

	/**
	 * Gets the total serial resistance of the given pin pair
	 * @param p one of the two paired pins
	 * @return [Ohm] resistance
	 */
	public static double Rc(Pin p) {
		return (p instanceof CombinedPin ? ((CombinedPin)p).R : 0.0) + (p.link instanceof CombinedPin ? ((CombinedPin)p.link).R : 0.0);
	}

}
