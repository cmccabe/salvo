package scorched.android;

public class Player {
    /*================= Constants =================*/
    private static final int MAX_LIFE = 1000;

    public static final int MIN_POWER = 50;
    public static final int MAX_POWER = 1000;

    public static final int MIN_TURRET_ANGLE = 0;
    public static final int MAX_TURRET_ANGLE = 180;

    public static final int INVALID_PLAYER_ID = -1;

    /*================= Members =================*/
    private int mId;

    /** How much life we have. If this is 0 then we're dead. */
    private int mLife;

    /** The horizontal 'slot' that the tank occupies on the playing field. */
    private int mX = -1;

    /** Current y-position of the bottom of the tank. */
    private float mY;

    /** Current turret angle, in degrees. Turret angles are represented
     * like this:
     *            90
     *       135  |   45
     *         \  |  /
     *          \ | /
     *   180 =========== 0
     */
    private int mAngleDeg;

    /** Current turret angle in radians. You always need the angle in radians
     * for doing math */
    private float mAngleRad;

    /** The power that we're firing with. Measured on the same
     * scale as life. */
    private int mPower;

    /*================= Static =================*/

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

    public int getAngleDeg() {
        return mAngleDeg;
    }

    public float getAngleRad() {
        return mAngleRad;
    }

    public int getPower() {
        return mPower;
    }

    public boolean isAlive() {
        return mLife > 0;
    }

    public GameState getGameState() {
        return GameState.sHumanMoveState;
    }

    public Weapon getWeapon() {
        // Eventually this will return the specific type of weapon you
        // have selected.
        // It might be even better to cache the Weapon instance in Player
        // to avoid making another allocation.
        float dx = (float)Math.cos(mAngleRad);
        dx = (dx * mPower) / 10000.0f;
        float dy = (float)Math.sin(mAngleRad);
        dy = (dy * mPower) / 10000.0f;
        return new Weapon(getTurretX(), getTurretY(), dx, dy);
    }

    /** Return a float representing the X position of the end of the
     *  gun turret */
    private float getTurretX() {
        return (float)mX +
            ((float)Math.cos(mAngleRad) * Model.TURRET_LENGTH);
    }

    /** Return a float representing the Y position of the end of the
     *  gun turret */
    private float getTurretY() {
        return (float)mY +
            ((float)Model.PLAYER_SIZE / 2f) +
            ((float)Math.sin(mAngleRad) * Model.TURRET_LENGTH);
    }

    /*================= Operations =================*/
    public void setX(int x) {
        mX = x;
        assert(x >= 1);
        assert(x < (Model.MAX_X - 1));
    }

    public void calcY(Model model) {
        float h[] = model.getHeights();
        mY = h[mX];
    }

    /** set turret power. */
    public void setPower(int val) {
        mPower = val;
    }

    /** set turret angle.
     *  'val' is scaled to 0...1000 and must be normalized */
    public void setAngleDeg(int angleDeg) {
        if (angleDeg > MAX_TURRET_ANGLE) {
            angleDeg = MAX_TURRET_ANGLE;
        }
        if (angleDeg < MIN_TURRET_ANGLE) {
            angleDeg = MIN_TURRET_ANGLE;
        }
        mAngleDeg = angleDeg;
        mAngleRad = (float)Math.toRadians(angleDeg);
    }

    /** Move turret left by one degree */
    public void turretLeft() {
        setAngleDeg(mAngleDeg + 1);
    }

    /** Move turret right by one degree */
    public void turretRight() {
        setAngleDeg(mAngleDeg - 1);
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
        setAngleDeg(MAX_TURRET_ANGLE / 4);
        mPower = (3 * MAX_POWER) / 4;
    }
}
