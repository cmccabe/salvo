package scorched.android;

import java.util.HashMap;
import java.util.Random;

import android.os.Bundle;
import android.util.Log;

/**
 * Represents the game terrain.
 *
 *                 The Playing Field
 *  MAX_Y +----------------------------------+
 *        |                                  |
 *        |  __                              |
 *        | _  ___             __           _|
 *        |_      _    _______   ___     ___ |
 *        |        ____             _____    |
 *    0   +----------------------------------+
 *        0                                MAX_X
 *
 * X is measured from 0 to MAX_X. The height field
 * stores the height of the terrain at each slot; intermediate values
 * are interpolated. Weapons can have a fractional (float) X, but
 * players can not.

 * Y is measured from 0 (floor) to MAX_Y (extremely high in the air)
 *
 * Players are assumed to be square. The size of the player is PLAYER_SIZE.
 *
 */
public class Terrain {
    /*================= Constants =================*/
    /** The highest X coordinate */
    public static final int MAX_X = 100; //320;

    /** The highest displayable Y coordinate */
    public static final int MAX_DISPLAYABLE_Y = 240;

    /** The force of gravity.
      * As measured by change in downward force each sample */
    public static final float GRAVITY = 0.00006f;

    /** The highest terrain point */
    public static final float MAX_ELEVATION = 20;

    /*================= Data =================*/
    public static class MyVars {
        /** The playing field */
        public float mBoard[];
    }
    private MyVars mV;

    /*================= Access =================*/
    public float[] getHeights() {
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
