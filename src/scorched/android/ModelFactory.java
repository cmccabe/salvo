package scorched.android;

import java.util.HashMap;
import java.util.Random;

import android.os.Bundle;
import android.util.Log;

/**
 * The ModelFactory can creates Models; it is used to start a new
 * game.
 *
 * When we pick game settings, we're really creating a ModelFactory
 * which will later be used to create the in-game Model.
 */
public class ModelFactory {
    /*================= Types =================*/
    public static class PlayerFactory {
        /*================= Static =================*/
        public static PlayerFactory fromBundle(int index, Bundle map) {
            String tag = "PLAYER_" + index + "_";
            String name = map.getString(tag + "NAME");
            int startingLife = map.getInt(tag + "STARTING_LIFE", 0);
            int type = map.getInt(tag + "TYPE", 0);
            int color_ord = map.getInt(tag + "COLOR", 0);
            Player.PlayerColor color = Player.PlayerColor.values()[color_ord];
            return new PlayerFactory(index, name,
                                     startingLife, type, color);
        }

        /*================= Data =================*/
        private int mIndex;
        private String mName;
        private int mStartingLife;
        private int mType;
        private Player.PlayerColor mColor;

        /*================= Access =================*/
        public void saveState(Bundle map) {
            String tag = "PLAYER_" + mIndex + "_";
            if (map != null) {
                map.putString(tag + "NAME", mName);
                map.putInt(tag + "STARTING_LIFE", mStartingLife);
                map.putInt(tag + "TYPE", mType);
                map.putInt(tag + "COLOR", mColor.ordinal());
            }
        }

        /*================= Lifecycle =================*/
        public PlayerFactory(int index, String name,
                             int startingLife, int type,
                             Player.PlayerColor color) {
            mIndex = index;
            mName = name;
            mStartingLife = startingLife;
            mType = type;
            mColor = color;
        }
    }

    /*================= Constants =================*/
    private final static String KEY_DESIRED_TERRAIN_TYPE =
        "KEY_DESIRED_TERRAIN_TYPE";
    private final static String KEY_USE_RANDOM_PLAYER_PLACEMENT =
        "KEY_USE_RANDOM_PLAYER_PLACEMENT";
    private final static String KEY_NUMBER_OF_PLAYERS =
        "KEY_NUMBER_OF_PLAYERS";

    /*================= Data =================*/
    private Model.TerrainType mDesiredTerrainType;
    private boolean mUseRandomPlayerPlacement;
    private PlayerFactory mPlayers[];

    /*================= Access =================*/
    public Model toModel() {
        // return new Model(...);
    	return null;
    }

    /*================= Save / Restore =================*/
    public void saveState(Bundle map) {
        if (map != null) {
            map.putInt(KEY_DESIRED_TERRAIN_TYPE,
                        mDesiredTerrainType.ordinal());
            map.putBoolean(KEY_USE_RANDOM_PLAYER_PLACEMENT,
                        mUseRandomPlayerPlacement);
            map.putInt(KEY_NUMBER_OF_PLAYERS, mPlayers.length);
            for (PlayerFactory p : mPlayers) {
                p.saveState(map);
            }
        }
    }

    public void restoreState(Bundle map) {
        if (map != null) {
            int tt_ord = map.getInt(KEY_DESIRED_TERRAIN_TYPE, 0);
            mDesiredTerrainType = Model.TerrainType.values()[tt_ord];
            mUseRandomPlayerPlacement =
                map.getBoolean(KEY_USE_RANDOM_PLAYER_PLACEMENT, true);
            int numPlayers = map.getInt(KEY_NUMBER_OF_PLAYERS, 0);
            mPlayers = new PlayerFactory[numPlayers];
            for (int i = 0; i < numPlayers; ++i) {
                mPlayers[i] = PlayerFactory.fromBundle(i, map);
            }
        }
    }

    /*================= Lifecycle =================*/
    public ModelFactory() {
        // Some default game settings
        mDesiredTerrainType = Model.TerrainType.HILLY;
        mUseRandomPlayerPlacement = true;
        mPlayers = new PlayerFactory[5];
        mPlayers[0] = new PlayerFactory(0, "Captain Kangaroo", 1000, 0,
                    	Player.PlayerColor.RED);
        mPlayers[1] = new PlayerFactory(1, "Wululu", 1000, 0,
                    	Player.PlayerColor.BLUE);
        mPlayers[2] = new PlayerFactory(2, "Bogusman", 1000, 0,
                    	Player.PlayerColor.PINK);
        mPlayers[3] = new PlayerFactory(3, "Mr. Green", 1200, 0,
                    	Player.PlayerColor.GREEN);
        mPlayers[4] = new PlayerFactory(4, "Silent Bob", 1200, 0,
                    	Player.PlayerColor.PURPLE);
    }
}
