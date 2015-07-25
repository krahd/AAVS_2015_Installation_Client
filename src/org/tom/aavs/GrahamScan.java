package org.tom.aavs;

/*
 * Copyright (c) 2010, Bart Kiers
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * modified by tom
 */


import java.util.*;

import processing.core.*;

public final class GrahamScan {

    /**
     * An enum denoting a directional-turn between 3 PVectors (vectors).
     */
    protected static enum Turn { CLOCKWISE, COUNTER_CLOCKWISE, COLLINEAR }

    /**
     * Returns true iff all PVectors in <code>PVectors</code> are collinear.
     *
     * @param PVectors the list of PVectors.
     * @return       true iff all PVectors in <code>PVectors</code> are collinear.
     */
    protected static boolean areAllCollinear(List<PVector> PVectors) {

        if(PVectors.size() < 2) {
            return true;
        }

        final PVector a = PVectors.get(0);
        final PVector b = PVectors.get(1);

        for(int i = 2; i < PVectors.size(); i++) {

            PVector c = PVectors.get(i);

            if(getTurn(a, b, c) != Turn.COLLINEAR) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the convex hull of the PVectors created from <code>xs</code>
     * and <code>ys</code>. Note that the first and last PVector in the returned
     * <code>List&lt;java.awt.PVector&gt;</code> are the same PVector.
     *
     * @param xs the x coordinates.
     * @param ys the y coordinates.
     * @return   the convex hull of the PVectors created from <code>xs</code>
     *           and <code>ys</code>.
     * @throws IllegalArgumentException if <code>xs</code> and <code>ys</code>
     *                                  don't have the same size, if all PVectors
     *                                  are collinear or if there are less than
     *                                  3 unique PVectors present.
     */
    public static List<PVector> getConvexHull(int[] xs, int[] ys) throws IllegalArgumentException {

        if(xs.length != ys.length) {
            throw new IllegalArgumentException("xs and ys don't have the same size");
        }

        List<PVector> PVectors = new ArrayList<PVector>();

        for(int i = 0; i < xs.length; i++) {
            PVectors.add(new PVector(xs[i], ys[i]));
        }

        return getConvexHull(PVectors);
    }

    /**
     * Returns the convex hull of the PVectors created from the list
     * <code>PVectors</code>. Note that the first and last PVector in the
     * returned <code>List&lt;java.awt.PVector&gt;</code> are the same
     * PVector.
     *
     * @param PVectors the list of PVectors.
     * @return       the convex hull of the PVectors created from the list
     *               <code>PVectors</code>.
     * @throws IllegalArgumentException if all PVectors are collinear or if there
     *                                  are less than 3 unique PVectors present.
     */
    public static List<PVector> getConvexHull(List<PVector> PVectors) throws IllegalArgumentException {

        List<PVector> sorted = new ArrayList<PVector>(getSortedPVectorSet(PVectors));

        if(sorted.size() < 3) {
            throw new IllegalArgumentException("can only create a convex hull of 3 or more unique PVectors");
        }

        if(areAllCollinear(sorted)) {
            throw new IllegalArgumentException("cannot create a convex hull from collinear PVectors");
        }

        Stack<PVector> stack = new Stack<PVector>();
        stack.push(sorted.get(0));
        stack.push(sorted.get(1));

        for (int i = 2; i < sorted.size(); i++) {

            PVector head = sorted.get(i);
            PVector middle = stack.pop();
            PVector tail = stack.peek();

            Turn turn = getTurn(tail, middle, head);

            switch(turn) {
                case COUNTER_CLOCKWISE:
                    stack.push(middle);
                    stack.push(head);
                    break;
                case CLOCKWISE:
                    i--;
                    break;
                case COLLINEAR:
                    stack.push(head);
                    break;
            }
        }

        // close the hull
        stack.push(sorted.get(0));

        return new ArrayList<PVector>(stack);
    }

    /**
     * Returns the PVectors with the lowest y coordinate. In case more than 1 such
     * PVector exists, the one with the lowest x coordinate is returned.
     *
     * @param PVectors the list of PVectors to return the lowest PVector from.
     * @return       the PVectors with the lowest y coordinate. In case more than
     *               1 such PVector exists, the one with the lowest x coordinate
     *               is returned.
     */
    protected static PVector getLowestPVector(List<PVector> PVectors) {

        PVector lowest = PVectors.get(0);

        for(int i = 1; i < PVectors.size(); i++) {

            PVector temp = PVectors.get(i);

            if(temp.y < lowest.y || (temp.y == lowest.y && temp.x < lowest.x)) {
                lowest = temp;
            }
        }

        return lowest;
    }

    /**
     * Returns a sorted set of PVectors from the list <code>PVectors</code>. The
     * set of PVectors are sorted in increasing order of the angle they and the
     * lowest PVector <tt>P</tt> make with the x-axis. If tow (or more) PVectors
     * form the same angle towards <tt>P</tt>, the one closest to <tt>P</tt>
     * comes first.
     *
     * @param PVectors the list of PVectors to sort.
     * @return       a sorted set of PVectors from the list <code>PVectors</code>.
     * @see GrahamScan#getLowestPVector(java.util.List)
     */
    protected static Set<PVector> getSortedPVectorSet(List<PVector> PVectors) {

        final PVector lowest = getLowestPVector(PVectors);

        TreeSet<PVector> set = new TreeSet<PVector>(new Comparator<PVector>() {
            @Override
            public int compare(PVector a, PVector b) {

                if(a == b || a.equals(b)) {
                    return 0;
                }

                // use longs to guard against int-underflow
                double thetaA = Math.atan2((long)a.y - lowest.y, (long)a.x - lowest.x);
                double thetaB = Math.atan2((long)b.y - lowest.y, (long)b.x - lowest.x);

                if(thetaA < thetaB) {
                    return -1;
                }
                else if(thetaA > thetaB) {
                    return 1;
                }
                else {
                    // collinear with the 'lowest' PVector, let the PVector closest to it come first

                    // use longs to guard against int-over/underflow
                    double distanceA = (((long)lowest.x - a.x) * ((long)lowest.x - a.x)) +
                                                (((long)lowest.y - a.y) * ((long)lowest.y - a.y));
                    
                    double distanceB = (((long)lowest.x - b.x) * ((long)lowest.x - b.x)) +
                                                (((long)lowest.y - b.y) * ((long)lowest.y - b.y));

                    if(distanceA < distanceB) {
                        return -1;
                    }
                    else {
                        return 1;
                    }
                }
            }
        });

        set.addAll(PVectors);

        return set;
    }

    /**
     * Returns the GrahamScan#Turn formed by traversing through the
     * ordered PVectors <code>a</code>, <code>b</code> and <code>c</code>.
     * More specifically, the cross product <tt>C</tt> between the
     * 3 PVectors (vectors) is calculated:
     *
     * <tt>(b.x-a.x * c.y-a.y) - (b.y-a.y * c.x-a.x)</tt>
     *
     * and if <tt>C</tt> is less than 0, the turn is CLOCKWISE, if
     * <tt>C</tt> is more than 0, the turn is COUNTER_CLOCKWISE, else
     * the three PVectors are COLLINEAR.
     *
     * @param a the starting PVector.
     * @param b the second PVector.
     * @param c the end PVector.
     * @return the GrahamScan#Turn formed by traversing through the
     *         ordered PVectors <code>a</code>, <code>b</code> and
     *         <code>c</code>.
     */
    protected static Turn getTurn(PVector a, PVector b, PVector c) {

        // use longs to guard against int-over/underflow
        float crossProduct = (((long)b.x - a.x) * ((long)c.y - a.y)) -
                            (((long)b.y - a.y) * ((long)c.x - a.x));

        if(crossProduct > 0) {
            return Turn.COUNTER_CLOCKWISE;
        }
        else if(crossProduct < 0) {
            return Turn.CLOCKWISE;
        }
        else {
            return Turn.COLLINEAR;
        }
    }
}
