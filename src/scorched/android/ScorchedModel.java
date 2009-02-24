package scorched.android;

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
    private static final int MAX_HEIGHTS = 100;

    /*================= Height field stuff =================*/
    /** The height field determines what the playing field looks like. */
    private float mHeights[] = null;

    /** Initialize height field with random values */
    private void initHeights() {
        mHeights = new float[MAX_HEIGHTS];
        // Random height initialization
        for (int i = 0; i < MAX_HEIGHTS; i++) {
            mHeights[i] = ((i * 27 * 27) % 50);
        }
    }

    float[] getHeights() {
        return mHeights;
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
    public ScorchedModel() {
        initHeights();
    }
}
