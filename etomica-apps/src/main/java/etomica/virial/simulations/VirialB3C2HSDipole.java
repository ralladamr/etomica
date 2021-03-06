/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial.simulations;

import etomica.action.IAction;
import etomica.integrator.IntegratorEvent;
import etomica.integrator.IntegratorListener;
import etomica.atom.AtomTypeOriented;
import etomica.chem.elements.ElementSimple;
import etomica.data.histogram.HistogramNotSoSimple;
import etomica.data.histogram.HistogramSimple;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBoxCanvasG3DSys;
import etomica.graphics.DisplayBoxCanvasG3DSys.OrientedFullSite;
import etomica.graphics.SimulationGraphic;
import etomica.math.DoubleRange;
import etomica.potential.P2HSDipole;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresRotating;
import etomica.units.Pixel;
import etomica.util.Arrays;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.virial.*;
import etomica.virial.cluster.Standard;

import java.awt.*;

/**
 * B3,C2, dielectric constant
 * part two, test
 * hard sphere with dipole in the center
 * June 2014
 */
public class VirialB3C2HSDipole {

    public static void main(String[] args) {
        VirialParam params = new VirialParam();
        ParseArgs.doParseArgs(params, args);

        double temperature = params.temperature;
        long steps = params.numSteps;
        final int nPoints = params.nPoints;
        double sigmaHSRef = params.sigmaHSRef;
        double sigmaHS = params.sigmaHS;
        double refFrac = params.refFrac;
        double mu2 = params.mu2;

        System.out.println("TEST: dipolar hard sphere overlap sampling dielectric constant B3, C2, at T="+temperature+" Kelvin");
        double mu = Math.sqrt(mu2);

        final double[] HSB = new double[9];
        HSB[3] = Standard.B3HS(sigmaHSRef);
        
        System.out.println("sigmaHSRef: "+sigmaHSRef);
        double coefficient = 4 * Math.PI/9/temperature;    
        HSB[3]= HSB[3]*coefficient;
        
        System.out.println("B"+nPoints+"HS: "+HSB[nPoints]);
        System.out.println("sigmaHS: "+sigmaHS);
        System.out.println("dipole moment: "+mu);
        Space space = Space3D.getInstance();
        //HS ref system
        MayerHardSphere fRef = new MayerHardSphere(sigmaHSRef);
        MayerEHardSphere eRef = new MayerEHardSphere(sigmaHSRef);
        ClusterAbstract refCluster = Standard.virialCluster(nPoints, fRef, nPoints>3, eRef, true);
        refCluster.setTemperature(temperature);
        
        //Target system
        P2HSDipole pTarget = new P2HSDipole(space,sigmaHS, mu);
        //MayerGeneral fTarget = new MayerGeneral(pTarget);
        MayerPUGeneral fPlusUTarget = new MayerPUGeneral(space, sigmaHS, mu, pTarget);// bondlist[0]
        MuGeneral muTarget = new MuGeneral(mu, pTarget);//bondlist[1]
               // MayerMUGeneral fMinusUTarget = new MayerMUGeneral(pTarget);// bondList[0]
        //EnergyGeneral energyTarget = new EnergyGeneral(space, sigmaHS, mu, pTarget);//bondList[2]
        
        int nBondTypes = 2;
        ClusterBonds[] clusters = new ClusterBonds[0];
		int[][][] bondList = new int[nBondTypes][][];	
        ClusterAbstract targetCluster = null;
        bondList[0] = new int [][]{{0,2},{0,1}}; //
    	bondList[1] = new int [][]{{1,2}};//
        
        clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nPoints, bondList, false));
        
      //  bondList = new int[nBondTypes][][];	
    //	bondList[1] = new int [][]{{1,2}};
      //  bondList[2] = new int [][]{{0,1},{0,2}};
     //   clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nPoints, bondList, false));

        targetCluster = new ClusterSum(clusters,new double []{1.0}, new MayerFunction[]{fPlusUTarget,muTarget});
        targetCluster = new ClusterCoupledAtomFlipped(targetCluster, space);

        targetCluster.setTemperature(temperature);
        double refIntegral = HSB[nPoints];

        SpeciesSpheresRotating species = new SpeciesSpheresRotating(space, new ElementSimple("A")); 
        species.setIsDynamic(true);
        //simulation
        final SimulationVirialOverlap2 sim = new SimulationVirialOverlap2(space,species, temperature,refCluster,targetCluster,false);
        
        sim.integratorOS.setNumSubSteps(1000);
        sim.integratorOS.setAggressiveAdjustStepFraction(true);
        System.out.println(steps+" steps (1000 blocks of "+steps/1000+")");
        steps /= 1000;
        // displace the atoms to a certain distance
        for (int i=0; i<50 && sim.box[1].getSampleCluster().value(sim.box[1]) == 0; i++) {
            sim.mcMoveTranslate[1].doTrial();
            sim.mcMoveTranslate[1].acceptNotify();
            sim.box[1].trialNotify();
            sim.box[1].acceptNotify();
        }
        if (false) {
            double size = 10;
            sim.box[0].getBoundary().setBoxSize(Vector.of(new double[]{size, size, size}));
            sim.box[1].getBoundary().setBoxSize(Vector.of(new double[]{size, size, size}));
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE);
            simGraphic.getDisplayBox(sim.box[0]).setPixelUnit(new Pixel(300.0/size));
            simGraphic.getDisplayBox(sim.box[1]).setPixelUnit(new Pixel(300.0/size));
            simGraphic.getDisplayBox(sim.box[0]).setShowBoundary(false);
            simGraphic.getDisplayBox(sim.box[1]).setShowBoundary(false);
            
            ColorSchemeByType colorScheme = (ColorSchemeByType)simGraphic.getDisplayBox(sim.box[1]).getColorScheme();
            colorScheme.setColor(sim.getSpecies(0).getAtomType(0), Color.red);
            OrientedFullSite[] sites = new OrientedFullSite[2];

            sites[0] = new OrientedFullSite(Vector.of(new double[]{0.5, 0, 0}), Color.BLUE, 0.2);
            sites[1] = new OrientedFullSite(Vector.of(new double[]{-0.5, 0, 0}), Color.YELLOW, 0.2);
            ((DisplayBoxCanvasG3DSys)simGraphic.getDisplayBox(sim.box[0]).canvas).setOrientationSites(
                    (AtomTypeOriented) sim.getSpecies(0).getAtomType(0), sites);
            ((DisplayBoxCanvasG3DSys)simGraphic.getDisplayBox(sim.box[1]).canvas).setOrientationSites(
                    (AtomTypeOriented) sim.getSpecies(0).getAtomType(0), sites);
            simGraphic.makeAndDisplayFrame();

            sim.integratorOS.setNumSubSteps(1000);
            sim.setAccumulatorBlockSize(1000);
                
            // if running interactively, set filename to null so that it doens't read
            // (or write) to a refpref file
            sim.getController().removeAction(sim.ai);
            sim.getController().addAction(new IAction() {
                public void actionPerformed() {
                    sim.initRefPref(null, 10);
                    sim.equilibrate(null, 20);
                    sim.ai.setMaxSteps(Long.MAX_VALUE);
                }
            });
            sim.getController().addAction(sim.ai);
            if (Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref) || sim.refPref == 0) {
                throw new RuntimeException("Oops");
            }

            return;
        }
        
        // if running interactively, don't use the file
        String refFileName = args.length > 0 ? "refpref"+nPoints+"_"+temperature : null;
        // this will either read the refpref in from a file or run a short simulation to find it
        sim.initRefPref(refFileName, steps/100);
        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(refFileName, steps/40);
        if (sim.refPref == 0 || Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref)) {
            throw new RuntimeException("oops");
        }

        System.out.println("equilibration finished");

        sim.setAccumulatorBlockSize(steps);
        sim.integratorOS.setNumSubSteps((int)steps);
        sim.ai.setMaxSteps(1000);
        sim.integratorOS.getMoveManager().setEquilibrating(false);

        for (int i=0; i<2; i++) {
            System.out.println("MC Move step sizes "+sim.mcMoveTranslate[i].getStepSize()+" "+sim.mcMoveRotate[i].getStepSize());
        }
        if (refFrac >= 0) {
        	 sim.integratorOS.setRefStepFraction(refFrac);
             sim.integratorOS.setAdjustStepFraction(false);
        }

        if (false) {
            IntegratorListener progressReport = new IntegratorListener() {
                public void integratorInitialized(IntegratorEvent e) {}
                public void integratorStepStarted(IntegratorEvent e) {}
                public void integratorStepFinished(IntegratorEvent e) {
                    if ((sim.integratorOS.getStepCount()*10) % sim.ai.getMaxSteps() != 0) return;
                    System.out.print(sim.integratorOS.getStepCount()+" steps: ");
                    double[] ratioAndError = sim.dvo.getAverageAndError();
                    System.out.println("abs average: "+ratioAndError[0]*HSB[nPoints]+", error: "+ratioAndError[1]*HSB[nPoints]);
                }
            };
            sim.integratorOS.getEventManager().addListener(progressReport);
        }

        final HistogramSimple targHist = new HistogramSimple(70, new DoubleRange(-1, 8));
        final HistogramNotSoSimple targPiHist = new HistogramNotSoSimple(70, new DoubleRange(-1, 8));
        final ClusterAbstract finalTargetCluster = targetCluster.makeCopy();

        IntegratorListener histListenerTarget = new IntegratorListener() {
            public void integratorStepStarted(IntegratorEvent e) {}
            
            public void integratorStepFinished(IntegratorEvent e) {
                double r2Max = 0;
                double r2Min = Double.POSITIVE_INFINITY;
                CoordinatePairSet cPairs = sim.box[1].getCPairSet();
                for (int i=0; i<nPoints; i++) {
                    for (int j=i+1; j<nPoints; j++) {
                        double r2ij = cPairs.getr2(i, j);
                        if (r2ij < r2Min) r2Min = r2ij;
                        if (r2ij > r2Max) r2Max = r2ij;
                    }
                }

                double v = finalTargetCluster.value(sim.box[1]);
                double r = Math.sqrt(r2Max);
                if (r > 1) {
                    r = Math.log(r);
                }
                else {
                    r -= 1;
                }
                targHist.addValue(r);
                targPiHist.addValue(r, Math.abs(v));
            }

            public void integratorInitialized(IntegratorEvent e) {}
        };
        if (false) {/////
            System.out.println("collecting histograms");
            // only collect the histogram if we're forcing it to run the reference system
            sim.integrators[1].getEventManager().addListener(histListenerTarget);
        }
        sim.getController().actionPerformed();
        if (false) {////
            double[] xValues = targHist.xValues();
            double[] h = targHist.getHistogram();
            double[] hPI = targPiHist.getHistogram();
            for (int i=0; i<xValues.length; i++) {
                if (!Double.isNaN(h[i])) {
                    System.out.println(xValues[i]+" "+h[i] + "  "+hPI[i]);
                }
            }
        }
        System.out.println("final reference step frequency "+sim.integratorOS.getIdealRefStepFraction());
        System.out.println("actual reference step frequency "+sim.integratorOS.getRefStepFraction());
        sim.printResults(HSB[nPoints]);

	}

    /**
     * Inner class for parameters
     */
    public static class VirialParam extends ParameterBase {
        public int nPoints = 3;
        public double temperature = 1.0;
        public long numSteps = 10000000;
        public double sigmaHSRef = 2.0;
        public double sigmaHS = 1.0;
        public double mu2 = 1.0;
        public double refFrac = -1;
    }
}
