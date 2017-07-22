package electricity.components;

import electricity.INotify;

public class Inductor extends CurrentSource implements INotify {

	private final double L;
	private double E;

	public Inductor(double I0, double L) {
		super(I0);
		this.L = L;
		this.E = 0.5 * I0 * I0 * L;
	}

	@Override
	public void update(double[] states, double dt) {
		double dU = (states[B.Id_U] - states[A.Id_U]) * dt;
		E -= dU * I;
		dU /= L * 2.0;
		I = (E <= 0 ? 0 : Math.copySign(Math.sqrt(2.0 * E / L), I - dU)) - dU;
		circuit.setValue(cid, I);
	}

	@Override
	public int init() {
		circuit.notifier.add(this);
		return super.init();
	}

	@Override
	public String toString() {
		return super.toString() + String.format(" L=%.3gH", L);
	}

}
