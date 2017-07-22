package electricity.components;

import electricity.INotify;

public class Capacitor extends VoltageSource implements INotify {

	private final double C;
	private double E;

	public Capacitor(double U0, double C) {
		super(U0);
		this.C = C;
		this.E = U0 * U0 * C / 2;
	}

	@Override
	public void update(double[] states, double dt) {
		double dQ = states[id] * dt;
		E -= dQ * U;
		dQ /= C * 2.0;
		U = (E <= 0 ? 0 : Math.copySign(Math.sqrt(2.0 * E / C), U - dQ)) - dQ;
		circuit.setValue(cid, U);
	}

	@Override
	public int init() {
		circuit.notifier.add(this);
		return super.init();
	}

	@Override
	public String toString() {
		return super.toString() + String.format(" C=%.3gF", C);
	}

}
