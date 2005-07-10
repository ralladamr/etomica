package etomica.action;

import java.awt.Graphics;
import java.awt.Polygon;

import etomica.Atom;
import etomica.AtomIterator;
import etomica.AtomTreeNodeGroup;
import etomica.Phase;
import etomica.Simulation;
import etomica.SimulationEventManager;
import etomica.Space;
import etomica.atom.AtomFactoryMono;
import etomica.atom.AtomLinker;
import etomica.atom.AtomList;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.atom.iterator.AtomIteratorListTabbed;
import etomica.lattice.AbstractLattice;
import etomica.lattice.BravaisLattice;
import etomica.space.Vector;
import etomica.utility.OdeSolver;

/** 
 * Action that changes the system volume by distorting the space, causing
 * more volume to be placed at one point selected at random.  Space is distorted from
 * this point until reaching the boundaries, where it becomes a uniform translation.
 *
 * @author Nandou Lu
 * @author David Kofke
 */

public class PhaseInflatePoint extends PhaseActionAdapter implements Undoable, etomica.graphics.Drawable {
    
    private boolean drawPoints = true;
    private boolean drawCells = false;
    private boolean fillCells = false;
    MyLattice lattice;
    Vector s;  //temporary
    Vector r0;
    public boolean expand = true;
    private double lnJTot;
    private double deformationScale = 1.0;
    private AtomIteratorAllMolecules moleculeIterator;

    public PhaseInflatePoint(Simulation sim) {
        super("Point volume expansion");
        s = sim.space.makeVector();
        r0 = sim.space.makeVector();
//        lattice = new MyLattice(sim, 10,new VelocityField(2,20), 0.1);
        lattice = new MyLattice(sim,12,new VelocityField(2,20), 0.002);
//        setDeformationScale(0.01);
        moleculeIterator = new AtomIteratorAllMolecules();
    }
    public double lastLnJacobian() {return lnJTot;}
    
    public void setPhase(Phase p) {
        if(p == null) return;
        moleculeIterator.setPhase(p);
        r0.PEa1Tv1(-0.5, p.boundary().dimensions());//subtract old
        super.setPhase(p);
        r0.PEa1Tv1(0.5,phase.boundary().dimensions());//add new
    }
    
    public void setR0(Vector r) {
        r0.E(r);
        r0.PEa1Tv1(0.5,phase.boundary().dimensions());
    }
    public Vector getR0() {return r0;}
    
    public void setExpand(boolean b) {expand = b;}
    public boolean isExpand() {return expand;}
    
    public void setDeformationScale(double d) {
        deformationScale = d;
        
        AtomIterator iter = lattice.iterator();
        iter.reset();
        while(iter.hasNext()) {
            MySite site = (MySite)iter.nextAtom();
            site.calculateDeformedPosition(deformationScale);
        }
        iter.reset();
        while(iter.hasNext()) {
            MySite site = (MySite)iter.nextAtom();
            if(site.c1 != null) site.c1.updateConstants();
            if(site.c2 != null) site.c2.updateConstants();
        }//end while
    } 
    public double getDeformationScale() {return deformationScale;}
    
    public void actionPerformed(Vector r, boolean b) {
        setExpand(b);
        actionPerformed(r);
    }
    public void actionPerformed(Vector r) {
        r0.E(r);
        r0.PEa1Tv1(0.5,phase.boundary().dimensions());
        actionPerformed(phase);
    }
        
    public void actionPerformed(Phase p) {
    	setPhase(p);
        actionPerformed();
    }
    public void actionPerformed() {
        double scale = expand ? lattice.scale : 1.0/lattice.scale;
        lnJTot = 0.0;
        moleculeIterator.reset();
        while(moleculeIterator.hasNext()) {
            Atom a = moleculeIterator.nextAtom();
            s.E(a.coord.position());
            s.ME(r0);
            // centralImage returns the vector to add to s and does not transform s
            phase.boundary().centralImage(s);
            s.DE(phase.boundary().dimensions());
            MyCell cell = null;
            if(expand) {
                cell = lattice.getOriginalCell(s);
                cell.transform(s);
                lnJTot += cell.lnJacobian;
            }
            else {
                cell = lattice.getDeformedCell(s);
                cell.revert(s);//works only for scale = 1.0
                lnJTot -= cell.lnJacobian;
            }
            s.TE(phase.boundary().dimensions());
            s.PE(r0);
            phase.boundary().centralImage(s);
            s.TE(scale);
            a.coord.displaceTo(s);
        }
  //      phase.dimensions().TE(scale);
        phase.boundary().inflate(scale);
    }
    
    public void undo() {
        moleculeIterator.reset();
        while(moleculeIterator.hasNext()) {
            moleculeIterator.nextAtom().coord.replace();
        }
 //       phase.dimensions().DE(expand ? lattice.scale : 1.0/lattice.scale);
        phase.boundary().inflate(expand ? 1.0/lattice.scale : lattice.scale);
    }
        
    public boolean getDrawPoints() {return drawPoints;}
    public void setDrawPoints(boolean b) {drawPoints = b;}
    public boolean getFillCells() {return fillCells;}
    public void setFillCells(boolean b) {fillCells = b;}
    public boolean getDrawCells() {return drawCells;}
    public void setDrawCells(boolean b) {drawCells = b;}
    
    /**
     * Method to draw the grid of original/deformation points.
     * To use, add the instance of this class to the displayPhase via addDrawingObject.
     */
    public void draw(Graphics g, int[] origin, double scale) {
        int toPixels = (int)(scale * etomica.units.BaseUnit.Length.Sim.TO_PIXELS);
        if(drawPoints) drawPoints(g, origin, toPixels);
        if(drawCells) drawCells(g, origin, toPixels);
    }
    
    private void drawPoints(Graphics g, int[] origin, int toPixels) {
        double diameter = 1.0;
        int sigmaP = (int)(toPixels*diameter);
        double xL = phase.boundary().dimensions().x(0);
        double yL = phase.boundary().dimensions().x(1);
        AtomIterator iter = lattice.iterator();
        while(iter.hasNext()) {
                MySite site = (MySite)iter.nextAtom();
                Vector r = site.originalPosition;
                int xP = origin[0] + (int)(toPixels*(xL*r.x(0)-0.5*diameter));
                int yP = origin[1] + (int)(toPixels*(yL*r.x(1)-0.5*diameter));
                g.setColor(java.awt.Color.red);
                g.fillOval(xP,yP,sigmaP,sigmaP);
                
                r = site.deformedPosition;
                xP = origin[0] + (int)(toPixels*(xL*r.x(0)-0.5*diameter));
                yP = origin[1] + (int)(toPixels*(yL*r.x(1)-0.5*diameter));
                g.setColor(java.awt.Color.black);
                g.fillOval(xP,yP,sigmaP,sigmaP);
        }//end of while
    }//end of drawPoints
    
    private void drawCells(Graphics g, int[] origin, int toPixels) {
        AtomIterator iter = lattice.iterator();
        double toPixelsX = toPixels*phase.boundary().dimensions().x(0);
        double toPixelsY = toPixels*phase.boundary().dimensions().x(1);
        while(iter.hasNext()) {
            MySite site = (MySite)iter.nextAtom();
            MyCell cell = site.c1;
            if(cell == null) continue;
      //      java.awt.Polygon triangle = cell.getDeformedPolygon(origin,toPixelsX, toPixelsY);               
            java.awt.Polygon triangle = cell.getOriginalPolygon(origin,toPixelsX, toPixelsY);               
            if(fillCells) {g.setColor(etomica.graphics.ConstantsGraphic.randomColor()); g.fillPolygon(triangle);}
            else g.drawPolygon(triangle);
        }//end while
    }//end of drawCells
    
//    public static void main(String args[]) {
//        
//        Simulation sim = new etomica.graphics.SimulationGraphic(new Space2D());
//        Simulation.instance = sim;
//        final PhaseInflatePoint actionPointVolume = new PhaseInflatePoint();
//        final etomica.graphics.DisplayPhase displayPhase = new etomica.graphics.DisplayPhase(sim);
//        Integrator integrator = new IntegratorHard();
//        Potential2 p2 = new P2HardSphere();
//        Controller controller = new Controller();
//        displayPhase.setScale(0.7);
//        final Phase phase = new Phase(sim);
//        
//        actionPointVolume.setPhase(phase);
//
//		final MyLattice lattice = actionPointVolume.lattice;
//		System.out.println("Scale: "+lattice.scale);
//        
//       displayPhase.addDrawable(actionPointVolume);
//        actionPointVolume.setDrawCells(true);
//    //    actionPointVolume.setFillCells(true);
//        
//        SpeciesSpheres species = new SpeciesSpheres(0);
//		Simulation.instance.elementCoordinator.go();
//		
//	//	This listener allows interactive testing of lattice getCell methods
//		displayPhase.addDisplayPhaseListener(new etomica.graphics.DisplayPhaseListener() {
//		    public void displayPhaseAction(etomica.graphics.DisplayPhaseEvent evt) {
//    		    Space2D.Vector r = (Space2D.Vector)evt.point();
//		        MouseEvent mevt = evt.getMouseEvent();
//		        int id = mevt.getID();
//		        if(id == MouseEvent.MOUSE_PRESSED) {
//    //		        actionPointVolume.actionPerformed(r);
//    		    }
//    		    else if(id == MouseEvent.MOUSE_RELEASED) {
//    //		        actionPointVolume.undo();
//    		    }
//  		        
//		        displayPhase.repaint();
//  		        
//                Space2D.Vector dimensions = (Space2D.Vector)phase.boundary().dimensions();
//		        r.DE(dimensions);
//		        MyCell cell = lattice.getOriginalCell((Space2D.Vector)r);
////		        MyCell cell = lattice.getDeformedCell((Space2D.Vector)r);
//    		    System.out.println(cell.jacobian*lattice.scale*lattice.scale);
//		        java.awt.Graphics g = displayPhase.graphic(null).getGraphics();
//		        g.fillPolygon(cell.getOriginalPolygon(
//	///*	        g.fillPolygon(cell.getDeformedPolygon(
//		            displayPhase.getOrigin(), 
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(0),
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(1)));
//
//                //fill in neighboring cells on original lattice
//	    /*       g.fillPolygon(cell.nbr[0].getOriginalPolygon(
//		            displayPhase.getOrigin(), 
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(0),
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(1)));
//		        g.fillPolygon(cell.nbr[1].getOriginalPolygon(
//		            displayPhase.getOrigin(), 
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(0),
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(1)));
//		        g.fillPolygon(cell.nbr[2].getOriginalPolygon(
//		            displayPhase.getOrigin(), 
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(0),
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(1)));*/
//		        //fill in neighboring cells on deformed lattice
//	/*	        g.fillPolygon(cell.nbr[0].getDeformedPolygon(
//		            displayPhase.getOrigin(), 
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(0),
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(1)));
//		        g.fillPolygon(cell.nbr[1].getDeformedPolygon(
//		            displayPhase.getOrigin(), 
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(0),
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(1)));
//		        g.fillPolygon(cell.nbr[2].getDeformedPolygon(
//		            displayPhase.getOrigin(), 
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(0),
//		            displayPhase.getScale()*etomica.units.BaseUnit.Length.Sim.TO_PIXELS*dimensions.x(1)));
//		*/    }
//		});
//
//		etomica.graphics.SimulationGraphic.makeAndDisplayFrame(Simulation.instance);
//    }//end of main
//    */
    /**
     * Array of 2D triangular cells formed by bisecting the cells of a square lattice.
     */
    private class MyLattice implements AbstractLattice, java.io.Serializable {
        
        BravaisLattice squareLattice;
        AtomList siteList = new AtomList();
        AtomIteratorListTabbed iterator = new AtomIteratorListTabbed(siteList);
        MySite[][] sites;
        int size, size1;
        double scale;
        /**
         * Size is the number of sites in each dimension; considered non-periodically
         */
        MyLattice(Simulation sim, int size, final VelocityField vField, double deltaT) {
            this.size = size;
            size1 = size-1;
            LatticeFactoryCubic latticeFactory = 
                new LatticeFactoryCubic(sim.space, new MySiteFactory(sim.space), sim.space.D(), size, 1.0/(size-1));
            squareLattice = (BravaisLattice)latticeFactory.makeAtom();
            squareLattice.shiftFirstToOrigin();

            sites = new MySite[size][size];
            int[] index = new int[2];
            for(index[0]=0; index[0]<size; index[0]++) { //make array of sites for convenience of access
                for(index[1]=0; index[1]<size; index[1]++) {
                    sites[index[0]][index[1]] = (MySite)squareLattice.site(index);
                }
            }
            //assign site positions and make vectors for deformed positions
            for(int i=0; i<size; i++) {    
                for(int j=0; j<size; j++) {
                    MySite site = sites[i][j];
                    siteList.add(site);
                    site.originalPosition = (Vector)site.coord.position();
         //           site.originalPosition.PE(-0.5);
                    site.deformedPosition = new Vector();
                    site.fullyDeformedPosition = new Vector();
                    site.deformedPosition.E(site.originalPosition);
                } 
            }
            //Deform lattice
            //make a local rhs class from the velocity field suitable for input to the ode solver
            OdeSolver.Rhs rhs = new OdeSolver.Rhs() {
                public double[] dydx(OdeSolver.Variables xy) {
                    return vField.v(xy.y);
                }
            };
            //do integration
            for(int i=0; i<size; i++) {
                for(int j=0; j<size; j++) {
                    MySite site = sites[i][j];
                    OdeSolver.Variables xy0 = new OdeSolver.Variables(0.0, site.originalPosition.toArray());
                    OdeSolver.Variables[] xy = OdeSolver.rungeKuttaAdaptive(xy0,deltaT,1.e-7,rhs);
                    site.deformedPosition.E(xy[xy.length-1].y);
                } 
            }
            //symmetrize
            Vector tempVector = (Vector)Space.makeVector(2);
            for(int i=0; i<size; i++) {
                for(int j=0; j<=i; j++) {
                    MySite site1 = sites[i][j];
                    MySite site2 = sites[j][i];
                    double dx1 = site1.deformedPosition.x(0) - site1.originalPosition.x(0);
                    double dy1 = site1.deformedPosition.x(1) - site1.originalPosition.x(1);
                    double dx2 = site2.deformedPosition.x(0) - site2.originalPosition.x(0);
                    double dy2 = site2.deformedPosition.x(1) - site2.originalPosition.x(1);
                    tempVector.E(dy2,dx2);
                    site1.deformedPosition.PE(tempVector);
             //       site1.deformedPosition.x(0) += dy2;
             //       site1.deformedPosition.x(1) += dx2;
                    if(j != i) {
                        tempVector.E(dy1,dx1);
                        site2.deformedPosition.PE(tempVector);
                //        site2.deformedPosition.x(0) += dy1;
                //        site2.deformedPosition.x(1) += dx1;
                    }
                }
            }
            //reshape to rectangle...
            double avg = 0.0;
  /* // reshape while maintainting left and top edges
            for(int i=0; i<size; i++) {
                double x0 = sites[0][i].deformedPosition.x(0);
                for(int j=0; j<size; j++) {
    //                sites[j][i].deformedPosition.PE(0,-x0);
                }
                avg += sites[size-1][i].deformedPosition.x(0);
            }
            avg /= size;
            //...adjust X
            for(int i=0; i<size; i++) {
                double factorX = avg/sites[size-1][i].deformedPosition.x(0);
                for(int j=0; j<size; j++) {
                    Space.Vector position = sites[j][i].deformedPosition;
                    position.setx(0,factorX*position.x(0));//scale each row to uniform width
                }
            }*/
            //reshape keeping center fixed
            for(int i=0; i<size; i++) {
                double x0 = sites[0][i].deformedPosition.x(0);
                double y0 = sites[i][0].deformedPosition.x(1);
                for(int j=0; j<size; j++) {
                    sites[j][i].deformedPosition.PE(0,-x0);
                    sites[i][j].deformedPosition.PE(1,-y0);
                }
                avg += sites[size-1][i].deformedPosition.x(0);
            }
            avg /= size;
            scale = avg;
            //...adjust X
            for(int i=0; i<size; i++) {
//                double factorX = (avg-0.5)/(sites[size-1][i].deformedPosition.x(0)-0.5);
                double factorX = 1.0/sites[size-1][i].deformedPosition.x(0);
                for(int j=0; j<size; j++) {
                    sites[j][i].deformedPosition.setX(0,factorX*sites[j][i].deformedPosition.x(0));
                //    Space.Vector position = sites[j][i].deformedPosition;
                //    position.setx(0,factorX*position.x(0));//scale each row to uniform width
                }
            }
            //...adjust Y
            for(int i=0; i<size; i++) {
            //    double y0 = sites[i][0].deformedPosition.x(1);
                double factorY = 1.0/sites[i][size-1].deformedPosition.x(1);
                for(int j=0; j<size; j++) {
                    Vector position = sites[i][j].deformedPosition;
           //         position.PE(1,-y0);
                    position.setX(1,factorY*position.x(1));
                }
            }
            
            for(int i=0; i<size; i++) {
                for(int j=0; j<size; j++) {
                    MySite site = sites[i][j];
                    site.fullyDeformedPosition.E(site.deformedPosition);
                }
            }
            
            //make cells
            for(int i=0; i<size1; i++) {    //loops don't do last row/column of lattice
                for(int j=0; j<size1; j++) {
                    MySite site = sites[i][j];
                    if((i>=size/2 && j<=size/2-1) || (i<=size/2-1 && j>=size/2)) {
                        //vertexes listed in order of opposite to E/W edge, then N/S, then partner cell
                        site.c1 = new MyCell(sites[i+1][j], sites[i][j+1], sites[i][j]);//top left triangle
                        site.c2 = new MyCell(sites[i][j+1], sites[i+1][j], sites[i+1][j+1]);//bottom right triangle
                        site.cellN = site.c1; site.cellW = site.c1;//TL
                        site.cellE = site.c2; site.cellS = site.c2;//BR
                    }
                    else {
                        site.c1 = new MyCell(sites[i+1][j+1], sites[i][j], sites[i][j+1]);//bottom left triangle
                        site.c2 = new MyCell(site, sites[i+1][j+1], sites[i+1][j]);//top right triangle
                        site.cellW = site.c1; site.cellS = site.c1;//BL
                        site.cellN = site.c2; site.cellE = site.c2;//TR
                    }    
                } 
            }
            //set up cell neighbors
            for(int i=0; i<size1; i++) {    //loops don't do last row/column of lattice
                for(int j=0; j<size1; j++) {
                    MySite site = sites[i][j];
                    /*
                    site.c1.nbr[0] = sites[i][j+1].c2;
                    site.c1.nbr[1] = site.c2;
                    if(i > 0) site.c1.nbr[2] = sites[i-1][j].c2;
                    site.c2.nbr[0] = sites[i+1][j].c1;
                    site.c2.nbr[1] = site.c1;
                    if(j > 0) site.c2.nbr[2] = sites[i][j-1].c1;
                    */
                    if(i > 0) site.cellW.addNeighbor(sites[i-1][j].cellE);
                    else site.cellW.addNeighbor(null);
                    site.cellE.addNeighbor(sites[i+1][j].cellW);
                    if(j > 0) site.cellN.addNeighbor(sites[i][j-1].cellS);
                    else site.cellN.addNeighbor(null);
                    site.cellS.addNeighbor(sites[i][j+1].cellN);
                    site.c1.addNeighbor(site.c2);
                    site.c2.addNeighbor(site.c1);
                } 
            }
            
        }//end of MyLattice constructor
        
        //returns the deformed-lattice cell that contains the point r
        public MyCell getDeformedCell(Vector r) {
            //initial guess it cell from original lattice
            MyCell cell = getOriginalCell((Vector)r);
            //if point is not in cell, move to the cell lying on other side of an edge
            //that separates point from its interior.  Keep doing this until the containing
            //cell is located
            MyCell newCell = cell.outside((Vector)r);
            while(newCell != null) {
                cell = newCell;
                newCell = cell.outside((Vector)r);//returns null if cell contains r
            }
            return cell;
        }
        
        //returns the original-lattice cell that contains the point r
        public MyCell getOriginalCell(Vector r) {
            //find and return cell containing r in undeformed lattice
            int ix = (int)Math.floor(r.x(0) * size1);
            int iy = (int)Math.floor(r.x(1) * size1);
            ix = (ix >= size1) ? size1-1 : ix;
            ix = (ix < 0) ? 0 : ix;
            iy = (iy >= size1) ? size1-1 : iy;
            iy = (iy < 0) ? 0 : iy;
            MySite site = sites[ix][iy];
            double dx = r.x(0) - site.originalPosition.x(0);
            double dy = r.x(1) - site.originalPosition.x(1);
            if(dy < dx) {//TR
                return (dy < 1-dx) ? site.cellN : site.cellE;
            }
            return (dy < 1-dx) ? site.cellW : site.cellS;
        }
        
        //AbstractLattice methods
        public int D() {return 2;}
        public int[] getSize() {return squareLattice.getSize();} //not used
//        public int siteCount() {return 2*squareLattice.siteCount();}
        public Atom site(int[] index) {return squareLattice.site(index);} //not used
        
        public Site randomSite() {return (Site)squareLattice.siteList().getRandom();}
        public AtomIterator iterator() {iterator.reset(); return iterator;}     //iterator for all sites in lattice

        public AtomList siteList() {return squareLattice.siteList();}//not used?
        public void setSize(int[] dim) {} //not used
        public SimulationEventManager eventManager() {return null;}//not used
    }
    
    private class MySite extends Site {
        MyCell c1, c2;
        MyCell cellN, cellS, cellE, cellW;
        Vector originalPosition, deformedPosition, fullyDeformedPosition;
        MySite(Space space, AtomTreeNodeGroup parent) {
            super(space, etomica.NULL, parent);
        }
        //computes the location of the deformed-lattice site for the given deformation scale
        public void calculateDeformedPosition(double s) {
            deformedPosition.Ea1Tv1((1.-s),originalPosition);
            deformedPosition.PEa1Tv1(s, fullyDeformedPosition);
        }
    }
    private class MySiteFactory extends AtomFactoryMono {
        MySiteFactory(Space space) {super(space, AtomLinker.FACTORY);}
        protected Atom build(AtomTreeNodeGroup parent) {
            return new MySite(space, parent);
        }
    }

    private class MyCell implements java.io.Serializable {
        public MySite origin;
        public double jacobian, lnJacobian;
        public double a11, a12, a21, a22;
        public double deltaX, deltaY;
        double m21, m10, m20, q0, q1, q2;
        public MySite[] vertex = new MySite[3];
        public MyCell[] nbr = new MyCell[3];
        private int nbrIndex = 0;
        private double rDenom = 1.0;
        private double xb, xa, yb, ya;
        MyCell(MySite s0, MySite s1, MySite s2) {
            origin = s0;
            vertex[0] = s0;
            vertex[1] = s1;
            vertex[2] = s2;
            xb  = s1.originalPosition.x(0) - s0.originalPosition.x(0);
            xa  = s2.originalPosition.x(0) - s0.originalPosition.x(0);
            yb  = s1.originalPosition.x(1) - s0.originalPosition.x(1);
            ya  = s2.originalPosition.x(1) - s0.originalPosition.x(1);
            rDenom = 1.0/(ya*xb - yb*xa);
            updateConstants();
        }
        public void updateConstants() {
            MySite s0 = vertex[0];
            MySite s1 = vertex[1];
            MySite s2 = vertex[2];
            deltaX = origin.deformedPosition.x(0) - origin.originalPosition.x(0);
            deltaY = origin.deformedPosition.x(1) - origin.deformedPosition.x(1);
            double xbn = s1.deformedPosition.x(0) - s0.deformedPosition.x(0);
            double xan = s2.deformedPosition.x(0) - s0.deformedPosition.x(0);
            double ybn = s1.deformedPosition.x(1) - s0.deformedPosition.x(1);
            double yan = s2.deformedPosition.x(1) - s0.deformedPosition.x(1);
            a11 = (xbn*ya - xan*yb)*rDenom;
            a12 = (xan*xb - xa*xbn)*rDenom;
            a21 = (ya*ybn - yan*yb)*rDenom;
            a22 = (yan*xb - xa*ybn)*rDenom;
            jacobian = a11*a22 - a12*a21;
            lnJacobian = Math.log(jacobian);
            
            //set up constants used to determined whether a point is in the cell
            m10 = ybn/xbn;
            m20 = yan/xan;
            m21 = (yan - ybn)/(xan - xbn);
            if(m21 == 0.0) q0 = ybn;
            else if(Double.isInfinite(m21)) q0 = xbn;
            else q0 = (ybn - m21*xbn);// 1 - 0
            q0 *= q0;
            
            if(m20 == 0.0) q1 = -ybn;
            else if(Double.isInfinite(m20)) q1 = -xbn;
            else q1 = (yan-ybn - m20*(xan-xbn));// 2 - 1
            q1 *= q1;
            
            if(m10 == 0.0) q2 = -yan;
            else if(Double.isInfinite(m10)) q2 = -xan;
            else q2 = (yan - m10*xan); // 0 - 2
            q2 *= q2;
        }
        public void addNeighbor(MyCell cell) {
            nbr[nbrIndex++] = cell;
        }
        
        //returns null if point is inside this cell, otherwise returns neighbor cell
        //on other side of an edge that the point lies beyond
        //looks at each vertex and sees if point is closer than the opposite edge is to it
        public MyCell outside(Vector r) {
            if(outsideEdge(r, vertex[0].deformedPosition, q0, m21)) return nbr[0];
            if(outsideEdge(r, vertex[1].deformedPosition, q1, m20)) return nbr[1];
            if(outsideEdge(r, vertex[2].deformedPosition, q2, m10)) return nbr[2];
            return null; //point is inside the cell
        }
        
        private boolean outsideEdge(Vector r, Vector r0, double q, double m) {
            double dx = r.x(0) - r0.x(0);
            double dy = r.x(1) - r0.x(1);
            if(m == 0.0) return q < dy*dy; //opposite edge is horizontal
            if(Double.isInfinite(m)) return q < dx*dx; //opposite edge is vertical
            //opposite edge is neither horizontal nor vertical
            double mr0 = dy/dx;
            double delta = m - mr0;
            double L2 = q * (mr0*mr0 + 1)/(delta*delta); //distance from vertex to opposite edge along a line through r
            return L2 < (dx*dx + dy*dy);
        }
        
        public void transform(Vector r) {
            double dx = r.x(0) - origin.originalPosition.x(0);
            double dy = r.x(1) - origin.originalPosition.x(1);
            double dxn = a11*dx + a12*dy;
            double dyn = a21*dx + a22*dy;
            r.setX(0,origin.originalPosition.x(0) + (deltaX + dxn));
            r.setX(1,origin.originalPosition.x(1) + (deltaY + dyn));
        }

        public void revert(Vector r) {
            double dx = r.x(0) - origin.deformedPosition.x(0);
            double dy = r.x(1) - origin.deformedPosition.x(1);
            double dxn = (a22*dx - a12*dy)/jacobian;
            double dyn = -(a21*dx - a11*dy)/jacobian;
            r.setX(0, origin.originalPosition.x(0) + dxn);
            r.setX(1, origin.originalPosition.x(1) + dyn);
        }
        
        //returns a polygon object, suitable for drawing, corresponding to the original cell
        public java.awt.Polygon getOriginalPolygon(int[] origin, double toPixelsX, double toPixelsY) {
            java.awt.Polygon triangle = new Polygon(
                new int[] {(int)(vertex[0].originalPosition.x(0)*toPixelsX),
                           (int)(vertex[1].originalPosition.x(0)*toPixelsX),
                           (int)(vertex[2].originalPosition.x(0)*toPixelsX)},
                new int[] {(int)(vertex[0].originalPosition.x(1)*toPixelsY),
                           (int)(vertex[1].originalPosition.x(1)*toPixelsY),
                           (int)(vertex[2].originalPosition.x(1)*toPixelsY)}, 3);
            triangle.translate(origin[0],origin[1]); 
            return triangle;
        }//end of getOriginalPolygon
        //returns a polygon object, suitable for drawing, corresponding to the deformed cell
        public java.awt.Polygon getDeformedPolygon(int[] origin, double toPixelsX, double toPixelsY) {
            java.awt.Polygon triangle = new Polygon(
                new int[] {(int)(vertex[0].deformedPosition.x(0)*toPixelsX),
                           (int)(vertex[1].deformedPosition.x(0)*toPixelsX),
                           (int)(vertex[2].deformedPosition.x(0)*toPixelsX)},
                new int[] {(int)(vertex[0].deformedPosition.x(1)*toPixelsY),
                           (int)(vertex[1].deformedPosition.x(1)*toPixelsY),
                           (int)(vertex[2].deformedPosition.x(1)*toPixelsY)}, 3);
            triangle.translate(origin[0],origin[1]); 
            return triangle;
        }//end of getDeformedPolygon
        
    }//end of MyCell
}

class VelocityField {
    
    double[][] source;
    double[] velocity;
    int D;
    int nSource;
    double m = 1;
    VelocityField(int dim, int nSourceHalf) {
        D = dim;
        velocity = new double[D];
        nSource = 2*nSourceHalf + 1;
        source = new double[nSource][D];
        int k=0;
        for(int i=-nSourceHalf; i<=nSourceHalf; i++) {
            for(int j=0; j<D-1; j++) {
                source[k][j] = 0.5;
            }
            source[k][D-1] = 0.5+(double)i;
            k++;
        }
    }
        //written for 2D
    public double[] v(double[] r) {
        velocity[0] = 0.0;
        velocity[1] = 0.0;
        for(int k=0; k<nSource; k++) {
            double dx = r[0] - source[k][0];
            double dy = r[1] - source[k][1];
//            double r2 = Math.sqrt(dx*dx + dy*dy);
            double r2 = dx*dx + dy*dy;
            velocity[0] += dx/r2;
            velocity[1] += dy/r2;
         //   velocity[0] += dx/Math.sqrt(r2);
         //   velocity[1] += dy/Math.sqrt(r2);
        }
        velocity[0] *= m;
        velocity[1] *= m;
        return velocity;
    }
}//end of VelocityField
