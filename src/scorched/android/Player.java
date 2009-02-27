package scorched.android;

public class Player {
    /*================= Constants =================*/
    private static final int MAX_LIFE = 1000;

    /*================= Members =================*/
    private int mId;
    private int mSlot = -1;
    private int mLife;
    private float mHeight;
    private float mTurretAngle;
    private int mTurretPower;
    
    /*================= Static =================*/
    private final float avg4f(float a, float b, float c, float d) {
        return (a + b + c + d) / 4;
    }
    
    private final float min3f(float f, float g, float h) {
        if (f < g) {
                if (h < f)
                        return h;
                else
                        return f;
        }
        else if (h < g)
                return h;
        else
                return g;
    }

    /*================= Access =================*/
    public int getId() {
        return mId;
    }

    public int getSlot() {
        return mSlot;
    }

    public float getHeight() {
        return mHeight;
    }

    public float getTurretAngle() {
        return mTurretAngle;
    }

    public float getTurretPower() {
        return mTurretPower;
    }
    
    /*================= Operations =================*/
    public void setSlot(int slot) {
        mSlot = slot;
        assert(mSlot >= 1);
    }
    
    public void calcHeight(ScorchedModel model) {
        float h[] = model.getHeights();
        mHeight = avg4f(h[mSlot - 1], h[mSlot], h[mSlot + 1],
                                min3f(h[mSlot - 1], h[mSlot], h[mSlot + 1]));
    }

        /*================= Lifecycle =================*/
    public Player(int id) {
        mId = id;
        mLife = MAX_LIFE;
        mTurretAngle = 45;
        mTurretPower = 500;
    }
}
