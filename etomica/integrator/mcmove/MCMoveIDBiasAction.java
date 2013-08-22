package etomica.integrator.mcmove;

import etomica.action.IAction;
import etomica.integrator.IntegratorMC;
import etomica.math.SpecialFunctions;

/**
 * IAction which takes data from an MCMoveOverlapListener about acceptance
 * probabilities for insert/delete moves and uses them to update biasing of
 * the move.
 * 
 * @author Andrew Schultz
 */
public class MCMoveIDBiasAction implements IAction {
    protected final int maxDN;
    protected final int fixedN;
    protected double mu;
    protected final MCMoveOverlapListener mcMoveOverlapMeter;
    protected final MCMoveInsertDeleteBiased mcMoveID;
    protected final int numAtoms;
    protected final double temperature;
    protected final IntegratorMC integratorMC;
    protected int maxNumAtoms;
    protected double lastDefaultdADef;
    protected double pullFactor;

    public MCMoveIDBiasAction(IntegratorMC integratorMC, MCMoveInsertDeleteBiased mcMoveID, int maxDN, int fixedN, double mu,
            MCMoveOverlapListener mcMoveOverlapMeter, int numAtoms, double temperature) {
        this.integratorMC = integratorMC;
        this.mcMoveID = mcMoveID;
        this.maxDN = maxDN;
        this.fixedN = fixedN;
        this.mu = mu;
        this.mcMoveOverlapMeter = mcMoveOverlapMeter;
        this.numAtoms = numAtoms;
        this.temperature = temperature;
        pullFactor = 1;
    }

    /**
     * Sets a nominal bias for states that have not yet been visited.  The bias given is
     * exp(-mu), so this should actually get something like mu/kT.
     */
    public void setMu(double mu) {
        this.mu = mu;
        actionPerformed();
    }

    public void setNMaxReweight(int maxNumAtoms) {
        this.maxNumAtoms = maxNumAtoms;
    }

    public void setPullFactor(double pullFactor) {
        this.pullFactor = pullFactor;
    }

    public void setDefaultDaDef(double daDef) {
        lastDefaultdADef = daDef;
    }

    public void actionPerformed() {
        double[] ratios = mcMoveOverlapMeter.getRatios();
        double[] hist = mcMoveOverlapMeter.getHistogram();
        if (Double.isNaN(hist[0])) return;
        long[] numInsert = mcMoveOverlapMeter.getNumInsert();
        long[] numDelete = mcMoveOverlapMeter.getNumDelete();
        int n0 = mcMoveOverlapMeter.getMinNumAtoms();
        double daDefTot = 0;
        double wTot = 0;
        for (int i=0; i<ratios.length; i++) {
            if (!Double.isNaN(ratios[i])) {
                double iDaDef = Math.log(ratios[i]*((n0+i+1)/(ratios.length-i)));
                double w = 1.0/(1.0/numInsert[i]+1.0/numDelete[i+1]);
                daDefTot += iDaDef*w;
                wTot += w;
            }
        }
//        if (wTot == 0) return;
        double daDef = daDefTot/wTot;
        double lnr = 0;
        double[] lnbias = new double[ratios.length+1];
        for (int i=0; i<ratios.length; i++) {
            lnbias[i] = lnr;
            if (maxNumAtoms>0 && n0+i+1>maxNumAtoms) {
                lnr += mu/temperature;
                continue;
            }
            double lnratio = daDef + Math.log(((double)(numAtoms - (n0+i)))/(n0+i+1));
            if (false && !Double.isNaN(ratios[i])) {
                lnr -= Math.log(ratios[i]);
            }
            else if (!Double.isNaN(lnratio)) {
                if (-lnratio > mu/temperature) {
                    // bias needed for flat histogram is greater than chemical potential
                    // only apply enough bias so that hist[N]/hist[i] ~= 0.25
                    double d = -lnratio - Math.log(2)/(numAtoms - (n0+i));   // ratios.length-i = number of vacancies from below
                    if (d < mu/temperature) {
                        d = mu/temperature;
                    }
                    lnr += d;
                }
                else {
                    lnr -= lnratio;
                }
            }
            else {
                lnr += mu/temperature;
            }
            if (lnr - lnbias[i] < mu/temperature) {
                double offset = (lnbias[i] + mu/temperature) - lnr;
                if (offset > Math.log(2)) offset = Math.log(2);
                lnr += offset;
            }
            if (hist[i]*hist[i+1] == 0 && hist[i]+hist[i+1] > 10000) {
                long ni = numInsert[i]+numDelete[i]+1;
                long nip1 = numInsert[i+1]+numDelete[i+1]+1;
                lnr += pullFactor*Math.log(((double)ni)/((double)nip1));
            }
//            else if (hist[i+1]<hist[i]) {
//                if (-Math.log(hist[i]/hist[i+1]) > Math.log(4)) {
//                    lnr += 0.25*Math.log(hist[i]/hist[i+1]);
//                }
//            }
//            else if (Math.abs(Math.log(hist[i]/hist[i+1])) > Math.log(10)) {
//                lnr += 0.1*Math.log(hist[i]/hist[i+1]);
//            }
        }
        lnbias[ratios.length] = lnr;
        int minN = (fixedN < numAtoms ? fixedN : numAtoms) - maxDN - 1;
        int maxN = (fixedN > numAtoms ? fixedN : numAtoms) + maxDN + 1;
        double nominalLnBias = lnbias[lnbias.length-1];
        for (int i=0; i<lnbias.length; i++) {
            lnbias[i] -= nominalLnBias;
            int na = n0 + i;
            mcMoveID.setLnBias(na, lnbias[i]);
        }
        lnr = 0;
        if (Double.isNaN(daDef)) {
//            lastDefaultdADef -= 1;
            daDef = lastDefaultdADef;
        }
        for (int na=n0-1; na>=minN; na--) {
            double lnratio = daDef + Math.log(((double)(numAtoms-na))/(na+1));
            if (lnratio < mu/temperature) {
                double offset = -(lnratio + mu/temperature);
                if (offset < -Math.log(2)) offset = -Math.log(2);
                lnratio += offset;
                lnr += lnratio;
            }
            else {
                lnr += lnratio;
            }
            mcMoveID.setLnBias(na, lnr);
        }

        for (int na = n0+lnbias.length; na<=maxN; na++) {
            double lastLnB = lnbias[lnbias.length-1]+(na-(n0+lnbias.length-1))*(mu/temperature);
            mcMoveID.setLnBias(na, lastLnB);
        }
    }
}
