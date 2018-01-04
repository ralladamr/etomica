/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.lattice;

import java.util.Arrays;

/**
 * Generates iterates by alternately keeping or flipping the sign of each element of
 * iterates generated by another iterator.  Index of elements subject to iteration over sign
 * can be selected via the setReflecting method.
 * 
 * Care is taken not to generate additional elements by attempting to flip the sign of an element that is zero.
 * 
 * For example, if core iterator is an instance of IndexIteratorTriangularPermutations with D set to 2 at construction
 * and maxElement equal to 1, then this iterator returns the following iterates:
 * {0,0}<br>
 * {0,1}<br>
 * {0,-1}<br>
 * {1,0}<br>
 * {-1,0}<br>
 * {1,1}<br>
 * {1,-1}<br>
 * {-1,1}<br>
 * {-1,-1}<br>
 * 
 * @author David Kofke
 *
 */
public class IndexIteratorReflecting implements IndexIterator {

    /**
     * Constructs iterator to generate reflections from iterates of
     * given iterator.  Default has all indexes subject to reflection.
     */
    public IndexIteratorReflecting(IndexIterator iterator) {
        this.baseIterator = iterator;
        signIterator = new IndexIteratorRectangular(baseIterator.getD());
        nextIterate = new int[baseIterator.getD()];
        thisIterate = new int[baseIterator.getD()];
        boolean[] doReflect = new boolean[baseIterator.getD()];
        for(int i=0; i<doReflect.length; i++) {
            doReflect[i] = true;
        }
        setReflecting(doReflect);
    }
    
    public int getD() {
        return baseIterator.getD();
    }

    public boolean hasNext() {
        return hasNext;
    }
    
    public void reset() {
        baseIterator.reset();
        signIterator.reset();
        if(baseIterator.hasNext()) {
            baseIterate = baseIterator.next();
            hasNext = true;
            getNext();
        } else {
            hasNext = false;
        }
    }

    public int[] next() {
        System.arraycopy(nextIterate, 0, thisIterate, 0, nextIterate.length);
        getNext();
        return thisIterate;
    }        
     
    private void getNext() {
        boolean ok = true;
        do {
            ok = true;
            if(signIterator.hasNext()) {
                signs = signIterator.next();
            } else if(baseIterator.hasNext()) {
                baseIterate = baseIterator.next();
                signIterator.reset();
                signs = signIterator.next();
            } else {
                hasNext = false;
                break;//do
            }
            System.arraycopy(baseIterate, 0, nextIterate, 0, nextIterate.length);
            for(int i=0; i<nextIterate.length; i++) {
                if(signs[i] == 1) {
                    if(nextIterate[i]==0) {
                        ok = false;
                        break;//for
                    } else {
                        nextIterate[i] *= -1;
                    }
                }
            }
        } while(!ok);
    }
    
    /**
     * Indicates the indexes of the elements that are subject to reflection. A
     * value of "true" in the given array indicates that the element with the
     * corresponding index should be reflected; a value of "false" indicates
     * otherwise. Default is all true.
     * 
     * @throws IllegalArgumentException
     *             if the length of the given array is not equal to the getD();
     */
    public void setReflecting(boolean[] doReflect) {
        if(doReflect.length != baseIterator.getD()) {
            throw new IllegalArgumentException("doReflect.length and baseIterator dimension disagree: "+doReflect.length+", "+baseIterator.getD());
        }
        int[] size = new int[baseIterator.getD()];
        for(int i=0; i<size.length; i++) {
            size[i] = doReflect[i] ? 2 : 1; 
        }
        signIterator.setSize(size);
    }

    /**
     * Method to test and demonstrate class.
     */
    public static void main(String[] args) {
        IndexIteratorTriangularPermutations iterator = new IndexIteratorTriangularPermutations(3);
        IndexIteratorReflecting reflectIterator = new IndexIteratorReflecting(iterator);
        iterator.getCoreIterator().setMaxElementMin(1);
        iterator.getCoreIterator().setMaxElement(1);
        //reflectIterator.setReflecting(new boolean[] {true, true});
        reflectIterator.reset();
        System.out.println("Start");
        int count = 0;
        while(reflectIterator.hasNext()) {
            int[] a = reflectIterator.next();
            int sum = 0;
            for(int i=0; i<a.length; i++) {
                sum += a[i]*a[i];
            }
            System.out.println(++count+". "+Arrays.toString(a) +" "+Math.sqrt(sum));
        }
    }

    private final IndexIterator baseIterator;
    private final IndexIteratorRectangular signIterator;
    private int[] signs;
    private int[] baseIterate;
    private final int[] nextIterate;
    private final int[] thisIterate;
    private boolean hasNext;
}
