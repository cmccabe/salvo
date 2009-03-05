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
public class ScorchedModel {
    /*================= Constants =================*/
    private static final String TAG = "ScorchedModel";

    /** The highest X coordinate */
    public static final int MAX_X = 40;

    /** The highest Y coordinate */
    public static final int MAX_Y = 10000;

    /** The highest terrain point */
    public static final float MAX_ELEVATION = 10;
    
    /** Player size */
    public static final int PLAYER_SIZE = 3;

    /** How long the player's turret is */
    public static final int TURRET_LENGTH = 3;

    /** The force of gravity.
      * As measured by change in downward force each sample */
    public static final float GRAVITY = 0.0001f;

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
                mHeights = new float[MAX_X];
                for (int i = 0; i < MAX_X; i++) {
                    float tmp = (MAX_X - 1);
                    mHeights[i] = (tmp - i) / (MAX_X - 1);
                }
                break;

            case FLAT:
                mHeights = new float[MAX_X];
                float level = (float) (0.6 - (mRandom.nextFloat() / 4));
                for (int i = 0; i < MAX_X; i++) {
                    mHeights[i] = level;
                }
                break;

            case JAGGED:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, 2);
                break;

            case HILLY:
                mHeights = getRandomHeights();
                mHeights = movingWindow(mHeights, MAX_X / 10);
                break;

            case ROLLING:
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
        // We have to have enough slots to be sure that no two
        // players will get the same slot
        if (players.length >= MAX_X) {
            assert(false); 
        }

        // We must have MAX_X mod 2 == 0
        // Because of how drawScreen works
        assert(MAX_X % 2 == 0);

        initHeights(TerrainType.ROLLING);
        mPlayers = players;
        for (int i = 0; i < mPlayers.length; i++) {
        	int id = mPlayers[i].getId();
        	assert (id == i);
            mPlayers[i].setX(playerIdToSlot(id));
            mPlayers[i].calcY(this);
        }
        mCurPlayerId = 0;
    }
}
