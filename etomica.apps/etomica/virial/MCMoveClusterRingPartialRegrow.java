package etomica.virial;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IRandom;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomArrayList;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.integrator.mcmove.MCMoveBox;
import etomica.space.ISpace;
import etomica.util.HistogramExpanding;

/**
 * MCMove that partially regrows the beads of a ring polymer, accepting or
 * rejecting the move based on the sampling weight.  The move can (optionally)
 * regrow the beads such that the beads from multiple molecules are combined
 * to form a larger ring.
 * 
 * @author Andrew Schultz
 */
public class MCMoveClusterRingPartialRegrow extends MCMoveBox {

    public MCMoveClusterRingPartialRegrow(IRandom random, ISpace _space) {
        this(random, _space, new int[0][0]);
    }
    
    public MCMoveClusterRingPartialRegrow(IRandom random, ISpace _space, int[][] tangledMolecules) {
        super(null);
        this.space = _space;
        this.random = random;
        this.tangledMolecules = tangledMolecules;
        setNumTrial(10);
        dcom = space.makeVector();
        leafIterator = new AtomIteratorLeafAtoms();
        myAtoms = new AtomArrayList();
	}
    
    public void setNumTrial(int newNumTrial) {
        nTrial = newNumTrial;
        rTrial = new IVectorMutable[nTrial];
        for (int i=0; i<nTrial; i++) {
            rTrial[i] = space.makeVector();
        }
        pkl = new double[nTrial];
    }

    public int getNumTrial() {
        return nTrial;
    }
    
    public void setEnergyFactor(double factor) {
        fac = factor;
    }
    
    public void setNumBeads(int newNumBeads) {
        maxNumBeads = newNumBeads;
        oldPositions = new IVectorMutable[maxNumBeads];
        for (int i=0; i<maxNumBeads; i++) {
            oldPositions[i] = space.makeVector();
        }
    }
    
    public int getNumBeads() {
        return numBeads;
    }

    public void setBox(IBox p) {
        super.setBox(p);
        int nMolecules = box.getMoleculeList().getMoleculeCount();
        hist = new HistogramExpanding[nMolecules][0];
        for (int i=0; i<nMolecules; i++) {
            int nAtoms = box.getMoleculeList().getMolecule(i).getChildList().getAtomCount();
            hist[i] = new HistogramExpanding[nAtoms];
            for (int j=0; j<nAtoms; j++) {
                hist[i][j] = new HistogramExpanding(0.04);
            }
        }
        leafIterator.setBox(p);
    }
    
	//note that total energy is calculated
	public boolean doTrial() {
        weightOld = ((BoxCluster)box).getSampleCluster().value((BoxCluster)box);

        IMoleculeList molecules = box.getMoleculeList();
        wNew = 1;
        wOld = 1;
        double sigma = Math.sqrt(0.5/fac);
        numBeads = Math.round(random.nextInt((int)Math.round(maxNumBeads*0.9))+Math.round(maxNumBeads*0.1));

            iMolecule = random.nextInt(molecules.getMoleculeCount());
            int i = iMolecule;
            atoms = null;
            int nAtoms = 0;
            boolean single = true;
            int[] tangled = null;
            for (int j=0; single && j<tangledMolecules.length; j++) {
                for (int k=0; k<tangledMolecules[j].length; k++) {
                    if (i==tangledMolecules[j][k]) {
                        single = false;
                        tangled = tangledMolecules[j];
                        i = tangled[0];
                        break;
                    }
                }
            }
            if (single) {
                atoms = molecules.getMolecule(i).getChildList();
                nAtoms = atoms.getAtomCount();
            }
            else {
                myAtoms.clear();
                for (int j=0; j<tangled.length; j++) {
                    IAtomList jAtoms = molecules.getMolecule(tangled[j]).getChildList();
                    myAtoms.addAll(jAtoms);
                    nAtoms += jAtoms.getAtomCount();
                }
                atoms = myAtoms;
            }
            dcom.E(0);

            kStart = random.nextInt(nAtoms);

            IAtom atom0 = atoms.getAtom(kStart);
            IVector prevAtomPosition = atom0.getPosition();
            int kEnd = (kStart + numBeads + 1) % nAtoms;
            IAtom atomN = atoms.getAtom(kEnd);
            IVector lastAtomPosition = atomN.getPosition();

            double pPrev = 1;
            int k = kStart;
            if (false) {
                // configurational bias consideration of the reverse move
                for (int m=0; m<numBeads; m++) {
                    k++;
                    if (k == nAtoms) {
                        k = 0;
                    }
                    IAtom kAtom = atoms.getAtom(k);
                    double pSum = 0;
                    for (int l=0; l<nTrial-1; l++) {
                        for (int j=0; j<3; j++) {
                            rTrial[l].setX(j, sigma*random.nextGaussian());
                        }
                        rTrial[l].PE(prevAtomPosition);
                        double rin2 = rTrial[l].Mv1Squared(lastAtomPosition)/(numBeads-m);
                        pSum += Math.exp(-fac*rin2);
                    }
                    double rin2 = kAtom.getPosition().Mv1Squared(lastAtomPosition)/(numBeads-m);
                    double pOld = Math.exp(-fac*rin2);
                    wOld *= (pSum+pOld)/pPrev/nTrial;
                    pPrev = pOld;
                    prevAtomPosition = kAtom.getPosition();
                }
            }
            
            k = kStart;
            pPrev = 1;
            prevAtomPosition = atom0.getPosition();
            for (int m=0; m<numBeads; m++) {
                k++;
                if (k == nAtoms) {
                    k = 0;
                }
                IAtom kAtom = atoms.getAtom(k);
                IVectorMutable kPosition = kAtom.getPosition();
                dcom.ME(kPosition);
                oldPositions[m].E(kPosition);

                double k1 = fac/(numBeads-m);
                double k2 = fac;
                double x0 = 1.0/(1.0 + 1.0/(numBeads-m));
                sigma = Math.sqrt(0.5/(k1 + k2));
                
                for (int j=0; j<3; j++) {
                    kPosition.setX(j, sigma*random.nextGaussian());
                }
                
                kPosition.PEa1Tv1(x0, prevAtomPosition);
                kPosition.PEa1Tv1(1.0-x0, lastAtomPosition);
                
                if (false ) {
                    // configurational bias consideration of the forward move
                    for (int l=0; l<nTrial; l++) {
                        for (int j=0; j<3; j++) {
                            rTrial[l].setX(j, sigma*random.nextGaussian());
                        }
                        rTrial[l].PE(prevAtomPosition);
                        double rin2 = rTrial[l].Mv1Squared(lastAtomPosition)/(numBeads-m);
                        pkl[l] = Math.exp(-fac*rin2); //*rin2;
    //                    if (k==1) System.out.println(rTrial[l].squared()+" "+(nAtoms-k)+" "+pkl[l]);
                        if (l>0) pkl[l] += pkl[l-1];
                    }
                    double ranl = random.nextDouble() * pkl[nTrial-1];
                    int chosenTrial = nTrial-1;
                    for (int l=0; l<nTrial-1; l++) {
                        if (pkl[l] >= ranl) {
                            chosenTrial = l;
                            break;
                        }
                    }
    //                if (k==1) System.out.println(rTrial[chosenTrial].squared());
                    kAtom.getPosition().E(rTrial[chosenTrial]);
                    wNew *= pkl[nTrial-1]/pPrev/nTrial;
                    pPrev = pkl[chosenTrial] - (chosenTrial == 0 ? 0 : pkl[chosenTrial-1]);
                }
                dcom.PE(kPosition);
                prevAtomPosition = kPosition;
            }
            dcom.TE(-1.0/nAtoms);

            for (k=0; k<nAtoms; k++) {
                atoms.getAtom(k).getPosition().PE(dcom);
            }

        ((BoxCluster)box).trialNotify();
        weightNew = ((BoxCluster)box).getSampleCluster().value((BoxCluster)box);
//        System.out.println(wOld+" =?=> "+wNew);
		return true;
	}
	
    public double getA() {
        // we skip calculation of wOld because we're the only intramolecular move in town.
        return wNew/wOld * weightNew/weightOld;
    }

    public double getB() {
    	return 0.0;
    }
    
    public void rejectNotify() {
        int nAtoms = atoms.getAtomCount();
        for (int k=0; k<nAtoms; k++) {
            atoms.getAtom(k).getPosition().ME(dcom);
        }
        for (int k=kStart, m=0; m<numBeads; m++) {
            k++;
            if (k == nAtoms) {
                k = 0;
            }
            IAtom kAtom = atoms.getAtom(k);
            kAtom.getPosition().E(oldPositions[m]);
        }
    	((BoxCluster)box).rejectNotify();
    }
    
    public void acceptNotify() {
//        System.out.println("accepted");
    	((BoxCluster)box).acceptNotify();
    }
    
    public double energyChange() {
        return 0;
    }
    
    public AtomIterator affectedAtoms() {
        return leafIterator;
    }
    
    private static final long serialVersionUID = 1L;
    protected IAtomList atoms;
    protected final ISpace space;
    protected final IRandom random;
    protected IVectorMutable[] oldPositions;
    protected IVectorMutable[] rTrial;
    protected int nTrial;
    protected double[] pkl;
    // Rosenbluth weights
    protected double wOld, wNew;
    // cluster weights
    protected double weightOld, weightNew;
    protected final IVectorMutable dcom;
    protected final AtomIteratorLeafAtoms leafIterator;
    protected double fac;
    public HistogramExpanding[][] hist;
    protected final int[][] tangledMolecules;
    protected final AtomArrayList myAtoms;
    protected int numBeads, maxNumBeads;
    protected int kStart, iMolecule;
}