package scorched.android;

public class Player {
    /*================= Constants =================*/
    private static final int MAX_LIFE = 1000;

    private static final int MIN_POWER = 50;
    private static final int MAX_POWER = 1000;
    
    private static final float MIN_TURRET_ANGLE = 0;
    private static final float MAX_TURRET_ANGLE = 3.1415926535f;
    private static final float ANGLE_STEP = 0.031415926535f;

    /*================= Members =================*/
    private int mId;

    /** How much life we have. If this is 0 then we're dead. */
    private int mLife;

    /** The horizontal 'slot' that the tank occupies on the playing field. */
    private int mX = -1;

    /** Current y-position of the bottom of the tank. */
    private float mY;

    /** Current turret angle, in radians. Turret angles are represented 
     * like this:
     *           pi/2
     *     3pi/4  |   pi/4
     *         \  |  /
     *          \ | /
     *    pi =========== 0
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

    /** Get the x-coordinate of the center of the tank */
    public int getX() {
        return mX;
    }

    /** Get the y-coordinate of the bottom of the tank */
    public float getY() {
        return mY;
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
        float dx = (float)Math.cos(mAngle);
        dx = (dx * mPower) / 10000.0f;
        float dy = (float)Math.sin(mAngle);
        dy = (dy * mPower) / 10000.0f;
        return new Weapon(getTurretX(), getTurretY(), dx, dy);
    }
    
    /** Return a float representing the X position of the end of the 
     *  gun turret */
    private float getTurretX() {
        float x = mX;
        float turretX = (float)Math.cos(mAngle);
        return x + (ScorchedModel.TURRET_LENGTH * turretX);
    }

    /** Return a float representing the Y position of the end of the 
     *  gun turret */
    private float getTurretY() {
        float turretY = (float)Math.sin(mAngle);
        return mY + (ScorchedModel.TURRET_LENGTH * turretY);
    }

    /*================= Operations =================*/
    public void setX(int x) {
        mX = x;
        assert(x >= 1);
        assert(x < (ScorchedModel.MAX_X - 1));
    }
    
    public void calcY(ScorchedModel model) {
        float h[] = model.getHeights();
        mY = avg4f(h[mX - 1], h[mX], h[mX + 1],
                                min3f(h[mX - 1], h[mX], h[mX + 1]));
    }

    /** set turret power. */
    public void setPower(int val) {
        mPower = val;
    }

    /** set turret angle.
     *  'val' is scaled to 0...1000 and must be normalized */
    public void setAngle(int val) {
        float angle = 1000 - val;
        angle *= MAX_TURRET_ANGLE;
        angle /= 1000f;
        mAngle = angle;
    }

    /** Move turret left by one degree */
    public void turretLeft() {
        mAngle += ANGLE_STEP;
        if (mAngle > MAX_TURRET_ANGLE) {
            mAngle = MAX_TURRET_ANGLE;
        }
    }

    /** Move turret right by one degree */
    public void turretRight() {
        mAngle -= ANGLE_STEP;
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
        if (mPower < MIN_POWER) {
            mPower = MIN_POWER;
        }
    }

    /*================= Lifecycle =================*/
    public Player(int id) {
        mId = id;
        mLife = MAX_LIFE;
        mAngle = MAX_TURRET_ANGLE / 4;
        mPower = MAX_POWER / 2;
    }
}
