package com.senchas.salvo;

import com.senchas.salvo.RunGameAct.RunGameActAccessor;
import android.util.Log;

/* Represents an ongoing explosion
 *
 * This class is mutable and designed to be reused. This is to cut down on
 * the number of memory allocations, as usual.
 */
public class Explosion {
    /*================= Constants =================*/
    public static final int MAX_TIME = 1000;

    /*================= Data =================*/
    /** X coordinate of the center of the explosion */
    private float mX;

    /** Y coordinate of the center of the explosion */
    private float mY;

    /** The weapon that is causing this explosion */
    private WeaponType mWeapon;

    /** True if this object is in use-- otherwise, it should be ignored */
    private boolean mInUse;

    /** True if the explosion has reached its maximum size */
    private boolean mFinished;

    /** The time we started displaying the explosion */
    private long mStartTime;

    /*================= Access =================*/
    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public boolean getInUse() {
        return (mStartTime != 0);
    }

    public boolean getFinished(long time) {
        long diff = time - mStartTime;
        return (diff > MAX_TIME);
    }

    public int getCurExplosionSize(long time) {
        int full = mWeapon.getExplosionRadius();
        long diff = time - mStartTime;
        if (diff > MAX_TIME)
            return full;
        else {
            return (int)((full * diff) / MAX_TIME);
        }
    }

    public WeaponType getWeaponType() {
        return mWeapon;
    }

    /*================= Operations =================*/
    public void clearInUse() {
        mStartTime = 0;
    }

    /** Deal direct damage to players */
    public void doDirectDamage(RunGameActAccessor game) {
        Player players[] = game.getModel().getPlayers();
        for (Player p : players) {
            if (! p.isAlive())
                continue;
            float dist = Util.calcDistance(mX, mY, p.getX(), p.getY());
            float safeDist = Player.COLLISION_RADIUS +
                                mWeapon.getExplosionRadius();
            if (dist < safeDist) {
                int reduction;
                int full = mWeapon.getFullDamage();
                if (dist < safeDist / 2)
                    reduction = 0;
                else {
                    float d = safeDist / 2;
                    reduction = (int)((dist * d) / d);
                }
                int damage = full - reduction;
                Log.w("doDirectDamage", "full=" + full +
                                        " ,reduction=" + reduction +
                                        " ,damage=" + (full - reduction) +
                                        " ,dist=" + dist +
                                        " ,safeDist=" + safeDist);
                p.takeDamage(damage);
            }
        }
    }

    /** Change the terrain to reflect this explosion */
    public void editTerrain(RunGameActAccessor game) {
        Terrain terrain = game.getModel().getTerrain();
        short board[] = terrain.getBoard();

        Util.Pair pair = new Util.Pair();
        int eSize = mWeapon.getExplosionRadius();

        // do the circle collision algorithm on each height
        int x = (int)mX;
        int y = (int)mY;
        for (int slice = Math.max(0, x - eSize);
                 slice < Math.min(x + eSize, Terrain.MAX_X);
                 slice++) {
            Util.circAt(x, y, eSize, slice, pair);
            editTerrainSlice(board, slice, pair);
        }
    }

    /** Helper function for editTerrain that does the work at a single
     * terrain slice. */
    private void editTerrainSlice(short board[], int slice,
                                  Util.Pair pair) {
        if (pair.yLower == 0) {
            // The explosion isn't relevant at this terrain slice
            return;
        }
        if (pair.yLower < board[slice]) {
            // The explosion is too far up in the air to have hit the ground
            // at this terrain slice.
            return;
        }
        if (pair.yUpper > board[slice]) {
            // The explosion is completely underground
            board[slice] += (pair.yLower - pair.yUpper);
            return;
        }
        board[slice] += (pair.yLower - board[slice]);
        if (board[slice] > Terrain.MAX_Y)
            board[slice] = Terrain.MAX_Y;
        return;
    }

    /*================= Lifecycle =================*/
    public void initialize(float x, float y, WeaponType weapon) {
        mX = x;
        mY = y;
        mWeapon = weapon;

        mStartTime = System.currentTimeMillis();
        mFinished = false;
    }

    public Explosion() {
        mStartTime = 0;
    }
}
