package simulate;
import java.awt.Graphics;
import java.awt.Color;
import java.util.Random;

public class PhaseSpace2D extends PhaseSpace {
    
    public final int D() {return 2;}
    public final boolean pbc = true;
 
    public PhaseSpace.AtomCoordinate makeAtomCoordinate(Atom a) {return new AtomCoordinate(a);}
    public PhaseSpace.MoleculeCoordinate makeMoleculeCoordinate(Molecule m) {return new MoleculeCoordinate(m);}
    public simulate.AtomPair makeAtomPair(Atom a1, Atom a2) {return new AtomPair(a1, a2);}
    public PhaseSpace.Vector makeVector() {return new Vector();}
    
    public final simulate.AtomPair.Iterator.A makePairIteratorFull(Atom iF, Atom iL, Atom oF, Atom oL) {return new PairIteratorFull(iF,iL,oF,oL);}
    public final simulate.AtomPair.Iterator.A makePairIteratorHalf(Atom iL, Atom oF, Atom oL) {return new PairIteratorHalf(iL,oF,oL);}
    public final simulate.AtomPair.Iterator.A makePairIteratorFull() {return new PairIteratorFull();}
    public final simulate.AtomPair.Iterator.A makePairIteratorHalf() {return new PairIteratorHalf();}
    
    public final double volume() {return dimensions.x*dimensions.y;}  //infinite volume unless using PBC
    public final PhaseSpace.Vector dimensions() {return dimensions;}
 
    public static final class Vector implements PhaseSpace.Vector {  //declared final for efficient method calls
        public static final Random random = new Random();
        public static final Vector ORIGIN = new Vector(0.0,0.0);
        double x, y;
        public Vector () {x = 0.0; y = 0.0;}
        public Vector (double a1, double a2) {x = a1; y = a2;}
        public double component(int i) {return (i==0) ? x : y;}
        public void setComponent(int i, double d) {if(i==0) x=d; else y=d;}
        public void E(PhaseSpace.Vector u) {E((Vector)u);}
        public void PE(PhaseSpace.Vector u) {PE((Vector)u);}
        public void ME(PhaseSpace.Vector u) {ME((Vector)u);}
        public void TE(PhaseSpace.Vector u) {TE((Vector)u);}
        public void DE(PhaseSpace.Vector u) {DE((Vector)u);}
        public double dot(PhaseSpace.Vector u) {return dot((Vector)u);}
        public void E(Vector u) {x = u.x; y = u.y;}
        public void E(double a) {x = a; y = a;}
        public void Ea1Tv1(double a1, Vector u1) {x = a1*u1.x; y = a1*u1.y;}
        public void PEa1Tv1(double a1, Vector u1) {x += a1*u1.x; y += a1*u1.y;}
        public void PE(Vector u) {x += u.x; y += u.y;}
        public void ME(Vector u) {x -= u.x; y -= u.y;}
        public void TE(double a) {x *= a; y *= a;}
        public void DE(double a) {x /= a; y /= a;}
        public double squared() {return x*x + y*y;}
        public double dot(Vector u) {return x*u.x + y*u.y;}
        public void randomStep(double d) {x += (2.*random.nextDouble()-1.0)*d; y+= (2.*random.nextDouble()-1.0)*d;} //uniformly distributed random step in x and y, within +/- d
        public void setRandom(double d) {x = random.nextDouble()*d; y = random.nextDouble()*d;}
        public void setRandom(double dx, double dy) {x = random.nextDouble()*dx; y = random.nextDouble()*dy;}
        public void setRandom(Vector u) {setRandom(u.x,u.y);}
        public void setToOrigin() {x = ORIGIN.x; y = ORIGIN.y;}
        public void randomDirection() {x = Math.cos(2*Math.PI*random.nextDouble()); y = Math.sqrt(1.0 - x*x);}
    }
    
    public static final class VectorInt {  //declared final for efficient method calls
        int x, y;
        public VectorInt () {x = 0; y = 0;}
        public VectorInt (int a1, int a2) {x = a1; y = a2;}
        public void E(VectorInt u) {x = u.x; y = u.y;}
        public void E(int a) {x = a; y = a;}
        public void PE(VectorInt u) {x += u.x; y += u.y;}
        public void TE(int a) {x *= a; y *= a;}
        public void DE(int a) {x /= a; y /= a;}
        public int squared() {return (x*x + y*y);}
        public int dot(VectorInt u) {return x*u.x + y*u.y;}
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
        private final Vector rLast = new Vector();
        private final Vector temp = new Vector();
        
        public void translateTo(PhaseSpace.Vector u) {translateTo((Vector)u);}      
        public void translateBy(PhaseSpace.Vector u) {r.PE((Vector)u);}
        public void translateToward(PhaseSpace.Vector u, double d) {translateToward((Vector)u,d);}
        public void translateToward(Vector u, double d) {temp.Ea1Tv1(d,u); translateBy(temp);}
        public void displaceTo(PhaseSpace.Vector u) {displaceTo((Vector)u);}  //want to eliminate these casts
        public void displaceBy(PhaseSpace.Vector u) {displaceBy((Vector)u);}
        public void displaceWithin(double d) {temp.setToOrigin(); temp.randomStep(d); displaceBy(temp);}
        public void displaceToRandom() {rLast.E(r); r.setRandom(dimensions.x, dimensions.y);} 
        public void accelerate(PhaseSpace.Vector u) {p.PE((Vector)u);}
        public void translateTo(Vector u) {r.E(u);}      
        public void translateBy(Vector u) {  //if using PBC, apply here
            r.PE(u);
            if(pbc) {
                if(r.x > dimensions.x) r.x -= dimensions.x;  //PBC
                else if(r.x < 0.0) r.x += dimensions.x;
                if(r.y > dimensions.y) r.y -= dimensions.y;
                else if(r.y < 0.0) r.y += dimensions.y;
            }
        }     
        public void translateToRandom() {r.setRandom(dimensions.x, dimensions.y);}
        public void displaceTo(Vector u) {rLast.E(r); translateTo(u);}  
        public void displaceBy(Vector u) {rLast.E(r); translateBy(u);}
        public void accelerateBy(Vector u) {p.PE(u);}
        public void accelerateToward(PhaseSpace.Vector u, double d) {accelerateToward((Vector)u,d);}
        public void accelerateToward(Vector u, double d) {temp.Ea1Tv1(d,u); accelerateBy(temp);}
        public void replace() {r.E(rLast);}
        public void inflate(double s) {r.TE(s);}
        public double kineticEnergy() {return 0.5*p.squared()*atom.type.rm();}
        public void randomizeMomentum(double temperature) {  //not very sophisticated; random only in direction, not magnitude
            double momentum = Math.sqrt(atom.mass()*temperature*(double)D()/Constants.KE2T);  //need to divide by sqrt(m) to get velocity
            p.randomDirection();
            p.TE(momentum);
        }
        public void scaleMomentum(double scale) {p.TE(scale);}
        public PhaseSpace.Vector position() {return r;}
        public PhaseSpace.Vector momentum() {return p;}
        public double position(int i) {return r.component(i);}
        public double momentum(int i) {return p.component(i);}
        public PhaseSpace.Vector velocity() {temp.E(p); temp.TE(atom.type.rm()); return temp;}  //returned vector is not thread-safe
        
        //following methods are same in all PhaseSpace classes
        public PhaseSpace.AtomCoordinate nextCoordinate() {return nextCoordinate;}
        public PhaseSpace.AtomCoordinate previousCoordinate() {return previousCoordinate;}
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
        public void updateR() {  //recomputes COM position from atom positions
            AtomCoordinate c = (AtomCoordinate)molecule.firstAtom.coordinate;
            if(molecule.nAtoms==1) {r.E(c.r);}  //one atom in molecule
            else {  //multiatomic
                r.Ea1Tv1(c.atom.mass(),c.r);
                do {c=c.nextCoordinate; r.PEa1Tv1(c.atom.mass(),c.r);} while (c.atom!=molecule.lastAtom);
                r.DE(molecule.mass());
            }
        }
        public void updateP() {  //recomputes total momentum from atom momenta
            AtomCoordinate c = (AtomCoordinate)molecule.firstAtom.coordinate;
            p.E(c.p);
            if(molecule.nAtoms==1) {return;}  //one atom in molecule
            do {c=c.nextCoordinate; p.PE(c.p);} while (c.atom!=molecule.lastAtom);
        }
        public void translateToward(PhaseSpace.Vector u, double d) {temp.Ea1Tv1(d,(Vector)u); translateBy(temp);}
        public void translateBy(PhaseSpace.Vector u) {translateBy((Vector)u);}
        public void translateBy(Vector u) {
            AtomCoordinate c = (AtomCoordinate)molecule.firstAtom.coordinate;
            c.translateBy(u);
            if(molecule.nAtoms == 1) {return;}
            do {c=c.nextCoordinate; c.translateBy(u);} while (c.atom!=molecule.lastAtom);
        }
        public void displaceToRandom(PhaseSpace.Vector dim) {temp.setRandom((Vector)dim); displaceTo(temp);} 
        public void translateTo(PhaseSpace.Vector u) {
            updateR();  //update COM vector
            temp.E((Vector)u);  //temp = destination vector
            temp.ME(r);   //temp = destination - original = dr
            translateBy(temp);
        }
        public void displaceBy(PhaseSpace.Vector u) {displaceBy((Vector)u);}
        public void displaceBy(Vector u) {
            AtomCoordinate c = (AtomCoordinate)molecule.firstAtom.coordinate;
            c.displaceBy(u);
            if(molecule.nAtoms == 1) {return;}
            do {c=c.nextCoordinate; c.displaceBy(u);} while (c.atom!=molecule.lastAtom);
        }
        public void displaceTo(PhaseSpace.Vector u) {displaceTo((Vector)u);}
        public void displaceTo(Vector u) {
            updateR();  //update COM vector
            temp.E(u);  //temp = destination vector
            temp.ME(r);   //temp = destination - original = dr
            displaceBy(temp);
        }
        public void displaceWithin(double d) {
            temp.setRandom(d);
            displaceBy(temp);
        }
            
        public void displaceToRandom() {
            temp.setRandom(dimensions.x, dimensions.y);
            displaceTo(temp);
        }
        public void translateToRandom() {
            temp.setRandom(dimensions.x, dimensions.y);
            translateTo(temp);
        }
        public void replace() {
            AtomCoordinate c = (AtomCoordinate)molecule.firstAtom.coordinate;
            c.replace();
            if(molecule.nAtoms == 1) {return;}
            do {c=c.nextCoordinate; c.replace();} while (c.atom!=molecule.lastAtom);
        }
        public void inflate(double s) {
            updateR();
            temp.Ea1Tv1(s-1.0,r);
            displaceBy(temp);   //displaceBy doesn't use temp
        }
        public PhaseSpace.Vector position() {updateR(); return r;}
        public PhaseSpace.Vector momentum() {updateP(); return p;}
        public double position(int i) {updateR(); return r.component(i);}
        public double momentum(int i) {updateR(); return r.component(i);}
        public double kineticEnergy() {return 0.5*p.squared()*molecule.rm();}
        public void randomizeMomentum(double temperature) {
            AtomCoordinate c = (AtomCoordinate)molecule.firstAtom.coordinate;
            c.randomizeMomentum(temperature);
            if(molecule.nAtoms == 1) {return;}
            do {c=c.nextCoordinate; c.randomizeMomentum(temperature);} while (c.atom!=molecule.lastAtom);
        }
        //the rest of these methods are just copied from AtomCoordinate, and need to be translated for MoleculeCoordinate
//        public void accelerate(PhaseSpace.Vector u) {p.PE((Vector)u);}
//        public PhaseSpace.Vector velocity() {temp.E(p); temp.TE(molecule.rm()); return temp;}  //returned vector is not thread-safe
        //
        public PhaseSpace.MoleculeCoordinate nextCoordinate() {return nextCoordinate;}
        public PhaseSpace.MoleculeCoordinate previousCoordinate() {return previousCoordinate;}
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
           
    public double[][] imageOrigins(int nShells) {
        return new double[0][D()];
    }
    
    private final class AtomPair implements simulate.AtomPair {  //could be static except for PBC
        AtomCoordinate c1;
        AtomCoordinate c2;
        private double drx, dry, dvx, dvy;
        public AtomPair() {}
        public AtomPair(Atom a1, Atom a2) {
            if(a1 != null && a2 != null) {
                c1 = (AtomCoordinate)a1.coordinate;  //cast from PhaseSpace.AtomCoordinate
                c2 = (AtomCoordinate)a2.coordinate;
                reset();
            }
        }
        public void reset() {
            drx = c2.r.x - c1.r.x;   //change for PBC
            dry = c2.r.y - c1.r.y;
            if(pbc) {
                double d2 = 0.5*dimensions.x;
                if(drx > 0) {drx -= (drx > +d2) ? dimensions.x : 0.0;}
                else        {drx += (drx < -d2) ? dimensions.x : 0.0;}
                d2 = 0.5*dimensions.y;
                if(dry > 0) {dry -= (dry > +d2) ? dimensions.y : 0.0;}
                else        {dry += (dry < -d2) ? dimensions.y : 0.0;}
            }
        }
            
             //Room to optimize all this by precomputing and saving components of dr and dv        
        public double r2() {
            return drx*drx + dry*dry;
        }
        public double v2() {
            double rm1 = c1.atom.type.rm();
            double rm2 = c2.atom.type.rm();
            double dvx = rm2*c2.p.x - rm1*c1.p.x;
            double dvy = rm2*c2.p.y - rm1*c1.p.y;
            return dvx*dvx + dvy*dvy;
        }
        public double vDotr() {
            double rm1 = c1.atom.type.rm();
            double rm2 = c2.atom.type.rm();
            double dvx = rm2*c2.p.x - rm1*c1.p.x;
            double dvy = rm2*c2.p.y - rm1*c1.p.y;
            return drx*dvx + dry*dvy;
        }
        public void push(double impulse) {  //changes momentum in the direction joining the atoms
            c1.p.x += impulse*drx;
            c1.p.y += impulse*dry;
            c2.p.x -= impulse*drx;
            c2.p.y -= impulse*dry;
        }
        public void setSeparation(double r2New) {
            double ratio = c2.atom.type.mass()*c1.atom.type.rm();  // (mass2/mass1)
            double delta = (Math.sqrt(r2New/r2()) - 1.0)/(1+ratio);
            c1.r.x -= ratio*delta*drx;
            c1.r.y -= ratio*delta*dry;
            c2.r.x += delta*drx;
            c2.r.y += delta*dry;
            //need call reset?
        }
        public final Atom atom1() {return c1.atom();}
        public final Atom atom2() {return c2.atom();}
    }
    
    //These iterators are identical in every PhaseSpace class; they are repeated in each
    //because they make direct use of the Coordinate type in the class; otherwise casting would be needed
    // Perhaps interitance would work, but haven't tried it
    
    //"Full" --> Each iteration of inner loop begins with same first atom
    private class PairIteratorFull implements simulate.AtomPair.Iterator.A {
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
            pair.c1 = outer;   //c1 should always be outer
            pair.c2 = inner;
            pair.reset();
            if(inner == iLast) {                                     //end of inner loop
                if(outer == oLast) {hasNext = false;}                //all done
                else {outer = outer.nextCoordinate; inner = iFirst;} //advance outer, reset inner
            }
            else {inner = inner.nextCoordinate;}
            return pair;
        }
        public final void allDone() {hasNext = false;}   //for forcing iterator to indicate it has no more pairs
        public boolean hasNext() {return hasNext;}
    }
    
    //"Half" --> Each iteration of inner loop begins with atom after outer loop atom
    private class PairIteratorHalf implements simulate.AtomPair.Iterator.A {
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
            pair.c1 = outer;   //c1 should always be outer
            pair.c2 = inner;
            pair.reset();
            if(inner == iLast) {                                     //end of inner loop
                if(outer == oLast) {hasNext = false;}                //all done
                else {outer = outer.nextCoordinate; inner = outer.nextCoordinate;} //advance outer, reset inner
            }
            else {inner = inner.nextCoordinate;}
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
    public final Vector dimensions = new Vector(1.0,1.0);
    
}