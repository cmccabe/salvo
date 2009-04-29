package scorched.android;

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
    public static final float GRAVITY = 0.06f;

    /*================= Data =================*/
    public static class MyVars {
        /** The playing field */
        public short mBoard[];
    }
    private MyVars mV;

    /*================= Access =================*/
    public short[] getHeights() {
        return mV.mBoard;
    }

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
