package etomica.chem.elements;

/**
 * Class for the Argon element.
 *
 * @author Andrew
 */
public class Argon extends ElementChemical {

	protected Argon(String symbol) {
        this(symbol, 39.950);
    }
    
    protected Argon(String symbol, double mass) {
        super(symbol, mass, 18);
    }

    public static final Argon INSTANCE = new Argon("Ar");
    
	private static final long serialVersionUID = 1L;
}
