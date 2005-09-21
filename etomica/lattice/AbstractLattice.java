package etomica.lattice;

/**
 * Interface for a generic lattice, which is a collection of sites that can be
 * accessed individually via specification of a set of integers.  Any object can
 * play the role of a site. 
 */
public interface AbstractLattice {

    /**
     * Dimension of the lattice.  The value of D describes the number of
     * integers needed to specify a site (via the site method).
     */
    public int D();

    /**
     * Returns the site specified by the given index.  The number of integers
     * in the index array must be equal to D, the lattice dimension.  Returned
     * object may be built and configured when called, or it may be pre-built
     * when array is constructed or sized and returned with call.  Repeated calls
     * with the same or different arguments may or may not return the same
     * object instance, depending on how concrete lattice is implemented.
     */
    public Object site(int[] index);
    
}