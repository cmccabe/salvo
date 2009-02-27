package scorched.android;

import java.util.Random;

import android.os.Bundle;

/**
 * Model for the Scorched Android game.
 * 
 * The model contains the 'meat' of the game logic.
 * It contains all game state except for state relating to the user interface.
 */
public class ScorchedModel {
    /*================= Constants =================*/
    private static final String TAG = "ScorchedModel";
    public static final int MAX_HEIGHTS = 21;
    public static final int HEIGHTS_PER_POLY = 3;

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
        return ((MAX_HEIGHTS /2) + (MAX_HEIGHTS * playerId)) / mPlayers.length;
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
        }
    }
}
