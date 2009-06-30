package etomica.models.oneDHardRods;

import etomica.api.IBox;
import etomica.api.IPotential;
import etomica.api.IPotentialMaster;
import etomica.api.IVectorMutable;
import etomica.data.DataSourceScalar;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinition.BasisCell;
import etomica.units.Null;
import etomica.util.DoubleRange;
import etomica.util.Histogram;
import etomica.util.HistogramExpanding;
import etomica.util.HistogramSimple;


/**
 * Uses brute force to calculate energy of a system with a set number of 
 * normal modes, and the rest of the degrees of freedom taken by Gaussians.
 * Assumes 1D system - otherwise, choose a mode and eliminate i loops.
 * 
 * @author cribbin
 *
 */
public class MeterCompareSingleModeBrute extends DataSourceScalar {
    int numTrials, numAccept;
    IPotential potentialTarget, potentialHarmonic;
    MeterPotentialEnergy meterPE;
    
    private double eigenVectors[][][];
    private IVectorMutable[] waveVectors;
    int comparedWV;
    protected double temperature;
    private double[] waveVectorCoefficients;
    private double wvc;
    private CoordinateDefinition coordinateDefinition;
    private double[] realT, imagT;
    private double[][] uOld, omegaSquared;
    private double[] uNow, deltaU;
    int coordinateDim;
    private double energyHardRod, energyHarmonic;
//    Histogram histogramNRG, histogramImagCoord, histogramRealCoord;
    
    private static final long serialVersionUID = 1L;
    
    public MeterCompareSingleModeBrute(IPotentialMaster potentialMaster, 
            CoordinateDefinition cd, IBox box){
        this("meterCompareMode", potentialMaster, cd, box);
    }
    
    public MeterCompareSingleModeBrute(String string, IPotentialMaster 
            potentialMaster, CoordinateDefinition cd, IBox box){
        super(string, Null.DIMENSION);
        setCoordinateDefinition(cd);
        realT = new double[coordinateDim];
        imagT = new double[coordinateDim];
        deltaU = new double[coordinateDim];
        meterPE = new MeterPotentialEnergy(potentialMaster);
        meterPE.setBox(box);
        
//        histogramNRG = new HistogramSimple(new DoubleRange(0.0, 5.0));
//        histogramImagCoord = new HistogramExpanding(0.1);
//        histogramRealCoord = new HistogramExpanding(0.1);
    }
        
    
    public double getDataAsScalar() {
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        BasisCell cell = cells[0];
        uOld = new double[cells.length][coordinateDim];
        double normalization = 1/Math.sqrt(cells.length);
        energyHardRod = 0.0;
        energyHarmonic = 0.0;
        
        //CALCULATE THE HARMONIC PART OF THE ENERGY
        //get normal mode coordinate of "last" waveVector
        coordinateDefinition.calcT(waveVectors[comparedWV], realT, imagT);
        
        double[] realCoord = new double[coordinateDim];
        double[] imagCoord = new double[coordinateDim];
        double[] normalCoord = new double[coordinateDim];
        for(int j = 0; j < coordinateDim; j++){
            realCoord[j] = 0.0;
            imagCoord[j] = 0.0;
        }
        for(int i = 0; i < coordinateDim; i++){  //Loop would go away
            for(int j = 0; j < coordinateDim; j++){
                realCoord[i] += eigenVectors[comparedWV][i][j] * realT[j];
                imagCoord[i] += eigenVectors[comparedWV][i][j] * imagT[j];
            }
            if(Double.isInfinite(omegaSquared[comparedWV][i])){
                continue;
            }
            //Calculate the energy due to the Gaussian modes.
            //NAN IS THIS RIGHT?
            normalCoord[i] = realCoord[i]*realCoord[i] + imagCoord[i] * imagCoord[i];
            energyHarmonic += wvc * normalCoord[comparedWV] * omegaSquared[comparedWV][i];
        }
        
//        histogramNRG.addValue(energyHarmonic);
//        histogramRealCoord.addValue(realCoord);
//        histogramImagCoord.addValue(imagCoord);
        
//        System.out.println("single real: " + realCoord);
//        System.out.println("single imag: " + imagCoord);
        
        
        //CALCULATE THE HARD ROD PART OF THE ENERGY
        for(int iCell = 0; iCell < cells.length; iCell++){
            //store original positions
            uNow = coordinateDefinition.calcU(cells[iCell].molecules);
            System.arraycopy(uNow, 0, uOld[iCell], 0, coordinateDim);
            cell = cells[iCell];
            for(int j = 0; j < coordinateDim; j++){
                deltaU[j] = 0.0;
            }
            
            //Calculate the contributions to the current position of the 
            //zeroed mode, and subtract it from the overall position.
            double kR = waveVectors[comparedWV].dot(cell.cellPosition);
            double coskR = Math.cos(kR);
            double sinkR = Math.sin(kR);
            for(int i = 0; i < coordinateDim; i++){  //Loop would go away
                //Calculate the current coordinates.
                for(int j = 0; j < coordinateDim; j++){
                    //NAN IS THIS RIGHT?
                    deltaU[j] -= wvc*eigenVectors[comparedWV][i][j] *
                        2.0 * (realCoord[j]*coskR - imagCoord[j]*sinkR);
                }
            }

            for(int i = 0; i < coordinateDim; i++){
                deltaU[i] *= normalization;
            }
            
            for(int i = 0; i < coordinateDim; i++) {
                uNow[i] += deltaU[i];
            }
            coordinateDefinition.setToU(cells[iCell].molecules, uNow);
        }
        energyHardRod = meterPE.getDataAsScalar();
        
        // Set all the atoms back to the old values of u
        for (int iCell = 0; iCell<cells.length; iCell++) {
            cell = cells[iCell];
            coordinateDefinition.setToU(cell.molecules, uOld[iCell]);
        }
        
//        if(getDataInfo().getLabel() == "meterBinA" && energyHardRod != 0.0 ){
//            System.out.println("energyOld  " + energyOld);
//            System.out.println("energyNM  " + energyHardRod);
//            System.out.println("energyOP  " + energyHarmonic);
//        }
        
        return energyHardRod + energyHarmonic;
    }

    
    public void setEigenVectors(double[][][] eigenVectors) {
        this.eigenVectors = eigenVectors;
    }
    public void setWaveVectors(IVectorMutable[] waveVectors) {
        this.waveVectors = waveVectors;
    }
    public void setComparedWV(int comparedWV) {
        this.comparedWV = comparedWV;
        wvc = waveVectorCoefficients[comparedWV];
    }
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    public void setWaveVectorCoefficients(double[] waveVectorCoefficients) {
        this.waveVectorCoefficients = waveVectorCoefficients;
    }
    public void setCoordinateDefinition(CoordinateDefinition cd){
        coordinateDefinition = cd;
        coordinateDim = coordinateDefinition.getCoordinateDim();
    }
    
    public void setSpringConstants(double[][] sc){
        omegaSquared = sc;
    }
    
    public void setOmegaSquared(double[][] sc){
        omegaSquared = sc;
    }
    
    

}
