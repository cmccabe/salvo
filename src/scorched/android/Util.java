package scorched.android;

import java.util.Random;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * General utility stuff that doesn't really fit anywhere else.
 */
public abstract class Util {
    /*================= Types =================*/
    /** Runnable that creates a Toast and displays it */
    public static class DoToast implements Runnable {
        private Context mContext;
        private String mString;

        /*================= Operations =================*/
        public void run() {
            Toast toast = Toast.makeText(mContext, mString, 30);
            toast.setGravity(Gravity.TOP, 0, 30);
            toast.show();
        }

        /*================= Lifecycle =================*/
        public DoToast(Context context, String string) {
            mContext = context;
            mString = string;
        }
    }

    /*================= Static =================*/
    /** Returns the Euclidian distance between (x0,y0) and (x1,1) */
    public static float calcDistance(float x0, float y0, float x1, float y1) {
        return (float)
            Math.sqrt(((x1 - x0) * (x1 - x0)) + ((y1 - y0) * (y1 - y0)));
    }

    /** Convenience function that makes a key for player-specific data */
    public static String indexToString(int playerNum) {
        StringBuilder b = new StringBuilder(40);
        b.append("P").append(playerNum).append("_");
        return b.toString();
    }

    /** Convenience function that makes a key for player-specific data */
    public static String indexToString(int playerNum, String key) {
        StringBuilder b = new StringBuilder(80);
        b.append("P").append(playerNum).append("_");
        b.append(key);
        return b.toString();
    }

    /*================= Data =================*/
    /** A source of random numbers. */
    public static Random mRandom = new Random();
}
