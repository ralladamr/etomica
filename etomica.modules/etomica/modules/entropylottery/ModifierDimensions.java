package etomica.modules.entropylottery;

import etomica.modifier.Modifier;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.units.Dimension;
import etomica.units.Length;

public class ModifierDimensions implements Modifier {

    public ModifierDimensions(IBox box) {
        this.box = box;
    }

    public Dimension getDimension() {
        return Length.DIMENSION;
    }

    public String getLabel() {
        return "Dimensions";
    }

    public double getValue() {
        return box.getBoundary().getDimensions().x(0);
    }

    public void setValue(double newValue) {
        if (newValue <= 0 || newValue > 1000) {
            throw new IllegalArgumentException("Bogus value for dimension");
        }
        IVector dim = box.getBoundary().getDimensions();
        dim.setX(0, newValue);
        box.getBoundary().setDimensions(dim);
    }

    private final IBox box;
}
