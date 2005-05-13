package etomica.math.geometry;

import etomica.Space;
import etomica.exception.MethodNotImplementedException;
import etomica.space.Vector;

/**
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 * 
 * @author David Kofke
 *  
 */

/*
 * History Created on May 13, 2005 by kofke
 */
public class PolygonGeneral extends Polygon {

    /**
     * @param embeddedSpace
     * @param nSides
     */
    public PolygonGeneral(Space embeddedSpace, int nSides) {
        super(embeddedSpace, nSides);
    }

    /**
     * @param edges
     */
    public PolygonGeneral(LineSegment[] edges) {
        super(edges);
    }

    /**
     * Returns the value of the area enclosed by the polygon
     */
    //must override in subclass (until a general algorithm is implemented)
    public double getArea() {
        throw new MethodNotImplementedException(
                "General formula for area not in place");
    }

    /**
     * Returns true if the given point lies inside or on an edge of the polygon
     */
    //must override in subclass (until a general algorithm is implemented)
    public boolean contains(Vector vector) {
        throw new MethodNotImplementedException(
                "General formula for 'contains' not in place");
    }

    public void updateVertices() {
        //does nothing, becuse in this case the vertices are the official
        //representation of the polytope
    }

}