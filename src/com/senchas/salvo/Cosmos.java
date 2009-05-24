package com.senchas.salvo;

import com.senchas.salvo.PlayerColor;
import com.senchas.salvo.WeaponType.Armory;

import java.util.Arrays;
import java.util.EmptyStackException;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Cosmos for the Scorched Android game.
 *
 * The Cosmos owns all game state that is preserved across rounds. In other
 * words, state for the game as a whole.
 */
public class Cosmos {
    /*================= Constants =================*/
    public static final String KEY_NUM_PLAYERS = "KEY_NUM_PLAYERS";

    /*================= Types =================*/
    /** Player information which is preserved across rounds */
    public static class PlayerInfo {
        /*================= Data =================*/
        public static class MyVars {
            /** Total earnings for the whole game */
            public int mEarnings;
        }
        private MyVars mV;

        /** The weapons that this player owns */
        Armory mArmory;

        /*================= Access =================*/
        public int getEarnings() {
            return mV.mEarnings;
        }

        public Armory getArmory() {
            return mArmory;
        }

        /*================= Operations =================*/
        public void saveState(int index, Bundle map) {
            AutoPack.autoPack(map, Util.indexToString(index), mV);
            mArmory.saveState(index, map);
        }

        /*================= Lifecycle =================*/
        public static PlayerInfo fromInitial() {
            MyVars v = new MyVars();
            v.mEarnings = Util.mRandom.nextInt(2000); //TODO: change this to 0
            return new PlayerInfo(v, Armory.fromDefault());
        }

        public static PlayerInfo fromBundle(int index, Bundle map) {
            MyVars v = (MyVars)AutoPack.autoUnpack(map,
                            Util.indexToString(index), MyVars.class);
            Armory armory = Armory.fromBundle(index, map);
            return new PlayerInfo(v, armory);
        }

        public PlayerInfo(MyVars v, Armory armory) {
            mV = v;
            mArmory = armory;
        }
    }

    /** Provides a view of the players, sorted by earnings */
    public static class LeaderboardAdaptor extends BaseAdapter {
        /*================= Types =================*/
        public static class Entry implements Comparable <Entry> {
            /*================= Data =================*/
            private int mEarnings;
            private String mName;
            private PlayerColor mPlayerColor;

            /*================= Access =================*/
            public int getEarnings() {
                return mEarnings;
            }

            public String getName() {
                return mName;
            }

            public int getColor() {
                return mPlayerColor.toInt();
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
                    else if (getColor() < another.getColor())
                        return 1;
                    else if (getColor() > another.getColor())
                        return -1;
                    else
                        return 0;
                }
            }

            /*================= Lifecycle =================*/
            public void initialize(int earnings,
                                   String name,
                                   PlayerColor playerColor) {
                mEarnings = earnings;
                mName = name;
                mPlayerColor = playerColor;
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
            RelativeLayout lay = null;
            TextView left = null, right = null;

            // Figure out if we can reuse convertView for our purposes
            if (convertView != null) {
                if (convertView instanceof RelativeLayout) {
                    RelativeLayout ll = (RelativeLayout)convertView;
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
                lay = new RelativeLayout(c);
                left = new TextView(c);
                right = new TextView(c);
                lay.addView(left);
                RelativeLayout.LayoutParams left_params =
                    (RelativeLayout.LayoutParams) left.getLayoutParams();
                left_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

                lay.addView(right);
                RelativeLayout.LayoutParams right_params =
                    (RelativeLayout.LayoutParams) right.getLayoutParams();
                right_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }

            // Set up the layout
            //lay.setOrientation(LinearLayout.HORIZONTAL);
            lay.setHorizontalGravity(Gravity.LEFT);

            Entry entry = mEntries[position];

            // Set up left view
            left.setTextSize(TypedValue.COMPLEX_UNIT_MM, 3);
            left.setTextColor(entry.getColor());
            //left.setTypeface(BOLD);
            left.setText(entry.getName());

            right.setTextSize(TypedValue.COMPLEX_UNIT_MM, 3);
            right.setTextColor(entry.getColor());
            right.setText(Integer.toString(entry.getEarnings()) + "    ");
            return lay;
        }

        /*================= Lifecycle =================*/
        public void initialize(Cosmos cosmos, Model model) {
            PlayerInfo pi[] = cosmos.getPlayerInfo();
            Player pl[] = model.getPlayers();
            if (pi.length != pl.length) {
                throw new RuntimeException("must have " +
                    "cosmos.getPlayerInfo().length == " +
                    "model.getPlayers().length");
            }
            mLen = pi.length;

            for (int i = 0; i < mLen; i++) {
                mEntries[i].initialize(pi[i].getEarnings(),
                                       pl[i].getName(),
                                       pl[i].getBaseColor());
            }
            Arrays.sort(mEntries, 0, mLen);
        }

        public LeaderboardAdaptor() {
            mEntries = new Entry[Model.MAX_PLAYERS];
            for (int i = 0; i < mEntries.length; i++)
                mEntries[i] = new Entry();
            mLen = 0;
        }
    }

    /*================= Data =================*/
    public static class MyVars {
        /** The current round, counting from 1 */
        public short mCurRound;

        /** The total number of rounds we expect to play */
        public short mNumRounds;
    }
    private MyVars mV;

    /** The player information */
    private final PlayerInfo mPlayerInfo[];

    private final LeaderboardAdaptor mLeaderboardAdaptor;

    /*================= Access =================*/
    public PlayerInfo[] getPlayerInfo() {
        return mPlayerInfo;
    }

    public Armory getArmory(int idx) {
        return mPlayerInfo[idx].getArmory();
    }

    public LeaderboardAdaptor getLeaderboardAdaptor(Model model) {
        mLeaderboardAdaptor.initialize(this, model);
        return mLeaderboardAdaptor;
    }

    public boolean moreRoundsRemaining() {
        return (mV.mCurRound < mV.mNumRounds);
    }

    /*================= Operations =================*/
    public void nextRound() {
        mV.mCurRound++;
    }

    public void saveState(Bundle map) {
        AutoPack.autoPack(map, AutoPack.EMPTY_STRING, mV);
        map.putShort(KEY_NUM_PLAYERS, (short)mPlayerInfo.length);
        for (int i = 0; i < mPlayerInfo.length; ++i)
            mPlayerInfo[i].saveState(i, map);
    }

    /*================= Lifecycle =================*/
    public static Cosmos fromInitial(short numRounds, int numPlayers) {
        MyVars v = new MyVars();
        v.mCurRound = 0;
        v.mNumRounds = numRounds;
        PlayerInfo pi[] = new PlayerInfo[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            pi[i] = PlayerInfo.fromInitial();
        }
        return new Cosmos(v, pi);
    }

    public static Cosmos fromBundle(Bundle map) {
        MyVars v = (MyVars)AutoPack.autoUnpack(map,
                AutoPack.EMPTY_STRING, MyVars.class);
        int numPlayers = map.getShort(KEY_NUM_PLAYERS);
        PlayerInfo pi[] = new PlayerInfo[numPlayers];
        for (int i = 0; i < numPlayers; ++i)
            pi[i] = PlayerInfo.fromBundle(i, map);
        return new Cosmos(v, pi);
    }

    private Cosmos(MyVars v, PlayerInfo pi[]) {
        mV = v;
        mPlayerInfo = pi;
        mLeaderboardAdaptor = new LeaderboardAdaptor();
    }
}
