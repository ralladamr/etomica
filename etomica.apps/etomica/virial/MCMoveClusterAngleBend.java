package etomica.virial;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.IVectorMutable;
import etomica.atom.MoleculeSource;
import etomica.atom.MoleculeSourceRandomMolecule;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.mcmove.MCMoveBoxStep;
import etomica.space.ISpace;

/**
 * An MC Move for cluster simulations that bends the bond angle for 3-atom
 * molecule.  The COM is not moved and both end atoms are moved by the same
 * amount such that the move does not alter the orientation of the molecule.
 * 
 * @author Andrew Schultz
 */
public class MCMoveClusterAngleBend extends MCMoveBoxStep {

    public MCMoveClusterAngleBend(ISimulation sim, IPotentialMaster potentialMaster, ISpace _space) {
    	this(potentialMaster,sim.getRandom(), 1.0, _space);
    }
    
    public MCMoveClusterAngleBend(IPotentialMaster potentialMaster, 
            IRandom random, double stepSize, ISpace _space) {
        super(potentialMaster);
        this.space = _space;
        this.random = random;
        moleculeSource = new MoleculeSourceRandomMolecule();
        ((MoleculeSourceRandomMolecule)moleculeSource).setRandom(random);
        setStepSizeMax(Math.PI/2);
        setStepSizeMin(0.0);
        setStepSize(stepSize);
        perParticleFrequency = true;

        weightMeter = new MeterClusterWeight(potential);
        energyMeter = new MeterPotentialEnergy(potential);
        energyMeter.setIncludeLrc(false);
        work1 = _space.makeVector();
        work2 = _space.makeVector();
        work3 = _space.makeVector();
    }

    public void setBox(IBox p) {
        super.setBox(p);
        weightMeter.setBox(p);
        energyMeter.setBox(p);
        dTheta = new double[p.getMoleculeList().getMoleculeCount()];
    }
    
    public void setSpecies(ISpecies newSpecies) {
        species = newSpecies;
    }

    public boolean doTrial() {
        uOld = energyMeter.getDataAsScalar();
        wOld = weightMeter.getDataAsScalar();

        IMoleculeList moleculeList = box.getMoleculeList();
        for(int i=0; i<moleculeList.getMoleculeCount(); i++) {
            if (species != null && moleculeList.getMolecule(i).getType() != species) {
                continue;
            }
            IMolecule molecule = moleculeList.getMolecule(i);
            IAtomList childList = molecule.getChildList();
            int numChildren = childList.getAtomCount();
            if (numChildren != 3) continue;
            double dt = stepSize * (random.nextDouble() - 0.5);
            dTheta[i] = dt;
            transform(molecule, dt);
        }
        ((BoxCluster)box).trialNotify();
        wNew = weightMeter.getDataAsScalar();
        uNew = energyMeter.getDataAsScalar();
        return true;
    }
    
    protected void transform(IMolecule molecule, double dt) {
        IAtomList childList = molecule.getChildList();
        int numChildren = childList.getAtomCount();
        if (numChildren != 3) return;
        
        IVectorMutable pos0 = childList.getAtom(0).getPosition();
        IVectorMutable pos1 = childList.getAtom(1).getPosition();
        IVectorMutable pos2 = childList.getAtom(2).getPosition();
        
        work1.Ev1Mv2(pos0, pos1);
        double bondLength01 = Math.sqrt(work1.squared());
        // normalize bond lengths -- we'll scale our vectors back up later
        work1.TE(1.0/bondLength01);
        work2.Ev1Mv2(pos2, pos1);
        double bondLength12 = Math.sqrt(work2.squared());
        work2.TE(1.0/bondLength12);
        double dot = work1.dot(work2);
        work2.PEa1Tv1(-dot, work1);
        work2.TE(1.0/Math.sqrt(work2.squared()));

        double cdt = Math.cos(dt);
        double sdt = Math.sin(dt);
        
        work3.E(pos0);
        pos0.E(pos1);
        pos0.PEa1Tv1(bondLength01*cdt, work1);
        pos0.PEa1Tv1(bondLength01*sdt, work2);
        work3.ME(pos0);

        // normalize bond lengths -- we'll scaled our vectors back up later
        work2.Ev1Mv2(pos2, pos1);
        work2.TE(1.0/bondLength12);
        dot = work1.dot(work2);
        work1.PEa1Tv1(-dot, work2);
        work1.TE(1.0/Math.sqrt(work1.squared()));

        work3.PE(pos2);
        pos2.E(pos1);
        pos2.PEa1Tv1(bondLength12*cdt, work2);
        pos2.PEa1Tv1(bondLength12*sdt, work1);
        work3.ME(pos2);
        
        // translate COM back to its original position
        work3.TE(1.0/3.0);
        pos0.PE(work3);
        pos1.PE(work3);
        pos2.PE(work3);
    }
    
    public void acceptNotify() {
//        System.out.println("accepted wiggle");
        ((BoxCluster)box).acceptNotify();
    }

    public void rejectNotify() {
//        System.out.println("rejected wiggle");
        IMoleculeList moleculeList = box.getMoleculeList();
        for(int i=0; i<box.getMoleculeList().getMoleculeCount(); i++) {
            if (dTheta[i] == 0) continue;
            transform(moleculeList.getMolecule(i), -dTheta[i]);
        }
        ((BoxCluster)box).rejectNotify();
    }

    public double getB() {
        return -(uNew - uOld);
    }
    
    public double getA() {
        return wOld == 0 ? 1 : wNew/wOld;
    }
	
    public double energyChange() {return uNew - uOld;}

    public AtomIterator affectedAtoms() {
        affectedAtomIterator.setList(box.getLeafList());
        return affectedAtomIterator;
    }
    
    /**
     * @return Returns the atomSource.
     */
    public MoleculeSource getAtomSource() {
        return moleculeSource;
    }
    /**
     * @param atomSource The atomSource to set.
     */
    public void setMoleculeSource(MoleculeSource source) {
        moleculeSource = source;
    }

    private static final long serialVersionUID = 1L;
    protected final AtomIteratorArrayListSimple affectedAtomIterator = new AtomIteratorArrayListSimple();
    protected final MeterClusterWeight weightMeter;
    protected final MeterPotentialEnergy energyMeter;
    protected final IVectorMutable work1, work2, work3;
    protected double[] dTheta;
    protected double wOld, wNew;
    protected final ISpace space;
    protected ISpecies species;
    protected double uOld;
    protected double uNew = Double.NaN;
    protected final IRandom random;
    protected MoleculeSource moleculeSource;
}