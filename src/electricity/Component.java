package electricity;

/**
 * An electronic component may contain one or more internal state variables (usually Voltage or Current) and also provides <b>exactly as many</b> linear equations to define how these or other variables relate to each other.
 * @author CD4017BE
 */
public abstract class Component {

	/**The circuit instance this component belongs to.<br><b>variable will be set by circuit, do not modify! */
	public Circuit circuit;
	/**The first equation id of this component.<br><b>variable will be set by circuit, do not modify! */
	public int id;
	
	/**
	 * makes this Component add its equations to the given matrix. Each equation is defined in its own sub array row of index {@code id, id + 1, ...}, where elements at {@code 0 <= index < states} refer to state variables and {@code index >= states} refer to fixed parameters.
	 * @param mat matrix representing the linear equation system
	 * @param states first index of parameter columns
	 */
	public abstract void setEquations(double[][] mat, int states);

	/**
	 * Initializes this component on the given circuit.<br>At this point the variables {@code circuit} and {@code id} are already set.
	 * @return amount of states and equations this component requires
	 */
	public abstract int init();

	/**
	 * Used to get the pins of this component (or from connected components if this is a Junction).
	 * @return array containing all pins this interacts with
	 */
	public abstract Pin[] getPins();

	/**
	 * Set the partner of the given pin to the new pin linked to it
	 * @param p Partner of the pin that changed
	 */
	public abstract void swapPin(Pin p);

}