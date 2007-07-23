package etomica.densityofstate;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.atom.AtomSourceRandomLeaf;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveAtom;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.cell.PotentialMasterCell;
import etomica.potential.P2SoftSphericalTruncated;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space3d.Space3D;
import etomica.species.Species;
import etomica.species.SpeciesSpheresMono;
import etomica.util.HistoryCollapsingAverage;
import etomica.yukawa.P2Yukawa;

/**
 * A Yukawa Monte-Carlo simulation in 3D
 */

public class DensityOfStatesPolynomial extends Simulation{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public IntegratorMC integrator;
	public MCMoveAtom mcMoveAtom;
	public SpeciesSpheresMono species;
	public Box box;
	public P2Yukawa potential;
	public Controller controller;
	public ActivityIntegrate activityIntegrate; 
	
	public DensityOfStatesPolynomial(){
		this(500);
	}
	
	
	
	public DensityOfStatesPolynomial(int numAtoms){
		super(Space3D.getInstance(), false);
		
		potentialMaster = new PotentialMaster(space);
		integrator = new IntegratorMC(this, potentialMaster);
		mcMoveAtom = new MCMoveAtom(this, potentialMaster);
		mcMoveAtom.setAtomSource(new AtomSourceRandomLeaf());
		mcMoveAtom.setStepSize(0.2);
		integrator.getMoveManager().addMCMove(mcMoveAtom);
		integrator.getMoveManager().setEquilibrating(false);
		activityIntegrate = new ActivityIntegrate(integrator);
		getController().addAction(activityIntegrate);
		species = new SpeciesSpheresMono(this);
		box.setNMolecules(species, numAtoms);
		box = new Box(this);
		box.setDensity(0.65);
		potential = new P2Yukawa(this);
		double truncationRadius = 3.0*potential.getKappa();
		if(truncationRadius > 0.5*box.getBoundary().getDimensions().x(0)){
			throw new RuntimeException("Truncaiton radius too large.  Max allowed is "+0.5*box.getBoundary().getDimensions().x(0));
		}
		P2SoftSphericalTruncated potentialTruncated = new P2SoftSphericalTruncated(potential, truncationRadius);
		((PotentialMasterCell)potentialMaster).setCellRange(3);
		((PotentialMasterCell)potentialMaster).setRange(potentialTruncated.getRange());
		potentialMaster.addPotential(potentialTruncated, new Species[] {species, species});
			
		integrator.getMoveEventManager().addListener(((PotentialMasterCell)potentialMaster).getNbrCellManager(box).makeMCMoveListener());
		
		new ConfigurationLattice(new LatticeCubicFcc()).initializeCoordinates(box);
		integrator.setBox(box);
		
		((PotentialMasterCell)potentialMaster).getNbrCellManager(box).assignCellAll();
		
	}
	
	public PotentialMaster potentialMaster;
	
	public static void main(String[] args){
		
		int numAtoms = 500;
		long maxSteps = 1000;
		double temperature = 6.0;
		
		if (args.length > 0){
			numAtoms = Integer.valueOf(args[0]).intValue();
		}
		if (args.length > 1){
			temperature = Double.parseDouble(args[1]);
		}
		if (args.length > 2) {
			maxSteps = Long.parseLong(args[2]);
		}
		 
		DensityOfStatesPolynomial sim = new DensityOfStatesPolynomial(numAtoms);
		sim.activityIntegrate.setMaxSteps(maxSteps);
		sim.integrator.setTemperature(temperature);
		sim.getController().actionPerformed();
		
		//Calling Class DataProcessorPhi
		
		MeterPotentialEnergyFromIntegrator energyMeter = new MeterPotentialEnergyFromIntegrator(sim.integrator);
		
		//Passing E
		AccumulatorHistory accumulatorHistory = new AccumulatorHistory(new HistoryCollapsingAverage(200));
		AccumulatorAverage accumulatorAverage = new AccumulatorAverageFixed(200);
		DataPump dataPumpE = new DataPump(energyMeter, accumulatorAverage);
		sim.integrator.addIntervalAction(dataPumpE);
		
		
		//
		
		DataProcessorPhi phi = new DataProcessorPhi();
		phi.setTemperature(temperature);
		
		
		DataPump dataPump = new DataPump(energyMeter, phi);
		sim.integrator.addIntervalAction(dataPump);
		
		AccumulatorAverage b = new AccumulatorAverageFixed();
		phi.setDataSink(b);
		
		
		sim.activityIntegrate.setMaxSteps(maxSteps);
		sim.getController().reset();
		sim.getController().actionPerformed();
		
		DataDoubleArray phiData = (DataDoubleArray)((DataGroup)b.getData()).getData(AccumulatorAverage.StatType.AVERAGE.index);
		
		//calculating bn
		double[] bn = new double [10];
		
		for (int n = 0; n < 10; n++){
			
			bn[n]=(phiData.getValue(n))/(phiData.getValue(n+10));
			
			System.out.println(bn[n]);
		}
		
		
		
		//Edit Start
		 
		
		/*double[][] DOS = new double [0][0];	
		
        for(int j = 0; j < u[0].length; j++){
        	for (int n = 0; n < 10; n++){
        		DOS[0][j] += phiData.getValue(n)*phi(n,temperature,u[0][j]);
        		}
        	System.out.println(DOS[0][j]);
        	
			}*/
		//Edit End
	}	
        
        
		//DataLogger dataLogger = new DataLogger();
		//DataTableWriter dataTableWriter = new DataTableWriter();
		//dataLogger.setFileName("histogram" + "@" + (int)(temperature*10) + ".dat");
		//dataLogger.setDataSink(dataTableWriter);
		//dataLogger.putDataInfo(histogram.getDataInfo());
		
		//dataLogger.setWriteInterval(1);
		//dataLogger.putData(histogram.getData());
		//dataLogger.closeFile();
	
		
		//DisplayPlot HistogramDisplay = new DisplayPlot();
		//histogram.addDataSink(HistogramDisplay.getDataSet().makeDataSink());
		
		//SimulationGraphic simGraphic = new SimulationGraphic(sim);
        //simGraphic.makeAndDisplayFrame();
        //simGraphic.panel().add(HistogramDisplay.graphic());
        //ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());
        //colorScheme.setColor(sim.species.getMoleculeType(), java.awt.Color.red);


}
