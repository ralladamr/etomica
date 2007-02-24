package etomica.config;

import etomica.atom.AtomArrayList;
import etomica.atom.AtomLeaf;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.space.IVector;
import etomica.space.Space;

/**
 * General class for a collection of linearly linked atoms.
 * 
 * @author nancycribbin
 */

public abstract class ConformationChain extends Conformation {

	public ConformationChain(Space space){	
		super(space);		
		atomIterator = new AtomIteratorArrayListSimple();
		//orientationVector = space.makeVector();
		//wrongNumberOfVectors = "Wrong number of vectors in the argument to ConformationChain subclass.";
	}
	
	/**
	 * Resets the vector instructions.
	 */
	protected abstract void reset();
	
	/**
	 * 
	 * @return the instructions to get to the location of the next molecule from
	 * the current one.
	 */
	protected abstract IVector nextVector();
	
	/**
	 * Places a set of atoms in a linearly connected fashion.
	 * @ param atomlist a list of atoms in the order in which they are linked
	 */
	public void initializePositions(AtomArrayList atomlist){
		
		//First, check that we actually have some atoms
		int size = atomlist.size();
        	if(size == 0) return;
        
        	atomIterator.setList(atomlist);
        	atomIterator.reset();
        
        	reset();
		
        	//space.makeVector() zeroes the made Vector automatically
        	IVector currentPosition = space.makeVector();
        
        	//Zero the first atom.
        	((AtomLeaf)atomIterator.nextAtom()).getCoord().getPosition().E(0.0);
        	
        	while(atomIterator.hasNext()){
        		AtomLeaf a = (AtomLeaf)atomIterator.nextAtom();
        		//TODO someday, we might want a to be a chunk-of-atoms
        		currentPosition.PE(nextVector());
        		a.getCoord().getPosition().E(currentPosition);
        	}
	}
	
	/**
	 * The vector drawn from the head of the molecule to the tail of the molecule.
	 */
/*	private Vector calculateOrientationVector(AtomList atomlist){
		//atomIterator.reset();
		Atom a = atomlist.getFirst();
		Atom z = atomlist.getLast();
		
		Vector ov = space.makeVector();
		
		ov.E(z.coord.position());
		ov.ME(a.coord.position());
		return ov;
	}
*/	
	
	//TODO Should we have a method here that moves the atom somehow- rotate, translate, etc.?
	
	private final AtomIteratorArrayListSimple atomIterator;

	//private Vector orientationVector;	
}
