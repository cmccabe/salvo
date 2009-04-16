package scorched.android;

import java.util.HashMap;
import java.util.Random;

import android.os.Bundle;
import android.util.Log;

/**
 * Creates a Terrain
 *
 * Each TerrainFactory creates a different looking terrain.
 */
public enum TerrainFactory {
    /*================= Values =================*/
    Triangular(new TriangularStrat()),
    Flat(new FlatStrat()),
    Jagged(new JaggedStrat()),
    Hilly(new HillyStrat()),
    Rolling(new RollingStrat());

    /*================= Static =================*/
    /** Return a random elevation
     *
     * @param bot    Minimum percentage from the bottom
     * @param top    Maximum percentage from the bottom
     */
    private static short randElevation(float bot, float top) {
        int rand = Util.mRandom.nextInt((int)(Terrain.MAX_Y * (top - bot)));
        int midRand = (int)(rand + (bot * Terrain.MAX_Y));
        return (short)(Terrain.MAX_Y - midRand);
    }

    /*================= Types =================*/
    private interface TerrainStrategy {
        public abstract Terrain toTerrain();
    }

    public static class TriangularStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short h[] = new short[Terrain.MAX_X];
            for (int i = 0; i < Terrain.MAX_X; i++) {
                int tmp = (Terrain.MAX_X - 1);
                h[i] = (short)((Terrain.MAX_Y * i) / Terrain.MAX_X);
            }
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class FlatStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = new short[Terrain.MAX_X];
            short e = randElevation(0.4f, 0.6f);
            for (int i = 0; i < Terrain.MAX_X; i++)
                h[i] = e;
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class JaggedStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = getRandomHeights();
            h = movingWindow(h, 3);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    public static class HillyStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = getRandomHeights();
            h = movingWindow(h, 10);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    private static class SplineSet {
        /*================= Types =================*/
        private class Spline {
            /*================= Data =================*/
            /** The x coordinate of the leftmost control point of this
             * spline */
            private float mX;

            /** The y coordinates of the control points */
            private float mY0, mY1, mY2;

            /*================= Access =================*/
            public float getX() {
                return mX;
            }

            public float getVal(int x) {
                final float t = x - mX;
                final float iv = splineXSize();
                final float c = iv * iv;

                // contribution of the first control point
                // B[0,2](t) = (1 - t)^2
                float contrib0 = ((iv - t) * (iv - t)) / c;

                // contribution of the second control point
                // B[1,2](t) = 2 * (1 - t) * t
                float contrib1 = (2 * (iv - t) * t) / c;

                // contribution of the third control point
                // B[2,2](t) = t^2
                float contrib2 = (t * t) / c;

                return (contrib0 * mY0) +
                        (contrib1 * mY1) +
                        (contrib2 * mY2);
            }

            /*================= Lifecycle =================*/
            public Spline(float x,
                          float y0, float y1, float y2) {
                mX = x;
                mY0 = y0;
                mY1 = y1;
                mY2 = y2;
            }
        }

        /*================= Data =================*/
        /** The spacing between adjacent control points */
        private float mS;

        /** The splines in this set */
        private Spline mSplines[];

        /*================= Access =================*/
        /** The size of each individual spline.
         *  Since we're using cubic splines, this is equal to three times the
         *  space between control points. */
        private final float splineXSize() {
            return 3.0f * mS;
        }

        /** Finds the spline which includes x.
         *  This could be optimized a bit more through binary search or
         *  something. */
        private Spline getSpline(int x) {
            for (Spline spline: mSplines) {
                float sx = spline.getX();
                if ((x >= sx) && (x < (sx + splineXSize())))
                    return spline;
            }
            throw new RuntimeException("getStartingSpline: can't find a " +
                "spline which includes x = " + x);
        }

        /** Return the value of the SplineSet at x */
        public short getVal(int x) {
            Spline spline = getSpline(x);
            return (short)spline.getVal(x);
        }

        /*================= Lifecycle =================*/
        public SplineSet(int boardSize, short[] controlPoints) {
            if (controlPoints.length % 2 != 1) {
                throw new RuntimeException("must give an odd number of " +
                                           "control points");
            }

            mS = boardSize / (controlPoints.length - 2);

            mSplines = new Spline[controlPoints.length / 2];
            float x = -mS;
            int i = 0;
            for (int c = 0; c < controlPoints.length - 2; c+=2) {
                mSplines[i] = new Spline(x,
                                 controlPoints[c],
                                 controlPoints[c+1],
                                 controlPoints[c+2]);
                i++;
                x += splineXSize();
            }
        }
    }

    public static class RollingStrat implements TerrainStrategy {
        public Terrain toTerrain() {
            short[] h = new short[Terrain.MAX_X];
            short[] controlPoints = new short[5];
            controlPoints[0] = randElevation(0f, 1f);
            for (int i = 0; i < controlPoints.length - 1; i++)
                controlPoints[i] = randElevation(0.1f, 0.8f);
            controlPoints[controlPoints.length - 1] = randElevation(0f, 1f);

            SplineSet splines = new SplineSet(Terrain.MAX_X, controlPoints);
            for (int i = 0; i < h.length; i++)
                h[i] = splines.getVal(i);
            Terrain.MyVars v = new Terrain.MyVars();
            v.mBoard = h;
            return new Terrain(v);
        }
    }

    /*================= Utility =================*/
    private static short[] getRandomHeights() {
        short[] h = new short[Terrain.MAX_X];
        for (int i = 0; i < Terrain.MAX_X; i++) {
            h[i] = (short)Util.mRandom.nextInt(Terrain.MAX_Y);
        }
        return h;
    }

    private static short[] movingWindow(short[] input, int windowSize) {
        short[] h = new short[input.length];
        for (int i = 0; i < Terrain.MAX_X; i++) {
            short acc = 0;
            for (int j = 0; j < windowSize; ++j) {
                acc += input[(i + j) % Terrain.MAX_X];
            }
            h[i] = (short)(acc / windowSize);
        }
        return h;
    }

    /*================= Data =================*/
    private final TerrainStrategy mStrat;

    /*================= Access =================*/
    public Terrain createTerrain() {
        return mStrat.toTerrain();
    }

    public String toString() {
        return this.name() + " terrain";
    }

    /*================= Lifecycle =================*/
    private TerrainFactory(TerrainStrategy strat) {
        mStrat = strat;
    }
}
