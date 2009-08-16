package com.senchas.salvo;

import com.senchas.salvo.Cosmos.PlayerInfo;
import com.senchas.salvo.RunGameAct.RunGameActAccessor;
import com.senchas.salvo.WeaponType.ExplosionAttributes;
import android.util.Log;

/* Represents an ongoing explosion
 *
 * This class is mutable and designed to be reused. This is to cut down on
 * the number of memory allocations, as usual.
 */
public class Explosion {
    /*================= Constants =================*/
    public static final int MAX_TIME = 1000;

    /** If a hit is closer than this radius, it is considered a bullseye
     *  which should do full damage.
     */
    public static final int BULLSEYE_RADIUS =
        Projectile.PROJECTILE_COLLISION_RADIUS + Player.COLLISION_RADIUS + 2;

    /** How much money players earn from being the last surviving tank */
    public static final int SURVIVOR_BONUS = 200;

    /** How much money players earn from making a kill */
    private static final int KILL_BONUS = 150;

    public static final Explosion EMPTY_ARRAY[] = new Explosion[0];

    /*================= Data =================*/
    /** X coordinate of the center of the explosion */
    private int mX;

    /** Y coordinate of the center of the explosion */
    private int mY;

    /** The attributes of this explosion */
    private ExplosionAttributes mAttr;

    /** The perpetrator of this explosion */
    private int mPerp;

    /** The time we started displaying the explosion */
    private long mStartTime;

    /*================= Access =================*/
    public int getX() {
        return mX;
    }

    public int getY() {
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
        int full = mAttr.getRadius();
        long diff = time - mStartTime;
        if (diff > MAX_TIME)
            return full;
        else {
            return (int)((full * diff) / MAX_TIME);
        }
    }

    public ExplosionAttributes getExplosionAttributes() {
        return mAttr;
    }

    public int getPerp() {
        return mPerp;
    }

    /*================= Operations =================*/
    public void clearInUse() {
        mStartTime = 0;
    }

    /** Deal direct damage to players */
    public void doDirectDamage(RunGameActAccessor game) {
        int full = mAttr.getFullDamage();
        if (full == 0)
            return;
        Player players[] = game.getModel().getPlayers();
        for (Player p : players) {
            if (! p.isAlive())
                continue;
            float dist = Util.calcDistance(mX, mY, p.getX(), p.getY());
            int safeDist = Player.COLLISION_RADIUS + mAttr.getRadius();
            boolean damagedUs = false;
            if (dist < safeDist) {
                int damage = Util.linearInterpolation(full, 0,
                        BULLSEYE_RADIUS, safeDist,
                        (int)dist);
                if (Util.mDebug > 1) {
                    StringBuilder b = new StringBuilder(80 * 5);
                    b.append("doDirectDamage(player=").append(p.getName());
                    b.append(" full=").append(full);
                    b.append(" dist=").append(dist);
                    b.append(" safeDist=").append(safeDist);
                    b.append(" damage=").append(damage);
                    Log.w(this.getClass().getName(), b.toString());
                }
                p.takeDamage(damage);

                // award money to the player who made the shot
                int damageEarnings = (mPerp == p.getId()) ? -damage : damage;
                PlayerInfo perpInfo =
                    game.getCosmos().getPlayerInfo()[mPerp];
                perpInfo.earnMoney(damageEarnings);
                if ((! p.isAlive()) && (mPerp != p.getId())) {
                    perpInfo.earnMoney(KILL_BONUS);
                }

                damagedUs = true;
            }
            if (dist < Brain.AGGRESSION_NOTIFICATION_DISTANCE) {
                p.getBrain().notifyAggression(mPerp, dist, damagedUs);
            }
        }
    }

    /** Change the terrain to reflect this explosion */
    public void editTerrain(RunGameActAccessor game) {
        Terrain terrain = game.getModel().getTerrain();
        short board[] = terrain.getBoard();

        Util.Pair pair = new Util.Pair();
        int eSize = mAttr.getRadius();
        // do the circle collision algorithm on each height
        for (int slice = Math.max(0, mX - eSize);
                 slice < Math.min(mX + eSize, Terrain.MAX_X);
                 slice++) {
            Util.circAt(mX, mY, eSize, slice, pair);
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
    public void initialize(int x, int y, ExplosionAttributes attr, int perp) {
        mX = x;
        mY = y;
        mAttr = attr;
        mPerp = perp;

        mStartTime = System.currentTimeMillis();
    }

    public Explosion() {
        mStartTime = 0;
    }
}
