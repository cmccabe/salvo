package scorched.android;

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

    private boolean checkCollisions() {
        if (mX < 0)
            return true;
        else if (mX > Terrain.MAX_X)
            return true;
        else if (mY > Terrain.MAX_Y)
            return true;
        // NOTE: no check for min Y.
        // We want to let projectiles go as far up in the air as possible.
        return false;
    }

    public boolean hasExploded() {
        if (mExploded)
            return true;
        else {
            mExploded = checkCollisions();
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
