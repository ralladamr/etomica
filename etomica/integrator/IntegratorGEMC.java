package etomica.integrator;

import etomica.EtomicaElement;
import etomica.EtomicaInfo;
import etomica.integrator.mcmove.MCMoveMoleculeExchange;
import etomica.integrator.mcmove.MCMoveVolumeExchange;
import etomica.potential.PotentialMaster;

/**
 * Simple Gibbs-ensemble Monte Carlo integrator. Used to evaluate fluid-fluid
 * phase coexistence. Written to apply to only two phases.
 * 
 * @author David Kofke
 */

public class IntegratorGEMC extends IntegratorManagerMC implements EtomicaElement {

    public IntegratorGEMC(PotentialMaster potentialMaster) {
        super(potentialMaster);
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo(
                "Gibbs-ensemble Monte Carlo simulation of phase coexistence");
        return info;
    }

    public void addIntegrator(Integrator newIntegrator) {
        if (!(newIntegrator instanceof IntegratorPhase)) {
            throw new IllegalArgumentException("Sub integrators must be able to handle a phase");
        }
        if (nIntegrators == 2) {
            throw new IllegalArgumentException("Only 2 sub-integrators can be added");
        }
        super.addIntegrator(newIntegrator);
        if (nIntegrators == 2) {
            volumeExchange = new MCMoveVolumeExchange(potential,
                    (IntegratorPhase)integrators[0],(IntegratorPhase)integrators[1]);
            moleculeExchange = new MCMoveMoleculeExchange(potential,
                    (IntegratorPhase)integrators[0],(IntegratorPhase)integrators[1]);
            moveManager.addMCMove(volumeExchange);
            moveManager.addMCMove(moleculeExchange);
        }
    }

    /**
     * Returns the object that performs the volume-exchange move in the GE
     * simulation. Having handle to this object is needed to adjust trial
     * frequency and view acceptance rate.
     */
    public MCMoveVolumeExchange getMCMoveVolumeExchange() {
        return volumeExchange;
    }

    /**
     * Returns the object that performs the molecule-exchange move in the GE
     * simulation. Having handle to this object is needed to adjust trial
     * frequency and view acceptance rate.
     */
    public MCMoveMoleculeExchange getMCMoveMoleculeExchange() {
        return moleculeExchange;
    }

    private MCMoveVolumeExchange volumeExchange;
    private MCMoveMoleculeExchange moleculeExchange;

}