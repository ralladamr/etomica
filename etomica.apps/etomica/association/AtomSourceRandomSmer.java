package etomica.association;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IRandom;
import etomica.atom.AtomSource;

public class AtomSourceRandomSmer implements AtomSource {
	public void setRandomNumberGenerator(IRandom newRandom) {
        random = newRandom;
    }
    
    /**
     * Returns the random number generator used to pick atoms
     */
    public IRandom getRandomNumberGenerator() {
        return random;
    }
    
    public void setAssociationManager(AssociationManager associationManager){
    	this.associationManager = associationManager;
    }

	public IAtom getAtom() {
    	IAtomList atoms = associationManager.getAssociatedAtoms();
    	if (atoms.getAtomCount() == 0 || atoms.getAtomCount() == 1) {//all the atoms are monomer or dimer
    		return null;
    	}
        return atoms.getAtom(random.nextInt(atoms.getAtomCount()));
	}

	public void setBox(IBox p) {

	}
	protected IRandom random;
	protected AssociationManager associationManager;
}
