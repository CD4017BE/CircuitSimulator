package electricity.components;

import java.util.Random;

import electricity.INotify;
import electricity.IResistorMergable;
import static electricity.MathUtil.*;
import static electricity.IResistorMergable.*;

public class WorkResistor extends BiPole implements INotify, IResistorMergable {
	private final Random rand;
	private final double Rwork;
	private final float T;
	private boolean running;
	private int swId;
	
	public WorkResistor(double R, double T_2) {
		this.T = (float)T_2;
		this.Rwork = R;
		this.running = false;
		this.rand = new Random();
	}
	
	@Override
	public void setEquations(double[][] mat, int states) {
		double[] row = mat[id];
		row[id] = (running ? Rwork : BlockResistance) + Rc(A);
		row[A.Id_U] = -1.0;
		row[B.Id_U] = 1.0;
	}

	@Override
	public void update(double[] states, double dt) {
		if (rand.nextFloat() < (float)dt / T)
			circuit.setSwitch(swId, running = !running);
	}

	@Override
	public int init() {
		this.swId = circuit.nextSwitch();
		circuit.notifier.add(this);
		return super.init();
	}

	@Override
	public void updateResistor() {}

}
