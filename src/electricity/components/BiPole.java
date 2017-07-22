package electricity.components;

import electricity.Component;
import electricity.Pin;

/**
 * Base class for all electronic Components with two pins connected to {@link Junction}s.<br>
 * Internal state variable of this component is its electric current flowing from pin A to pin B.
 * @author CD4017BE
 */
public abstract class BiPole extends Component {

	public Pin A, B;

	public BiPole() {
		A = new Pin(this);
		B = new Pin(A);
	}

	@Override
	public int init() {
		if (A.I != this || B.I != this) throw new IllegalStateException("Not my own pins!\n" + toString() + "\n" + A.toString() + "\n" + B.toString());
		A.id_I = id;
		B.id_I = id;
		return 1;
	}

	@Override
	public void swapPin(Pin p) {
		if (p == A) B = A.link;
		else A = B.link;
	}

	@Override
	public String toString() {
		return String.format("%s Id:%d PinA:%d PinB:%d", getClass().getSimpleName(), id, A == null ? -1 : A.Id_U, B == null ? -1 : B.Id_U);
	}

	public Pin[] getPins() {
		return new Pin[]{A, B};
	}

}
