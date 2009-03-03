package scorched.android;

import java.util.Random;

import android.os.Bundle;

/**
 * Model for the Scorched Android game.
 * 
 * ScorchedModel contains all game state except for state relating 
 * to the user interface.
 * It owns all classes, like Player, that contain important game state.
 * 
 * The Game Board
 * height
 *  1.0   +------------------------------------------+
 *        |                                          |
 *        |  __                                    __|
 *        | _  ___             _____              _  |
 *        |_      _    ________     ________     _   |
 *        |        ____                     _____    |
 *  0.0   +------------------------------------------+
 *         01234567...                               MAX_HEIGHTS
 *
 * Height, or Y, is measured from 0 (floor) to 1 (ceiling).
 * The draw routines do whatever scaling is required.
 *
 * Slot, or X, is measured from 0 to MAX_HEIGHTS. The height field
 * stores the height of the terrain at each slot; intermediate values
 * are interpolated. Weapons can have a fractional (float) X, but
 * players can not.
 *
 * Players are assumed to be square, and their X size is defined as
 * SLOTS_PER_PLAYER slots.
 */
public class ScorchedModel {
    /*================= Constants =================*/
    private static final String TAG = "ScorchedModel";

    /** How many entries are there in the height field */
    public static final int MAX_HEIGHTS = 41;

    /** How many 'slots' each player takes up */
    public static final int SLOTS_PER_PLAYER = 3;

    /** How many 'slots' the player's turret takes up */
    public static final int SLOTS_PER_TURRET = 3;

    enum TerrainType {
        TRIANGULAR,
        FLAT,
        //PLATEAUS,
        JAGGED,
        HILLY,
        ROLLING,
    };
        
    /*================= Data =================*/
    /** A source of random numbers TODO: seed with current time */
    public static Random mRandom = new Random();

    /** The height field determines what the playing field looks like. */
    private float mHeights[] = null;

    /** The players */
    private static Player mPlayers[] = null;

    private int mCurPlayerId;

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

    /** Advances to the next player's turn, if there are any players left.
     * Returns false if the round is over, true otherwise */
    public boolean nextPlayer() {
        int oldPlayer = mCurPlayerId;
        int player = mCurPlayerId + 1;
        while (true) {
            if (player > mPlayers.length)
                player = 0;
            if (player == oldPlayer)
                return false;
            if (mPlayers[player].isAlive()) {
                mCurPlayerId = player;
                return true;
            }
            else
                player++;
        }
    }

    /*================= Height field stuff =================*/

    /** Initialize height field with random values */
    private void initHeights(TerrainType t) {
        // Random height initialization
        switch (t)
        {
            case TRIANGULAR:
                mHeights = new float[MAX_HEIGHTS];
                for (int i = 0; i < MAX_HEIGHTS; i++) {
                    float tmp = (MAX_HEIGHTS - 1);
                    mHeights[i] = (tmp - i) / (MAX_HEIGHTS - 1);
                }
                break;

            case FLAT:
                mHeights = new float[MAX_HEIGHTS];
                float level = (float) (0.6 - (mRandom.nextFloat() / 4));
                for (int i = 0; i < MAX_HEIGHTS; i++) {
                    mHeights[i] = level;
                }
                break;

            case JAGGED:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, 2);
                break;

            case HILLY:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, MAX_HEIGHTS / 10);
                break;

            case ROLLING:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, MAX_HEIGHTS / 3);
                break;
        }
    }

    private float[] getRandomHeights() {
        float[] h = new float[MAX_HEIGHTS];
        for (int i = 0; i < MAX_HEIGHTS; i++) {
            h[i] = mRandom.nextFloat();
        }
        return h;
    }

    private float[] movingWindow(float[] input, int windowSize) {
        float[] h = new float[MAX_HEIGHTS];
        
        for (int i = 0; i < MAX_HEIGHTS; i++) {
            float acc = 0;
            for (int j = 0; j < windowSize; ++j) {
                acc += input[(i + j) % MAX_HEIGHTS];
            }
            h[i] = acc / windowSize;
        }
        return h;
    }

    /** Gets the terrain slot for player number N */
    public int playerIdToSlot(int playerId) {
        return ((MAX_HEIGHTS /2) + 
                    (MAX_HEIGHTS * playerId)) / mPlayers.length;
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
    public ScorchedModel(Player players[]) {
        initHeights(TerrainType.ROLLING);
        mPlayers = players;
        if (mPlayers.length > MAX_HEIGHTS) {
            // We have to have enough slots to be sure that no two
            // players will get the same slot
            assert(false); 
        }
        for (int i = 0; i < mPlayers.length; i++) {
        	int id = mPlayers[i].getId();
        	assert (id == i);
            mPlayers[i].setSlot(playerIdToSlot(id));
            mPlayers[i].calcHeight(this);
        }
        mCurPlayerId = 0;
    }
}
