package scorched.android;

import java.util.HashMap;
import java.util.Random;

import android.os.Bundle;
import android.util.Log;

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

    /*================= Data =================*/
    public static class MyVars {
        /** The index of the current player */
        public int mCurPlayerId;
    }
    MyVars mV;

    /** The playing field */
    private Terrain mTerrain;

    /** The players */
    private Player mPlayers[];

    /** Maps slots to players */
    private HashMap<Integer, Player> mSlotToPlayer;

    /*================= Access =================*/
    public float[] getHeights() {
        return mTerrain.getHeights();
    }

    public Player getPlayer(int num) {
        return mPlayers[num];
    }

    public int getNumberOfPlayers() {
        return mPlayers.length;
    }

    public Player getCurPlayer() {
        return mPlayers[mV.mCurPlayerId];
    }

    public int getCurPlayerId() {
        return mV.mCurPlayerId;
    }

    /*================= Operations =================*/
    /** Sets mCurPlayerId to the next valid player id-- or to
     * INVALID_PLAYER_ID if there are none. */
    public void nextPlayer() {
        int oldPlayer = mV.mCurPlayerId;
        int player = mV.mCurPlayerId + 1;

        while (true) {
            if (player >= mPlayers.length) {
                if (oldPlayer == Player.INVALID_PLAYER_ID) {
                    // We searched the whole array and didn't find any valid
                    // players.
                    mV.mCurPlayerId = Player.INVALID_PLAYER_ID;
                    return;
                }
                else {
                    player = 0;
                }
            }
            if (player == oldPlayer) {
                // We're back at the player we started at.
                // I guess he's the only valid one.
                return;
            }
            if (mPlayers[player].isAlive()) {
                mV.mCurPlayerId = player;
                return;
            }
            else {
                player++;
            }
        }
    }

    /** Returns the player in this slot, or null if there is nobody there. */
    public Player slotToPlayer(int slot) {
        return mSlotToPlayer.get(new Integer(slot));
    }

    /** Gets the terrain slot for player number N */
    private int playerIdToSlot(int playerId) {
        return ((Terrain.MAX_X /2) +
                    (Terrain.MAX_X * playerId)) / mPlayers.length;
    }

    private float square(float x) {
        return x * x;
    }

    private int constrainSlot(int slot) {
        if (slot < 0)
            return 0;
        else if (slot >= Terrain.MAX_X)
            return Terrain.MAX_X - 1;
        else
            return slot;
    }

    /** Updates the height field with the result of an explosion at (x0,y0)
     *  with the given radius
     */
    public void doExplosion(float x0, float y0, float radius) {
        final float xLeft = x0 - (radius/2f);
        final float xRight = x0 + (radius/2f);
        final int firstSlot = constrainSlot((int)(xLeft + 0.5f));
        final int lastSlot = constrainSlot((int)(xRight + 0.5f));

        for (int slot = firstSlot; slot <= lastSlot; slot++) {
             // The equation for a circle is
             // (x - x0)^2 + (y - y0)^2 = r
             //
             // Solved for y:
             //              _____________
             //   y = y0 +  | r - (x-x0)^2
             //          - \|
             //
            final float tmp = radius - square(slot - x0);
            if (tmp <= 0)
                continue;
            final float tmp2 = (float)Math.sqrt(tmp);
            final float top = y0 + tmp2;
            final float bottom = y0 - tmp2;
            doSlotCollision(slot, top, bottom);
         }
    }

    /** Changes the height field at X='slot' to reflect an explosion
     *  that extends upward to 'top' and downward to 'bottom'.
     */
    private void doSlotCollision(int slot, float top, float bottom) {
        float[] h = mTerrain.getHeights();
        Log.w(this.getClass().getName(),
            "doSlotCollision(slot=" + slot +
           ",top=" + top + ",bottom=" + bottom);
        final float slotHeight = h[slot];
        if (bottom >= slotHeight) {
            // The explosion is too far up in the air to affect the ground
            return;
        }
        else if (top <= slotHeight) {
            // The explosion is completely underground
            h[slot] -= (top - bottom);
        }
        else {
            // Part of the explosion is underground, but part is in the air
            h[slot] -= (slotHeight - bottom);
        }
    }

    /*================= Save State =================*/
    public void saveState(Bundle map) {
        AutoPack.autoPack(map, AutoPack.EMPTY_STRING, mV);
        mTerrain.saveState(map);
        map.putShort(KEY_NUM_PLAYERS, (short)mPlayers.length);
    }

    /*================= Lifecycle =================*/
    public static Model fromBundle(Bundle map) {
        MyVars v = (MyVars) AutoPack.
            autoUnpack(map, AutoPack.EMPTY_STRING, MyVars.class);
        Terrain terrain = Terrain.fromBundle(map);
        int numPlayers = map.getInt(KEY_NUM_PLAYERS);
        // TODO: implement Player.fromBundle, etc.
        //Player players[] = new Player[numPlayers];
        //for (int i = 0; i < numPlayers; ++i)
            //players.add(Player.fromBundle(i, map));
        return new Model(v, terrain, null);
    }

    public Model(MyVars v, Terrain terrain, Player players[]) {
        mV = v;
        mTerrain = terrain;
        mPlayers = players;

        //////
        for (int i = 0; i < mPlayers.length; i++) {
            mPlayers[i].setId(i);
            mPlayers[i].setX(playerIdToSlot(i));
            mPlayers[i].calcY(this);
        }
        //////

        mSlotToPlayer = new HashMap<Integer, Player>();
        for (int i = 0; i < mPlayers.length; i++) {
            mSlotToPlayer.put(new Integer(mPlayers[i].getX()), mPlayers[i]);
        }
    }
}
