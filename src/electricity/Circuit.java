package electricity;

import static electricity.MathUtil.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import electricity.components.BiPole;
import electricity.components.Ground;
import electricity.components.Junction;
import electricity.components.Resistor;

public class Circuit {

	private Collection<Component> components;
	private ArrayList<Parameter> parameters;
	public ArrayList<INotify> notifier, preNotifier;
	private BitSet switchStates;
	private HashMap<BitSet, double[][]> matrixCache;
	private int states, switches, topolIdx;
	private double[][] matrix;
	private double[] values, result;
	private Constant constant;
	private boolean updateMatrix, updateValues, needsSetup;
	public byte skipTick;

	private Circuit() {
		components = new HashSet<Component>();
		circuits.add(this);
	}

	private void addComp(Component comp) {
		if (comp.circuit != this) {
			comp.circuit = this;
			components.add(comp);
			needsSetup = true;
			if (comp instanceof Junction)
				topolIdx += ((Junction)comp).pins.length - 2;
		}
	}

	private void removeComp(Component comp) {
		if (comp.circuit == this) {
			comp.circuit = null;
			components.remove(comp);
			needsSetup = true;
			if (comp instanceof Junction)
				topolIdx -= ((Junction)comp).pins.length - 2;
		}
	}

	public int nodes() {return states;}
	public int parameters() {return parameters.size();}
	public int switches() {return switches;}
	public int listener() {return notifier.size() + preNotifier.size();}

	public int getConstant() {
		if (constant == null) {
			constant = new Constant();
			constant.id = addPar(constant);
		}
		return constant.id;
	}

	public int addPar(Parameter par) {
		parameters.add(par);
		return parameters.size() - 1;
	}

	public void setValue(int i, double val) {
		if (values[i] != val) {
			values[i] = val;
			updateValues = true;
		}
	}

	public double getValue(int i) {
		return values[i];
	}

	public int nextSwitch() {
		return switches++;
	}

	public void setSwitch(int i, boolean state) {
		if (switchStates.get(i) ^ state) {
			switchStates.flip(i);
			updateMatrix = true;
		}
	}

	public boolean getSwitch(int i) {
		return switchStates.get(i);
	}

	private void setup() {
		states = 0;
		switches = 0;
		if (switchStates != null) switchStates.clear();
		else switchStates = new BitSet();
		if (matrixCache != null) matrixCache.clear();
		else matrixCache = new HashMap<BitSet, double[][]>();
		if (preNotifier != null) preNotifier.clear();
		else preNotifier = new ArrayList<INotify>();
		if (notifier != null) notifier.clear();
		else notifier = new ArrayList<INotify>();
		if (parameters != null) parameters.clear();
		else parameters = new ArrayList<Parameter>();
		constant = null;
		Ground ground = null;
		for (Iterator<Component> it = components.iterator(); it.hasNext();) {
			Component comp = it.next();
			if (ground == null && comp instanceof Junction) {
				comp = ground = new Ground(((Junction)comp).pins);
				comp.circuit = this;
				it.remove();
			}
			comp.id = states;
			states += comp.init();
		}
		if (ground != null) components.add(ground);
		values = new double[parameters.size()];
		result = new double[states];
		for (Parameter par : parameters) par.setValue(values);
		needsSetup = false;
		updateMatrix = true;
	}

	public void update(double dt) {
		if(topolIdx < 0) return;//No closed circuits -> No current flow -> nothing to simulate
		if((topolIdx & 1) != 0) throw test();//topological index can only be uneven if there are unconnected pins
		if(needsSetup) setup();
		updateData();
		for (INotify n : preNotifier) n.update(result, dt);
		if (skipTick > 0) {
			skipTick = -1;
			return;
		} else skipTick = 0;
		updateData();
		for (INotify n : notifier) n.update(result, dt);
	}

	private void updateData() {
		if (updateMatrix) {
			matrix = matrixCache.get(switchStates);
			if (matrix == null) {
				double[][] mat = new double[states][states + parameters.size()];
				for (Component comp : components) comp.setEquations(mat, states);
				solveMatrix(mat);
				matrix = new double[states][parameters.size()];
				for (int i = 0; i < states; i++)
					System.arraycopy(mat[i], states, matrix[i], 0, parameters.size());
				matrixCache.put((BitSet)switchStates.clone(), matrix);
			}
			updateMatrix = false;
			updateValues = true;
		}
		if (updateValues) {
			for (int i = 0; i < states; i++) {
				double[] row = matrix[i];
				double x = 0;
				for (int j = 0; j < values.length; j++)
					x += row[j] * values[j];
				result[i] = x;
			}
		}
	}

	private Circuit mergeCircuit(Circuit circuit) {
		if (circuit == this) return this;
		if (circuit.components.size() > this.components.size()) {
			for (Component c : components) c.circuit = circuit;
			circuit.components.addAll(components);
			circuit.topolIdx += topolIdx;
			circuits.remove(this);
			return circuit;
		}
		for (Component c : circuit.components) c.circuit = this;
		components.addAll(circuit.components);
		topolIdx += circuit.topolIdx;
		circuits.remove(circuit);
		return this;
	}

	/**
	 * checks all connections to detect if the circuit was split into multiple pieces
	 */
	private void scan() {
		if (components.isEmpty()) return;
		HashSet<Component> scanned = new HashSet<Component>();
		ArrayDeque<Pin> tocheck = new ArrayDeque<Pin>();
		while (true) {
			{
				Component c = components.iterator().next();
				Junction j;
				if (c instanceof Junction) j = (Junction)c;
				else if (c instanceof BiPole) j = ((BiPole)c).A.U;
				else j = c.getPins()[0].U;
				scanned.add(j);
				for (Pin p : j.pins) tocheck.add(p);
			}
			while(!tocheck.isEmpty()) {
				Pin p = tocheck.pop();
				scanned.add(p.I);
				if (p.I instanceof BiPole) {
					p = p.link;
					Junction j = p.U;
					if (scanned.add(j))
						for (Pin pin : j.pins)
							if (pin != p)
								tocheck.add(pin);
				} else for (Pin p2 : p.I.getPins()) {
					if (p2 == p) continue;
					Junction j = p2.U;
					if (scanned.add(j))
						for (Pin pin : j.pins)
							if (pin != p2)
								tocheck.add(pin);
				}
			}
			if (scanned.size() < components.size()) {
				Circuit circuit = new Circuit();
				for (Component c : scanned) {
					removeComp(c);
					circuit.addComp(c);
				}
			} else return;
			scanned.clear();
		}
	}

	public IllegalStateException test() {
		String log = "";
		BitSet ids = new BitSet();
		int tidx = 0;
		for (Component c : components) {
			if (c instanceof Junction) {
				Junction j = (Junction)c;
				tidx += j.pins.length - 2;
				//if (ids.get(j.id)) log += String.format("ERROR duplicate id:\n %s\n", j);
				ids.set(j.id);
				for (Pin p : j.pins) {
					if (p.U != j || p.Id_U != j.id) log += String.format("ERROR Pin incorrectly connected:\n %s\n %s\n", j, p);
					if (!components.contains(p.I)) log += String.format("ERROR connected component not in circuit:\n %s\n", p.I);
				}
			} else if (c instanceof BiPole) {
				BiPole b = (BiPole)c;
				//if (ids.get(b.id)) log += String.format("ERROR duplicate id: %s\n", b);
				ids.set(b.id);
				if (b.A.link != b.B || b.B.link != b.A) log += String.format("ERROR Pins not paired:\n %s\n %s\n", b.A, b.B);
				if (b.A.I != b || b.A.id_I != b.id) log += String.format("ERROR Pin not linked to component:\n %s\n %s\n", b, b.A);
				if (b.B.I != b || b.B.id_I != b.id) log += String.format("ERROR Pin not linked to component:\n %s\n %s\n", b, b.B);
				if (!components.contains(b.A.U)) log += String.format("ERROR connected junction not in circuit:\n %s\n %s\n", b.A, b.A.U);
				if (!components.contains(b.B.U)) log += String.format("ERROR connected junction not in circuit:\n %s\n %s\n", b.B, b.B.U);
			} else System.err.printf("ERROR unknown component: %s\n", c);
		}
		if (ids.cardinality() != components.size()) log += "ERROR missing ids\n";
		if (tidx != topolIdx) log += String.format("ERROR topologic index is %d but should be %d\n", topolIdx, tidx);
		return log.isEmpty() ? null : new IllegalStateException("Something with the connection system gone wrong:\n" + log);
	}

	private static class Constant implements Parameter {
		int id;
		@Override
		public void setValue(double[] vec) {
			vec[id] = 1;
		}
	}

	//-------- The global handling part --------

	/**All circuit instances to be simulated */
	public static Collection<Circuit> circuits = new ArrayList<Circuit>();
	private static Set<Circuit> needRescan = new HashSet<Circuit>();

	public static void simulate(double dt) {
		if (!needRescan.isEmpty()) {
			for (Circuit c : needRescan) c.scan();
			needRescan.clear();
		}
		for(Circuit c : circuits) c.update(dt);
	}

	/**
	 * Connects the given pins with each other and also puts their components on a common circuit instance which is then accessible via the {@code circuit} field in {@link Component}.<br>
	 * This is also the way how Electric components should be initially registered for simulation.
	 * @param pins the pins to connect
	 */
	public static void connectPins(Pin... pins) {
		if (pins.length == 2) {
			Pin A = pins[0], B = pins[1];
			if (A.I instanceof Resistor && B.I instanceof IResistorMergable) {
				mergeResistor((IResistorMergable)B.I, B, (Resistor)A.I, A);
				return;
			} else if (A.I instanceof IResistorMergable && B.I instanceof Resistor) {
				mergeResistor((IResistorMergable)A.I, A, (Resistor)B.I, B);
				return;
			}
		}
		Circuit circuit = null;
		for (Pin p : pins)
			if (p.I.circuit != null)
				if (circuit == null) circuit = p.I.circuit;
				else circuit = circuit.mergeCircuit(p.I.circuit);
		if (circuit == null) circuit = new Circuit();
		for (Pin p : pins) circuit.addComp(p.I);
		circuit.addComp(new Junction(pins));
	}

	/**
	 * Replaces the pins connected via the given Junction with a new set of pins.<br>
	 * The old Junction is always removed and if the given set of new pins is not empty, they create a new Junction.
	 * @param j Junction to remove
	 * @param pins new pins to connect instead
	 */
	public static void reconnectPins(Junction j, Pin... pins) {
		Circuit circuit = j.circuit;
		circuit.removeComp(j);
		if (pins.length > 0) connectPins(pins);
		boolean rescan = false;
		for (Pin p : j.pins)
			if (p.U == j) {
				p.U = null;
				rescan |= p.I.circuit == circuit;
			}
		if (circuit.components.isEmpty()) circuits.remove(circuit);
		else if (rescan) needRescan.add(circuit);
	}

	/**
	 * splits a combined Resistor to insert additional connections
	 * @param pin shared Pin to split
	 * @param idx position at which to split (close to master)
	 * @param addPins pins to add to the connection, index 0 and 1 should be null to be filled with the newly created pins
	 * @throws ArrayIndexOutOfBoundsException if addPins.length < 2
	 */
	public static void insertJunction(CombinedPin pin, int idx, Pin... addPins) { 
		Component c = pin.I;
		Circuit circuit = c.circuit;
		if (idx == 0) {
			Resistor r = pin.content.remove(idx);
			pin.R -= r.R;
			pin.I = r;
			addPins[1] = new Pin(pin.link);
			addPins[0] = new Pin(pin);
			r.swapPin(pin);
			r.updateResistor();
			circuit.addComp(r);
			c.swapPin(addPins[1].link);
			((IResistorMergable)c).updateResistor();
		} else {
			int i = pin.content.size() - 1;
			Resistor r = pin.content.remove(i);
			pin.R -= r.R;
			boolean b = r.A == pin;
			r.A = new Pin(r);
			r.B = new Pin(r.A);
			Pin p2 = b ? r.A : r.B;
			p2.U = pin.U; pin.U = null;
			circuit.addComp(r);
			p2 = p2.link;
			if (idx < i) {
				r.swapPin(p2 = new CombinedPin(p2));
				for (--i; i >= idx; i--) {
					Resistor r2 = pin.content.remove(i); 
					pin.R -= r2.R;
					((CombinedPin)p2).addResistor(r2, r2.A == pin ? r2.A : r2.B);
				}
			}
			r.updateResistor();
			((IResistorMergable)pin.I).updateResistor();
			addPins[0] = pin;
			addPins[1] = p2;
		}
		connectPins(addPins);
	}

	/**
	 * unregister the given component and remove it from its circuit.<br>
	 * When a component should be completely removed then also {@code reconnectPins(pin);} must be called for all its pins.
	 * @param c
	 */
	public static void removeComponent(Component c) {
		Circuit circuit = c.circuit;
		if (c instanceof IResistorMergable) {
			for (Pin p : c.getPins())
				if (p instanceof CombinedPin) {
					CombinedPin pin = (CombinedPin)p;
					if (pin.I == c) {
						Resistor r = pin.content.remove(0);
						pin.R -= r.R;
						pin.I = r;
						circuit.addComp(new Junction(new Pin(pin)));
						r.swapPin(pin);
						r.updateResistor();
						circuit.addComp(r);
					} else {
						circuit = pin.I.circuit;
						int i = pin.content.size() - 1;
						Resistor r = pin.content.remove(i);
						pin.R -= r.R;
						if (r == c) {
							circuit.addComp(new Junction(pin));
							break;
						}
						boolean b = r.A == pin;
						r.A = new Pin(r);
						r.B = new Pin(r.A);
						Pin p2 = b ? r.A : r.B;
						p2.U = pin.U;
						circuit.addComp(new Junction(pin));
						circuit.addComp(r);
						p2 = p2.link;
						for (--i; i >= 0; i--) {
							Resistor r2 = pin.content.remove(i);
							pin.R -= r2.R;
							if (r2 == c) break;
							if (!(p2 instanceof CombinedPin)) r.swapPin(p2 = new CombinedPin(p2));
							((CombinedPin)p2).addResistor(r2, r2.A == pin ? r2.A : r2.B);
						}
						circuit.addComp(new Junction(p2));
						r.updateResistor();
						((IResistorMergable)pin.I).updateResistor();
					}
				} else if (p != null && p.U.pins.length == 1)
					circuit.removeComp(p.U);
		}
		if (circuit != null) {
			c.circuit.removeComp(c);
			if (circuit.components.isEmpty()) circuits.remove(circuit);
			else needRescan.add(circuit);
		}
	}

	/**
	 * merges the resistor into the mergable component
	 * @param rm mergable component
	 * @param A pin of rm to connect with B
	 * @param r the resistor to merge into rm
	 * @param B pin of r to connect with A
	 */
	private static void mergeResistor(IResistorMergable rm, Pin A, Resistor r, Pin B) {
		if (A instanceof CombinedPin) ((CombinedPin)A).merge(B);
		else A = new CombinedPin(A, B);
		Component c = (Component)rm;
		c.swapPin(A.link);
		rm.updateResistor();
		if (r.circuit != null) {
			if (c.circuit == null) r.circuit.addComp(c);
			else c.circuit.mergeCircuit(r.circuit);
			r.circuit.removeComp(r);
		} else if (c.circuit == null)
			new Circuit().addComp(c);
	}

}
