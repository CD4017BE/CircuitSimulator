package electricity.components;

import electricity.Component;
import electricity.INotify;
import electricity.IResistorMergable;
import electricity.Pin;
import static electricity.MathUtil.*;
import static electricity.IResistorMergable.*;

/**
 * Implements a bipolar transistor with the following pin usage:<br>
 * For NPN-transistors E2 is emitter, C2 is collector and E1 & C1 are base.<br>
 * For PNP-transistors E1 is emitter, C1 is collector and E2 & C2 are base.<br>
 * Where the two base pins must always share the same Junction.
 * @author CD4017BE
 */
public class Transistor extends Component implements INotify, IResistorMergable {

	public static final double PassPotential = 0.7;
	protected int swId1, swId2, cst, idC;
	/**1: passBE, 2: passBC */
	protected byte state;
	public Pin E1, E2, C1, C2;
	private final double Xn_, Xi_;

	/**
	 * Creates a new Transistor instance with given amplification factors
	 * @param Xn normal current amplification factor (Xn > 0)
	 * @param Xi inverse current amplification factor (Xi > 0, usually Xi << Xn)
	 */
	public Transistor(double Xn, double Xi) {
		E1 = new Pin(this);
		E2 = new Pin(E1);
		C1 = new Pin(this);
		C2 = new Pin(C1);
		this.Xn_ = 1.0 - 1.0 / Xn;
		this.Xi_ = 1.0 - 1.0 / Xi;
	}

	@Override
	public int init() {
		this.idC = id + 1;
		this.swId1 = circuit.nextSwitch();
		this.swId2 = circuit.nextSwitch();
		this.cst = circuit.getConstant();
		circuit.setSwitch(swId1, (state & 1) != 0);
		circuit.setSwitch(swId2, (state & 2) != 0);
		circuit.preNotifier.add(this);
		if (E1.I != this || E2.I != this || C1.I != this || C2.I != this)
			throw new IllegalStateException("Not my own pins!\n" + toString() + "\n" + E1.toString() + "\n" + E2.toString() + "\n" + C1.toString() + "\n" + C2.toString());
		E1.id_I = id;
		E2.id_I = id;
		C1.id_I = idC;
		C2.id_I = idC;
		return 2;
	}

	@Override
	public void setEquations(double[][] mat, int states) {
		double[] rowE = mat[id], rowC = mat[idC];
		int icst = cst + states;
		double R = PassResistance;
		switch (state) {
		case 1: //amplification
			// Ube - Ud = Ibe * (Rpass + Re)
			rowE[E1.Id_U] = 1;
			rowE[E2.Id_U] = -1;
			rowE[id] = -R - Rc(E1);
			rowE[icst] = PassPotential;
			// -Ibc = Xn * (Ibe + Ibc)
			rowC[id] = Xn_;
			rowC[idC] = 1;
			break;
		case 2: //inverse amplification
			// -Ibe = Xi * (Ibe + Ibc)
			rowE[id] = 1;
			rowE[idC] = Xi_;
			// Ubc - Ud = Ibc * (Rpass + Rc)
			rowC[C1.Id_U] = 1;
			rowC[C2.Id_U] = -1;
			rowC[idC] = -R - Rc(C1);
			rowC[icst] = PassPotential;
			break;
		case 0: //blocking
			R = BlockResistance;
		default: //saturation
			// Ube - Ud = Ibe * (R + Re)
			rowE[E1.Id_U] = 1;
			rowE[E2.Id_U] = -1;
			rowE[id] = -(R + Rc(E1));
			rowE[icst] = PassPotential;
			// Ubc - Ud = Ibc * (R + Rc)
			rowC[C1.Id_U] = 1;
			rowC[C2.Id_U] = -1;
			rowC[idC] = -(R + Rc(C1));
			rowC[icst] = PassPotential;
		}
	}

	@Override
	public void update(double[] states, double dt) {
		if (circuit.skipTick > 0) return;
		double Ibe = states[id], Ibc = states[idC];
		switch(state) {
		case 0:
			if (Ibe > 0) state |= 1;
			if (Ibc > 0) state |= 2;
			if (state == 1 || state == 2) circuit.skipTick = 1;
			break;
		case 1:
			if (Ibe < 0) state &= ~1;
			else if (states[C1.Id_U] - states[C2.Id_U] - Ibc * (PassResistance + Rc(C1)) > PassPotential) state |= 2;
			break;
		case 2:
			if (Ibc < 0) state &= ~2;
			else if (states[E1.Id_U] - states[E2.Id_U] - Ibe * (PassResistance + Rc(E1)) > PassPotential) state |= 1;
			break;
		default:
			if (Ibe < 0 && Ibc * Xi_ + Ibe < 0) state &= ~1;
			if (Ibc < 0 && Ibe * Xn_ + Ibc < 0) state &= ~2;
		}
		circuit.setSwitch(swId1, (state & 1) != 0);
		circuit.setSwitch(swId2, (state & 2) != 0);
	}

	@Override
	public Pin[] getPins() {
		return new Pin[]{E1, C1, E2, C2};
	}

	@Override
	public void swapPin(Pin p) {
		if (p == E1) E2 = E1.link;
		else if (p == E2) E1 = E2.link;
		else if (p == C1) C2 = C1.link;
		else C1 = C2.link;
	}

	@Override
	public void updateResistor() {}

}
