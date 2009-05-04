package com.senchas.salvo;

import android.graphics.Color;
import android.util.Log;

/* Represents a projectile flying across the screen.
 *
 * This class is mutable and designed to be reused. This is to cut down on
 * the number of memory allocations, as usual.
 */
public class Projectile {
    /*================= Constants =================*/
    /** The maximum number of steps we will take before simply exploding the
     * projectile in the air. This is to avoid the game getting "stuck"
     */
    private static final int MAX_STEPS = 50000;

    /** Radius of the projectile */
    public static final int PROJECTILE_RADIUS = 5;
    public static final int PROJECTILE_COLLISION_RADIUS = 4;

    public static final int PROJECTILE_COLOR = Color.argb(0xff, 0xff, 0, 0);

    /*================= Data =================*/
    private float mX, mY;
    private float mDeltaX, mDeltaY;
    private float mWind;
    private boolean mInUse;

    private boolean mExploded;
    private int mCurStep;

    /*================= Accessors =================*/
    public float getCurX() {
        return mX;
    }

    public float getCurY() {
        return mY;
    }

    public boolean getInUse() {
        return mInUse;
    }

    public boolean isOffscreen() {
        return (mY + PROJECTILE_RADIUS < 0);
    }

    /*================= Operations =================*/
    public void changeInUse(boolean inUse) {
        mInUse = inUse;
    }

    public void step() {
        mCurStep++;
        if (mCurStep > MAX_STEPS)
            mExploded = true;
        mX += mDeltaX;
        mY += mDeltaY;
        mDeltaY += Terrain.GRAVITY;
        mDeltaX += mWind;
    }

    private boolean checkCollisions(Model model) {
        if (mX < 0)
            return true;
        else if (mX > Terrain.MAX_X)
            return true;
        else if (mY > Terrain.MAX_Y)
            return true;
        // NOTE: no check for min Y.
        // Projectiles can sail as far up as they want.

        // Check collisions against terrain
        Util.Pair pair = new Util.Pair();
        short board[] = model.getTerrain().getBoard();
        int x = (int)mX;
        int y = (int)mY;
        for (int slice = Math.max(0, x - PROJECTILE_RADIUS);
                 slice < Math.min(x + PROJECTILE_RADIUS, Terrain.MAX_X);
                 slice++) {
            Util.circAt(x, y, PROJECTILE_COLLISION_RADIUS, slice, pair);
            if (board[slice] < pair.yLower) {
                return true;
            }
        }

        // Check collisions against players
        Player players[] = model.getPlayers();
        for (Player p : players) {
            if (! p.isAlive())
                continue;
            if (Util.calcDistance(mX, mY, p.getX(), p.getY()) <
                    Player.COLLISION_RADIUS + PROJECTILE_COLLISION_RADIUS) {
                return true;
            }
        }

        return false;
    }

    public boolean hasExploded(Model model) {
        if (mExploded)
            return true;
        else {
            mExploded = checkCollisions(model);
            return mExploded;
        }
    }

    /*================= Lifecycle =================*/
    public void initialize(float x, float y, float deltaX, float deltaY,
                           int wind) {
        mX = x;
        mY = y;
        mDeltaX = deltaX;
        mDeltaY = deltaY;
        mWind = wind;
        mWind /= 1300;

        mExploded = false;
        mCurStep = 0;
        mInUse = true;
    }

    public Projectile() { }
}
