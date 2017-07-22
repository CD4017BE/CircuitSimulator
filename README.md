# CircuitSimulator
attempt to simulate the real behavior of electronic circuits

The programm reads an "ascii circuit plan" from a text file, simulates its behavior (voltages, currents, etc.) and draws a diagram which displays how measured voltages or currents developed over time.
The algorithm is based on linear equations.

This is mostly a just for fun project but maybe one can do useful things with this.

## How to run
Exceute the java programm (from console) with the file path of the circuit plan as first program argument.
Like so: `java -jar CircuitSimulator.jar path/to/my/circuit.txt`.

It will then write some profiling data to the console and finally create a PNG image file showing the graph in the same directory where the text file was. (Warning: depending on simulation settings the image can get extremely wide and at about 1000000 pixels usual image programs can't show it anymore)

## Text File Format
The first part declares all the properties of the electronic components to use, and how to simulate and draw the graph.

Each entry starts with a two character identifier of the declared object and is followed by `[`, then a list of numbers as parameters separated with `,` and finally the close bracket `]`. Everything that doesn't match this pattern is ignored so there can be whitespace, newlines or description text between individual declarations.

**Identifiers:**
*  simulation settings `XY[`simulation time in seconds`, `graph image height in pixels`, `time interval of each "tick" in seconds`]` (each simulation tick will be one pixel on image)
*  measurement device `M_[`type`]`, typeID = {0: current(Ampere), 1: voltage(Volt), 2:charge(Coulomb)} where `_` can be r, g or b corresponding to red, green and blue channel in the graph
*  Voltage Source `U_[`voltage(V)`]`
*  Current Source `I_[`current(A)`]`
*  Resistor `R_[`resistance(Ohm)`]`
*  Capacitor `C_[`init.voltage(V)`,`capacity(F)`]`
*  Inductor `L_[`init.current(A)`,`inductivity(H)`]`
*  NPN-Transistor `T_[`forward amplifier`,`reverse amplifier`]` (Base-Emitter voltage is fixed to 0.7V)
*  PNP-Transistor `t_[`forward amplifier`,`reverse amplifier`]` (Base-Emitter voltage is fixed to 0.7V)
*  "Consumer"-Resistor that randomly turns on and off `W_[`resistance(Ohm)`,`average interval(s)`]`
for electronic components the `_` can be any character, it's just used to distinguish between different components of same type

The actual circuit plan starts the next line after the first occurence of the `;` character and it is drawn in ascii-art:
*  `-` horizontal connection
*  `|` vertical connection
*  `+` crossing cables
*  `*` cable junction
*  `/` left-up or down-right curve connection
*  `\` left-down or up-right curve connection
*  `>` Diode passing left to right
*  `<` Diode passing right to left
*  `^` Diode passing down to up
*  `V` Diode passing up to down
*  `M_`,`U_`,`I_`,`R_`,`C_`,`L_`,`W_` electronic components like declared above that will connect on two opposite sides of left, top, bottom of first character or right side of second character. (Current/Voltage flow will be left to right or down to up)
*  `T_` NPN-Transistor with connecting Base left or right, Collector up and Emitter pin down (first character only)
*  `t_` PNP-Transistor with connecting Base left or right, Collector down and Emitter pin up (first character only)

## Bugs
can usually be fixed by just increasing simulation tick rate (= reducing tick interval).
And capacitors sometimes behave a little bit inaccurate when switching from positive to negative charge or vice versa.

And generally there is no warranty that the electronics wouldn't get on fire or explode when building the circuit in reality :)