package etomica.spin.heisenberg_interacting.heisenberg;

import etomica.atom.IAtomList;
import etomica.atom.IAtomOriented;
import etomica.molecule.DipoleSource;
import etomica.molecule.IMoleculeList;
import etomica.potential.*;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.util.numerical.BesselFunction;

public class PotentialCalculationFSum implements PotentialCalculationMolecular {
    protected final Vector ei, ej;
    protected final double mu, J, bt; //TODO should I add final here
    protected Vector dr;
    protected double FSum = 0;
    protected DipoleSource dipoleSource;


    public PotentialCalculationFSum(Space space, double dipoleMagnitude, double interactionS, double beta) {
        dr = space.makeVector();
        ei = space.makeVector();
        ej = space.makeVector();

        J = interactionS;
        mu = dipoleMagnitude;
        bt = beta;
    }

    public void doCalculation(IAtomList atoms, IPotentialAtomic potential) {
        if (!(potential instanceof IPotentialAtomicSecondDerivative)) {//TODO ??? should I do similar stuff here????
            return;
        }
        //get total force here!!!
        IPotentialAtomicSecondDerivative potentialSecondDerivative = (IPotentialAtomicSecondDerivative) potential;
        Vector[][] t = potentialSecondDerivative.gradientAndTorque(atoms);

        IAtomOriented atom1 = (IAtomOriented) atoms.getAtom(0);
        IAtomOriented atom2 = (IAtomOriented) atoms.getAtom(1);

        //TODO the acos returns 0 to Pi but t1 is form 0 to 2Pi
        ei.E(atom1.getOrientation().getDirection());
        ej.E(atom2.getOrientation().getDirection());


        double bJ = bt * J;
        double bJ2 = bJ * bJ;
        double cos = ei.dot(ej);
        double sin = Math.sqrt(1 - cos * cos);
        double cos2 = 2 * cos * cos - 1;
        double exp2bJ = Math.exp(-2 * bJ * cos);
        double exp1bJ = Math.exp(-bJ * cos);
        double bessel0 = BesselFunction.doCalc(true, 0, bJ);
        double bessel1 = BesselFunction.doCalc(true, 1, bJ);

        //Try to implement the constant part here
        FSum += 0.5 * exp2bJ * (bessel0 + bessel1) + (exp1bJ * (-4 - bJ - 2 * cos + bJ * cos2)
                + (bessel0 + bessel1) * (2 - bJ2 - 2 * bJ * cos + bJ2 * cos2));
        FSum += -exp2bJ * (bessel0 + bessel1) * (exp1bJ + bJ * (bessel0 + bessel1)) * t[1][0].getX(0);//TODO how to get forces?

        //FSum need to be times bmu^2 in meter
    }

    public void doCalculation(IMoleculeList molecules, IPotentialMolecular potential) {
        if (!(potential instanceof IPotentialMolecularSecondDerivative)) {
            return;
        }


    }

    public void setDipoleSource(DipoleSource newDipoleSource) {
        dipoleSource = newDipoleSource;
    }

    public void zeroSum() {
        FSum = 0.0;
    }

    /**
     * Returns the current value of the energy sum.
     */
    public double getSum() {
        return FSum;
    }


}