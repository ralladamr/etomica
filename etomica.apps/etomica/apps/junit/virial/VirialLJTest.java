package etomica.apps.junit.virial;

import junit.framework.TestCase;
import etomica.potential.P2LennardJones;
import etomica.potential.Potential2Spherical;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.virial.ClusterAbstract;
import etomica.virial.MayerEHardSphere;
import etomica.virial.MayerESpherical;
import etomica.virial.MayerGeneralSpherical;
import etomica.virial.MayerHardSphere;
import etomica.virial.SpeciesFactorySpheres;
import etomica.virial.cluster.Standard;
import etomica.virial.simulations.SimulationVirialOverlap;

/**
 * Virial junit test.  This just runs a simple simulation and checks that the
 * results (the Bennett alpha parameter as well as the final ratio and
 * uncertainty) are within expected limits.
 *
 * @author Andrew Schultz
 */
public class VirialLJTest extends TestCase {

    public static void main(String[] args) {
        testVirialLJ();
    }
    
    public static void testVirialLJ() {
        final int nPoints = 3;
        double temperature = 1;
        long steps = 1000;
        double sigmaHSRef = 1.5;

        Space space = Space3D.getInstance();
        
        MayerHardSphere fRef = new MayerHardSphere(space,sigmaHSRef);
        MayerEHardSphere eRef = new MayerEHardSphere(space,sigmaHSRef);
        Potential2Spherical pTarget = new P2LennardJones(space,1.0,1.0);
        MayerGeneralSpherical fTarget = new MayerGeneralSpherical(space,pTarget);
        MayerESpherical eTarget = new MayerESpherical(space,pTarget);
        ClusterAbstract targetCluster = Standard.virialCluster(nPoints, fTarget, nPoints>3, eTarget, true);
        targetCluster.setTemperature(temperature);
        ClusterAbstract refCluster = Standard.virialCluster(nPoints, fRef, nPoints>3, eRef, true);
        refCluster.setTemperature(temperature);

        final SimulationVirialOverlap sim = new SimulationVirialOverlap(space,new SpeciesFactorySpheres(), temperature,refCluster,targetCluster);
        sim.integratorOS.setNumSubSteps(1000);
        // this will run a short simulation to find it
        sim.initRefPref(null, steps/100);
        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(null, steps/40);
        assertTrue("Ref pref (alpha) within expected limits: "+sim.refPref, Math.abs(sim.refPref - 1.34) < 0.08);
        
        sim.ai.setMaxSteps(steps);
        sim.getController().actionPerformed();

        double ratio = sim.dsvo.getDataAsScalar();
        double error = sim.dsvo.getError();
        // check against expected values, 0.0604 +/- 0.0036
        assertTrue("Final ratio within expected limits: "+ratio, Math.abs(ratio - 0.0604) < 0.007);
        // improvements to the algorithm might lower this.  be wary of changes that raise it.
        // improvements to uncertainty estimation might alter this up or down, but it shouldn't change by much.
        assertTrue("Ratio uncertainty within expected limits: "+error, Math.abs(error - 0.0036) < 0.0003);
    }
}
