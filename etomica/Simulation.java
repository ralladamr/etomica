package simulate;

import java.awt.*;
import java.awt.event.*;
import java.beans.Beans;
import java.util.*;

public class Simulation extends Panel {

    public static int D;  //dimension (2-D, 3-D, etc;)
    protected final Space space;

    public Controller controller;
    Phase firstPhase;
    Phase lastPhase;
    Display firstDisplay;
    Display lastDisplay;
              
    public Simulation(Space s) {
        space = s;
        D = space.D();
        setSize(400,300);
    }
              
    public int getD() {return space.D();}
    public Space space() {return space;}

    public void add(Controller c) {
        if(controller != null) {return;}  //already added a controller
        super.add(c);
        controller = c;
        c.parentSimulation = this;
    }
                
    public void add(Display d) {
        super.add(d);
        addDisplay(d);
    }
    
    public void addDisplay(Display d) {
        d.parentSimulation = this;
        if(lastDisplay != null) {lastDisplay.setNextDisplay(d);}
        else {firstDisplay = d;}
        lastDisplay = d;
        if(haveIntegrator()) {
            controller.integrator.addIntegrationIntervalListener(d);
        }
//        if(d.displayTool != null) {super.add(d.displayTool);}
        for(Phase p=firstPhase; p!=null; p=p.nextPhase()) {
            d.setPhase(p);
        }
        d.repaint();
    }    
              
    public void add(Phase p) {
        super.add(p);
        p.initialize(this);
        if(lastPhase != null) {lastPhase.setNextPhase(p);}
        else {firstPhase = p;}
        lastPhase = p;
        if(haveIntegrator()) {
            controller.integrator.registerPhase(p);
            p.gravity.addObserver(controller.integrator);
            p.integrator = controller.integrator;
        }
        for(Display d=firstDisplay; d!=null; d=d.getNextDisplay()) {d.setPhase(p);}
        for(Species s=firstSpecies; s!=null; s=s.nextSpecies()) {p.add(s.makeAgent(p));}
    }
    
    //Make a real phase and copy properties from virtual phase
    //Temporary method to permit manipulation of phase at design time (without customizer)
//    public void add(VirtualPhase p) {
//        Phase phase = space.makePhase(Space2D.Boundary.PERIODIC);             //specific to 2D
//        for(Meter m=p.firstMeter; m!=null; m=m.nextMeter()) {phase.add(m);}
//        phase.add(p.configuration);
//        phase.setG(p.getG());
//        add(phase);
//    }
              
    public void add(Species species) {
        species.parentSimulation = this;
        if(lastSpecies != null) {lastSpecies.setNextSpecies(species);}
        else {firstSpecies = species;}
        lastSpecies = species;
        if(species.getSpeciesIndex() > speciesCount-1) {setSpeciesCount(species.getSpeciesIndex()+1);}
        species.setNMolecules(species.getNMolecules());
        species.add(species.colorScheme);
        species.add(species.configurationMolecule);
        for(Phase p=firstPhase; p!=null; p=p.nextPhase()) {p.add(species.makeAgent(p));}
    }
                
    /* Resizes potential arrays, keeping all elements already filled in, and
    setting to p1Null or p2IdealGas the newly formed elements
    */
    private void setSpeciesCount(int n) {
        Potential1 p1[] = new Potential1[n];
        Potential2 p2[][] = new Potential2[n][n];
        for(int i=0; i<speciesCount; i++) {
            p1[i] = potential1[i];
            p2[i][i] = potential2[i][i];
            for(int j=i+1; j<speciesCount; j++) {        //could use system arraycopy
                p2[i][j] = p2[j][i] = potential2[i][j];
                            
            }
        }
        for(int i=speciesCount; i<n; i++) {
            p1[i] = p1Null;
            p2[i][i] = p2IdealGas;
            for(int j=0; j<n; j++) {
                p2[i][j] = p2[j][i] = p2IdealGas;
            }
        }
        potential1 = p1;
        potential2 = p2;
        speciesCount = n;
    }
              
    public void add(Potential1 p1) {
        super.add(p1);
        p1.setSimulation(this);
        if(p1.speciesIndex+1 > speciesCount) {setSpeciesCount(p1.speciesIndex+1);}
        this.potential1[p1.speciesIndex] = p1;
    }
                
    public void add(Potential2 p2) {
        super.add(p2);
        p2.setSimulation(this);
        int idx = Math.max(p2.species1Index,p2.species2Index);
        if(idx+1 > speciesCount) {setSpeciesCount(idx+1);}
        this.potential2[p2.species1Index][p2.species2Index] = p2;
        this.potential2[p2.species2Index][p2.species1Index] = p2;
    }
                
    public Phase firstPhase() {return firstPhase;}
    public Phase lastPhase() {return lastPhase;}
              
    public boolean haveIntegrator() {
        return (controller != null && controller.integrator != null);
    }
    
    //This can be made much more clever
    public Potential getPotential(AtomPair pair) {
        if(pair.atom1() == pair.atom2()) {
            return pair.atom1().parentPhase().potential();}  //should rewrite AtomPair to hold phase
        else {
            return potential2[pair.atom2().getSpeciesIndex()][pair.atom1().getSpeciesIndex()].getPotential(pair.atom1(),pair.atom2());
        }
    }
    
    /**
    * Total number of species contained in this phase.
    */
    int speciesCount=0;
      
    /**
    * Symmetric array of all two-body potentials.  Potentials are associated with species, and each species
    * is assigned a unique index to idenfity it.  Potential2[i][j] is the two-body potential
    * for Species indexed i and j, respectively.  The potential for i=j is merely the one describing the 
    * molecular interactions for that species.
    * 
    * @see Species#speciesIndex
    * @see Potential2
    */
    public Potential2[][] potential2;
      
    /**
    * Array of all one-body potentials.  Potentials are associated with species, and each species
    * is assigned a unique index to idenfity it.  Potential1[i] is the one-body potential
    * for Species indexed i.
    * 
    * @see Species#speciesIndex
    * @see Potential1
    */
    public Potential1[] potential1;
      
    /**
    * First species in the linked list of species in this phase.
    */
    Species firstSpecies;
     
    /**
    * Last species in the linked list of species in this phase.
    */
    Species lastSpecies;
            
    private Potential1 p1Null = new P1Null();
    private Potential2 p2IdealGas = new P2IdealGas();
}


