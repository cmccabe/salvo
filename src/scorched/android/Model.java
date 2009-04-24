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
                throw new RuntimeException("NextTurnInfo: round is a draw; "
                                           "nobody has won");
            }
            return mCurPlayerHasWon;
        }

        /** Returns the player who should move next. */
        public int getNextPlayerId() {
            if (isDraw()) {
                throw new RuntimeException("NextTurnInfo: round is a draw; "
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
            mCurPlayerId = Player.INVALID_PLAYER_ID;
            mNextPlayerId = Player.INVALID_PLAYER_ID;
        }
    }

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

    /*================= Access =================*/
    // Might be nice to use an iterator for this stuff... maybe?
    public short[] getHeights() {
        return mTerrain.getHeights();
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

    /*================= Operations =================*/
    public void setCurPlayerId(int id) {
        if (id == Player.INVALID_PLAYER_ID) {
            throw new IllegalArgumentException("setNextPlayerId: " +
                "can't use INVALID_PLAYER_ID");
        }
        mCurPlayerId = id;
    }

//    private float square(float x) {
//        return x * x;
//    }
//
//    private int constrainSlot(int slot) {
//        if (slot < 0)
//            return 0;
//        else if (slot >= Terrain.MAX_X)
//            return Terrain.MAX_X - 1;
//        else
//            return slot;
//    }
//
//    /** Updates the height field with the result of an explosion at (x0,y0)
//     *  with the given radius
//     */
//    public void doExplosion(float x0, float y0, float radius) {
//        final float xLeft = x0 - (radius/2f);
//        final float xRight = x0 + (radius/2f);
//        final int firstSlot = constrainSlot((int)(xLeft + 0.5f));
//        final int lastSlot = constrainSlot((int)(xRight + 0.5f));
//
//        for (int slot = firstSlot; slot <= lastSlot; slot++) {
//             // The equation for a circle is
//             // (x - x0)^2 + (y - y0)^2 = r
//             //
//             // Solved for y:
//             //              _____________
//             //   y = y0 +  | r - (x-x0)^2
//             //          - \|
//             //
//            final float tmp = radius - square(slot - x0);
//            if (tmp <= 0)
//                continue;
//            final float tmp2 = (float)Math.sqrt(tmp);
//            final float top = y0 + tmp2;
//            final float bottom = y0 - tmp2;
//            doSlotCollision(slot, top, bottom);
//         }
//    }
//
//    /** Changes the height field at X='slot' to reflect an explosion
//     *  that extends upward to 'top' and downward to 'bottom'.
//     */
//    private void doSlotCollision(int slot, float top, float bottom) {
//        short[] h = mTerrain.getHeights();
//        Log.w(this.getClass().getName(),
//            "doSlotCollision(slot=" + slot +
//           ",top=" + top + ",bottom=" + bottom);
//        final float slotHeight = h[slot];
//        if (bottom >= slotHeight) {
//            // The explosion is too far up in the air to affect the ground
//            return;
//        }
//        else if (top <= slotHeight) {
//            // The explosion is completely underground
//            h[slot] -= (top - bottom);
//        }
//        else {
//            // Part of the explosion is underground, but part is in the air
//            h[slot] -= (slotHeight - bottom);
//        }
//    }

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
        int numPlayers = map.getInt(KEY_NUM_PLAYERS);
        Player players[] = new Player[numPlayers];
        for (int i = 0; i < numPlayers; ++i)
            players[i] = Player.fromBundle(i, map);
        return new Model(v, terrain, players);
    }

    public Model(MyVars v, Terrain terrain, Player players[]) {
        mV = v;
        mTerrain = terrain;
        mPlayers = players;

        // TODO: create fast-access cache for x -> player
    }
}
