package etomica.nbr;

import etomica.api.IAtomType;
import etomica.api.IPotential;
import etomica.api.ISimulation;
import etomica.api.ISimulationEventManager;
import etomica.api.ISpecies;
import etomica.api.ISpeciesManager;
import etomica.atom.AtomTypeAgentManager;
import etomica.box.BoxAgentManager;
import etomica.box.BoxAgentManager.BoxAgentSource;
import etomica.potential.PotentialArray;
import etomica.potential.PotentialGroup;
import etomica.potential.PotentialMaster;
import etomica.space.ISpace;
import etomica.util.Arrays;

public abstract class PotentialMasterNbr extends PotentialMaster implements AtomTypeAgentManager.AgentSource {

    protected PotentialMasterNbr(ISimulation sim, BoxAgentSource boxAgentSource, 
            BoxAgentManager boxAgentManager, ISpace _space) {
        super(_space);
        simulation = sim;
        this.boxAgentSource = boxAgentSource;
        this.boxAgentManager = boxAgentManager;
        rangedAgentManager = new AtomTypeAgentManager(this);
        intraAgentManager = new AtomTypeAgentManager(this);

        ISpeciesManager speciesManager = sim.getSpeciesManager();
        ISimulationEventManager simEventManager = sim.getEventManager();
        rangedAgentManager.init(speciesManager, simEventManager);
        intraAgentManager.init(speciesManager, simEventManager);
        rangedPotentialIterator = rangedAgentManager.makeIterator();
        intraPotentialIterator = intraAgentManager.makeIterator();
        boxAgentManager.setSimulation(sim);
    }
    
    public PotentialGroup makePotentialGroup(int nBody) {
        return new PotentialGroupNbr(nBody, space);
    }
    
    public void addPotential(IPotential potential, ISpecies[] species) {
        super.addPotential(potential, species);
        if (!(potential instanceof PotentialGroup)) {
             if (potential.getRange() == Double.POSITIVE_INFINITY) {
                 System.err.println("You gave me a molecular range-independent potential and I'm very confused now");
                 return;
             }
             //the potential is range-dependent 
             for (int i=0; i<species.length; i++) {
                 addRangedPotential(potential,species[i]);
             }
             addRangedPotentialForTypes(potential, species);
        }
    }

    public void potentialAddedNotify(IPotential subPotential, PotentialGroup pGroup) {
        super.potentialAddedNotify(subPotential, pGroup);
        IAtomType[] atomTypes = pGroup.getAtomTypes(subPotential);
        if (atomTypes == null) {
            if (pGroup.nBody() == 1 && subPotential.getRange() == Double.POSITIVE_INFINITY) {
                boolean found = false;
                for (int i=0; i<allPotentials.length; i++) {
                    if (allPotentials[i] == pGroup) {
                        found = true;
                    }
                }
                if (!found) {
                    allPotentials = (IPotential[])etomica.util.Arrays.addObject(allPotentials, pGroup);
                }
                //pGroup is PotentialGroupNbr
                IAtomType[] parentType = getAtomTypes(pGroup);
                ((PotentialArray)intraAgentManager.getAgent(parentType[0])).addPotential(pGroup);
            }
            else {
                //FIXME what to do with this case?  Fail!
                System.err.println("You have a child-potential of a 2-body PotentialGroup or range-dependent potential, but it's not type-based.  Enjoy crashing or fix bug 85");
            }
            return;
        }
        if (subPotential.getRange() == Double.POSITIVE_INFINITY) {
            if (subPotential.nBody() > 1) {
                //what to do with this case?
                // -- should only happen for 0 or 1-body potentials, which should be fine
                System.err.println("you have an infinite-ranged potential that's type based!  I don't like you.");
            }
            return;
        }
        for (int i=0; i<atomTypes.length; i++) {
            addRangedPotential(subPotential,atomTypes[i]);
        }
        addRangedPotentialForTypes(subPotential, atomTypes);
    }

    protected abstract void addRangedPotentialForTypes(IPotential subPotential, IAtomType[] atomTypes);
    
    protected void addRangedPotential(IPotential potential, IAtomType atomType) {
        
        PotentialArray potentialAtomType = (PotentialArray)rangedAgentManager.getAgent(atomType);
        potentialAtomType.addPotential(potential);
        atomType.setInteracting(true);
        boolean found = false;
        for (int i=0; i<allPotentials.length; i++) {
            if (allPotentials[i] == potential) {
                found = true;
            }
        }
        if (!found) {
            allPotentials = (IPotential[])etomica.util.Arrays.addObject(allPotentials, potential);
        }
    }
    
    public void removePotential(IPotential potential) {
        super.removePotential(potential);
        if (potential.getRange() < Double.POSITIVE_INFINITY) {
            rangedPotentialIterator.reset();
            while (rangedPotentialIterator.hasNext()) {
                ((PotentialArray)rangedPotentialIterator.next()).removePotential(potential);
            }
        }
        else if (potential instanceof PotentialGroup) {
            intraPotentialIterator.reset();
            while (intraPotentialIterator.hasNext()) {
                ((PotentialArray)intraPotentialIterator.next()).removePotential(potential);
            }
        }
        allPotentials = (IPotential[])Arrays.removeObject(allPotentials,potential);
    }
    
    public PotentialArray getRangedPotentials(IAtomType atomType) {
        return (PotentialArray)rangedAgentManager.getAgent(atomType);
    }

    public PotentialArray getIntraPotentials(ISpecies atomType) {
        return (PotentialArray)intraAgentManager.getAgent(atomType);
    }
    
    public final BoxAgentManager getCellAgentManager() {
        return boxAgentManager;
    }
    
    public Class getTypeAgentClass() {
        return PotentialArray.class;
    }
    
    public Object makeAgent(IAtomType type) {
        return new PotentialArray();
    }
    
    public void releaseAgent(Object agent, IAtomType type) {
    }

    /**
     * Returns the simulation associated with this PotentialMaster
     */
    public ISimulation getSimulation() {
        return simulation;
    }

    protected AtomTypeAgentManager.AgentIterator rangedPotentialIterator;
    protected AtomTypeAgentManager.AgentIterator intraPotentialIterator;
    protected final AtomTypeAgentManager rangedAgentManager;
    protected final AtomTypeAgentManager intraAgentManager;
    protected IPotential[] allPotentials = new IPotential[0];
    protected BoxAgentSource boxAgentSource;
    protected final ISimulation simulation;
    protected BoxAgentManager boxAgentManager;
}
