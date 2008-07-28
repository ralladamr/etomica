package etomica.modules.sam;

import etomica.api.IAtom;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.atom.MoleculeAgentManager;
import etomica.atom.MoleculeAgentManager.MoleculeAgentSource;
import etomica.nbr.NeighborCriterion;

/**
 * Returns first leaf atom of each polymer molecule and the atom its bonded to.
 */
public class CriterionTether3 implements NeighborCriterion, MoleculeAgentSource {

    public CriterionTether3(ISimulation sim, ISpecies polymerSpecies, IAtomTypeLeaf surfaceType) {
        this.sim = sim;
        this.polymerSpecies = polymerSpecies;
        this.surfaceType = surfaceType;
    }
    
    public void setBox(IBox newBox) {
        if (box != newBox) {
            if (box != null) {
                bondManager.dispose();
            }
            bondManager = new MoleculeAgentManager(sim, newBox, this);
        }
        box = newBox;
        polymerList = box.getMoleculeList(polymerSpecies);
    }

    public void setBondedSurfaceAtoms(IMolecule polymerMolecule, IAtomSet surfaceAtoms) {
        bondManager.setAgent(polymerMolecule, surfaceAtoms);
    }

    public boolean accept(IAtomSet pair) {
        IAtomLeaf atom1 = (IAtomLeaf)pair.getAtom(0);
        IAtomLeaf atom2 = (IAtomLeaf)pair.getAtom(1);
        if (atom1.getIndex() != 0 || atom1.getParentGroup().getType() != polymerSpecies) {
            IAtomLeaf foo = atom2;
            atom2 = atom1;
            atom1 = foo;
            if (atom1.getIndex() != 0 || atom1.getParentGroup().getType() != polymerSpecies) {
                return false;
            }
        }
        if (atom2.getType() != surfaceType) {
            return false;
        }
        IAtomSet bondedSurfaceAtoms = ((IAtomSet)bondManager.getAgent(atom1.getParentGroup()));
        if (bondedSurfaceAtoms == null) {
            return false;
        }
        for (int i=0; i<bondedSurfaceAtoms.getAtomCount(); i++) {
            if (bondedSurfaceAtoms.getAtom(i) == atom2) {
                return true;
            }
        }
        return false;
    }

    public boolean needUpdate(IAtom atom) {
        return false;
    }

    public void reset(IAtom atom) {
    }

    public boolean unsafe() {
        return false;
    }

    public Class getMoleculeAgentClass() {
        return IAtomSet.class;
    }

    public Object makeAgent(IMolecule a) {
        return null;
    }

    public void releaseAgent(Object agent, IMolecule atom) {
    }

    protected final ISimulation sim;
    protected IBox box;
    protected final ISpecies polymerSpecies;
    protected IAtomSet polymerList;
    protected MoleculeAgentManager bondManager;
    protected int cursor;
    protected int surfaceCursor;
    protected IMolecule targetMolecule;
    protected final IAtomTypeLeaf surfaceType;
}
