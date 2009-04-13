package scorched.android;

import java.util.Random;

/**
 * General utility stuff that doesn't really fit anywhere else.
 */
public abstract class Util {
    /*================= Static =================*/
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
