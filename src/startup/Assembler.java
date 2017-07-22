package startup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import electricity.Circuit;
import electricity.Pin;
import electricity.components.BiPole;
import electricity.components.Capacitor;
import electricity.components.CurrentSource;
import electricity.components.Diode;
import electricity.components.Inductor;
import electricity.components.MultiMeter;
import electricity.components.MultiMeter.Unit;
import electricity.components.Resistor;
import electricity.components.Transistor;
import electricity.components.VoltageSource;
import electricity.components.WorkResistor;

public class Assembler {

	public static void main(String[] args) {
		File file;
		if (args.length == 0) file = new File("./circuit.txt");
		else file = new File(args[0]);
		try {
			Assembler.run(file);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	public static void run(File file) throws NumberFormatException, IOException {
		System.out.print("Loading circuit: ");
		long t = System.nanoTime();
		HashMap<String, double[]> settings = new HashMap<String, double[]>();
		int cycles = 1000, precision = 512;
		double dt = 0.05;
		double[][] buffer = new double[3][];
		double[][] cfg = new double[3][];
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			Pattern p = Pattern.compile("([WRLCUIMXTt]\\w)\\[([e\\.\\,\\d\\s\\-]+)\\]");
			String line;
			while((line = reader.readLine()) != null) {
				Matcher m = p.matcher(line);
				while(m.find()) {
					String[] val = m.group(2).split(",");
					double[] data = new double[val.length];
					for (int i = 0; i < data.length; i++)
						data[i] = Double.parseDouble(val[i].trim());
					settings.put(m.group(1), data);
				}
				if (line.indexOf(';') >= 0) {
					line = reader.readLine();
					break;
				}
			}
			
			{double[] data = settings.get("XY");
			if (data != null) {
				precision = (int)data[1];
				dt = data[2];
				cycles = (int)(data[0] / dt);
			}}
			
			HashMap<Pos, Module> modules = new HashMap<Pos, Module>();
			ArrayList<IInit> toInit = new ArrayList<IInit>();
			for (int y = 0; line != null; line = reader.readLine(), y++) {
				char[] cl = line.toCharArray();
				for (int x = 0; x < cl.length; x++) {
					char c = cl[x];
					Module m;
					switch(c) {
					case '+': m = new CrossJunction(); break;
					case '-': m = new OrientedBipole(new Resistor(R_cable), Side.left, Side.right); break;
					case '|': m = new OrientedBipole(new Resistor(R_cable), Side.down, Side.up); break;
					case '/': {
						Module m1 = modules.get(new Pos(x, y - 1));
						if (m1 == null || !m1.canConnect(Side.up))
							m = new OrientedBipole(new Resistor(R_cable), Side.right, Side.down);
						else
							m = new OrientedBipole(new Resistor(R_cable), Side.left, Side.up);
					} break;
					case '\\': {
						Module m1 = modules.get(new Pos(x, y - 1));
						if (m1 == null || !m1.canConnect(Side.up))
							m = new OrientedBipole(new Resistor(R_cable), Side.left, Side.down);
						else
							m = new OrientedBipole(new Resistor(R_cable), Side.right, Side.up);
					} break;
					case 't': case 'T': {
						double[] data = settings.get("" + c + cl[x + 1]);
						Module m1 = modules.get(new Pos(x - 1, y));
						Side b = m1 != null && m1.canConnect(Side.right) ? Side.left : Side.right;
						m = new TransistorComp(b, c == 'T', data[0], data[1]);
						modules.put(new Pos(x + 1, y), m);
					} break;
					case '*': m = new AnyJunction(); break;
					case '>': m = new OrientedBipole(new Diode(), Side.left, Side.right); break;
					case '<': m = new OrientedBipole(new Diode(), Side.right, Side.left); break;
					case '^': m = new OrientedBipole(new Diode(), Side.down, Side.up); break;
					case 'V': m = new OrientedBipole(new Diode(), Side.up, Side.down); break;
					case 'R': {
						double[] data = settings.get("" + c + cl[x + 1]);
						m = new NonOrBipole(new Resistor(data[0]));
						modules.put(new Pos(x + 1, y), m);
					} break;
					case 'W': {
						double[] data = settings.get("" + c + cl[x + 1]);
						m = new NonOrBipole(new WorkResistor(data[0], data[1]));
						modules.put(new Pos(x + 1, y), m);
					} break;
					case 'C': {
						double[] data = settings.get("" + c + cl[x + 1]);
						m = new NonOrBipole(new Capacitor(data[0], data[1]));
						modules.put(new Pos(x + 1, y), m);
					} break;
					case 'L': {
						double[] data = settings.get("" + c + cl[x + 1]);
						m = new NonOrBipole(new Inductor(data[0], data[1]));
						modules.put(new Pos(x + 1, y), m);
					} break;
					case 'U': {
						double[] data = settings.get("" + c + cl[x + 1]);
						m = new NonOrBipole(new VoltageSource(data[0]));
						modules.put(new Pos(x + 1, y), m);
					} break;
					case 'I': {
						double[] data = settings.get("" + c + cl[x + 1]);
						m = new NonOrBipole(new CurrentSource(data[0]));
						modules.put(new Pos(x + 1, y), m);
					} break;
					case 'M': {
						char cc = cl[x + 1];
						int i = cc == 'r' ? 0 : cc == 'g' ? 1 : 2;
						double[] data = settings.get("" + c + cc);
						final double[] b = new double[cycles];
						buffer[i] = b;
						cfg[i] = data;
						m = new NonOrBipole(new MultiMeter(Unit.values()[(int)data[0]], new Measure(b)));
						modules.put(new Pos(x + 1, y), m);
					} break;
					default: continue;
					}
					modules.put(new Pos(x, y), m);
					if (y > 0 && m.canConnect(Side.up)) {
						Module m2 = modules.get(new Pos(x, y - 1));
						if (m2 != null && m2.canConnect(Side.down))
							Circuit.connectPins(m.getPin(Side.up), m2.getPin(Side.down));
					}
					if (x > 0 && m.canConnect(Side.left)) {
						Module m2 = modules.get(new Pos(x - 1, y));
						if (m2 != null && m2.canConnect(Side.right))
							Circuit.connectPins(m.getPin(Side.left), m2.getPin(Side.right));
					}
					if (m instanceof IInit) toInit.add((IInit)m);
				}
			}
			reader.close();
			for (IInit init : toInit)
				init.init();
			
			t = System.nanoTime() - t;
			System.out.printf("%.3f ms\n> %d Tiles, %d Circuits, %d Measures\nSimulation: ", (double)t * 1e-6D, modules.size(), Circuit.circuits.size(), buffer.length);
		}
		t = System.nanoTime();
		
		for (int i = 0; i < cycles; i++)
			Circuit.simulate(dt);
		
		t = System.nanoTime() - t;
		System.out.printf("%.3f ms for %d cycles\n", (double)t * 1e-6D, cycles);
		for (Circuit circuit : Circuit.circuits)
			System.out.printf(" - %d Nodes, %d Parameters, %d Switches, %d Listeners\n", circuit.nodes(), circuit.parameters(), circuit.switches(), circuit.listener());
		System.out.printf("Draw graph: ");
		t = System.nanoTime();
		
		Graph graph = new Graph(cycles, precision);
		for (int i = 0; i < 3; i++)
			if (buffer[i] != null) {
				double[] c = cfg[i];
				graph.draw(i, buffer[i], c.length > 1 ? c[1] : Double.NaN, c.length > 2 ? c[2] : Double.NaN);
			}
		
		t = System.nanoTime() - t;
		System.out.printf("%.3f ms\nSaving image: ", (double)t * 1e-6D);
		t = System.nanoTime();
		
		graph.save(new File(file.getAbsolutePath().replace(".txt", ".png")));
		
		t = System.nanoTime() - t;
		System.out.printf("%.3f ms\ndone!", (double)t * 1e-6D);
	}

	static final double R_cable = 0.0001;
	
	static class Pos {
		int x, y;
		Pos(int x, int y) {this.x = x; this.y = y;}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Pos other = (Pos) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}
	}

	static interface Module {
		Pin getPin(Side side);
		boolean canConnect(Side side);
	}

	static interface IInit {
		void init();
	}

	static class CrossJunction implements Module {
		final Resistor R_lr = new Resistor(R_cable), R_du = new Resistor(R_cable);
		@Override
		public Pin getPin(Side side) {
			switch(side) {
			case left: return R_lr.A;
			case right: return R_lr.B;
			case up: return R_du.B;
			default: return R_du.A;
			}
		}
		@Override
		public boolean canConnect(Side side) {
			return true;
		}
	}

	static class OrientedBipole implements Module {
		final BiPole C;
		final Side A, B;
		OrientedBipole(BiPole comp, Side A, Side B) {
			this.A = A; this.B = B; this.C = comp;
		}
		@Override
		public Pin getPin(Side side) {
			if (side == A) return C.A;
			if (side == B) return C.B;
			return null;
		}
		@Override
		public boolean canConnect(Side side) {
			return side == A || side == B; 
		}
	}

	static class NonOrBipole implements Module {
		final BiPole C;
		Side A = null, B = null;
		NonOrBipole(BiPole comp) {
			this.C = comp;
		}
		@Override
		public Pin getPin(Side side) {
			if (A == null)
				switch(side) {
				case left: case right:
					A = Side.left; B = Side.right;
					break;
				case up: case down:
					A = Side.down; B = Side.up;
					break;
				}
			if (side == A) return C.A;
			if (side == B) return C.B;
			return null;
		}
		@Override
		public boolean canConnect(Side side) {
			return A == null || side == A || side == B; 
		}
	}

	static class AnyJunction implements Module, IInit {
		final Resistor[] Rs = new Resistor[4];
		int cons = 0;
		@Override
		public Pin getPin(Side side) {
			Resistor R = Rs[side.ordinal()];
			if (R == null) {
				Rs[side.ordinal()] = R = new Resistor(R_cable / 2.0);
				cons++;
			}
			return R.A;
		}
		@Override
		public boolean canConnect(Side side) {
			return true;
		}
		@Override
		public void init() {
			Pin[] pins = new Pin[cons];
			int j = 0;
			for (Resistor R : Rs)
				if (R != null) pins[j++] = R.B;
			Circuit.connectPins(pins);
		}
	}

	static class TransistorComp implements Module {

		final Side base;
		final Transistor T;
		final Resistor R;
		final boolean npn;

		TransistorComp(Side base, boolean npn, double Xn, double Xi) {
			this.base = base;
			this.npn = npn;
			T = new Transistor(Xn, Xi);
			R = new Resistor(R_cable);
			if (npn) Circuit.connectPins(R.B, T.E1, T.C1);
			else Circuit.connectPins(R.B, T.E2, T.C2);
		}

		@Override
		public Pin getPin(Side side) {
			if (side == Side.down) return npn ? T.E2 : T.C1;
			if (side == Side.up) return npn ? T.C2 : T.E1;
			if (side == base) return R.A;
			else return null;
		}

		@Override
		public boolean canConnect(Side side) {
			return side == Side.down || side == Side.up || side == base;
		}

	}
	
	enum Side {
		left, right, up, down;
	}

	static class Measure implements DoubleConsumer {
		final double[] data;
		int t = 0;

		Measure(double[] data) {
			this.data = data;
		}
		
		@Override
		public void accept(double value) {
			data[t++] = value;
		}

	}

}
