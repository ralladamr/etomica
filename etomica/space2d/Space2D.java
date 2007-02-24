package etomica.space2d;

import etomica.EtomicaInfo;
import etomica.space.IVector;
import etomica.space.NearestImageTransformer;
import etomica.space.Orientation;
import etomica.space.RotationTensor;
import etomica.space.Space;

/**
 * Factory and methods appropriate to a 2-dimensional space.  This is
 * a singleton class that can be accessed only via the static getInstance method.
 */
public final class Space2D extends Space {

    /**
     * Private constructor for singleton.
     */
    private Space2D() {
        super();
    }

    /**
     * @return Returns the instance.
     */
    public static Space2D getInstance() {
        return INSTANCE;
    }

    public int D() {
        return 2;
    }

    public int powerD(int n) {
        return n * n;
    }

    public double powerD(double a) {
        return a * a;
    }

    /**
     * Returns the square root of the given value, a^(1/D), which is a^(1/2).
     */
    public double rootD(double a) {
        return Math.sqrt(a);
    }

    /**
     * Returns PI r^2.
     */
    public double sphereVolume(double r) {
        return Math.PI * r * r;
    } //volume of a sphere of radius r

    /**
     * Returns 2 PI r.
     */
    public double sphereArea(double r) {
        return 2.0 * Math.PI * r;
    } //surface area of sphere of radius r (used for differential shell volume)

    public IVector makeVector() {
        return new Vector2D();
    }

    public Orientation makeOrientation() {
        return new Orientation2D();
    }

    public etomica.space.Tensor makeTensor() {
        return new Tensor2D();
    }

    public RotationTensor makeRotationTensor() {
        return new RotationTensor2D();
    }

    public int[] makeArrayD(int i) {
        return new int[] { i, i };
    }

    public double[] makeArrayD(double d) {
        return new double[] { d, d };
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Two-dimensional space");
        return info;
    }

    /**
     * Computes the square of the magnitude of the difference of two vectors, subject
     * to a nearest image transformation.  This method constructs a new vector that
     * is used as the work-vector input to the other r2 method.
     */
    public static final double r2(Vector2D u1, Vector2D u2, NearestImageTransformer b) {
        return r2(u1, u2, b, new Vector2D());
    }

    /**
     * Computes the square of the magnitude of the difference of two vectors, subject
     * to a nearest image transformation.  
     * @param u1 one of the vectors and is unchanged by the calculation
     * @param u2 the other vector and is unchanged by the calculation
     * @param b a nearest image transformation
     * @param work a work vector used for the calculation.
     */
    public static final double r2(Vector2D u1, Vector2D u2, NearestImageTransformer b,
            Vector2D work) {
        work.Ev1Mv2(u1, u2);
        b.nearestImage(work);
        return work.squared();
    }

    /**
     * Required to guarantee singleton when deserializing.
     * 
     * @return the singleton INSTANCE
     */
    private Object readResolve() {
        return INSTANCE;
    }

    private static final Space2D INSTANCE = new Space2D();
    private static final long serialVersionUID = 1L;

}//end of Space2D
