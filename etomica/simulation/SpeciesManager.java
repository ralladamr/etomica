package etomica.simulation;

import java.util.HashMap;
import java.util.LinkedList;

import etomica.api.IAtomType;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.chem.elements.Element;
import etomica.util.Arrays;

/**
 * The SpeciesManager manages Species and AtomTypes on behalf of the
 * Simulation.
 * 
 * @author Andrew Schultz
 */
public class SpeciesManager implements java.io.Serializable {

    public SpeciesManager(ISimulation sim) {
        this.sim = sim;
        speciesList = new ISpecies[0];
        numAtomTypes = 0;
        elementSymbolHash = new HashMap<String,Element>();
        elementAtomTypeHash = new HashMap<Element,LinkedList<IAtomType>>();
        typeReservoirCount = -1;
        moleculeTypes = new ISpecies[0];
    }

    /**
     * Adds species to the list of all species in the simulation, and
     * adds new species agent to every box currently in simulation.
     * This is called by the Species constructor.
     * 
     * @return the index assigned to the new species
     */
    public void addSpecies(ISpecies species) {
        for (int i=0; i<speciesList.length; i++) {
            if (speciesList[i] == species) {
                throw new IllegalArgumentException("Species already exists");
            }
        }
        int index = speciesList.length;
        species.setIndex(index);
        speciesList = (ISpecies[])Arrays.addObject(speciesList,species);
        
        // PotentialMaster depends on leafTypeA.index > leafTypeB.index if
        // speciesA.index > speciesB.index
        // bump all leaf indices up one. sorry.
        int previousIndex = Integer.MAX_VALUE;
        for (int i=moleculeTypes.length-1; i>-1; i--) {
            IAtomTypeLeaf[] leafTypes = moleculeTypes[i].getChildTypes();
            for (int j=leafTypes.length-1; j>-1; j--) {
                IAtomTypeLeaf leafType = leafTypes[j];
                int oldIndex = leafType.getIndex();
                if (oldIndex >= previousIndex) {
                    throw new RuntimeException("Leaf type indicies seem to be out of order.  "+previousIndex+" came after "+oldIndex);
                }
                previousIndex = oldIndex;
                moleculeTypes[i].getChildTypes()[j].setIndex(oldIndex+1);
                sim.getEventManager().fireEvent(new SimulationAtomTypeIndexChangedEvent(leafType, oldIndex));
            }
        }
        
        species.setIndex(index);
        numAtomTypes++;
        species.setSpeciesManager(this);
        moleculeTypes = (ISpecies[])Arrays.addObject(moleculeTypes, species);

        // this just fires an event for listeners to receive
        atomTypeAddedNotify(species);

        IBox[] boxList = sim.getBoxs();
        for (int i=0; i<boxList.length; i++) {
            boxList[i].addSpeciesNotify(species);
        }

        sim.getEventManager().fireEvent(new SimulationSpeciesAddedEvent(species));
    }
    
    public void boxAddedNotify(IBox newBox) {
        for(int i=0; i<speciesList.length; i++) {
            newBox.addSpeciesNotify(speciesList[i]);
        }
    }

    /**
     * Removes the given AtomTypes associated with the given Species from the 
     * Simulation and does cleanup, including renumbering indices and firing 
     * AtomType-related event notifications.
     */
    public boolean removeSpecies(ISpecies removedSpecies) {
        boolean success = false;
        for (int i=0; i<speciesList.length; i++) {
            if (speciesList[i] == removedSpecies) {
                success = true;
                break;
            }
        }
        if (!success) {
            return false;
        }

        sim.getEventManager().fireEvent(new SimulationSpeciesRemovedEvent(removedSpecies));
        
        speciesList = (ISpecies[])Arrays.removeObject(speciesList,removedSpecies);
        for (int i=removedSpecies.getIndex(); i<speciesList.length; i++) {
            speciesList[i].setIndex(i);
        }

        IBox[] boxList = sim.getBoxs();
        for (int i=0; i<boxList.length; i++) {
            boxList[i].removeSpeciesNotify(removedSpecies);
        }

        for (int i=0; i<moleculeTypes.length; i++) {
            if (moleculeTypes[i] == removedSpecies) {
                ISpecies removedType = moleculeTypes[i];
                moleculeTypes = (ISpecies[])Arrays.removeObject(
                        moleculeTypes, removedType);
                atomTypeRemovedNotify(removedType);
                break;
            }
        }
        
        sim.getEventManager().fireEvent(new SimulationAtomTypeMaxIndexEvent(numAtomTypes));
        return true;
    }

    /**
     * Returns an array of the Species in the Simulation.
     */
    public ISpecies[] getSpecies() {
        return speciesList;
    }

    /**
     * This method notifies the SpeciesManager that the give atomType was added
     * to the system.  This method should be called by the AtomType at the top
     * of the AtomType hierarchy whenver it receives notification of a new
     * AtomType.
     */
    public void atomTypeAddedNotify(IAtomType newChildType) {
        if (newChildType instanceof IAtomTypeLeaf) {
            Element newElement = ((IAtomTypeLeaf)newChildType).getElement();
            Element oldElement = elementSymbolHash.get(newElement.getSymbol());
            if (oldElement != null && oldElement != newElement) {
                // having two AtomTypes with the same Element is OK, but having
                // two Elements with the same symbol is not allowed.
                throw new IllegalStateException("Element symbol "+newElement.getSymbol()+" already exists in this simulation as a different element");
            }
            // remember the element so we can check for future duplication
            elementSymbolHash.put(newElement.getSymbol(), newElement);
            LinkedList<IAtomType> atomTypeList = elementAtomTypeHash.get(newElement);
            if (atomTypeList == null) {
                atomTypeList = new LinkedList<IAtomType>();
                elementAtomTypeHash.put(newElement, atomTypeList);
            }
            atomTypeList.add(newChildType);
        }

        sim.getEventManager().fireEvent(new SimulationAtomTypeAddedEvent(newChildType));
    }

    /**
     * Reassigns indices from the reservoir to the given AtomTypes.
     */
    private void recycleIndices(IAtomType[] atomTypes) {
        // now iterate over remaining AtomTypes and re-use old indices that are
        // less than remaining indices
        for (int i=0; i<atomTypes.length; i++) {
            if (atomTypes[i].getIndex() >= numAtomTypes) {
                int oldIndex = atomTypes[i].getIndex();
                // give this type an index from the reservoir
                atomTypes[i].setIndex(requestTypeIndex());
                sim.getEventManager().fireEvent(new SimulationAtomTypeIndexChangedEvent(atomTypes[i], oldIndex));
                if (typeReservoirCount == typeIndexReservoir.length) {
                    // we ran out of indices to recycle
                    return;
                }
            }
            if (atomTypes[i] instanceof ISpecies) {
                recycleIndices(((ISpecies)atomTypes[i]).getChildTypes());
                if (typeReservoirCount == typeIndexReservoir.length) {
                    // we ran out of indices to recycle
                    return;
                }
            }
        }
    }

    /**
     * Returns an array of indices for the give parent AtomType.  The array
     * of indices does not include the given parent's index.
     */
    private static int[] getChildIndices(ISpecies atomType) {
        int[] childIndices = new int[0];
        for (int i=0; i<atomType.getChildTypes().length; i++) {
            IAtomType childType = atomType.getChildTypes()[i];
            childIndices = Arrays.resizeArray(childIndices, childIndices.length+1);
            childIndices[childIndices.length-1] = childType.getIndex();
        }
        return childIndices;
    }

    public int requestTypeIndex() {
        if (typeIndexReservoir == null) {
            // if no reservoir, just return the next index
            return ++numAtomTypes;
        }
        // if we have a reservoir, it's because we're in the middle of a
        // recycling pass
        return typeIndexReservoir[typeReservoirCount++];
    }

    public void atomTypeRemovedNotify(IAtomType removedType) {
        typeReservoirCount = 0;
        // put the removed AtomType indices in a reservoir
        if (removedType instanceof IAtomTypeLeaf) {
            Element oldElement = ((IAtomTypeLeaf)removedType).getElement();
            elementSymbolHash.remove(oldElement.getSymbol());
            typeIndexReservoir = new int[]{removedType.getIndex()};
        }
        else if (removedType instanceof ISpecies) {
            removeElements((ISpecies)removedType);
            int[] childIndices = null;
            childIndices = getChildIndices((ISpecies)removedType);
            typeIndexReservoir = new int[childIndices.length+1];
            numAtomTypes -= typeIndexReservoir.length;
            typeIndexReservoir[0] = removedType.getIndex();
            System.arraycopy(childIndices, 0, typeIndexReservoir, 1, childIndices.length);
            java.util.Arrays.sort(typeIndexReservoir);
        }
        // now replace any AtomType index that's higher than a removed index
        // with the removed index.
        
        typeReservoirCount = 0;
        for (int i=0; i<moleculeTypes.length; i++) {
            recycleIndices(moleculeTypes[i].getChildTypes());
        }
        typeIndexReservoir = null;
        typeReservoirCount = -1;
    }
    
    public ISpecies[] getMoleculeTypes() {
        return moleculeTypes;
    }

    /**
     * Removes all elements from the element hash which are children of the
     * given parent AtomType
     */
    private void removeElements(ISpecies oldParentType) {
        IAtomType[] oldChildTypes = oldParentType.getChildTypes();
        for (int i=0; i<oldChildTypes.length; i++) {
            if (oldChildTypes[i] instanceof IAtomTypeLeaf) {
                Element oldElement = ((IAtomTypeLeaf)oldChildTypes[i]).getElement();
                elementSymbolHash.remove(oldElement.getSymbol());
            }
            else if (oldChildTypes[i] instanceof ISpecies) {
                removeElements((ISpecies)oldChildTypes[i]);
            }
        }
    }

    /**
     * Returns an Element symbol starting with symbolBase that does not yet 
     * exist in the Simulation.  Return values will be like "base0, base1, base2..." 
     */
    public String makeUniqueElementSymbol(String symbolBase) {
        int n = 0;
        while (elementSymbolHash.containsKey(symbolBase+n)) {
            n++;
        }
        // reserve this symbol so future calls to makeUniqueElementSymbol won't return it
        // this will get repalced by the actual Element when it gets added via childTypeAddedNotify
        elementSymbolHash.put(symbolBase+n, null);
        return symbolBase+n;
    }

    private static final long serialVersionUID = 1L;
    private ISpecies[] speciesList;
    private final HashMap<String,Element> elementSymbolHash;
    private final HashMap<Element,LinkedList<IAtomType>> elementAtomTypeHash;
    private int numAtomTypes;
    private int[] typeIndexReservoir;
    private int typeReservoirCount;
    private final ISimulation sim;
    private ISpecies[] moleculeTypes;
}
