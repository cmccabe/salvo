package scorched.android;

import java.util.HashMap;
import java.util.Random;

import android.os.Bundle;
import android.util.Log;

/**
 * Model for the Scorched Android game.
 *
 * The Model owns all game state-- except for state relating
 * to the user interface.
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
 */
public class Model {
    /*================= Constants =================*/
    private static final String TAG = "Model";

    /** The highest X coordinate */
    public static final int MAX_X = 100;

    /** The highest Y coordinate */
    public static final int MAX_Y = 10000;

    /** The highest terrain point */
    public static final float MAX_ELEVATION = 20;

    /** Player size */
    public static final int PLAYER_SIZE = 1;

    /** How long the player's turret is */
    public static final int TURRET_LENGTH = 1;

    /** The force of gravity.
      * As measured by change in downward force each sample */
    public static final float GRAVITY = 0.00006f;

    enum TerrainType {
        // TODO: move terrain generation functions into this enum
        Triangular,
        Flat,
        //Plateaus,
        Jagged,
        Hilly,
        Rolling;

        /** Returns an array of strings where each entry describes a 
         * terrain type */
        public static String [] getStrings() {
            TerrainType t[] = TerrainType.values();
            String ret[] = new String[t.length];
            for (int i = 0; i < t.length; i++)
                ret[i] = t[i].toString() + " terrain";
            return ret;
        }
    };

    /*================= Data =================*/
    /** A source of random numbers. TODO: seed with current time */
    public Random mRandom = new Random();

    /** The height field determines what the playing field looks like. */
    private float mHeights[] = null;

    /** The players */
    private Player mPlayers[] = null;

    private int mCurPlayerId;

    /** Maps slots to players */
    private HashMap<Integer, Player> mSlotToPlayer;

    /*================= Access =================*/
    public float[] getHeights() {
        return mHeights;
    }

    public Player getPlayer(int num) {
        return mPlayers[num];
    }

    public int getNumberOfPlayers() {
        return mPlayers.length;
    }

    public Player getCurPlayer() {
        return mPlayers[mCurPlayerId];
    }

    public int getCurPlayerId() {
        return mCurPlayerId;
    }

    /** Sets mCurPlayerId to the next valid player id-- or to
     * INVALID_PLAYER_ID if there are none. */
    public void nextPlayer() {
        int oldPlayer = mCurPlayerId;
        int player = mCurPlayerId + 1;

        while (true) {
            if (player >= mPlayers.length) {
                if (oldPlayer == Player.INVALID_PLAYER_ID) {
                    // We searched the whole array and didn't find any valid
                    // players.
                    mCurPlayerId = Player.INVALID_PLAYER_ID;
                    return;
                }
                else {
                    player = 0;
                }
            }
            if (player == oldPlayer) {
                // We're back at the player we started at.
                // I guess he's the only valid one.
                return;
            }
            if (mPlayers[player].isAlive()) {
                mCurPlayerId = player;
                return;
            }
            else {
                player++;
            }
        }
    }

    /** Returns the player in this slot, or null if there is nobody there. */
    public Player slotToPlayer(int slot) {
        return mSlotToPlayer.get(new Integer(slot));
    }

    /*================= Height field stuff =================*/

    /** Initialize height field with random values */
    private void initHeights(TerrainType t) {
        // Random height initialization
        switch (t)
        {
            case Triangular:
                mHeights = new float[MAX_X];
                for (int i = 0; i < MAX_X; i++) {
                    float tmp = (MAX_X - 1);
                    mHeights[i] = (tmp - i) / (MAX_X - 1);
                }
                break;

            case Flat:
                mHeights = new float[MAX_X];
                float level = (float) (0.6 - (mRandom.nextFloat() / 4));
                for (int i = 0; i < MAX_X; i++) {
                    mHeights[i] = level;
                }
                break;

            case Jagged:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, 2);
                break;

            case Hilly:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, 3);
                break;

            case Rolling:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, MAX_X / 3);
                break;
        }
    }

    private float[] getRandomHeights() {
        float[] h = new float[MAX_X];
        for (int i = 0; i < MAX_X; i++) {
            h[i] = mRandom.nextFloat() * MAX_ELEVATION;
        }
        return h;
    }

    private float[] movingWindow(float[] input, int windowSize) {
        float[] h = new float[MAX_X];

        for (int i = 0; i < MAX_X; i++) {
            float acc = 0;
            for (int j = 0; j < windowSize; ++j) {
                acc += input[(i + j) % MAX_X];
            }
            h[i] = acc / windowSize;
        }
        return h;
    }

    /** Gets the terrain slot for player number N */
    private int playerIdToSlot(int playerId) {
        return ((MAX_X /2) + (MAX_X * playerId)) / mPlayers.length;
    }

    private float square(float x) {
        return x * x;
    }

    private int constrainSlot(int slot) {
        if (slot < 0)
            return 0;
        else if (slot >= MAX_X)
            return MAX_X - 1;
        else
            return slot;
    }

    /** Updates the height field with the result of an explosion at (x0,y0)
     *  with the given radius
     */
    public void doExplosion(float x0, float y0, float radius) {
        final float xLeft = x0 - (radius/2f);
        final float xRight = x0 + (radius/2f);
        final int firstSlot = constrainSlot((int)(xLeft + 0.5f));
        final int lastSlot = constrainSlot((int)(xRight + 0.5f));

        for (int slot = firstSlot; slot <= lastSlot; slot++) {
             // The equation for a circle is
             // (x - x0)^2 + (y - y0)^2 = r
             //
             // Solved for y:
             //              _____________
             //   y = y0 +  | r - (x-x0)^2
             //          - \|
             //
            final float tmp = radius - square(slot - x0);
            if (tmp <= 0)
                continue;
            final float tmp2 = (float)Math.sqrt(tmp);
            final float top = y0 + tmp2;
            final float bottom = y0 - tmp2;
            doSlotCollision(slot, top, bottom);
         }
    }

    /** Changes the height field at X='slot' to reflect an explosion
     *  that extends upward to 'top' and downward to 'bottom'.
     */
    private void doSlotCollision(int slot, float top, float bottom) {
        Log.w(TAG,"doSlotCollision(slot=" + slot +
                                ",top=" + top +
                                ",bottom=" + bottom);
        final float slotHeight = mHeights[slot];
        if (bottom >= slotHeight) {
            // The explosion is too far up in the air to affect the ground
            return;
        }
        else if (top <= slotHeight) {
            // The explosion is completely underground
            mHeights[slot] -= (top - bottom);
        }
        else {
            // Part of the explosion is underground, but part is in the air
            mHeights[slot] -= (slotHeight - bottom);
        }
    }

    /*================= Save / Restore =================*/
    void saveState(Bundle map) {
        if (map != null) {
            /*map.putDouble(KEY_FUEL, Double.valueOf(mFuel));
            etc... */
        }
    }

    public synchronized void restoreState(Bundle map) {
        /* do a bunch of stuff with savedState ... */
    }

    /*================= Lifecycle =================*/
    public Model(Player players[]) {
        // We have to have enough slots to be sure that no two
        // players will get the same slot
        if (players.length >= MAX_X) {
            assert(false);
        }

        // We must have MAX_X mod 2 == 0
        // Because of how drawScreen works
        assert(MAX_X % 2 == 0);

        initHeights(TerrainType.Hilly);
        mPlayers = players;
        for (int i = 0; i < mPlayers.length; i++) {
            int id = mPlayers[i].getId();
            assert (id == i);
            mPlayers[i].setX(playerIdToSlot(id));
            mPlayers[i].calcY(this);
        }
        mCurPlayerId = Player.INVALID_PLAYER_ID;

        mSlotToPlayer = new HashMap<Integer, Player>();
        for (int i = 0; i < mPlayers.length; i++) {
            mSlotToPlayer.put(new Integer(mPlayers[i].getX()), mPlayers[i]);
        }
    }
}
