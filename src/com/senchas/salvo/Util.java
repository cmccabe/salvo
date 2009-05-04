package com.senchas.salvo;

import java.util.Random;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * General utility stuff that doesn't really fit anywhere else.
 */
public abstract class Util {
    /*================= Types =================*/
    /** Runnable that creates a Toast and displays it */
    public static class DoToast implements Runnable {
        private Context mContext;
        private String mString;

        /*================= Operations =================*/
        public void run() {
            Toast toast = Toast.makeText(mContext, mString, 30);
            toast.setGravity(Gravity.TOP, 0, 30);
            toast.show();
        }

        /*================= Lifecycle =================*/
        public DoToast(Context context, String string) {
            mContext = context;
            mString = string;
        }
    }

    public static class Pair {
        public int yLower;
        public int yUpper;
    }

    /*================= Static =================*/
    /** Returns the Euclidian distance between (x0,y0) and (x1,1) */
    public static float calcDistance(float x0, float y0, float x1, float y1) {
        return (float)
            Math.sqrt(((x1 - x0) * (x1 - x0)) + ((y1 - y0) * (y1 - y0)));
    }

    /** Convenience function that makes a key for player-specific data */
    public static String indexToString(int playerNum) {
        StringBuilder b = new StringBuilder(40);
        b.append("P").append(playerNum).append("_");
        return b.toString();
    }

    /** Convenience function that makes a key for player-specific data */
    public static String indexToString(int playerNum, String key) {
        StringBuilder b = new StringBuilder(80);
        b.append("P").append(playerNum).append("_");
        b.append(key);
        return b.toString();
    }

    /** Returns the top and bottom y-coordinates of a circle centered at
     * (cx, cy) evaulated at x.
     *
     * @return  Returns the output in a mutable data structure called Pair.
     * If the circle doesn't have a y coordinate at x, returns 0 in both
     * y-coordiantes.
     */
    public static void circAt(int x0, int y0, int radius, int x, Pair out) {
        // The equation for a circle is
        // (x - x0)^2 + (y - y0)^2 = r^2
        //
        // Solved for y:
        //              _______________
        //   y = y0 +  | r^2 - (x-x0)^2
        //          - \|
        //

        float tmp = (radius * radius) - ((x - x0) * (x - x0));
        if (tmp < 0) {
            out.yLower = out.yUpper = 0;
        }
        else {
            out.yLower = (int)(y0 + (float)Math.sqrt(tmp));
            out.yUpper = (int)(y0 - (float)Math.sqrt(tmp));
        }
    }

    /** Given an input range [inMin, inMax], an output range
     * [outMin, outMax], and an input x, returns an x that
     * is scaled to the output range.
     *
     * @return the scaled x
     */
    public static int linearInterpolation(int outMin, int outMax,
                                          int inMin, int inMax,
                                          int x) {
        if (x < inMin)
            return outMin;
        else if (x > inMax)
            return outMax;
        int ret = outMin + ((x - inMin) * (outMax - outMin)) / (inMax - inMin);
        return ret;
    }

    /*================= Data =================*/
    /** A source of random numbers. */
    public static Random mRandom = new Random();
}
