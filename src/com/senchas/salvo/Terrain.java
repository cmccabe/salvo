package com.senchas.salvo;

import java.util.HashMap;
import java.util.Random;

import android.os.Bundle;
import android.util.Log;

/**
 * Represents the game terrain.
 *
 *                 The Playing Field
 * -65535
 *        +----------------------------------+
 *        |                                  |
 *       ...                                ...
 *        |                                  |
 *        |     negative Y zone              |
 *        |     (not visible onscreen)       |
 *        |                                  |
 *    0   +----------------------------------+
 *        |                                  |
 *        |  __                              |
 *        | _  ___             __           _|
 *        |_      _    _______   ___     ___ |
 *        |        ____             _____    |
 *  MAX_Y +----------------------------------+
 *        0                                MAX_X
 *
 * X is measured from 0 to MAX_X. For each X value, there is an associated
 * height field value, measured in pixels.

 * The lowest point has Y = MAX_Y.
 * If a missile's Y coordinate is less than 0, it will not be visible
 * onscreen.
 *
 */
public class Terrain {
    /*================= Constants =================*/
    /** The highest X coordinate */
    public static final int MAX_X = 480;

    /** The highest displayable Y coordinate */
    public static final int MAX_Y = 320;

    /** The space between the left wall and the leftmost player.
     *  Also the space between the right wall and the rightmost player.
     */
    public static final int SIDE_BUFFER_SIZE = 30;

    /** The force of gravity.
      * As measured by change in downward force each sample */
    public static final float GRAVITY = 0.1f;

    /** The maximum wind that we can have.
     * Wind is a purely horizontal constant acceleration on the projectile */
    public static final int MAX_WIND = 50;

    public static final int TERRAIN_ANGLE_DELTA = 10;

    /*================= Data =================*/
    public static class MyVars {
        /** The playing field */
        public short mBoard[];
    }
    private MyVars mV;

    /*================= Access =================*/
    public short[] getBoard() {
        return mV.mBoard;
    }

    /** Gets the average value of the samples between A and B.
     *
     * We treat values that are out of range as having the same value as
     * the relevant edge.
     */
    public float getAverageValue(int a, int b) {
        if (a > b) {
            throw new RuntimeException("getAverageValue: must have a <= b");
        }
        short h[] = mV.mBoard;
        int acc = 0;
        for (int i = a; i <= b; i++) {
            if (i < 0)
                acc += h[0];
            else if (i >= h.length)
                acc += h[h.length - 1];
            else
                acc += h[i];
        }
        float ret = acc;
        return ret / (b - a);
    }

    /** Given an x value, returns an angle which gives an idea of how far
     * from the horizontal the terrain is around that X.
     *
     * Consider a small interval [x - delta, x + delta].
     * We average the samples in the first half of the interval and call that
     * value y0.
     * We average the samples in the second half of the interval and call
     * that value y1.
     * Then the terrain angle is the angle between
     * (x - delta, y0) and (x + delta, y1) in radians.
     */
    public float getTerrainAngle(int x) {
        float y0 = getAverageValue(x - TERRAIN_ANGLE_DELTA, x);
        float y1 = getAverageValue(x, x + TERRAIN_ANGLE_DELTA);
        return (float)Math.atan2(y1 - y0, TERRAIN_ANGLE_DELTA * 2);
    }

    /*================= Operations =================*/

    /*================= Save State =================*/
    public void saveState(Bundle map) {
        AutoPack.autoPack(map, AutoPack.EMPTY_STRING, mV);
    }

    /*================= Lifecycle =================*/
    public static Terrain fromBundle(Bundle map) {
        MyVars v = (MyVars) AutoPack.
            autoUnpack(map, AutoPack.EMPTY_STRING, MyVars.class);
        return new Terrain(v);
    }

    public Terrain(MyVars v) {
        mV = v;
    };
}
