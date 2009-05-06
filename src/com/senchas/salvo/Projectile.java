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
    /** The maximum number of steps we will take before simply detonating the
     * projectile in the air. This is to avoid the game getting "stuck"
     */
    private static final int MAX_STEPS = 700;

    /** Radius of the projectile */
    public static final int PROJECTILE_RADIUS = 5;
    public static final int PROJECTILE_COLLISION_RADIUS = 4;

    public static final int PROJECTILE_COLOR = Color.argb(0xff, 0xff, 0, 0);

    public static final Projectile EMPTY_ARRAY[] = new Projectile[0];

    /*================= Data =================*/
    private float mX, mY;
    private float mDeltaX, mDeltaY;
    private boolean mIsRolling;
    private float mWind;
    private boolean mInUse;

    /** The first step on which we'll check for collisions.
     * Prior to this step, we'll just pass through terrain!
     */
    private int mFirstCollidableStep;

    private int mCurStep;

    /** The weapon that shot this projectile */
    private WeaponType mWeapon;

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

    public void step(Model model, GameState.BallisticsState.Accessor ball) {
        // TODO: implement roller by moving delta logic off to WeaponType
        if (mWeapon.isProjectile()) {
            mX += mDeltaX;
            mY += mDeltaY;
            mDeltaY += Terrain.GRAVITY;
            mDeltaX += mWind;

            mCurStep++;
            if (mCurStep > mFirstCollidableStep) {
                if ((mCurStep >= MAX_STEPS) ||
                        checkBoundaryCollisions(model) ||
                        checkTerrainCollisions(model) ||
                        checkPlayerCollisions(model)) {
                    mWeapon.detonate(model, (int)mX, (int)mY, ball);
                    mInUse = false;
                }
            }
        }
        else if (mWeapon.isRoller()) {
            short h[] = model.getTerrain().getBoard();
            mX += mDeltaX;
            int prevY = (int)mY;
            mY = model.getTerrain().safeGetVal((int)mX);

            mCurStep++;
            if ((mCurStep >= MAX_STEPS) || (mY < prevY) ||
                    checkBoundaryCollisions(model) ||
                    checkPlayerCollisions(model)) {
                mWeapon.detonate(model, (int)mX, (int)mY, ball);
                mInUse = false;
            }
        }
        else {
            throw new RuntimeException("weapon is not a roller or " +
                                       "projectile");
        }
    }

    /** Return true if the projectile has collided with a world boundary.
     *
     * NOTE: there is no check for min Y.
     * Projectiles can sail as far up as they want.
     */
    private boolean checkBoundaryCollisions(Model model) {
        if (mX < 0)
            return true;
        else if (mX > Terrain.MAX_X)
            return true;
        else if (mY > Terrain.MAX_Y)
            return true;
        else
            return false;
    }

    /** Return true if the projectile has collided with the terrain */
    private boolean checkTerrainCollisions(Model model) {
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
        return false;
    }

    private boolean checkPlayerCollisions(Model model) {
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

    /*================= Lifecycle =================*/
    public void initialize(int x, int y, float deltaX, float deltaY,
                           int wind, WeaponType weapon,
                           int firstCollidableStep) {
        if (firstCollidableStep > MAX_STEPS) {
            throw new RuntimeException(
                "can't have firstCollidableStep > MAX_STEPS because " +
                "it would interfere with the MAX_STEPS mechanism");
        }

        mX = x;
        mY = y;
        mDeltaX = deltaX;
        mDeltaY = deltaY;
        mWind = wind;
        mWind /= 1300;
        mFirstCollidableStep = firstCollidableStep;

        mCurStep = 0;
        mInUse = true;
        mWeapon = weapon;
    }

    public Projectile() { }
}
