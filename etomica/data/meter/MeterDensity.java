package etomica.data.meter;

import etomica.EtomicaInfo;
import etomica.Phase;
import etomica.data.DataSourceScalar;
import etomica.species.Species;
import etomica.units.Dimension;
import etomica.units.DimensionRatio;

/**
 * Meter for measurement of the total molecule number density in a phase
 * Molecule number density is defined (number of molecules)/(volume of phase)
 */
public class MeterDensity extends DataSourceScalar implements Meter {
    
    public MeterDensity() {
        super("Number Density",new DimensionRatio(Dimension.QUANTITY,Dimension.VOLUME));
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Number density (molecules/volume) in a phase");
        return info;
    }
    
    public void setSpecies(Species s) {
        species = s;
    }
    public Species getSpecies() {
    	return species;
    }

    public double getDataAsScalar() {
        if (phase == null) throw new IllegalStateException("must call setPhase before using meter");
        return (species == null ? 
        			phase.moleculeCount() : 
        			phase.getAgent(species).moleculeCount())
				/phase.volume();
    }
    
    /**
     * @return Returns the phase.
     */
    public Phase getPhase() {
        return phase;
    }
    /**
     * @param phase The phase to set.
     */
    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    private Phase phase;
    private Species species;
}
