package etomica.modules.pistoncylinder;

import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IBoundary;
import etomica.api.IVectorMutable;
import etomica.api.IVector;
import etomica.atom.iterator.ApiLeafAtoms;
import etomica.atom.iterator.AtomsetIteratorBoxDependent;
import etomica.potential.P1HardMovingBoundary;


/**
 * Our own ApiFiltered that's box-dependent
 */
public class ApiFilteredCylinder extends ApiLeafAtoms implements AtomsetIteratorBoxDependent {
    public ApiFilteredCylinder(AtomFilterInCylinder filter) {
        super();
        this.filter = filter;
    }

    public IAtomList next() {
        IAtomList list = super.next();
        while (list != null && !filter.accept(list)) {
            list = super.next();
        }
        return list;
    }
    
    public int size() {
        int count = 0;
        reset();
        for (Object a = next(); a != null; a = next()) {
            count++;
        }
        return count;
    }
        
    
    private static final long serialVersionUID = 1L;
    protected final AtomFilterInCylinder filter;

    /**
     * Filter to expclude any pair with an atom within some distance from a 
     * wall. 
     */
    public static class AtomFilterInCylinder {
        public AtomFilterInCylinder(IBoundary boundary, P1HardMovingBoundary pistonPotential, double padding) {
            dimensions = boundary.getDimensions();
            this.pistonPotential = pistonPotential;
            this.padding = padding;
            // bit flipper goes back and forth between 1 and 2
            bitFlipper = 1;
        }
        
        public boolean accept(IAtomList atoms) {
            double radius = pistonPotential.getCollisionRadius()+padding;
            // always reject if both atoms are near a wall.  always accept if
            // both atoms are away from the wall.  If one is near and one not, 
            // accept the pair half the time.  RDF needs this to avoid 
            // over-counting pairs with one near the wall.  Ideally, we'd 
            // accept them all and weight them half as much. 
            int numOut = 0;
            for (int i=0; i<2; i++) {
                IVectorMutable pos = ((IAtomPositioned)atoms.getAtom(i)).getPosition();
                
                if (pos.x(0) < -0.5*dimensions.x(0)+radius ||
                    pos.x(0) >  0.5*dimensions.x(0)-radius) {
                    numOut++;
                }
                else if ((pos.getD() == 2 && (pos.x(1) < pistonPotential.getWallPosition()+radius ||
                                              pos.x(1) >  0.5*dimensions.x(1)-radius)) ||
                         (pos.getD() == 3 && (pos.x(1) > pistonPotential.getWallPosition()-radius ||
                                 pos.x(1) < -0.5*dimensions.x(1)+radius ||
                                 pos.x(2) < -0.5*dimensions.x(2)+radius ||
                                 pos.x(2) >  0.5*dimensions.x(2)-radius))) {
                    numOut++;
                }
            }
            // twiddle the last two bits, 1=>2, 2=>1
            // numOut=0 is always accepted, numOut=2 is never accepted
            bitFlipper ^= 3;
            return numOut < bitFlipper;
        }
        
        private double padding;
        private final IVector dimensions;
        private final P1HardMovingBoundary pistonPotential;
        private int bitFlipper;
    }
}