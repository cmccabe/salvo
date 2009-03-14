package scorched.android;

import java.util.Iterator;
import java.util.Stack;

import android.util.Log;

public class Weapon
{
    /*================= Constants =================*/
    final private String TAG = "Weapon";

    /** The maximum number of times we will recalculate trajectory before
     *  giving up and triggering the explosion. */
    final private int MAX_NUM_SAMPLES = 10000;

    /** The minimum distance squared (in slots) we will accept between two points on
     * our trajectory before we accept the need to keep draw both points */
    final private float MIN_UPDATE_DIST_SQUARED = (float)0.1;

    /*================= Types =================*/
    static class Point {
        /*================= Data =================*/
        private float mX, mY;
        /*================= Access =================*/
        public float getX() {
            return mX;
        }
        public float getY() {
            return mY;
        }
        public String toStr() {
            String ret = "(x=" + mX + ",y=" + mY + ")";
            return ret;
        }
        /*================= Operations =================*/
        public void setX(float x) {
            mX = x;
        }
        public void setY(float y) {
            mY = y;
        }
        /*================= Lifecycle =================*/
        Point(float x, float y) {
            mX = x;
            mY = y;
        }
    }

    /*================= Data =================*/
    /** Current velocity */
    float mDeltaX, mDeltaY;

    /** (Most of) the points the weapon has traversed on the screen. */
    Stack < Point > mPoints;

    /** Number of times nextSample() has been called */
    int mCurNumSamples;

    boolean mNeedsRedraw;

    /*================= Access =================*/
    public Iterator<Point> getPoints() {
        Iterator<Point> iter = mPoints.iterator();
        // The second element of the list is a duplicate, to simplify
        // class logic. Don't allow outsiders to see this.
        iter.next();
        return iter;
    }

    /** Returns the distance squared between two points */
    float distanceSquared(float x1, float y1, float x2, float y2) {
        float xDelta = (x1 - x2);
        float yDelta = (y1 - y2);
        return (xDelta * xDelta) + (yDelta * yDelta);
    }

    public boolean getNeedsRedraw() {
        return mNeedsRedraw;
    }


    /*================= Operations =================*/
    /** Calculate the trajectory of this weapon */
    public void calculateTrajectory(Model model, float x[], float y[]) {
        
    }

    /** Move the projectile forward. */
    public void nextSample() {
        // do gravity
        mDeltaY -= Model.GRAVITY;

        // TODO: add (horizontal) wind

        // The order of points is like this: [old, cur, next]
        mCurNumSamples++;
        Point cur = mPoints.pop();
        Point old = mPoints.peek();
        float nextX = cur.getX() + mDeltaX;
        float nextY = cur.getY() + mDeltaY;
        if (distanceSquared(nextX, nextY, old.getX(), old.getY()) <
                MIN_UPDATE_DIST_SQUARED) {
            cur.setX(nextX);
            cur.setY(nextY);
            mPoints.push(cur);
            mNeedsRedraw = false;
        }
        else {
            mPoints.push(cur);
            mPoints.push(new Point(nextX, nextY));
            mNeedsRedraw = true;
        }
        Log.w(TAG, "nextSample(): old:" + old.toStr() +
                    ", cur:" + cur.toStr() + ",nextX=" + nextX + ",nextY=" + nextY);
    }

    /** If the projectile collided with the terrain, return the point
     * at which it collided. Otherwise, return null. */
    public Point testCollision() {
        // If this weapon has been in the air for too long, just time out.
        // Users don't want turns to take an extremely long time.
        if (mCurNumSamples > MAX_NUM_SAMPLES) {
            return mPoints.peek();
        }
        // TODO: implement collision testing
        return null;
    }

    /*================= Lifecycle =================*/
    public Weapon(float x, float y, float deltaX, float deltaY) {
        Log.w(TAG, "Weapon: creating with x=" + x + ", y=" + y +
                   ", deltaX=" + deltaX + ", deltaY=" + deltaY);
        mDeltaX = deltaX;
        mDeltaY = deltaY;
        mPoints = new Stack < Point >();
        mPoints.push(new Point(x, y));
        mPoints.push(new Point(x, y));
        mCurNumSamples = 0;
        mNeedsRedraw = true;
    }
}
