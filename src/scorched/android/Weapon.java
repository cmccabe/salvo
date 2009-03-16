package scorched.android;

import java.util.Iterator;
import java.util.Stack;

import android.util.Log;

public enum Weapon
{
    instance;

    /*================= Constants =================*/
    private final static String TAG = "Weapon";

    /** The maximum number of samples we can store in our trajectory */
    private final static int MAX_SAMPLES = 400;

    /** The maximum number of times we will recalculate trajectory before
     *  giving up and triggering the explosion. */
    private final static int MAX_RECALC = 10000;

    /** The minimum distance squared (in slots) we will accept between two
     * points on our trajectory before we accept the need to keep draw
     * both points */
    private final static float MIN_UPDATE_DIST_SQ = (float)0.1;

    /*================= Static =================*/
    /** Returns the distance squared between two points */
    private static float distanceSquared(float x1, float y1,
                                         float x2, float y2) {
        float x = (x1 - x2);
        float y = (y1 - y2);
        return (x * x) + (y * y);
    }

    /*================= Data =================*/
    private float mX[] = new float[MAX_SAMPLES];

    private float mY[] = new float[MAX_SAMPLES];

    private float mDeltaX, mDeltaY;

    private short mTotalSamples;

    private short mTotalCalculations;

    /*================= Access =================*/
    public float[] getX() {
        return mX;
    }

    public float[] getY() {
        return mY;
    }

    public short getTotalSamples() {
        return mTotalSamples;
    }

    /** Returns true if the x, y coordinates of x[index], y[index] 
     *  are inside the earth */
    private boolean testCollision(Model model, int index) {
        float x = mX[index];
        float y = mY[index];

        // The edges of the screen are 'hard' boundaries
        if ((x < 0) || (x >= Model.MAX_X - 1)) {
            return true;
        }

        // Use linear interpolation to check if we're under the ground
        float h[] = model.getHeights();
        int ix = (int)x;
        float xSlop = x - (float)ix;
        float interpolatedY = ((1.0f - xSlop) * h[ix]) + (xSlop * h[ix + 1]);
        return (y <= interpolatedY);
    }

    /*================= Operations =================*/
    /** Calculate the trajectory of this weapon */
    public void calculateTrajectory(Model model) {
        float x = mX[0];
        float y = mY[0];
        short index;
        mTotalCalculations = 0;
        for (index = 0; index < MAX_SAMPLES - 1; index++) {
            if (testCollision(model, index))
                break;
            if (!getNextCoords(index))
                break;
        }
        mTotalSamples = (short)(index + 1);
    }

    /** Gets the next coordinates for mX and mY.
     *  Each point in the trajectory must be a certain minimum distance
     *  away from the previous one. Otherwise we would exceed memory
     *  resources.
     *
     *  @return         true if we got the next coordinates
     *                  false if we've done too many calculations and have
     *                  to quit.
     */
    private boolean getNextCoords(int index) {
        float x = mX[index];
        float y = mY[index];
        float prevX = x;
        float prevY = y;
        mTotalCalculations++;
        while (mTotalCalculations < MAX_RECALC) {
            x += mDeltaX;
            y += mDeltaY;
            //mDeltaX -= wind
            mDeltaY -= Model.GRAVITY;
            if (distanceSquared(x, y, prevX, prevY) > MIN_UPDATE_DIST_SQ) {
                mX[index + 1] = x;
                mY[index + 1] = y;
                Log.w(TAG, "setting mX[" + (index + 1) + "] = " + x + ", " +
                                   "mY[" + (index + 1) + "] = " + y);
                return true;
            }
            mTotalCalculations++;
        }
        return false;
    }

    /*================= Lifecycle =================*/
    public void initialize(float initX, float initY,
                           float deltaX, float deltaY) {
        mX[0] = initX;
           mY[0] = initY;
        mDeltaX = deltaX;
           mDeltaY = deltaY;
           mTotalSamples = 1;
    }

    private Weapon() {
        mX = new float[MAX_SAMPLES];
        mY = new float[MAX_SAMPLES];
    }
}
