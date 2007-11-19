package etomica.potential;

import etomica.atom.AtomSet;
import etomica.atom.IAtomGroup;
import etomica.atom.IAtomPositioned;
import etomica.box.Box;
import etomica.space.IVector;
import etomica.space.NearestImageTransformer;
import etomica.space.Space;

/**
 * Two-centered Lennard Jones molecule with a quadrupole.
 *
 * @author Jayant K. Singh
 */
public class P22CLJQ extends Potential2 {

    public P22CLJQ(Space space) {
        this(space, 1, 1, 1);
    }

    public P22CLJQ(Space space, double sigma, double epsilon,  double moment) {
        super(space);
        setSigma(sigma);
        setEpsilon(epsilon);
        setQuadrupolarMomentSquare(moment);
        com1 = space.makeVector();
        com2 = space.makeVector();
        v12 = space.makeVector();
        v1 = space.makeVector();
        v2 = space.makeVector();
        dr = space.makeVector();
    }

    public void setHardCoreDiamterSq(double val){
        hsdiasq=val;
    }

    public void setBox(Box box) {
        nearestImageTransformer = box.getBoundary();
    }

    public double getRange() {
        return Double.POSITIVE_INFINITY;
    }

    public double energy(AtomSet pair){
        double ener=0.0;

        AtomSet mol1 = ((IAtomGroup)pair.getAtom(0)).getChildList(); 
        AtomSet mol2 = ((IAtomGroup)pair.getAtom(1)).getChildList(); 
        IAtomPositioned bead11 = (IAtomPositioned)mol1.getAtom(0);
        IAtomPositioned bead12 = (IAtomPositioned)mol1.getAtom(1);

        IAtomPositioned bead21 = (IAtomPositioned)mol2.getAtom(0);
        IAtomPositioned bead22 = (IAtomPositioned)mol2.getAtom(1);

        // LJ contributation

        dr.Ev1Mv2(bead11.getPosition(), bead21.getPosition());
        nearestImageTransformer.nearestImage(dr);
        ener=ener+calculateEnergy(dr.squared());

        dr.Ev1Mv2(bead11.getPosition(), bead22.getPosition());
        nearestImageTransformer.nearestImage(dr);
        ener=ener+calculateEnergy(dr.squared());

        dr.Ev1Mv2(bead12.getPosition(), bead21.getPosition());
        nearestImageTransformer.nearestImage(dr);
        ener=ener+calculateEnergy(dr.squared());

        dr.Ev1Mv2(bead12.getPosition(), bead22.getPosition());
        nearestImageTransformer.nearestImage(dr);
        ener=ener+calculateEnergy(dr.squared());

        if(Q2!=0.0 && !Double.isInfinite(ener)){

            com1.Ev1Pv2(bead11.getPosition(), bead12.getPosition());
            com1.TE(0.5);

            com2.Ev1Pv2(bead21.getPosition(), bead22.getPosition());
            com2.TE(0.5);
            v12.Ev1Mv2(com1, com2);
            nearestImageTransformer.nearestImage(v12);
            double r2=v12.squared();

            v12.TE(1/Math.sqrt(r2));

            // axis one
            dr.Ev1Mv2(bead11.getPosition(), bead12.getPosition());
            nearestImageTransformer.nearestImage(dr);

            v1.E(dr);

            v1.normalize();

            //axis two
            dr.Ev1Mv2(bead21.getPosition(), bead22.getPosition());
            nearestImageTransformer.nearestImage(dr);

            v2.E(dr);

            v2.normalize();

            // cos1 and sin1 are the cosine and sine of the angle (theta1)
            // between v1 and v12
            double cos1 = v1.dot(v12);
            // cos2 and sin2 are the cosine and sine of the angle (theta2)
            // between v2 and v12
            double cos2 = v2.dot(v12);
            // cos12sin1sin2 is the cosine of phi12, the angle between the
            // projections of v1 and v2 onto the plane perpendicular to v12
            // between the molecules multiplied by sin1 and sin2
            
            // temp = sin1*sin2*cos(phi12) - 4*cos1*cos2
            // cos(phi12) = v1 dot v2 - (v1 dot dr) * (v1 dot dr) / (sqrt(1-(v1 dot dr)^2)sqrt(1-(v2 dot dr)^2)
            // [ do some algebra!]
            // temp = v1 dot v2 - 5*cos1*cos2
            double temp = v1.dot(v2) - 5*cos1*cos2;
            
            double uqq = (3.0*Q2)/(4.0*r2*r2*Math.sqrt(r2));
            double uQuad = (uqq)*(1.0-5.0*(cos1*cos1+cos2*cos2+3*cos1*cos1*cos2*cos2) +2*(temp*temp));
            ener += uQuad;
        }
        return ener;
    }

    protected double calculateEnergy(double r2){
        if(r2<hsdiasq*sigma2) {
            return Double.POSITIVE_INFINITY;
        }
        double s2 = sigma2/(r2);
        double s6 = s2*s2*s2;
        return epsilon4*s6*(s6 - 1.0);
    }

    public double getSigma() {return sigma;}

    public final void setSigma(double s) {
        sigma = s;
        sigma2 = s*s;
    }

    public double getEpsilon() {return epsilon;}

    public final void setEpsilon(double eps) {
        epsilon = eps;
        epsilon4 = 4*epsilon;
    }

    public final void setQuadrupolarMomentSquare(double moment){
        Q2=moment;	
    }

    private static final long serialVersionUID = 1L;
    private double sigma , sigma2;
    private double epsilon, epsilon4;
    private double hsdiasq=1.0/Math.sqrt(2);
    private double Q2;
    private NearestImageTransformer nearestImageTransformer;

    private final IVector com1;
    private final IVector com2;
    private final IVector v12;
    private final IVector v1;
    private final IVector v2;
    private final IVector dr;
}
