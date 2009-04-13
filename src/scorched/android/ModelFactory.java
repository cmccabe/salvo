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

    /** Represents the amount of cash players start with */
    public static enum StartingCash {
        C0(0),
        C1000(1000),
        C2000(2000),
        C3000(3000),
        C4000(4000),
        C5000(5000),
        C10000(10000);

        /*================= Static =================*/
        public static StartingCash fromShort(short cash) {
            for (StartingCash c : StartingCash.values()) {
                if (c.toShort() == cash)
                    return c;
            }
            throw new RuntimeException
                ("StartingCash.fromShort: no such enum " +
                "value as " + cash);
        }

        /*================= Data =================*/
        private short mCash;

        /*================= Access =================*/
        public String toString() {
            if (this == C0)
                return "starting cash: none";
            else {
                StringBuilder b = new StringBuilder(80);
                b.append("starting cash: $");
                b.append(mCash);
                return b.toString();
            }
        }

        public short toShort() {
            return mCash;
        }

        /*================= Lifecycle =================*/
        StartingCash(int amount) {
            mCash = (short)amount;
        }
    };

    public static class PlayerFactory
    {
        /*================= Static =================*/
        public static PlayerFactory fromBundle(int index, Bundle map) {
            MyVars mV =
                (MyVars) AutoPack.autoUnpack(map, Util.indexToString(index),
                                              MyVars.class);
            return new PlayerFactory(mV);
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
        public static class MyVars {
            public String mName;
            public short mLife;
            public BrainFactory mBrain;
            public Player.PlayerColor mColor;
        }
        private MyVars mV;

        /*================= Access =================*/
        public synchronized void saveState(int index, Bundle map) {
            if (map == null)
                return;
            AutoPack.autoPack(map, Util.indexToString(index), mV);
        }

        public synchronized String getName() {
            return mV.mName;
        }

        public synchronized BrainFactory getBrain() {
            return mV.mBrain;
        }

        public synchronized short getLife() {
            return mV.mLife;
        }

        public synchronized Player.PlayerColor getColor() {
            return mV.mColor;
        }

        /*================= Operations =================*/
        public synchronized void setName(String name) {
            mV.mName = name;
        }

        public synchronized void setBrain(BrainFactory fac) {
            mV.mBrain = fac;
        }

        public synchronized void setLife(short life) {
            mV.mLife = life;
        }

        public synchronized void setColor(Player.PlayerColor color) {
            mV.mColor = color;
        }

        /*================= Lifecycle =================*/
        public static String getRandomUnusedName
                (LinkedList < PlayerFactory > plays) {
            String ret;
            while (true) {
                int idx = Math.abs(Util.mRandom.nextInt()) %
                            RandomStartingNames.STARTING_NAMES.length;
                ret = RandomStartingNames.STARTING_NAMES[idx];
                int i;
                for (i = 0; i < plays.size(); i++) {
                    if (ret.equals(plays.get(i).getName()))
                        break;
                }
                if (i == plays.size())
                    return ret;
            }
        }

        public static PlayerColor getRandomUnusedColor
                (LinkedList < PlayerFactory > plays) {
            LinkedList < PlayerColor > unused = getAvailableColors(plays);
            if (unused.size() == 0) {
                throw new RuntimeException("getRandomUnusedColor(): " +
                    "there appear to be no unused colors left!");
            }
            int idx = Math.abs(Util.mRandom.nextInt()) % unused.size();
            return unused.get(idx);
        }

        public static PlayerFactory
            createDefault(LinkedList < PlayerFactory > plays)
        {
            MyVars v = new MyVars();
            v.mName = getRandomUnusedName(plays);
            v.mLife = (short)Player.DEFAULT_STARTING_LIFE;
            v.mBrain = BrainFactory.MEDIUM;
            v.mColor = getRandomUnusedColor(plays);
            return new PlayerFactory(v);
        }

        private PlayerFactory(MyVars v) {
            mV = v;
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
            b.append(p.getBrain().toString());
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
    private final static String KEY_NUM_PLAYERS = "KEY_NUM_PLAYERS";

    /*================= Data =================*/
    public static class MyVars {
        public Model.TerrainType mDesiredTerrainType;
        public boolean mUseRandomPlayerPlacement;
        public short mNumRounds;
        public short mStartingCash;
    }
    private MyVars mV;
    private LinkedList < PlayerFactory > mPlayers;

    /** A helper object that helps this class talk to a Listview */
    private PlayerListAdapter mAdapter;

    /*================= Access =================*/
    public synchronized Model.TerrainType getDesiredTerrainType() {
        return mV.mDesiredTerrainType;
    }

    public synchronized boolean getRandomPlayerPlacement() {
        return mV.mUseRandomPlayerPlacement;
    }

    public synchronized short getNumRounds() {
        return mV.mNumRounds;
    }

    public synchronized short getStartingCash() {
        return mV.mStartingCash;
    }

    public synchronized PlayerFactory getPlayerFactory(int index) {
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

    /** Returns true if we can delete a player */
    public synchronized boolean canDeletePlayer() {
        return (!(mPlayers.size() - 1 < Model.MIN_PLAYERS));
    }

    /** Returns true if all players are computers */
    public synchronized boolean everyoneIsAComputer() {
        for (PlayerFactory p: mPlayers) {
            if (p.getBrain() == BrainFactory.HUMAN)
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

    /** Gets the position of a playerFactory in the list */
    public synchronized int getPlayerPosition(PlayerFactory p) {
        for (int i = 0; i < mPlayers.size(); i++) {
            if (mPlayers.get(i) == p)
                return i;
        }
        throw new RuntimeException("can't find player " + p +
                " in mPlayers");
    }

    /*================= Save / Restore =================*/
    public synchronized void saveState(Bundle map) {
        if (map == null)
            return;
        AutoPack.autoPack(map, "", mV);
        map.putInt(KEY_NUM_PLAYERS, mPlayers.size());
        for (int i = 0; i < mPlayers.size(); i++)
            mPlayers.get(i).saveState(i, map);
    }

    public final synchronized void restoreState(Bundle map) {
        if (map == null)
            return;
        mV = (MyVars)AutoPack.autoUnpack(map, "", MyVars.class);
        int numPlayers = map.getInt(KEY_NUM_PLAYERS);
        mPlayers = new LinkedList < PlayerFactory >();
        for (int i = 0; i < numPlayers; ++i)
            mPlayers.add(PlayerFactory.fromBundle(i, map));
    }

    /*================= Operations =================*/
    public synchronized void setTerrainType(TerrainType ty) {
        mV.mDesiredTerrainType = ty;
    }

    public synchronized void modifyRandomPlayerPlacement(boolean b) {
        mV.mUseRandomPlayerPlacement = b;
    }

    public synchronized void setNumRounds(short numRounds) {
        mV.mNumRounds = (short)numRounds;
    }

    public synchronized void setStartingCash(short startingCash) {
        mV.mStartingCash = startingCash;
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
        if (!canDeletePlayer())
            throw new TooFewPlayers("");

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
        restoreState(b);
    }

    public ModelFactory() {
        mV = new MyVars();
        mV.mDesiredTerrainType = Model.TerrainType.Hilly;
        mV.mUseRandomPlayerPlacement = true;
        mV.mNumRounds = (short)3;
        mV.mStartingCash = (short)0;
        createDefaultPlayers();
    }

    /** Create some default players */
    private final void createDefaultPlayers() {
        mPlayers = new LinkedList < PlayerFactory >();
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
        mPlayers.get(0).setBrain(BrainFactory.HUMAN);
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
        mPlayers.add(PlayerFactory.createDefault(mPlayers));
    }
}
