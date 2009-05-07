package com.senchas.salvo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

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
 * Model for the Scorched Android game.
 *
 * The Model owns all game state-- except for state relating
 * to the user interface.
 */
public class Model {
    /*================= Constants =================*/
    /** The minimum number of players we can have */
    public static final int MIN_PLAYERS = 2;

    /** The maximum number of players we can have */
    public static final int MAX_PLAYERS = 9;

    /** Player size */
    public static final int PLAYER_SIZE = 1;

    /** How long the player's turret is */
    public static final int TURRET_LENGTH = 1;

    public static final String KEY_NUM_PLAYERS = "KEY_NUM_PLAYERS";

    /*================= Types =================*/
    /** Represents information about who should move next in a given round.
     *
     * I would love to make this class immutable. However, due to VM
     * crapitude, we have to minimize memory allocations at all costs.
     * So this is mutable...
     */
    public static class NextTurnInfo {
        /*================= Data =================*/
        private int mNextPlayerId;
        private boolean mCurPlayerHasWon;

        /*================= Access =================*/
        /** Returns true if the game is a draw (nobody can win) */
        public boolean isDraw() {
            return (mNextPlayerId == Player.INVALID_PLAYER_ID);
        }

        /** Returns true if the current player has won */
        public boolean curPlayerHasWon() {
            if (isDraw()) {
                throw new RuntimeException("NextTurnInfo: round is a draw; " +
                                           "nobody has won");
            }
            return mCurPlayerHasWon;
        }

        /** Returns the player who should move next. */
        public int getNextPlayerId() {
            if (isDraw()) {
                throw new RuntimeException("NextTurnInfo: round is a draw; " +
                                           "there is no next player");
            }
            return mNextPlayerId;
        }

        /*================= Lifecycle =================*/
        public void initialize(int nextPlayerId, boolean curPlayerHasWon) {
            mNextPlayerId = nextPlayerId;
            mCurPlayerHasWon = curPlayerHasWon;
        }

        public NextTurnInfo() {
            mNextPlayerId = Player.INVALID_PLAYER_ID;
        }
    }

    /** Provides a view of the players, sorted by earnings */
    public static class LeaderboardAdaptor extends BaseAdapter {
        /*================= Types =================*/
        public static class Entry implements Comparable <Entry> {
            /*================= Data =================*/
            private Player mPlayer;

            /*================= Access =================*/
            public String getName() {
                return mPlayer.getName();
            }

            public int getEarnings() {
                return mPlayer.getEarnings();
            }

            public int getColor() {
                return mPlayer.getBaseColor().toInt();
            }

            public int compareTo(Entry another) {
                if (getEarnings() < another.getEarnings())
                    return 1;
                else if (getEarnings() > another.getEarnings())
                    return -1;
                else {
                    int c = getName().compareTo(another.getName());
                    if (c < 0)
                    	return 1;
                    else if (c > 0)
                    	return -1;
                    else
                    	return 0;
                }
            }

            /*================= Lifecycle =================*/
            public void initialize(Player player) {
                mPlayer = player;
            }

            public Entry() {
            }
        }

        /*================= Data =================*/
        private Entry mEntries[];

        /** Number of players */
        private int mLen;

        /*================= Access =================*/
        public boolean areAllItemsEnabled() { return true; }

        public boolean isEnabled(int position) { return true; }

        public boolean areAllItemsSelectable() { return false; }

        public long getItemId(int position) { return position; }

        public int getCount() { return mLen; }

        public Object getItem(int position) {
            return mEntries[position];
        }

        public View getView(int position, View convertView,
                            ViewGroup parent) {
            Context c = parent.getContext();
            LinearLayout lay = null;
            TextView left = null, right = null;

            // Figure out if we can reuse convertView for our purposes
            if (convertView != null) {
                if (convertView instanceof LinearLayout) {
                    LinearLayout ll = (LinearLayout)convertView;
                    if (ll.getChildCount() == 2) {
                        View u = ll.getChildAt(0);
                        View l = ll.getChildAt(1);
                        if ((u instanceof TextView) &&
                            (l instanceof TextView)) {
                            left = (TextView)u;
                            right = (TextView)l;
                            lay = ll;
                        }
                    }
                }
            }
            if (lay == null) {
                lay = new LinearLayout(c);
                left = new TextView(c);
                right = new TextView(c);
                lay.addView(left);
                lay.addView(right);
            }

            // Set up the layout
            lay.setOrientation(LinearLayout.HORIZONTAL);
            lay.setHorizontalGravity(Gravity.LEFT);

            Entry entry = mEntries[position];

            // Set up left view
            left.setTextSize(TypedValue.COMPLEX_UNIT_MM, 3);
            left.setTextColor(entry.getColor());
            //left.setTypeface(BOLD);
            left.setText(entry.getName());

            right.setTextSize(TypedValue.COMPLEX_UNIT_MM, 3);
            right.setTextColor(entry.getColor());
            right.setText(Integer.toString(entry.getEarnings()));
            return lay;
        }

        /*================= Lifecycle =================*/
        public void initialize(Model model) {
            Player plays[] = model.getPlayers();
            for (int i = 0; i < plays.length; i++) {
                mEntries[i].initialize(plays[i]);
            }
            mLen = plays.length;
            Arrays.sort(mEntries, 0, mLen);
        }

        public LeaderboardAdaptor() {
            mEntries = new Entry[MAX_PLAYERS];
            for (int i = 0; i < mEntries.length; i++)
            	mEntries[i] = new Entry();
            mLen = 0;
        }
    }

    /*================= Data =================*/
    public static class MyVars {
        /** The index of the current player */
        public int mCurPlayerId;

        /** The background image */
        public Background mBackground;

        /** The foreground paint */
        public Foreground mForeground;

        /** The wind we have this round */
        public int mWind;
    }
    private final MyVars mV;

    /** The playing field */
    private final Terrain mTerrain;

    /** The players */
    private final Player mPlayers[];

    private final LeaderboardAdaptor mLeaderboardAdaptor;

    /*================= Access =================*/
    public Background getBackground() {
        return mV.mBackground;
    }

    public Foreground getForeground() {
        return mV.mForeground;
    }

    public Terrain getTerrain() {
        return mTerrain;
    }

    public Player[] getPlayers() {
        return mPlayers;
    }

    public Player getCurPlayer() {
        return mPlayers[mV.mCurPlayerId];
    }

    public int getCurPlayerId() {
        return mV.mCurPlayerId;
    }

    public int getWind() {
        return mV.mWind;
    }

    /** Gets information about who should move next. */
    public void getNextPlayerInfo(NextTurnInfo info) {
        int nextPlayerId = Player.INVALID_PLAYER_ID;

        for (int i = 1; i <= mPlayers.length; i++) {
            int index = (mV.mCurPlayerId + i) % mPlayers.length;
            if (mPlayers[index].isAlive()) {
                if (nextPlayerId == Player.INVALID_PLAYER_ID) {
                    nextPlayerId = index;
                }
                else {
                    info.initialize(nextPlayerId, false);
                    return;
                }
            }
        }
        if (nextPlayerId == Player.INVALID_PLAYER_ID) {
            // Nobody is still alive
            info.initialize(Player.INVALID_PLAYER_ID, false);
            return;
        }
        else {
            // Only one player is still standing. He's the winner.
            info.initialize(nextPlayerId, true);
            return;
        }
    }

    public boolean foregroundIsLight() {
        return mV.mForeground.isLight();
    }

    public LeaderboardAdaptor getLeaderboardAdaptor() {
        mLeaderboardAdaptor.initialize(this);
        return mLeaderboardAdaptor;
    }

    /*================= Operations =================*/
    public void setCurPlayerId(int id) {
        if (id == Player.INVALID_PLAYER_ID) {
            throw new IllegalArgumentException("setNextPlayerId: " +
                "can't use INVALID_PLAYER_ID");
        }
        mV.mCurPlayerId = id;
    }

    /*================= Save State =================*/
    public void saveState(Bundle map) {
        AutoPack.autoPack(map, AutoPack.EMPTY_STRING, mV);
        mTerrain.saveState(map);
        map.putShort(KEY_NUM_PLAYERS, (short)mPlayers.length);
        for (int i = 0; i < mPlayers.length; ++i)
            mPlayers[i].saveState(i, map);
    }

    /*================= Lifecycle =================*/
    public static Model fromBundle(Bundle map) {
        MyVars v = (MyVars) AutoPack.
            autoUnpack(map, AutoPack.EMPTY_STRING, MyVars.class);
        Terrain terrain = Terrain.fromBundle(map);
        int numPlayers = map.getShort(KEY_NUM_PLAYERS);
        Player players[] = new Player[numPlayers];
        for (int i = 0; i < numPlayers; ++i)
            players[i] = Player.fromBundle(i, map);
        return new Model(v, terrain, players);
    }

    public Model(MyVars v, Terrain terrain, Player players[]) {
        mV = v;
        mTerrain = terrain;
        mPlayers = players;
        mLeaderboardAdaptor = new LeaderboardAdaptor();
    }
}
