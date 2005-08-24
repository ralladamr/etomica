/*
 * History
 * Created on Aug 31, 2004 by kofke
 */
package etomica.atom.iterator;

import etomica.species.Species;

/**
 * Iterator that is set using a Species.  Extends
 * AtomsetIteratorPhaseDependent because the species-dependent
 * iterates are necessarily phase-dependent too.
 */
public interface AtomsetIteratorSpeciesDependent extends
		AtomsetIteratorPhaseDependent {

	public void setSpecies(Species species);
}
