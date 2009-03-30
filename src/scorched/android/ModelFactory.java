package scorched.android;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import scorched.android.Model.TerrainType;
import scorched.android.Player.PlayerColor;

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
    /** Thrown if the current operation cannot be performed because there are
     *  too few players */
    public class TooFewPlayers extends RuntimeException {
        public final static long serialVersionUID = 1;
        public TooFewPlayers(String message) {
            super(message);
        }
    }

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

        /*================= Data =================*/
        private short mShort;

        /*================= Access =================*/
        public String toString() {
            StringBuilder b = new StringBuilder(10);
            b.append(mShort).append(" round");
            if (this != ONE)
                b.append("s");
            return b.toString();
        }

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

        public static enum PlayerType {
            HUMAN("Human player"),
            COMPUTER_EASY("Computer: Easy"),
            COMPUTER_MEDIUM("Computer: Medium"),
            COMPUTER_HARD("Computer: Hard");

            /*================= Data =================*/
            private final String mName;

            /*================= Access =================*/
            public String toString() { return mName; }

            /*================= Lifecycle =================*/
            PlayerType(String name) { mName = name; }
        }

        /*================= Static =================*/
        public static PlayerFactory fromBundle(int index, Bundle map) {
            String tag = "PLAYER_" + index + "_";
            String name = map.getString(tag + "NAME");
            int typeInt = map.getInt(tag + "PLAYER_TYPE");
            PlayerType type = PlayerType.values()[typeInt];
            short startingLife = map.getShort(tag + "STARTING_LIFE");
            int color_ord = map.getInt(tag + "COLOR");
            Player.PlayerColor color = Player.PlayerColor.values()[color_ord];
            return new PlayerFactory(name, type,
                                     startingLife, color);
        }

        /** Gets a list of colors that aren't currently in use by a player */
        public static LinkedList < Player.PlayerColor >
                getAvailableColors(LinkedList < PlayerFactory > plays) {
            HashSet < PlayerColor > used = new HashSet<PlayerColor>();
            LinkedList < PlayerColor > unused = new LinkedList<PlayerColor>();
            for (PlayerFactory p : plays)
                used.add(p.getColor());
            PlayerColor[] all = PlayerColor.values();
            for (PlayerColor c : all) {
                if (!used.contains(c))
                    unused.add(c);
            }
            return unused;
        }

        /*================= Data =================*/
        private int mIndex;
        private String mName;
        private short mLife;
        private PlayerType mType;
        private Player.PlayerColor mColor;

        /*================= Access =================*/
        public synchronized void saveState(int index, Bundle map) {
            String tag = "PLAYER_" + index + "_";
            if (map != null) {
                map.putString(tag + "NAME", mName);
                map.putInt(tag + "PLAYER_TYPE", mType.ordinal());
                map.putShort(tag + "STARTING_LIFE",
                            mLife);
                map.putInt(tag + "COLOR", mColor.ordinal());
            }
        }

        public synchronized String getName() {
            return mName;
        }

        public synchronized PlayerType getType() {
            return mType;
        }

        public synchronized short getLife() {
            return mLife;
        }

        public synchronized Player.PlayerColor getColor() {
            return mColor;
        }

        /*================= Operations =================*/
        public synchronized void setName(String name) {
            mName = name;
        }

        public synchronized void setType(PlayerType t) {
            mType = t;
        }

        public synchronized void setLife(short life) {
            mLife = life;
        }

        public synchronized void setColor(Player.PlayerColor color) {
            mColor = color;
        }

        /*================= Lifecycle =================*/
        public static String getRandomUnusedName
                (LinkedList < PlayerFactory > plays) {
            String ret;
            while (true) {
                int idx = Math.abs(Model.mRandom.nextInt()) %
                            ModelFactory.STARTING_NAMES.length;
                ret = ModelFactory.STARTING_NAMES[idx];
                for (PlayerFactory p : plays) {
                    if (ret.equals(p.getName()))
                        continue;
                }
                break;
            }
            return ret;
        }

        public static PlayerColor getRandomUnusedColor
                (LinkedList < PlayerFactory > plays) {
            LinkedList < PlayerColor > unused = getAvailableColors(plays);
            if (unused.size() == 0) {
                throw new RuntimeException("getRandomUnusedColor(): " +
                    "there appear to be no unused colors left!");
            }
            int idx = Math.abs(Model.mRandom.nextInt()) % unused.size();
            return unused.get(idx);
        }

        public static PlayerFactory
            createDefault(LinkedList < PlayerFactory > plays)
        {
            return new PlayerFactory(getRandomUnusedName(plays),
                    PlayerType.COMPUTER_MEDIUM,
                    (short)Player.DEFAULT_STARTING_LIFE,
                    getRandomUnusedColor(plays));
        }

        public PlayerFactory(String name,
                             PlayerType type,
                             short life,
                             Player.PlayerColor color) {
            mName = name;
            mType = type;
            mLife = life;
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
            b.append(p.getType().toString());
            b.append(": ");
            b.append(p.getLife());
            b.append("%");

            upper.setTextSize(TypedValue.COMPLEX_UNIT_MM, 4);
            lower.setText(b.toString());
            return lay;
        }

        /*================= Lifecycle =================*/
        public PlayerListAdapter() { }
    }

    /*================= Constants =================*/
    protected final static String STARTING_NAMES[] = {
        "Notorious AIG",
        "Richter",
        "Belmont",
        "Locutus",
        "Silent Bob",
        "Terminator",
        "Kerrigan",
        "Batman",
        "Fat Tony",
        "Ballmer",
        "Sisyphus",
        "Zeus",
        "Hermes",
        "Mars",
        "Clinton",
        "G.W.",
        "Cheney",
        "K.G.",
        "Una Persona",
        "Cornelius",
        "Sirius",
        "Col. Sanders",
        "Barfy",
        "Stewie",
        "Robotnik",
        "Smoochy",
        "Kafka",
        "Jeff",
        "Ultros",
        "Dio"
    };

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

    public PlayerFactory getPlayerFactory(int index) {
        return mPlayers.get(index);
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
        return (mPlayers.size() + 1 <= Model.MAX_PLAYERS);
    }

    /** Returns true if all players are computers */
    public synchronized boolean everyoneIsAComputer() {
        for (PlayerFactory p: mPlayers) {
            if (p.getType() == PlayerFactory.PlayerType.HUMAN)
                return false;
        }
        return true;
    }

    /** Returns true if a player is already using the given color */
    public synchronized boolean colorInUse(Player.PlayerColor color) {
        for (PlayerFactory p: mPlayers) {
            if (p.getColor() == color)
                return true;
        }
        return false;
    }

    /** Gets a list of colors that aren't currently in use by a player */
    public synchronized LinkedList < Player.PlayerColor >
            getAvailableColors() {
        return PlayerFactory.getAvailableColors(mPlayers);
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
            for (int i = 0; i < mPlayers.size(); i++) {
                mPlayers.get(i).saveState(i, map);
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

    public synchronized PlayerFactory addPlayerFactory() {
        PlayerFactory p = PlayerFactory.createDefault(mPlayers);
        mPlayers.add(p);
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
        return p;
    }

    /** Notify the ModelFactory that its data set has changed.
     *
     *  You must call this method after mutating a PlayerFactory in order
     *  to see these changes reflected in the playerListAdapter
     *
     *  Perhaps this should be refactored so that PlayerFactory is a
     *  non-static inner class which calls this method itself?
     */
    public synchronized void notifyDataSetChanged() {
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    /** Deletes the PlayerFactory p
     *
     * @return          The PlayerFactory that is now located where p used to
     *                  be.
     *
     * @throws          RuntimeError if the PlayerFactory p is not found in
     *                  our list of PlayerFactory objects
     *
     *                  TooFewPlayers if we have too few players to remove
     *                  one.
     */
    public synchronized PlayerFactory deletePlayerFactory(PlayerFactory p) {
        if (mPlayers.size() - 1 < Model.MIN_PLAYERS) {
            throw new TooFewPlayers("");
        }

        int i;
        for (i = 0; i < mPlayers.size(); i++) {
            if (mPlayers.get(i) == p) {
                break;
            }
        }
        if (i == mPlayers.size()) {
            StringBuilder b = new StringBuilder(80);
            b.append("deletePlayerFactory: player named ");
            b.append(p.getName());
            b.append(" not found in mPlayers.");
            throw new RuntimeException(b.toString());
        }

        mPlayers.remove(i);
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
        if (i == 0)
            return mPlayers.get(0);
        else
            return mPlayers.get(i - 1);
    }

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
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
        mPlayers.get(0).setType(PlayerFactory.PlayerType.HUMAN);
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
    }
}
