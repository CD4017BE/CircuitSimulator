package electricity;

import electricity.components.Junction;

/**
 * Pins define the access points of electronic components and must be connected to Junctions.
 * @author CD4017BE
 */
public class Pin {

	public Junction U;
	public Component I;
	public Pin link;
	public int Id_U;
	public int id_I;
	public boolean dir;

	/**
	 * create a source pin
	 */
	public Pin(Component I) {
		this.dir = false;
		this.I = I;
	}
	
	/**
	 * create a destination pin for given source pin
	 * @param link source pin
	 */
	public Pin(Pin link) {
		setLink(link);
	}

	/**
	 * links this pin to the given pin
	 * @param link pin
	 */
	public void setLink(Pin link) {
		this.dir = !link.dir;
		this.I = link.I;
		this.link = link;
		link.link = this;
	}

	@Override
	public String toString() {
		return String.format("%spin U:%d I:%d link_U:%d Owner: %s", dir ? "B" : "A", Id_U, id_I, link.Id_U, I.getClass().getSimpleName());
	}

}
