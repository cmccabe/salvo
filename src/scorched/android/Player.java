package scorched.android;

public class Player {
    /*================= Constants =================*/
    private static final int MAX_LIFE = 1000;
    private static final int MAX_POWER = 1000;
    private static final int MIN_TURRET_ANGLE = 0;
    private static final int MAX_TURRET_ANGLE = 180;

    /*================= Members =================*/
    private int mId;

    /** The horizontal 'slot' that the tank occupies on the playing field. */
    private int mSlot = -1;

    /** How much life we have. If this is 0 then we're dead. */
    private int mLife;

    /** Current y-position of the tank. */
    private float mHeight;

    /** Current turret angle. Turret angles are represented like this:
     *           90
     *       135  |   45
     *         \  |  /
     *          \ | /
     *    180 ========== 0
     */
    private float mAngle;

    /** The power that we're firing with. Measured on the same 
     * scale as life. */ 
    private int mPower;
    
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

    public float getAngle() {
        return mAngle;
    }

    public float getPower() {
        return mPower;
    }

    public boolean isAlive() {
        return mLife > 0;
    }

    public boolean isHuman() {
        return true;
    }

    public Weapon getWeapon() {
        // Eventually this will return the specific type of weapon you
        // have selected.
        // It might be even better to cache the Weapon instance in Player
        // to avoid making another allocation.
        return new Weapon(getTurretSlot(), getTurretHeight(),
        					(float)((Math.cos(Math.toRadians(mAngle)) * mPower) / 
        							10000.0),
        					(float)((Math.sin(Math.toRadians(mAngle)) * mPower) / 
        							(10000.0 * ScorchedModel.MAX_HEIGHTS)));
    }
    
    /** Return a float representing the X position of the gun turret */
    private float getTurretSlot() {
        float ret = mSlot;
        return ret;// + ((float)Math.cos(Math.toRadians(mAngle) * 
                   //     ScorchedModel.SLOTS_PER_TURRET));
    }

    /** Return a float representing the Y position of the gun turret */
    private float getTurretHeight() {
        return mHeight;// + ((float)Math.sin(Math.toRadians(mAngle) * 
                      //      ScorchedModel.SLOTS_PER_TURRET));
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

    /** set turret power. */
    public void setPower(int val) {
        mPower = val;
    }

    /** set turret angle.
     *  'val' is scaled to 0...1000 and must be normalized */
    public void setAngle(int val) {
        float angle = 1000 - val;
        angle *= 0.18;
        mAngle = angle;
    }

    /** Move turret left by one degree */
    public void turretLeft() {
        mAngle += 1;
        if (mAngle > MAX_TURRET_ANGLE) {
            mAngle = MAX_TURRET_ANGLE;
        }
    }

    /** Move turret right by one degree */
    public void turretRight() {
        mAngle -= 1;
        if (mAngle < MIN_TURRET_ANGLE) {
            mAngle = MIN_TURRET_ANGLE;
        }
    }

    public void powerUp() {
        mPower += 1;
        if (mPower > MAX_POWER) {
            mPower = MAX_POWER;
        }
    }

    public void powerDown() {
        mPower -= 1;
        if (mPower < 1) {
            mPower = 1;
        }
    }

    /*================= Lifecycle =================*/
    public Player(int id) {
        mId = id;
        mLife = MAX_LIFE;
        mAngle = 45;
        mPower = 500;
    }
}
