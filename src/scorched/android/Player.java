package scorched.android;

public class Player {
    /*================= Constants =================*/
    private static final int MAX_LIFE = 100;

    /*================= Members =================*/
    private int mId;
    private int mSlot = -1;
    private int mLife;

    /*================= Access =================*/
    int getId() {
        return mId;
    }

    int getSlot() {
        return mSlot;
    }

    /*================= Operations =================*/
    void setSlot(int slot) {
        mSlot = slot;
    }

    /*================= Lifecycle =================*/
    public Player(int id) {
        mId = id;
        mLife = MAX_LIFE;
    }
}
