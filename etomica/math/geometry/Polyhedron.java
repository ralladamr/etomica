package etomica.math.geometry;
import java.util.LinkedList;

import etomica.space.IVector;
import etomica.space3d.Vector3D;

/**
 * Representation of a mathematical polyhedron, a 3-dimensional polytope. Contains
 * all information needed to represent a polyhedron, methods to set its position
 * and orientation, and methods that return an external representation of it. 
 * Provides no external means to change the polyhedron size and shape, as the form
 * and capabilities of mutator methods are particular to the type of polyhedron
 * defined by the subclass. 
 */
public abstract class Polyhedron extends Polytope {
    
    /**
     * Constructs a polyhedron with the given faces for its sides.  Faces
     * should be constructed so they have the correct sharing of edges
     * between adjacent faces, and sharing of vertices between intersecting
     * edges.
     */
    protected Polyhedron(Polygon[] faces) {
        super(faces);
        this.faces = faces;
        this.edges = allEdges(faces);
    }
    
    /**
     * Returns all faces defined by the polyhedron.
     */
    public Polygon[] getFaces() {
        updateVertices();
        return faces;
    }
    
    /**
     * Returns all edges defined by the polyhedron.
     */
    public LineSegment[] getEdges() {
        updateVertices();
        return edges;
    }
        
    /**
     * Returns the sum of the length of the edges of the polyhedron
     */
    public double getPerimeter() {
        updateVertices();
        double sum = 0.0;
        for(int i=0; i<edges.length; i++) {
            sum += edges[i].getLength();
        }
        return sum;
    }

    /**
     * Returns the sum the area of the faces of the polyhedron
     */
    public double getSurfaceArea() {
        updateVertices();
        double sum = 0.0;
        for(int i=0; i<faces.length; i++) {
            sum += faces[i].getArea();
        }
        return sum;
    }
    
    /**
     * Returns the perpendicular distance to the nearest face of the 
     * polyhedron.  Assumes that given point is contained in polyhedron;
     * if not, returns NaN.
     * @param r
     * @return
     */
    public double distanceTo(IVector r) {
        updateVertices();
        if(!contains(r)) return Double.NaN;
        double d = Double.POSITIVE_INFINITY;
        Plane plane = new Plane();
        for(int i=0; i<faces.length; i++) {
            Polygon f = faces[i];
            plane.setThreePoints((Vector3D)f.getVertices()[0], 
                    (Vector3D)f.getVertices()[1], (Vector3D)f.getVertices()[2]);
            double d1 = Math.abs(plane.distanceTo((Vector3D)r));
            if(d1 < d) d = d1;
        }
        return d;
    }
    
    /**
     * Finds all edge instances in the given array of faces, and returns
     * them in an array.  Each edge appears in the array once, although it
     * should be part of multiple faces.  Used by constructor.
     */
    private static LineSegment[] allEdges(Polygon[] faces) {
        LinkedList list = new LinkedList();
        for(int i=0; i<faces.length; i++) {
            LineSegment[] edges= faces[i].getEdges();
            for(int j=0; j<edges.length; j++) {
                if(!list.contains(edges[j])) {
                    list.add(edges[j]);
                }
            }
        }
        return (LineSegment[])list.toArray(new LineSegment[0]);
    }

    protected final LineSegment[] edges;
    protected final Polygon[] faces;
    
 }
