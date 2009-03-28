package scorched.android;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import scorched.android.Model.TerrainType;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The ModelFactory can creates Models; it is used to start a new
 * game.
 *
 * When we pick game settings, we're really creating a ModelFactory
 * which will later be used to create the in-game Model.
 *
 * Thread-safety: this class implements its own thread safety.
 * It uses the heavy-hammer approach because performance is not
 * critical here, and I am lazy.
 */
public class ModelFactory {
    /*================= Types =================*/
    /** Represents the number of rounds in the game */
    public static enum NumRounds {
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        TEN(10),
        TWENTY(20);

        /*================= Static =================*/
        public static NumRounds fromShort(short s) {
            NumRounds t[] = NumRounds.values();
            for (NumRounds n : t) {
                if (n.toShort() == s)
                    return n;
            }
            throw new RuntimeException
                ("no NumRounds object with mShort = " + s);
        }

        public static String [] getStrings() {
            NumRounds t[] = NumRounds.values();
            String ret[] = new String[t.length];
            for (int i = 0; i < t.length; i++) {
                StringBuilder b = new StringBuilder(10);
                b.append(t[i].toShort()).append(" round");
                if (t[i] != ONE)
                    b.append("s");
                ret[i] = b.toString();
            }
            return ret;
        }

        /*================= Data =================*/
        private short mShort;

        /*================= Access =================*/
        public short toShort() {
            return mShort;
        }

        /*================= Lifecycle =================*/
        NumRounds(int i) {
            mShort = (short)i;
        }
    };

    /*================= Types =================*/
    public static class PlayerFactory {

        private static enum PlayerType {
            HUMAN("Human player"),
            COMPUTER_EASY("Computer: Easy"),
            COMPUTER_MEDIUM("Computer: Medium"),
            COMPUTER_HARD("Computer: Hard");

            public String toString() { return mName; }

            PlayerType(String name) { mName = name; }

            private final String mName;
        }

        /*================= Static =================*/
        public static PlayerFactory fromBundle(int index, Bundle map) {
            String tag = "PLAYER_" + index + "_";
            String name = map.getString(tag + "NAME");
            int typeInt = map.getInt(tag + "PLAYER_TYPE");
            PlayerType type = PlayerType.values()[typeInt];
            short startingLife = map.getShort(tag + "STARTING_LIFE_PERCENT");
            int color_ord = map.getInt(tag + "COLOR");
            Player.PlayerColor color = Player.PlayerColor.values()[color_ord];
            return new PlayerFactory(index, name, type,
                                     startingLife, color);
        }

        /*================= Data =================*/
        private int mIndex;
        private String mName;
        private short mStartingLifePercent;
        private PlayerType mType;
        private Player.PlayerColor mColor;

        /*================= Access =================*/
        public void saveState(Bundle map) {
            String tag = "PLAYER_" + mIndex + "_";
            if (map != null) {
                map.putString(tag + "NAME", mName);
                map.putInt(tag + "PLAYER_TYPE", mType.ordinal());
                map.putShort(tag + "STARTING_LIFE_PERCENT",
                            mStartingLifePercent);
                map.putInt(tag + "COLOR", mColor.ordinal());
            }
        }

        public String getName() {
            return mName;
        }

        public PlayerType getPlayerType() {
            return mType;
        }

        public short getStartingLifePercent() {
            return mStartingLifePercent;
        }

        public Player.PlayerColor getColor() {
            return mColor;
        }

        /*================= Lifecycle =================*/
        public PlayerFactory(int index, String name,
                             PlayerType type,
                             short startingLifePercent,
                             Player.PlayerColor color) {
            mIndex = index;
            mName = name;
            mType = type;
            mStartingLifePercent = startingLifePercent;
            mColor = color;
        }
    }

    public class PlayerListAdapter extends BaseAdapter
    {
        /*================= Access =================*/
        public boolean areAllItemsEnabled() { return true; }

        public boolean isEnabled(int position) { return true; }

        public boolean areAllItemsSelectable() { return true; }

        public long getItemId(int position) { return position; }

        public int getCount() { return mPlayers.size(); }

        public Object getItem(int position) {
            return mPlayers.get(position);
        }

        public View getView(int position, View convertView,
                            ViewGroup parent) {
            PlayerFactory p = mPlayers.get(position);
            Context c = parent.getContext();
            LinearLayout lay = null;
            TextView upper = null, lower = null;

            // This whole complicated rain-dance is to figure out if
            // we can reuse convertView for our purposes, or if we
            // need to create a new view.
            // The doxygen for BaseAdatper.getView() just hints darkly that
            // "you should check that this view is non-null and of an
            // appropriate type before using" so I don't really know
            // what kind of crazy garbage convertView might be.
            if (convertView != null) {
                if (convertView instanceof LinearLayout) {
                    LinearLayout ll = (LinearLayout)convertView;
                    if (ll.getChildCount() == 2) {
                        View u = ll.getChildAt(0);
                        View l = ll.getChildAt(1);
                        if ((u instanceof TextView) &&
                            (l instanceof TextView)) {
                            upper = (TextView)u;
                            lower = (TextView)l;
                            lay = ll;
                        }
                    }
                }
            }
            if (lay == null) {
                lay = new LinearLayout(c);
                upper = new TextView(c);
                lower = new TextView(c);
                lay.addView(upper);
                lay.addView(lower);
            }

            // Set up the layout
            lay.setOrientation(LinearLayout.VERTICAL);
            lay.setHorizontalGravity(Gravity.LEFT);

            // Set up upper view
            upper.setTextSize(TypedValue.COMPLEX_UNIT_MM, 6);
            upper.setTextColor(p.getColor().toInt());
            upper.setText(p.getName());
            //upper.setTextColor(p.getColor());
            //upper.setTypeface(BOLD);
            StringBuilder b = new StringBuilder(50);
            b.append(p.getPlayerType().toString());
            b.append(": ");
            b.append(p.getStartingLifePercent());
            b.append("%");

            upper.setTextSize(TypedValue.COMPLEX_UNIT_MM, 4);
            lower.setText(b.toString());
            return lay;
        }

        /*================= Lifecycle =================*/
        public PlayerListAdapter() { }
    }

    /*================= Constants =================*/
    private final static String KEY_DESIRED_TERRAIN_TYPE =
        "KEY_DESIRED_TERRAIN_TYPE";
    private final static String KEY_USE_RANDOM_PLAYER_PLACEMENT =
        "KEY_USE_RANDOM_PLAYER_PLACEMENT";
    private final static String KEY_NUM_ROUNDS = "KEY_NUM_ROUNDS";
    private final static String KEY_NUMBER_OF_PLAYERS =
        "KEY_NUMBER_OF_PLAYERS";

    /*================= Data =================*/
    private Model.TerrainType mDesiredTerrainType;
    private boolean mUseRandomPlayerPlacement;
    private short mNumRounds;
    private LinkedList < PlayerFactory > mPlayers;

    /** A helper object that helps this class talk to a Listview */
    private PlayerListAdapter mAdapter;

    /*================= Access =================*/
    public Model.TerrainType getDesiredTerrainType() {
        return mDesiredTerrainType;
    }

    public boolean getRandomPlayerPlacement() {
        return mUseRandomPlayerPlacement;
    }

    public short getNumRounds() {
        return mNumRounds;
    }

    public synchronized Model toModel() {
        // return new Model(...);
        return null;
    }

    public synchronized PlayerListAdapter getPlayerListAdapter() {
        if (mAdapter == null)
            mAdapter = new PlayerListAdapter();
        return mAdapter;
    }

    /** Returns true if we can add another player */
    public synchronized boolean canAddPlayer() {
        // return (mPlayers.size() + 1 < Model.MAX_PLAYERS);
        return false;
    }

    /** Returns true if all players are computers */
    public synchronized boolean everyoneIsAComputer() {
        for (PlayerFactory p: mPlayers) {
            if (p.getPlayerType() ==
                    PlayerFactory.PlayerType.HUMAN)
                return false;
        }
        return true;
    }

    /*================= Save / Restore =================*/
    public synchronized void saveState(Bundle map) {
        if (map != null) {
            map.putInt(KEY_DESIRED_TERRAIN_TYPE,
                        mDesiredTerrainType.ordinal());
            map.putBoolean(KEY_USE_RANDOM_PLAYER_PLACEMENT,
                        mUseRandomPlayerPlacement);
            map.putShort(KEY_NUM_ROUNDS, mNumRounds);
            map.putInt(KEY_NUMBER_OF_PLAYERS, mPlayers.size());
            for (PlayerFactory p : mPlayers) {
                p.saveState(map);
            }
        }
    }

    public final synchronized void restoreState(Bundle map) {
        if (map != null) {
            int tt_ord = map.getInt(KEY_DESIRED_TERRAIN_TYPE);
            mDesiredTerrainType = Model.TerrainType.values()[tt_ord];
            mUseRandomPlayerPlacement =
                map.getBoolean(KEY_USE_RANDOM_PLAYER_PLACEMENT);
            mNumRounds = map.getShort(KEY_NUM_ROUNDS);
            int numPlayers = map.getInt(KEY_NUMBER_OF_PLAYERS);
            mPlayers = new LinkedList < PlayerFactory >();
            for (int i = 0; i < numPlayers; ++i) {
                mPlayers.add(PlayerFactory.fromBundle(i, map));
            }
        }
    }

    /*================= Operations =================*/
    public synchronized void setTerrainType(TerrainType ty) {
        mDesiredTerrainType = ty;
    }

    public synchronized void modifyRandomPlayerPlacement(boolean b) {
        mUseRandomPlayerPlacement = b;
    }

    public synchronized void setNumRounds(short numRounds) {
        mNumRounds = (short)numRounds;
    }

    public synchronized void addPlayerFactory(PlayerFactory p) {
        mPlayers.add(p);
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    public synchronized void deletePlayerFactory(int pos) {
        mPlayers.remove(pos);
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

//    public synchronized void changePlayerFactory(int pos) {
//        mPlayers.remove(pos);
//        if (mAdapter)
//            mAdapter.notifyDataSetChanged();
//    }

    /*================= Lifecycle =================*/
    public ModelFactory(Bundle b) {
        // Some default game settings
        mDesiredTerrainType = Model.TerrainType.Hilly;
        mUseRandomPlayerPlacement = true;
        mNumRounds = (short)3;

        if (b == null)
            createDefaultPlayers();
        else
            restoreState(b);
    }

    /** Create some default players */
    private final void createDefaultPlayers() {
        mPlayers = new LinkedList < PlayerFactory >();
        mPlayers.add(new PlayerFactory(0, "Red",
                        PlayerFactory.PlayerType.HUMAN,
                        (short)100, Player.PlayerColor.RED));
        mPlayers.add(new PlayerFactory(1, "Yellow",
                        PlayerFactory.PlayerType.COMPUTER_EASY,
                        (short)100, Player.PlayerColor.YELLOW));
        mPlayers.add(new PlayerFactory(2, "Green",
                        PlayerFactory.PlayerType.COMPUTER_MEDIUM,
                        (short)100, Player.PlayerColor.GREEN));
        mPlayers.add(new PlayerFactory(3, "Cyan",
                        PlayerFactory.PlayerType.COMPUTER_HARD,
                        (short)90, Player.PlayerColor.CYAN));
        mPlayers.add(new PlayerFactory(4, "Blue",
                        PlayerFactory.PlayerType.COMPUTER_HARD,
                        (short)100, Player.PlayerColor.BLUE));
        mPlayers.add(new PlayerFactory(5, "Pink",
                        PlayerFactory.PlayerType.COMPUTER_HARD,
                        (short)75, Player.PlayerColor.PINK));
        mPlayers.add(new PlayerFactory(6, "Purple",
                PlayerFactory.PlayerType.COMPUTER_HARD,
                (short)75, Player.PlayerColor.PURPLE));
        mPlayers.add(new PlayerFactory(6, "Grey",
                PlayerFactory.PlayerType.COMPUTER_HARD,
                (short)75, Player.PlayerColor.GREY));
    }
}
