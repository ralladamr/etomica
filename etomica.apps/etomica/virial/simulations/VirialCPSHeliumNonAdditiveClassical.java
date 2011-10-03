package etomica.virial.simulations;


import java.awt.Color;

import etomica.action.IAction;
import etomica.api.IAtomList;
import etomica.api.IVectorMutable;
import etomica.data.IData;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.SimulationGraphic;
import etomica.potential.P2HePCKLJS;
import etomica.potential.P3CPSNonAdditiveHeSimplified;
import etomica.space.ISpace;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;
import etomica.util.Constants;
import etomica.util.ParameterBase;
import etomica.virial.ClusterAbstract;
import etomica.virial.ClusterSumNonAdditiveTrimerEnergy;
import etomica.virial.MayerFunction;
import etomica.virial.MayerGeneralSpherical;
import etomica.virial.MayerHardSphere;
import etomica.virial.SpeciesFactorySpheres;
import etomica.virial.cluster.Standard;

/* 
 * Adapted by Kate from VirialGCPM
 * 
 * Computes only the nonadditive component of either the third, fourth, or fifth virial coefficient for the
 * ab initio non-additive trimer potential for He developed by Cencek, Patkowski, and Szalewicz (JCP 131 064105 2009). 
 * 
 * 
 */


public class VirialCPSHeliumNonAdditiveClassical {
	
	protected static IVectorMutable r0, r1, r2;
	protected static IVectorMutable r01Vec, r12Vec, r02Vec;
	
	public VirialCPSHeliumNonAdditiveClassical(ISpace space) {
        r0 = space.makeVector();
        r1 = space.makeVector();
        r2 = space.makeVector();
        r01Vec = space.makeVector();
        r12Vec = space.makeVector();
        r02Vec = space.makeVector();
	}

    public static void main(String[] args) {
    	
    	Space space = Space3D.getInstance();

    	VirialCPSHeliumNonAdditiveClassical calc = new VirialCPSHeliumNonAdditiveClassical(space);
      

        VirialParam params = new VirialParam();
        
        double temperatureK; final int nPoints; double sigmaHSRef;
        long steps; int nullRegionMethod; int stepsPerBlock; long eqSteps; boolean adjustStepFreq;
        if (args.length == 0) {
        	
        	nPoints = params.nPoints;
            temperatureK = params.temperature;
            steps = params.numSteps;
            stepsPerBlock = params.stepsPerBlock;
            eqSteps = params.eqSteps;
            adjustStepFreq = params.adjustStepFreq;
            sigmaHSRef = params.sigmaHSRef;
            nullRegionMethod = params.nullRegionMethod;
            
            // number of overlap sampling steps
            // for each overlap sampling step, the simulation boxes are allotted
            // 1000 attempts for MC moves, total
            
        } else if (args.length == 8) {
            //ReadParameters paramReader = new ReadParameters(args[0], params);
            //paramReader.readParameters();
        	nPoints = Integer.parseInt(args[0]);
        	temperatureK = Double.parseDouble(args[1]);
            steps = Integer.parseInt(args[2]);
            stepsPerBlock = Integer.parseInt(args[3]);
            eqSteps = Integer.parseInt(args[4]);
            adjustStepFreq = Boolean.parseBoolean(args[5]);
            sigmaHSRef = Double.parseDouble(args[6]);
            nullRegionMethod = Integer.parseInt(args[7]);
            params.writeRefPref = true;
        	
        } else {
        	throw new IllegalArgumentException("Incorrect number of arguments passed.");
        }
        

        int numSubSteps = 1000;

        final double[] HSB = new double[7];
        HSB[2] = Standard.B2HS(sigmaHSRef);
        HSB[3] = Standard.B3HS(sigmaHSRef);
        HSB[4] = Standard.B4HS(sigmaHSRef);
        HSB[5] = Standard.B5HS(sigmaHSRef);
        HSB[6] = Standard.B6HS(sigmaHSRef);

        System.out.println("sigmaHSRef: "+sigmaHSRef);
        System.out.println("B"+nPoints+"HS: "+HSB[nPoints]);
        System.out.println("Helium overlap sampling B"+nPoints+"NonAdd at T="+temperatureK+ " K");
        System.out.println("null region method = "+nullRegionMethod);
        
        double temperature = Kelvin.UNIT.toSim(temperatureK);

        System.out.println(steps+" steps ("+steps/1000+" blocks of 1000)");
        steps /= 1000;

        

        
        
        P2HePCKLJS p2 = new P2HePCKLJS(space);
        final P3CPSNonAdditiveHe p3NonAdd = new P3CPSNonAdditiveHe(space);
        //final P3CPSNonAdditiveHeSimplified p3NonAdd = new P3CPSNonAdditiveHeSimplified(space);
        p3NonAdd.setNullRegionMethod(nullRegionMethod);
    	MayerGeneralSpherical fTarget = new MayerGeneralSpherical(p2);
    	ClusterSumNonAdditiveTrimerEnergy targetCluster = Standard.virialNonAdditiveTrimerEnergy(nPoints, fTarget, p3NonAdd, nPoints>3, false);
    	targetCluster.setNo72B2B3NonAdd(false);
    	targetCluster.setTemperature(temperature);

    	
    	MayerHardSphere fRef = new MayerHardSphere(sigmaHSRef);
        ClusterAbstract refCluster = Standard.virialCluster(nPoints, (MayerFunction)fRef, nPoints>3, null, false);
        refCluster.setTemperature(temperature);


        final SimulationVirialOverlap sim = new SimulationVirialOverlap(space,new SpeciesFactorySpheres(), 
                temperature, refCluster,targetCluster, false);
        
        sim.integratorOS.setAdjustStepFreq(adjustStepFreq);
        System.out.println("adjustStepFreq = " + adjustStepFreq);
        
        sim.setAccumulatorBlockSize(stepsPerBlock);
        System.out.println(stepsPerBlock+" steps per block");
        
        ///////////////////////////////////////////////
        // Initialize non-overlapped configuration
        ///////////////////////////////////////////////
        
        IAtomList atoms = sim.box[1].getLeafList();
        if (nPoints == 3) {
	        for (int i=1;i<atoms.getAtomCount();i++) {
	        	atoms.getAtom(i).getPosition().setX(0, i*sigmaHSRef);
	        }
        } else if (nPoints == 4) {
	        
	        atoms.getAtom(1).getPosition().setX(0, sigmaHSRef);
	        
	        atoms.getAtom(2).getPosition().setX(0, sigmaHSRef);
	        atoms.getAtom(2).getPosition().setX(1, sigmaHSRef);
	        
	        atoms.getAtom(3).getPosition().setX(1, sigmaHSRef);
	        
        } else if (nPoints == 5) {
        	
        	atoms.getAtom(1).getPosition().setX(0, sigmaHSRef);
        	atoms.getAtom(1).getPosition().setX(1, sigmaHSRef);
        	
	        atoms.getAtom(2).getPosition().setX(0, sigmaHSRef);
	        atoms.getAtom(2).getPosition().setX(1, -sigmaHSRef);
	        
	        atoms.getAtom(3).getPosition().setX(0, -sigmaHSRef);
	        atoms.getAtom(3).getPosition().setX(1, sigmaHSRef);
	        
	        atoms.getAtom(4).getPosition().setX(0, -sigmaHSRef);
	        atoms.getAtom(4).getPosition().setX(1, -sigmaHSRef);
	        
        } else {
        	throw new RuntimeException("Wrong number of points");
        }
        
        /*
        IAtomList atoms0 = sim.box[0].getLeafList();
        for (int i=1;i<atoms0.getAtomCount();i++) {
        	atoms0.getAtom(i).getPosition().setX(0, i*sigmaHSRef*1.3);
        } */ 
        
        if (false) {
            sim.box[0].getBoundary().setBoxSize(space.makeVector(new double[]{10,10,10}));
            sim.box[1].getBoundary().setBoxSize(space.makeVector(new double[]{10,10,10}));
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, space, sim.getController());
            simGraphic.getDisplayBox(sim.box[0]).setShowBoundary(false);
            simGraphic.getDisplayBox(sim.box[1]).setShowBoundary(false);
            SpeciesSpheresMono species = (SpeciesSpheresMono)sim.getSpecies(0);
            ((ColorSchemeByType)simGraphic.getDisplayBox(sim.box[0]).getColorScheme()).setColor(species.getAtomType(0), Color.WHITE);
            ((ColorSchemeByType)simGraphic.getDisplayBox(sim.box[1]).getColorScheme()).setColor(species.getAtomType(0), Color.WHITE);
            simGraphic.makeAndDisplayFrame();
    
            sim.integratorOS.setNumSubSteps(numSubSteps);
            sim.setAccumulatorBlockSize(1000);
                
            // if running interactively, set filename to null so that it doens't read
            // (or write) to a refpref file
            sim.getController().removeAction(sim.ai);
//            sim.getController().addAction(new IAction() {
//                public void actionPerformed() {
//                    sim.initRefPref(null, 0);
//                    sim.equilibrate(null,0);
//                    sim.ai.setMaxSteps(Long.MAX_VALUE);
//                }
//            });
            sim.getController().addAction(sim.ai);
            if ((Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref) || sim.refPref == 0)) {
                throw new RuntimeException("Oops");
            }
            
            return;
        }

        // if running interactively, don't use the file
        String refFileName = args.length > 0 ? "refpref"+nPoints+"_"+params.temperature : null;
        // this will either read the refpref in from a file or run a short simulation to find it
        sim.initRefPref(refFileName, steps/40);
        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(refFileName, eqSteps); // 5000 IntegratorOverlap steps = 5e6 steps
        System.out.println((eqSteps*1000) + " equilibration steps (" + eqSteps + " Integrator Overlap Steps)"); 
        if (sim.refPref == 0 || Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref)) {
            throw new RuntimeException("oops");
        }
        
        sim.setAccumulatorBlockSize((int)steps);
        
        System.out.println("equilibration finished");
        System.out.println("MC Move step sizes (ref)    "+sim.mcMoveTranslate[0].getStepSize());
        System.out.println("MC Move step sizes (target) "+sim.mcMoveTranslate[1].getStepSize());
        
        IAction progressReport = new IAction() {
            public void actionPerformed() {
                //System.out.print(sim.integratorOS.getStepCount()+" steps: ");
                IAtomList atoms = sim.box[1].getLeafList();
                
                r0.E( atoms.getAtom(0).getPosition() );
                r1.E( atoms.getAtom(1).getPosition() );
                r2.E( atoms.getAtom(2).getPosition() );
                
                r01Vec.Ev1Mv2(r0,r1);
                r02Vec.Ev1Mv2(r0,r2);
                r12Vec.Ev1Mv2(r1,r2);
                
	    		double r01 = (Math.sqrt(r01Vec.squared()));
	    		double r02 = (Math.sqrt(r02Vec.squared()));
	    		double r12 = (Math.sqrt(r12Vec.squared()));
	    		 
	    		
	    		double U = Kelvin.UNIT.fromSim(p3NonAdd.energy(atoms));
	    		 System.out.println(r01+"  "+r02+"  " +r12+"  " +U);
                
            }
        };
        //sim.integratorOS.addIntervalAction(progressReport);
        //sim.integratorOS.setActionInterval(progressReport, (int)(steps/10));
        //sim.integratorOS.getEventManager().addListener(new IntegratorListenerAction(progressReport, 1 ));
        
        sim.integratorOS.getMoveManager().setEquilibrating(false);
        sim.ai.setMaxSteps(steps);
        sim.getController().actionPerformed();

        System.out.println("final reference step frequency "+sim.integratorOS.getStepFreq0());
        System.out.println("actual reference step frequency "+sim.integratorOS.getActualStepFreq0());
        
        double[] ratioAndError = sim.dsvo.getOverlapAverageAndError();
        double ratio = ratioAndError[0];
        double error = ratioAndError[1];
        System.out.println("ratio average: "+ratio+", error: "+error);
        System.out.println("abs average: "+ratio*HSB[nPoints]+", error: "+error*HSB[nPoints]);
        DataGroup allYourBase = (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("reference ratio average: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].RATIO_ERROR.index)).getData()[1]);
        System.out.println("reference average: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].ERROR.index)).getData()[0]);
        System.out.println("reference overlap average: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].ERROR.index)).getData()[1]);
        
        
        System.out.println("reference autocorrelation function: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].BLOCK_CORRELATION.index)).getData()[0]);
        
        System.out.println("reference overlap autocorrelation function: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[0].BLOCK_CORRELATION.index)).getData()[1]);
        
        System.out.println();
        
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.accumulators[1].getNBennetPoints()-sim.dsvo.minDiffLocation()-1);
        System.out.println("target ratio average: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].RATIO_ERROR.index)).getData()[1]);
        System.out.println("target average: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].ERROR.index)).getData()[0]);
        System.out.println("target overlap average: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].ERROR.index)).getData()[1]);

        
        System.out.println("target autocorrelation function: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].BLOCK_CORRELATION.index)).getData()[0]);
        
        System.out.println("target overlap autocorrelation function: "+((DataDoubleArray)allYourBase.getData(sim.accumulators[1].BLOCK_CORRELATION.index)).getData()[1]);
    
        
        System.out.println();
        System.out.println("cm"+((nPoints-1)*3)+"/mol"+(nPoints-1)+": ");
        System.out.println("abs average: "+ratio*HSB[nPoints]*Math.pow(Constants.AVOGADRO*1e-24,nPoints-1)+", error: "+error*HSB[nPoints]*Math.pow(Constants.AVOGADRO*1e-24,nPoints-1));
	}



    /**
     * Inner class for parameters
     */
    public static class VirialParam extends ParameterBase {
        public int nPoints = 3;
        public double temperature = 50.0;   // Kelvin
        public long numSteps = 1000000;
        public int stepsPerBlock = 1000;
        public long eqSteps=1000;
        public boolean adjustStepFreq = false;
        public double sigmaHSRef = 3;
        public int nullRegionMethod = 1;
        public boolean writeRefPref;
    }
}
