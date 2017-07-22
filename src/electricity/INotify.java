package electricity;

/**
 * Implemented by all parts or electric components that need to update or react to state changes. There are two different update stages a component can register to:<br>
 * {@link Circuit.preNotifier} get a test run before the actual Tick to allow state changes.<br>
 * {@link Circuit.notifier} run after all preNotifier have been called and eventually the state has been recalculated.
 * @author CD4017BE
 */
public interface INotify {
	/**
	 * called at the end of each calculation cycle.
	 * @param states values of all the state variables
	 * @param dt [s] time interval passed
	 */
	public void update(double[] states, double dt);
}
