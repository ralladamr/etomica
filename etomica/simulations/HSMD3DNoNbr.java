// Source file generated by Etomica

package etomica.simulations;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import etomica.ConfigurationLattice;
import etomica.Default;
import etomica.Phase;
import etomica.Simulation;
import etomica.Space;
import etomica.Species;
import etomica.SpeciesSpheresMono;
import etomica.action.PhaseImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomList;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeCubicFcc;
import etomica.potential.P2HardSphere;
import etomica.potential.PotentialMaster;
import etomica.space3d.Space3D;
import etomica.utility.EtomicaObjectInputStream;

public class HSMD3DNoNbr extends Simulation {

    public Phase phase;
    public IntegratorHard integrator;
    public SpeciesSpheresMono species;
    public P2HardSphere potential;
    
    public HSMD3DNoNbr() {
        this(Space3D.getInstance());
    }
    private HSMD3DNoNbr(Space space) {
        super(space, true, new PotentialMaster(space));

        int numAtoms = 256;
        Default.makeLJDefaults();
        Default.ATOM_SIZE = 1.0;
        Default.BOX_SIZE = 14.4573*Math.pow((numAtoms/2020.0),1.0/3.0);

        integrator = new IntegratorHard(potentialMaster);
        integrator.setIsothermal(false);
        integrator.setTimeStep(0.01);

        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheresMono(this);
        species.setNMolecules(numAtoms);
        potential = new P2HardSphere(space);
        this.potentialMaster.setSpecies(potential,new Species[]{species,species});

        phase = new Phase(this);
//        phase.setBoundary(new BoundaryTruncatedOctahedron(space));
        integrator.addPhase(phase);
        integrator.addListener(new PhaseImposePbc(phase));
        new ConfigurationLattice(new LatticeCubicFcc()).initializeCoordinates(phase);
        
        //ColorSchemeByType.setColor(speciesSpheres0, java.awt.Color.blue);

 //       MeterPressureHard meterPressure = new MeterPressureHard(integrator);
 //       DataAccumulator accumulatorManager = new DataAccumulator(meterPressure);
        // 	DisplayBox box = new DisplayBox();
        // 	box.setDatumSource(meterPressure);
 //       phase.setDensity(0.7);
    } //end of constructor

    public static void main( String[] args )
    {
    	String filename = "test.bin";
		
    	try
    	{
    	    FileOutputStream fos = null;
    	    ObjectOutputStream out = null;
    	    HSMD3DNoNbr simulation = new HSMD3DNoNbr();
    	    fos = new FileOutputStream( filename);
			out = new ObjectOutputStream(fos);
			out.writeObject( simulation );
			out.close();
			fos.close();
			System.out.println( "Serialization of class HSMD3DNoNbr succeeded.");
    	}
    	catch(IOException ex)
    	{
    	    System.err.println( "Exception:" + ex.getMessage() );
    	    ex.printStackTrace();
    	}
    	
    	// Serialize back
    	try
    	{
    	    FileInputStream fis = null;
    	    EtomicaObjectInputStream in = null;
    	    fis = new FileInputStream(filename);
    	    in = new EtomicaObjectInputStream(fis);
    	    Simulation simulation = (etomica.Simulation) in.readObject();
    	    AtomList.rebuildAllLists(in);
    	    in.close();
    	    fis.close();
    	    
    	    System.out.println( "DeSerialization of class HSMD3DNoNbr succeeded.");

    	    // go daddy
    	    simulation.getController().run();
    	    System.out.println( "Simulation run ok");
    	}
    	catch( Exception ex ) {
    	    System.err.println( "Could not read simulation from file " + filename + ". Cause: " + ex.getMessage() );
    	    ex.printStackTrace();
    	}
		
		
    }
}//end of class
