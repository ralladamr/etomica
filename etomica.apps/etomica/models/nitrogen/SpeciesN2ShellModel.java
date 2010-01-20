package etomica.models.nitrogen;

import etomica.api.IAtomTypeSphere;
import etomica.api.IMolecule;
import etomica.atom.Atom;
import etomica.atom.AtomLeafDynamic;
import etomica.atom.AtomTypeSphere;
import etomica.atom.Molecule;
import etomica.chem.elements.ElementSimple;
import etomica.chem.elements.Nitrogen;
import etomica.space.ISpace;
import etomica.species.Species;

/**
 * 
 * 
 * Species nitrogen molecule (shell model) 
 * 
 * Reference: Fabianski R. et al, Calculations on the stability of low temperature solid nitrogen
 *             phases, JCP 112(15) 6745 (2000)
 *             
 * @author Tai Boon Tan
 *
 */
public class SpeciesN2ShellModel extends Species {

    public SpeciesN2ShellModel(ISpace space) {
        this(space, false);
    }
    
    public SpeciesN2ShellModel(ISpace space, boolean isDynamic) {
        super();
        this.space = space;
        this.isDynamic = isDynamic;
        
        nType = new AtomTypeSphere(Nitrogen.INSTANCE, 3.1);
        pType = new AtomTypeSphere(new ElementSimple("P", 1.0), 0.0);
        addChildType(nType);
        addChildType(pType);

        setConformation(new ConformationNitrogenShellModel(space)); 
     }

     public IMolecule makeMolecule() {
         Molecule nitrogen = new Molecule(this, 5);
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, pType) : new Atom(space, pType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, pType) : new Atom(space, pType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, pType) : new Atom(space, pType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, nType) : new Atom(space, nType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, nType) : new Atom(space, nType));

         conformation.initializePositions(nitrogen.getChildList());
         return nitrogen;
     }

     public IAtomTypeSphere getNitrogenType() {
         return nType;
     }

     public AtomTypeSphere getPType() {
         return pType;
     }


     public int getNumLeafAtoms() {
         return 5;
     }
    
    public final static int indexCenter = 0;
    public final static int indexP1left  = 1;
    public final static int indexP1right  = 2;
    public final static int indexN1 = 3;
    public final static int indexN2 = 4;

    private static final long serialVersionUID = 1L;
    protected final ISpace space;
    protected final boolean isDynamic;
    protected final AtomTypeSphere nType, pType;
}