package simulate;

public class MeterPotentialEnergy extends simulate.Meter
{
  AtomPair.Iterator iterator;
  
  public MeterPotentialEnergy() {
    super();
    setLabel("Potential Energy");
  }

 /**
  * Computes total potential energy for all atom pairs in phaseSpace
  */
    public final double currentValue() {
      AtomPair.Iterator.AM allIntra = new AtomPair.Iterator.AM(phaseSpace);
      AtomPair.Iterator.AMAM allInter = new AtomPair.Iterator.AMAM(phaseSpace);
      
                        //could make special case for single species, monatomic
      
      double pe = 0.0;
      for(Species s1=phaseSpace.firstSpecies(); s1!=null; s1=s1.nextSpecies()) {
        Potential1 p1 = phaseSpace.potential1[s1.speciesIndex];
        allIntra.reset(s1);
        while(allIntra.hasNext()) {
            AtomPair pair = allIntra.next();
            pe += p1.getPotential(pair.atom1(),pair.atom2()).energy(pair);
        }
        for(Species s2=s1; s2!=null; s2=s2.nextSpecies()) {
            Potential2 p2 = phaseSpace.potential2[s1.speciesIndex][s2.speciesIndex];
            allInter.reset(s1,s2);
            while(allInter.hasNext()) {
                AtomPair pair = allInter.next();
                pe += p2.getPotential(pair.atom1(),pair.atom2()).energy(pair);
            }
        }
      }
      return pe;
    }
  
  /**
   * Computes intermolecular contribution to potential energy for atom.  Returns zero if atom is not in phaseSpace.
   * Does not include intramolecular contribution
   */
    public final double currentValue(Atom a) {
        if(phaseSpace != a.phaseSpace()) {return 0.0;}  //also handles condition that phase contains no atoms
        AtomPair.Iterator.A iterator = phaseSpace.makePairIteratorFull();
        Potential2[] p2 = phaseSpace.potential2[a.getSpeciesIndex()];
        double pe = 0.0;
        if(a.parentMolecule != phaseSpace.firstMolecule()) { //loop from first atom to last one in molecule before a
            iterator.reset(phaseSpace.firstAtom(),a.previousMoleculeLastAtom(),a,a); 
            while(iterator.hasNext()) {
                AtomPair pair = iterator.next();  //following line counts on iterator to put inner-looping atom as atom2
                pe += p2[pair.atom2().getSpeciesIndex()].getPotential(pair.atom1(),pair.atom2()).energy(pair);
            }
        }
        if(a.parentMolecule != phaseSpace.lastMolecule()) {  //loop from first atom in next molecule to last atom in phase
            iterator.reset(a.nextMoleculeFirstAtom(),phaseSpace.lastAtom(),a,a);  
            while(iterator.hasNext()) {
                AtomPair pair = iterator.next();  //following line counts on iterator to put inner-looping atom as atom2
                pe += p2[pair.atom2().getSpeciesIndex()].getPotential(pair.atom1(),pair.atom2()).energy(pair);
            }
        }
        return pe;
    }


  /**
   * Computes total potential energy for atom (not present in phaseSpace), with all atoms in phaseSpace
   * Does not check to see if atom is actually in phaseSpace, which would contribute a (probably catastrophic) self-ineraction
   * Does not include intramolecular potential of atom
   */
/*    public final double insertionValue(AtomC a) {
      double pe = 0.0;
      Potential2[] p2 = phaseSpace.potential2[a.getSpeciesIndex()];
      for(AtomC b=(AtomC)phaseSpace.firstAtom(); b!=null; b=b.getNextAtomC()) {       //intermolecular
          if(b == a) System.out.println("atom error");
          pe += p2[b.getSpeciesIndex()].getPotential(a,b).energy(a,b);
      }
      return pe;
    }*/
    
 /**
  * Computes and returns the intermolecular contribution of a molecule to energy of phaseSpace
  * Does not include intramolecular energy of molecule
  * @return  this molecule's inter-molecular potential energy, divided by kB, in Kelvin
  */
    public final double currentValue(Molecule m) {
      AtomPair.Iterator.FMAM iterator = new AtomPair.Iterator.FMAM(phaseSpace,m);
      int index = m.parentSpecies.speciesIndex;     
      double pe = 0.0;
      for(Species s1=phaseSpace.firstSpecies(); s1!=null; s1=s1.nextSpecies()) {
        Potential2 p2 = phaseSpace.potential2[s1.speciesIndex][index];
        iterator.reset(s1);
        while(iterator.hasNext()) {
            AtomPair pair = iterator.next();
            pe += p2.getPotential(pair.atom1(),pair.atom2()).energy(pair);
        }
      }
      return pe;
    }
         //looks like these methods are exactly the same
 /**
  * Computes and returns the total intermolecular energy of a molecule with all molecules in phaseSpace
  * Does not include intramolecular energy of molecule
  */
    public final double insertionValue(Molecule m) {

      AtomPair.Iterator.FMAM iterator = new AtomPair.Iterator.FMAM(phaseSpace,m);
      int index = m.parentSpecies.speciesIndex;                 
      double pe = 0.0;
      for(Species s1=phaseSpace.firstSpecies(); s1!=null; s1=s1.nextSpecies()) {
        Potential2 p2 = phaseSpace.potential2[s1.speciesIndex][index];
        iterator.reset(s1);
        while(iterator.hasNext()) {
            AtomPair pair = iterator.next();
            pe += p2.getPotential(pair.atom1(),pair.atom2()).energy(pair);
        }
      }
      return pe;
    }

    
/*    private double upListInter(AtomC a) {
      double pe = 0.0;
      AtomC nextMoleculeAtom = (AtomC)a.nextMoleculeFirstAtom();  //first atom on next molecule
      Potential2[] p2 = phaseSpace.potential2[a.getSpeciesIndex()];
      for(AtomC b=nextMoleculeAtom; b!=null; b=b.getNextAtomC()) {       //intermolecular
          pe += p2[b.getSpeciesIndex()].getPotential(a,b).energy(a,b);
      }
      return pe;
    }

    private double downListInter(AtomC a) {
      double pe = 0.0;
      AtomC previousMoleculeAtom = (AtomC)a.previousMoleculeLastAtom();  //first atom on next molecule
      Potential2[] p2 = phaseSpace.potential2[a.getSpeciesIndex()];
      for(AtomC b=previousMoleculeAtom; b!=null; b=b.getPreviousAtomC()) {       //intermolecular
          pe += p2[b.getSpeciesIndex()].getPotential(a,b).energy(a,b);
      }
      return pe;
    }

    private double upListIntra(AtomC a) {
      double pe = 0.0;
      AtomC nextMoleculeAtom = (AtomC)a.nextMoleculeFirstAtom();  //first atom on next molecule
      Potential1 p1 = phaseSpace.potential1[a.getSpeciesIndex()];            
      for(AtomC b=a.getNextAtomC(); b!=nextMoleculeAtom; b=b.getNextAtomC()) { //intramolecular
          pe += p1.getPotential(a,b).energy(a,b);
      }
      return pe;
    }

    private double downListIntra(AtomC a) {
      double pe = 0.0;
      AtomC previousMoleculeAtom = (AtomC)a.previousMoleculeLastAtom();  //first atom on next molecule
      Potential1 p1 = phaseSpace.potential1[a.getSpeciesIndex()];            
      for(AtomC b=a.getPreviousAtomC(); b!=previousMoleculeAtom; b=b.getPreviousAtomC()) { //intramolecular
          pe += p1.getPotential(a,b).energy(a,b);
      }
      return pe;
    }*/

}
