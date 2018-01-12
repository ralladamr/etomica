/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.potential;

import etomica.atom.IAtom;
import etomica.box.Box;
import etomica.space.Vector;
import etomica.space.Space;
import etomica.space.Tensor;

import java.util.List;


/**
 * Ideal-gas two-body potential, which defines no interactions and zero energy
 * for all pairs given to it.
 * <p> 
 * Useful as a placeholder where a potential is expected but it is desired to 
 * not have the atoms interact.
 */
public class P2Ideal extends Potential2 implements Potential2Soft,
        Potential2Spherical, PotentialHard {

    public P2Ideal(Space space) {
        super(space);
        zeroVector = new Vector[1];
        zeroVector[0] = space.makeVector();
        zeroTensor = space.makeTensor();
    }
    /**
     * Does nothing.
     */
    public void setBox(Box box) {
    }

    /**
     * Returns zero.
     */
    public double getRange() {
        return 0;
    }

    /**
     * Returns zero.
     * @param atoms
     */
    public double energy(List<IAtom> atoms) {
        return 0;
    }

    /**
     * Returns zero.
     * @param pair
     */
    public double hyperVirial(List<IAtom> pair) {
        return 0;
    }

    /**
     * Returns zero.
     * @param pair
     */
    public double virial(List<IAtom> pair) {
        return 0;
    }

    /**
     * Returns zero.
     */
    public double integral(double rC) {
        return 0;
    }

    /**
     * Returns zero.
     */
    public double u(double r2) {
        return 0;
    }

    /**
     * Returns zero.
     */
    public double du(double r2) {
        return 0;
    }

    /**
     * Returns zero.
     */
    public double lastCollisionVirial() {
        return 0;
    }

    /**
     * Returns a tensor of zeros.
     */
    public Tensor lastCollisionVirialTensor() {
        zeroTensor.E(0.0);
        return zeroTensor;
    }

    /**
     * Does nothing.
     */
    public void bump(List<IAtom> atom, double falseTime) {
    }

    /**
     * Returns Double.POSITIVE_INFINITY.
     */
    public double collisionTime(List<IAtom> atom, double falseTime) {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns zero.
     */
    public double energyChange() {
        return 0;
    }

    /**
     * Returns a zero vector.
     * @param atoms
     */
    public Vector[] gradient(List<IAtom> atoms) {
        zeroVector[0].E(0.0);
        return zeroVector;
    }
    
    public Vector[] gradient(List<IAtom> atoms, Tensor pressureTensor) {
        return gradient(atoms);
    }
        

    private static final long serialVersionUID = 1L;
    private final Vector[] zeroVector;
    private final Tensor zeroTensor;
}
