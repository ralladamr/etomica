package etomica.units;

import java.io.ObjectStreamException;

/**
 * Base for all temperature units. Internally, temperature always represents kT,
 * that is, the temperature multiplied by Boltzmann's constant; this gives a
 * quantity having dimensions of energy, and thus is in units of D-A^2/ps^2.  However,
 * temperature is treated as fundamental when defining its Dimension.
 */
public final class Temperature extends Dimension {

    public static final Dimension DIMENSION = new Temperature();
    public static final Unit SIM_UNIT = new SimpleUnit(DIMENSION, 1.0, "sim temperature units", "kB D-A^2/ps^2", Prefix.NOT_ALLOWED);

    private Temperature() {
        super("Temperature", 0, 0, 0, 0, 1, 0, 0);// LMTCtNl
    }
    
    public Unit getUnit(UnitSystem unitSystem) {
        return unitSystem.temperature();
    }

    /**
     * Required to guarantee singleton when deserializing.
     * 
     * @return the singleton DIMENSION
     */
    private Object readResolve() throws ObjectStreamException {
        return DIMENSION;
    }

    private static final long serialVersionUID = 1;

}