package electricity.components;

import java.util.function.DoubleConsumer;

import electricity.INotify;
import electricity.IResistorMergable;
import static electricity.MathUtil.*;
import static electricity.IResistorMergable.*;

public class MultiMeter extends BiPole implements INotify, IResistorMergable {

	private Unit unit;
	private final DoubleConsumer reader;
	private double x;

	public MultiMeter(Unit unit, DoubleConsumer reader) {
		this.reader = reader;
		this.unit = unit;
	}

	public void reset() {
		x = 0;
	}

	@Override
	public void setEquations(double[][] mat, int states) {
		double[] row = mat[id];
		row[id] = (unit.highImpedance ? BlockResistance : PassResistance) + Rc(A);
		row[A.Id_U] = -1.0;
		row[B.Id_U] = 1.0;
	}

	@Override
	public void update(double[] states, double dt) {
		if (unit == null) return;
		double I = states[id];
		switch(unit) {
		case Ampere: reader.accept(I); break;
		case Volt: reader.accept(I * BlockResistance); break;
		case Coulomb: reader.accept(x += I * dt); break;
		}
	}

	@Override
	public int init() {
		circuit.notifier.add(this);
		return super.init();
	}

	public enum Unit {
		Ampere(false), Volt(true), Coulomb(false);
		
		/**Whether high impedance is required for measurement */
		public final boolean highImpedance;
		
		private Unit(boolean hi) {
			highImpedance = hi;
		}
	}

	@Override
	public void updateResistor() {}

}
