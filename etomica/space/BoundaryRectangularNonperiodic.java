package etomica.space;

import etomica.simulation.Simulation;

/**
 * Boundary that is not periodic in any direction.  Volume is specified,
 * but nothing about boundary enforces the dimensions on the atoms.  This effect must
 * be introduced with a potential or other construct that is made consistent
 * with the boundary.
 */
public class BoundaryRectangularNonperiodic extends BoundaryRectangular {

    /**
     * Make a boundary with unit volume.
     */
    public BoundaryRectangularNonperiodic(Simulation sim) {
        this(sim.space);
    }

    /**
     * Make a boundary with unit volume.
     */
    public BoundaryRectangularNonperiodic(Space space) {
        super(space, new boolean[space.D()], 1.0);//boolean elements will all be false
        zero = space.makeVector();
    }

    /**
     * Returns a vector with all elements zero.
     */
    public Vector centralImage(Vector r) {
        zero.E(0.0);
        return zero;
    }

    /**
     * Does nothing.
     */
    public void nearestImage(Vector dr) {
    }

    /**
     * Returns a zero-length vector.
     */
    public float[][] getOverflowShifts(Vector r, double distance) {
        return shift0;
    }

    /**
     * Returns a zero-length vector.
     */
    public double[][] imageOrigins(int nShells) {
        return origins;
    }

    private final Vector zero;
    protected final double[][] origins= new double[0][0];//cannot be static because several phases may be using at once
    private static final long serialVersionUID = 1L;
}
