/*
 * History
 * Created on Aug 31, 2004 by kofke
 */
package etomica.atom.iterator;

import etomica.species.Species;

/**
 * Atom iterator that is set a Species.  Extends
 * AtomIteratorPhaseDependent because the species-dependent
 * iterates are necessarily phase-dependent too.
 */
public interface AtomIteratorSpeciesDependent extends
		AtomIteratorPhaseDependent, AtomsetIteratorSpeciesDependent {

	public void setSpecies(Species species);
}
