package scorched.android;

public class Player {
    /*================= Constants =================*/
    private static final int MAX_LIFE = 100;

    /*================= Members =================*/
    private float mX;
    private int mLife = MAX_LIFE;

    /*================= Access =================*/
    float GetX() {
        return mX;
    }

    /*================= Lifecycle =================*/
    public Player(float x) {
        mX = x;
    }
}
