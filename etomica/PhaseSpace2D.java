package simulate;
import java.awt.Graphics;
import java.awt.Color;

public class PhaseSpace2D extends PhaseSpace {
    
    public PhaseSpace2D() {
    }
 
    public PhaseSpace.AtomCoordinate makeAtomCoordinate(Atom a) {return new AtomCoordinate(a);}
    public PhaseSpace.MoleculeCoordinate makeMoleculeCoordinate(Molecule m) {return new MoleculeCoordinate(m);}
    public simulate.AtomPair makeAtomPair(Atom a1, Atom a2) {return new AtomPair(a1, a2);}
    
    public final AtomPair.Iterator.A makePairIteratorFull(Atom iF, Atom iL, Atom oF, Atom oL) {return new PairIteratorFull(iF,iL,oF,oL);}
    public final AtomPair.Iterator.A makePairIteratorHalf(Atom iL, Atom oF, Atom oL) {return new PairIteratorHalf(iL,oF,oL);}
    public final AtomPair.Iterator.A makePairIteratorFull() {return new PairIteratorFull();}
    public final AtomPair.Iterator.A makePairIteratorHalf() {return new PairIteratorHalf();}
    
    public final double volume() {return Double.MAX_VALUE;}  //infinite volume unless using PBC
 
    public static final class Vector implements PhaseSpace.Vector {  //declared final for efficient method calls
        double x, y;
        public Vector () {x = 0.0; y = 0.0;}
        public Vector (double a1, double a2) {x = a1; y = a2;}
        public void E(Vector u) {x = u.x; y = u.y;}
        public void E(double a) {x = a; y = a;}
        public void PE(Vector u) {x += u.x; y += u.y;}
        public void TE(double a) {x *= a; y *= a;}
        public void DE(double a) {x /= a; y /= a;}
        public double square() {return x*x + y*y;}
        public double dot(Vector u) {return x*u.x + y*u.y;}
    }
    
    abstract class Coordinate implements PhaseSpace.Coordinate {
        public final Vector r = new Vector();  //Cartesian coordinates
        public final Vector p = new Vector();  //Momentum vector
    }    
    
    //much of AtomCoordinate and MoleculeCoordinate are identical in every PhaseSpace class
    //They are duplicated because they extend Coordinate, which is unique to each PhaseSpace
    final class AtomCoordinate extends Coordinate implements PhaseSpace.AtomCoordinate {
        AtomCoordinate nextCoordinate, previousCoordinate;
        AtomCoordinate(Atom a) {atom = a;}  //constructor
        public final Atom atom;
        
        protected final Vector rLast = new Vector();
        protected final Vector temp = new Vector();
        public void translateTo(PhaseSpace.Vector u) {r.E((Vector)u);}      //if using PBC, apply here
        public void translateBy(PhaseSpace.Vector u) {r.PE((Vector)u);}
        public void displaceTo(PhaseSpace.Vector u) {rLast.E(r); r.E((Vector)u);}  //want to eliminate these casts
        public void displaceBy(PhaseSpace.Vector u) {rLast.E(r); r.PE((Vector)u);}
        public void accelerate(PhaseSpace.Vector u) {p.PE((Vector)u);}
        public void translateTo(Vector u) {r.E(u);}      //if using PBC, apply here
        public void translateBy(Vector u) {r.PE(u);}
        public void displaceTo(Vector u) {rLast.E(r); r.E(u);}  
        public void displaceBy(Vector u) {rLast.E(r); r.PE(u);}
        public void accelerate(Vector u) {p.PE(u);}
        public void replace() {r.E(rLast);}
        public void inflate(double s) {r.TE(s);}
        public double kineticEnergy() {return 0.5*p.square()/mass;}
        public PhaseSpace.Vector position() {return r;}
        public PhaseSpace.Vector momentum() {return p;}
        public PhaseSpace.Vector velocity() {temp.E(p); temp.DE(mass); return temp;}  //returned vector is not thread-safe
        
        //following methods are same in all PhaseSpace classes
        public PhaseSpace.AtomCoordinate nextCoordinate() {return nextCoordinate();}
        public PhaseSpace.AtomCoordinate previousCoordinate() {return previousCoordinate();}
        public final void setNextCoordinate(PhaseSpace.Coordinate c) {
           nextCoordinate = (AtomCoordinate)c;
           if(c != null) {((AtomCoordinate)c).previousCoordinate = this;}
        }
        public final void clearPreviousCoordinate() {previousCoordinate = null;}
        public final Atom previousAtom() {
            PhaseSpace.AtomCoordinate c = atom.coordinate.previousCoordinate();
            return (c==null) ? null : c.atom();
        }
        public final Atom nextAtom() {
            PhaseSpace.AtomCoordinate c = atom.coordinate.nextCoordinate();
            return (c==null) ? null : c.atom();
        }
        public final Atom atom() {return atom;}
    }    
    final class MoleculeCoordinate extends Coordinate implements PhaseSpace.MoleculeCoordinate {
        MoleculeCoordinate nextCoordinate, previousCoordinate;
        MoleculeCoordinate(Molecule m) {molecule = m;}  //constructor
        public final Molecule molecule;

        protected final Vector rLast = new Vector();
        protected final Vector temp = new Vector();
        public void translateTo(PhaseSpace.Vector uu) {
            AtomCoordinate c = (AtomCoordinate)molecule.firstAtom.coordinate;
            Vector u = (Vector)uu;
            c.translateTo(u);
            if(molecule.nAtoms == 1) {return;}
            do {c=c.nextCoordinate; c.translateTo(u);} while (c.atom!=molecule.lastAtom);
        }
        //the rest of these methods are just copied from AtomCoordinate, and need to be translated for MoleculeCoordinate
        public void translateBy(PhaseSpace.Vector u) {r.PE((Vector)u);}
        public void displaceTo(PhaseSpace.Vector u) {rLast.E(r); r.E((Vector)u);}  //want to eliminate these casts
        public void displaceBy(PhaseSpace.Vector u) {rLast.E(r); r.PE((Vector)u);}
        public void replace() {r.E(rLast);}
        public void inflate(double s) {r.TE(s);}
        public void accelerate(PhaseSpace.Vector u) {p.PE((Vector)u);}
        public double kineticEnergy() {return 0.5*p.square()/mass;}
        public PhaseSpace.Vector position() {return r;}
        public PhaseSpace.Vector momentum() {return p;}
        public PhaseSpace.Vector velocity() {temp.E(p); temp.DE(mass); return temp;}  //returned vector is not thread-safe
        public void update() {}
        
        public PhaseSpace.MoleculeCoordinate nextCoordinate() {return nextCoordinate();}
        public PhaseSpace.MoleculeCoordinate previousCoordinate() {return previousCoordinate();}
        public final void setNextCoordinate(PhaseSpace.Coordinate c) {
           nextCoordinate = (MoleculeCoordinate)c;
           if(c != null) {((MoleculeCoordinate)c).previousCoordinate = this;}
        }
        public final void clearPreviousCoordinate() {previousCoordinate = null;}
        public final Molecule previousMolecule() {
            PhaseSpace.MoleculeCoordinate c = molecule.coordinate.previousCoordinate();
            return (c==null) ? null : c.molecule();
        }
        public final Molecule nextMolecule() {
            PhaseSpace.MoleculeCoordinate c = molecule.coordinate.nextCoordinate();
            return (c==null) ? null : c.molecule();
        }
        public final Molecule molecule() {return molecule;}
    }    
    
   /**
    * Scales all dimensions by a constant multiplicative factor and recomputes volume
    *
    * @param scale the scaling factor. 
    */
    public void inflate(double scale) {
        dimensions.TE(scale);
    }
           
    private class AtomPair implements simulate.AtomPair {  //Inner AtomPair class
        AtomCoordinate c1;
        AtomCoordinate c2;
        public AtomPair() {}
        public AtomPair(Atom a1, Atom a2) {
            if(a1 != null && a2 != null) {
                c1 = (AtomCoordinate)a1.coordinate;  //cast from PhaseSpace.AtomCoordinate
                c2 = (AtomCoordinate)a2.coordinate;
            }
        }
        
        public double r2() {
            double dx = c1.r.x - c2.r.x;   //change for PBC
            double dy = c1.r.y - c2.r.y;
            return dx*dx + dy*dy;
        }
        public final Atom atom1() {return c1.atom();}
        public final Atom atom2() {return c2.atom();}
    }
    
    //These iterators are identical in every PhaseSpace class; they are repeated in each
    //because they make direct use of the Coordinate type in the class; otherwise casting would be needed
    // Perhaps interitance would work, but haven't tried it
    
    //"Full" --> Each iteration of inner loop begins with same first atom
    private class PairIteratorFull implements AtomPairIterator.A {
        final AtomPair pair = new AtomPair();
        AtomCoordinate outer, inner;
        private AtomCoordinate iFirst, iLast, oLast;
        private boolean hasNext;
        public PairIteratorFull() {hasNext = false;}  //null constructor
        public PairIteratorFull(Atom iF, Atom iL, Atom oF, Atom oL) {reset(iF,iL,oF,oL);}  //constructor
        public void reset(Atom iL, Atom oF, Atom oL) {reset(oF,iL,oF,oL);}  //take inner and outer first atoms as same
        public void reset(Atom iF, Atom iL, Atom oF, Atom oL) {
            if(iF == null || oF == null) {hasNext = false; return;}
            iFirst = (AtomCoordinate)iF.coordinate; 
            iLast =  (iL==null) ? null : (AtomCoordinate)iL.coordinate; 
            outer = (AtomCoordinate)oF.coordinate; 
            oLast =  (oL==null) ? null : (AtomCoordinate)oL.coordinate;
            inner = iFirst;
            hasNext = true;
        }
        public simulate.AtomPair next() {
            if(!hasNext) {return null;}
            pair.c1 = outer;
            pair.c2 = inner;
            if(inner == iLast) {                                     //end of inner loop
                if(outer == oLast) {hasNext = false;}                //all done
                else {outer = outer.nextCoordinate; inner = iFirst;} //advance outer, reset inner
            }
            return pair;
        }
        public final void allDone() {hasNext = false;}   //for forcing iterator to indicate it has no more pairs
        public boolean hasNext() {return hasNext;}
    }
    
    //"Half" --> Each iteration of inner loop begins with atom after outer loop atom
    private class PairIteratorHalf implements AtomPairIterator.A {
        final AtomPair pair = new AtomPair();
        AtomCoordinate outer, inner;
        private AtomCoordinate iFirst, iLast, oLast;
        private boolean hasNext;
        public PairIteratorHalf() {hasNext = false;}
        public PairIteratorHalf(Atom iL, Atom oF, Atom oL) {reset(iL,oF,oL);}  //constructor
        public void reset(Atom iF, Atom iL, Atom oF, Atom oL) {reset(iL,oF,oL);} //ignore first argument
        public void reset(Atom iL, Atom oF, Atom oL) {
            if(oF == null) {hasNext = false; return;}
            iLast =  (iL==null) ? null : (AtomCoordinate)iL.coordinate; 
            outer =  (AtomCoordinate)oF.coordinate; 
            oLast =  (iL==null) ? null : (AtomCoordinate)oL.coordinate;
            inner = outer.nextCoordinate;
            hasNext = (inner != null);
        }
        public simulate.AtomPair next() {
            pair.c1 = outer;
            pair.c2 = inner;
            if(inner == iLast) {                                     //end of inner loop
                if(outer == oLast) {hasNext = false;}                //all done
                else {outer = outer.nextCoordinate; inner = outer.nextCoordinate;} //advance outer, reset inner
            }
            return pair;
        }
        public final void allDone() {hasNext = false;}   //for forcing iterator to indicate it has no more pairs
        public boolean hasNext() {return hasNext;}
    }    

   /**
    * Draws a light gray outline of the space if <code>visible</code> is set to
    * <code>true</code>.  The size of the outline is determined by 
    * <code>drawSize[]</code>.
    *
    * @param g      the graphics object to which the image is drawn
    * @param origin the coordinate origin (in pixels) for drawing the image
    * @see Phase#paint
    * @see #computeDrawSize
    */
    public void paint(Graphics g, int[] origin, double scale) {
        g.setColor(Color.gray.brighter());
        g.drawRect(origin[0],origin[1],(int)(scale*dimensions.x)-1,(int)(scale*dimensions.y)-1);
    }
 /**
  * Size of Phase (width, height) in Angstroms
  * Default value is 1.0 for each dimension.
  */
    private final Vector dimensions = new Vector(1.0,1.0);
    
}