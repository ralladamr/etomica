package etomica.virial.simulations;


import java.awt.Color;

import etomica.api.IAtomList;
import etomica.api.IIntegratorEvent;
import etomica.api.IIntegratorListener;
import etomica.api.IVectorMutable;
import etomica.chem.elements.ElementSimple;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.SimulationGraphic;
import etomica.potential.IPotentialAtomicMultibody;
import etomica.potential.P2HePCKLJS;
import etomica.potential.P2HeSimplified;
import etomica.potential.P3CPSNonAdditiveHe;
import etomica.potential.P3CPSNonAdditiveHeSimplified;
import etomica.potential.Potential2Spherical;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;
import etomica.util.DoubleRange;
import etomica.util.HistogramNotSoSimple;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.virial.ClusterAbstract;
import etomica.virial.ClusterBonds;
import etomica.virial.ClusterDifference;
import etomica.virial.ClusterSum;
import etomica.virial.ClusterSumMultibody;
import etomica.virial.CoordinatePairSet;
import etomica.virial.MayerFunction;
import etomica.virial.MayerFunctionNonAdditive;
import etomica.virial.MayerFunctionSphericalThreeBody;
import etomica.virial.MayerGeneralSpherical;
import etomica.virial.MayerHardSphere;
import etomica.virial.cluster.Standard;
import etomica.virial.cluster.VirialDiagrams;

/* 
 * Adapted by Kate from VirialGCPM
 * 
 * Computes only the nonadditive component of either the third, fourth, or fifth virial coefficient for the
 * ab initio non-additive trimer potential for He developed by Cencek, Patkowski, and Szalewicz (JCP 131 064105 2009). 
 * 
 * 
 */


public class VirialHeNonAdditive {

    public static void main(String[] args) {

        VirialParam params = new VirialParam();
        boolean isCommandline = args.length > 0;
        ParseArgs.doParseArgs(params, args);
        
    	final int nPoints = params.nPoints;
    	final double temperatureK = params.temperature;
        long steps = params.numSteps;
        final double sigmaHSRef = params.sigmaHSRef;
        final boolean semiClassical = params.semiClassical;
        final int nullRegionMethod = params.nullRegionMethod;
        double refFrac = params.refFrac;
        final boolean subtractApprox = params.subtractApprox;
        final boolean calcApprox = !subtractApprox && params.calcApprox;
        final boolean minMulti = params.minMulti;
        
        final double[] HSB = new double[7];
        HSB[2] = Standard.B2HS(sigmaHSRef);
        HSB[3] = Standard.B3HS(sigmaHSRef);
        HSB[4] = Standard.B4HS(sigmaHSRef);
        HSB[5] = Standard.B5HS(sigmaHSRef);
        HSB[6] = Standard.B6HS(sigmaHSRef);

        System.out.println("sigmaHSRef: "+sigmaHSRef);
        System.out.println("B"+nPoints+"HS: "+HSB[nPoints]);
        System.out.println("Helium overlap sampling B"+nPoints+"NonAdd at T="+temperatureK+ " K");
        System.out.println("Using "+(semiClassical ? "semi" : "")+"classical pair potential");
        System.out.println("null region method = "+nullRegionMethod);
        if (calcApprox) System.out.println("Calculating coefficients for approximate potential");
        if (subtractApprox) {
            System.out.println("computing difference from approximate He");
        }

        final double temperature = Kelvin.UNIT.toSim(temperatureK);

        System.out.println(steps+" steps (1000 blocks of "+steps/1000+")");
        steps /= 1000;

        Space space = Space3D.getInstance();

        
        MayerHardSphere fRef = new MayerHardSphere(sigmaHSRef);
        
        MayerGeneralSpherical fTarget;
        MayerGeneralSpherical fTargetApprox;
        if (semiClassical) {
            P2HeSimplified p2cApprox = new P2HeSimplified(space);
            Potential2Spherical p2Approx = p2cApprox.makeQFH(temperature);
            
            P2HePCKLJS p2c = new P2HePCKLJS(space);
            Potential2Spherical p2 = p2c.makeQFH(temperature);

            fTarget = new MayerGeneralSpherical(calcApprox ? p2Approx : p2);
            fTargetApprox = new MayerGeneralSpherical(p2Approx);

        } else {
            P2HeSimplified p2Approx = new P2HeSimplified(space);
            
            P2HePCKLJS p2 = new P2HePCKLJS(space);

            fTarget = new MayerGeneralSpherical(calcApprox ? p2Approx : p2);
            fTargetApprox = new MayerGeneralSpherical(p2Approx);
        }

        IPotentialAtomicMultibody p3 = new P3CPSNonAdditiveHe(space);
        P3CPSNonAdditiveHeSimplified p3Approx = new P3CPSNonAdditiveHeSimplified(space);
        p3Approx.setParameters(temperatureK);

        final MayerFunctionSphericalThreeBody f3Target = new MayerFunctionSphericalThreeBody(calcApprox ? p3Approx : p3);
        

        VirialDiagrams flexDiagrams = new VirialDiagrams(nPoints, true, false);
        flexDiagrams.setDoMinimalMulti(true);
        flexDiagrams.setDoMultiFromPair(!minMulti);
        flexDiagrams.setDoMinimalBC(true);
        flexDiagrams.setDoReeHoover(true);
        flexDiagrams.setDoShortcut(true);
        ClusterSum fullTargetCluster = flexDiagrams.makeVirialCluster(fTarget, f3Target, false);

        VirialDiagrams rigidDiagrams = new VirialDiagrams(nPoints, false, false);
        rigidDiagrams.setDoReeHoover(true);
        rigidDiagrams.setDoShortcut(true);
        rigidDiagrams.setAllPermutations(true);
        ClusterSum refCluster = rigidDiagrams.makeVirialCluster(fRef);


        ClusterAbstract targetCluster = null;
        if (subtractApprox) {
            final ClusterSum[] targetSubtract = new ClusterSum[1];
            ClusterBonds[] minusBonds = fullTargetCluster.getClusters();
            double[] wMinus = fullTargetCluster.getWeights();
            MayerFunctionSphericalThreeBody f3TargetApprox = new MayerFunctionSphericalThreeBody(p3Approx);
            targetSubtract[0] = new ClusterSumMultibody(minusBonds, wMinus, new MayerFunction[]{fTargetApprox}, new MayerFunctionNonAdditive[]{f3TargetApprox});
            targetCluster = new ClusterDifference(fullTargetCluster, targetSubtract);
        }
        else {
            targetCluster = fullTargetCluster;
        }

        targetCluster.setTemperature(temperature);
    	
        refCluster.setTemperature(temperature);


        final SimulationVirialOverlap2 sim = new SimulationVirialOverlap2(space,new SpeciesSpheresMono(space, new ElementSimple("A")), 
                temperature, refCluster,targetCluster, false);
        sim.integratorOS.setAgressiveAdjustStepFraction(true);
        
        ///////////////////////////////////////////////
        // Initialize non-overlapped configuration
        ///////////////////////////////////////////////
        
        IAtomList atoms = sim.box[1].getLeafList();
        double r = 4;
        for (int i=1; i<nPoints; i++) {
            IVectorMutable v = atoms.getAtom(i).getPosition();
            v.setX(0, r*Math.cos(2*(i-1)*Math.PI/(nPoints-1)));
            v.setX(1, r*Math.sin(2*(i-1)*Math.PI/(nPoints-1)));
        }
        sim.box[1].trialNotify();
        sim.box[1].acceptNotify();
//        System.out.println(targetCluster.value(sim.box[1]));
//        System.exit(1);
        
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
    
            sim.integratorOS.setNumSubSteps(1000);
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

        long t1 = System.currentTimeMillis();
        // if running interactively, don't use the file
        String refFileName = null;
        if (isCommandline) {
            // if running interactively, don't use the file
            String tempString = ""+temperatureK;
            if (temperatureK == (int)temperatureK) {
                // temperature is an integer, use "200" instead of "200.0"
                tempString = ""+(int)temperatureK;
            }
            refFileName = "refpref"+nPoints+"_3b_"+tempString;
            refFileName += semiClassical ? "_sc" : "_c";
            if (calcApprox) {
                refFileName += "a";
            }
            else if (subtractApprox) {
                refFileName += "sa";
            }
        }
        // this will either read the refpref in from a file or run a short simulation to find it
        sim.initRefPref(refFileName, steps/40);

        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(refFileName, steps/20);
        if (sim.refPref == 0 || Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref)) {
            throw new RuntimeException("oops");
        }
        
        sim.setAccumulatorBlockSize((int)steps);
        sim.integratorOS.setNumSubSteps((int)steps);
        
        System.out.println("equilibration finished");
        System.out.println("MC Move step sizes (ref)    "+sim.mcMoveTranslate[0].getStepSize());
        System.out.println("MC Move step sizes (target) "+sim.mcMoveTranslate[1].getStepSize());
        
        final HistogramNotSoSimple hist = new HistogramNotSoSimple(100, new DoubleRange(0, sigmaHSRef));
        final HistogramNotSoSimple piHist = new HistogramNotSoSimple(100, new DoubleRange(0, sigmaHSRef));
        final ClusterAbstract finalTargetCluster = targetCluster.makeCopy();
        IIntegratorListener histListener = new IIntegratorListener() {
            public void integratorStepStarted(IIntegratorEvent e) {}
            
            public void integratorStepFinished(IIntegratorEvent e) {
                double r2Max = 0;
                CoordinatePairSet cPairs = sim.box[0].getCPairSet();
                for (int i=0; i<nPoints; i++) {
                    for (int j=i+1; j<nPoints; j++) {
                        double r2ij = cPairs.getr2(i, j);
                        if (r2ij > r2Max) r2Max = r2ij;
                    }
                }
                double v = sim.box[1].getSampleCluster().value(sim.box[0]);
                hist.addValue(Math.sqrt(r2Max), v);
                piHist.addValue(Math.sqrt(r2Max), Math.abs(v));
            }
            
            public void integratorInitialized(IIntegratorEvent e) {}
        };
        IIntegratorListener progressReport = new IIntegratorListener() {
            public void integratorInitialized(IIntegratorEvent e) {}
            public void integratorStepStarted(IIntegratorEvent e) {}
            public void integratorStepFinished(IIntegratorEvent e) {
//                if (Double.isInfinite(sim.dsvo.getOverlapAverageAndError()[0])) {
//                    sim.dsvo.getOverlapAverageAndError();
//                    throw new RuntimeException("oops");
//                }
                if ((sim.integratorOS.getStepCount()*10) % sim.ai.getMaxSteps() != 0) return;
                if (Double.isInfinite(sim.dvo.getAverageAndError()[0])) {
                    sim.dvo.getAverageAndError();
                    throw new RuntimeException("oops");
                }
                System.out.print(sim.integratorOS.getStepCount()+" steps: ");
                double[] ratioAndError = sim.dvo.getAverageAndError();
                System.out.println("abs average: "+ratioAndError[0]*HSB[nPoints]+", error: "+ratioAndError[1]*HSB[nPoints]);
            }
        };
        if (!isCommandline) {
            sim.integratorOS.getEventManager().addListener(progressReport);
            if (params.doHist) {
                IIntegratorListener histReport = new IIntegratorListener() {
                    public void integratorInitialized(IIntegratorEvent e) {}
                    public void integratorStepStarted(IIntegratorEvent e) {}
                    public void integratorStepFinished(IIntegratorEvent e) {
                        if ((sim.integratorOS.getStepCount()*10) % sim.ai.getMaxSteps() != 0) return;
                        double[] xValues = hist.xValues();
                        double[] h = hist.getHistogram();
                        double[] piH = piHist.getHistogram();
                        for (int i=0; i<xValues.length; i++) {
                            if (!Double.isNaN(h[i])) {
                                System.out.println(xValues[i]+" "+h[i]+" "+piH[i]);
                            }
                        }
                    }
                };
                sim.integratorOS.getEventManager().addListener(histReport);
            }
        }
        

        if (refFrac >= 0) {
            if (params.doHist) {
                sim.integrators[0].getEventManager().addListener(histListener);
            }
            sim.integratorOS.setRefStepFraction(refFrac);
            sim.integratorOS.setAdjustStepFraction(false);
        }


        sim.integratorOS.getMoveManager().setEquilibrating(false);
        sim.ai.setMaxSteps(1000);
        sim.getController().actionPerformed();
        
        long t2 = System.currentTimeMillis();
        
        if (params.doHist) {
            double[] xValues = hist.xValues();
            double[] h = hist.getHistogram();
            for (int i=0; i<xValues.length; i++) {
                if (!Double.isNaN(h[i]) && h[i]!=0) {
                    System.out.println(xValues[i]+" "+h[i]);
                }
            }
        }
        

        System.out.println("final reference step fraction "+sim.integratorOS.getIdealRefStepFraction());
        System.out.println("actual reference step fraction "+sim.integratorOS.getRefStepFraction());
        
        sim.printResults(HSB[nPoints]);
        
        System.out.println();
        System.out.println("time: "+(t2-t1)/1000.0);
	}



    /**
     * Inner class for parameters
     */
    public static class VirialParam extends ParameterBase {
        public int nPoints = 5;
        public double temperature = 300;   // Kelvin
        public long numSteps = 1000000;
        public double sigmaHSRef = 3.5;
        public int nullRegionMethod = 2;
        public double refFrac = -1;
        public boolean doHist = false;
        public boolean semiClassical = false;
        public boolean calcApprox = false;
        public boolean subtractApprox = false;
        public boolean minMulti = false;
    }
}
