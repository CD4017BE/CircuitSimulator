package electricity;

/**
 * Implemented by parts or electric components that refer to parameterized values.<br>
 * Use {@code int par = circuit.addPar(this);} during {@code init()} to get a parameter id;
 * @author CD4017BE
 *
 */
public interface Parameter {

	/**
	 * called at initialization to set all parameter values
	 * @param vec array of all parameters
	 */
	public void setValue(double[] vec);

}
