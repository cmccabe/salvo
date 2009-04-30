package scorched.android;

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

    public static final int PROJECTILE_COLOR = 0xffff0000;

    /*================= Data =================*/
    private float mX, mY;
    private float mDeltaX, mDeltaY;
    private float mWind;

    private boolean mExploded;
    private int mCurStep;

    /*================= Accessors =================*/
    public float getCurX() {
        return mX;
    }

    public float getCurY() {
        return mY;
    }

    /*================= Operations =================*/
    public void step() {
        mCurStep++;
        if (mCurStep > MAX_STEPS)
            mExploded = true;
        mX += mDeltaX;
        mY += mDeltaY;
        mDeltaY += Terrain.GRAVITY;
        mDeltaX += mWind;
    }

    /* Returns the bottom coordinate of a circle centered at (cx, cy)
     * evaulated at x
     */
    private int circAt(int x0, int y0, int radius, int x) {
         // The equation for a circle is
         // (x - x0)^2 + (y - y0)^2 = r
         //
         // Solved for y:
         //              _____________
         //   y = y0 +  | r - (x-x0)^2
         //          - \|
         //
         // Taking only the bottom coordinate:
         //              _____________
         //   y = y0 +  | r - (x-x0)^2
         //            \|

        float tmp = radius - (x - x0) * (x - x0);
        if (tmp <= 0)
            return 0;
        float bottom = y0 + (float)Math.sqrt(tmp);
        return (int)bottom;
    }

    /** Returns the Euclidian distance between (x0,y0) and (x1,1) */
    private float calcDistance(float x0, float y0, float x1, float y1) {
        return (float)
            Math.sqrt(((x1 - x0) * (x1 - x0)) + ((y1 - y0) * (y1 - y0)));
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
        short board[] = model.getTerrain().getBoard();
        int x = (int)mX;
        int y = (int)mY;
        for (int i = Math.max(0, x - PROJECTILE_RADIUS);
                 i < Math.min(x + PROJECTILE_RADIUS, Terrain.MAX_X);
                 i++) {
            int yb = circAt(x, y, PROJECTILE_RADIUS, i);
            if (board[i] <= yb) {
                return true;
            }
        }

        // Check collisions against players
        Player players[] = model.getPlayers();
        for (Player p : players) {
            if (calcDistance(mX, mY, p.getX(), p.getY()) <
                    Player.COLLISION_RADIUS) {
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
    }

    public Projectile() { }
}
