package scorched.android;

import scorched.android.ModelFactory.MyVars;
import android.graphics.Color;
import android.os.Bundle;

public class Player {
    /*================= Types =================*/
    public static enum PlayerColor {
        RED("red", Color.argb(0xff, 0xef,0x29, 0x29)),
        ORANGE("orange", Color.argb(0xff, 0xff, 0xbb, 0x44)),
        BROWN("brown", Color.argb(0xff, 0xa6, 0x7a, 0x3e)),
        YELLOW("yellow", Color.argb(0xff, 0xfc, 0xe9, 0x4f)),
        GREEN("green", Color.argb(0xff, 0x06, 0xd0, 0x30)),
        CYAN("cyan", Color.argb(0xff, 0x8d, 0xef, 0xef)),
        BLUE("blue", Color.argb(0xff, 0x72, 0x9f, 0xcf)),
        PINK("pink", Color.argb(0xff, 0xff, 0x83, 0xe9)),
        PURPLE("purple", Color.argb(0xff, 0xad, 0x7f, 0xa8)),
        GREY("grey", Color.argb(0xff, 0xd3, 0xd7, 0xcf));

        /*================= Static =================*/

        /*================= Data =================*/
        private final String mName;
        private final int mColor;

        /*================= Access =================*/
        public String toString() { return mName; }
        public int toInt() { return mColor; }

        /*================= Lifecycle =================*/
        private PlayerColor(String name, int color) {
            mName = name;
            mColor = color;
        }
    }

    /*================= Constants =================*/
    public static final int MIN_STARTING_LIFE = 25;
    public static final int DEFAULT_STARTING_LIFE = 100;
    public static final int MAX_STARTING_LIFE = 300;
    public static final int MAX_LIFE = MAX_STARTING_LIFE;
    public static final int MAX_NAME_LENGTH = 14;

    public static final int MIN_POWER = 50;
    public static final int MAX_POWER = 1000;

    public static final int MIN_TURRET_ANGLE = 0;
    public static final int MAX_TURRET_ANGLE = 180;

    public static final int INVALID_PLAYER_ID = -1;

    /*================= Members =================*/
    public static class MyVars {
        /** How much life we have. If this is 0 then we're dead. */
        public int mLife;

        /** The horizontal 'slot' that the tank occupies on the playing
         * field. */
        public int mX;

        /** Current y-position of the bottom of the tank. */
        public float mY;

        /** Current turret angle, in degrees. Turret angles are represented
         * like this:
         *            90
         *       135  |   45
         *         \  |  /
         *          \ | /
         *   180 =========== 0
         */
        public int mAngleDeg;

        /** The power that we're firing with. Measured on the same
         * scale as life. */
        public int mPower;

        /** Player name */
        public String mName;

        /** Our color */
        public PlayerColor mColor;
    }
    private MyVars mV;

    /** The brain that controls this player */
    public Brain mBrain;

    /** The index of this player in the players array */
    public int mId;

    /** Cached value of the current turret angle in radians.
     * You always need the angle in radians for doing math */
    private float mAngleRad;

    /*================= Static =================*/

    /*================= Access =================*/
    public String getIntroductionString() {
        StringBuilder b = new StringBuilder(80);
        b.append(mV.mName);
        b.append("'s turn");
        return b.toString();
    }

    public int getId() {
        return mId;
    }

    /** Get the x-coordinate of the center of the tank */
    public int getX() {
        return mV.mX;
    }

    /** Get the y-coordinate of the bottom of the tank */
    public float getY() {
        return mV.mY;
    }

    public int getAngleDeg() {
        return mV.mAngleDeg;
    }

    public float getAngleRad() {
        return mAngleRad;
    }

    public int getPower() {
        return mV.mPower;
    }

    public Player.PlayerColor getColor() {
        return mV.mColor;
    }

    public boolean isAlive() {
        return mV.mLife > 0;
    }

    public GameState getGameState() {
        return GameState.HumanMoveState.create();
    }

    /** Initialize the Weapon singleton with what we're firing */
    public void fireWeapon() {
        float dx = (float)Math.cos(mAngleRad);
        dx = (dx * mV.mPower) / 10000.0f;
        float dy = (float)Math.sin(mAngleRad);
        dy = (dy * mV.mPower) / 10000.0f;
        Weapon.instance.initialize(getTurretX(), getTurretY(), dx, dy,
                                   WeaponType.sBabyMissile);
    }

    /** Return a float representing the X position of the end of the
     *  gun turret */
    private float getTurretX() {
        return (float)mV.mX +
            ((float)Math.cos(mAngleRad) * Model.TURRET_LENGTH);
    }

    /** Return a float representing the Y position of the end of the
     *  gun turret */
    private float getTurretY() {
        return (float)mV.mY +
            ((float)Model.PLAYER_SIZE / 2f) +
            ((float)Math.sin(mAngleRad) * Model.TURRET_LENGTH);
    }

    /*================= Operations =================*/
    public void setX(int x, Terrain terrain) {
        mV.mX = x;
        // TODO: use averaging mechanism here to set tank height
        short h[] = terrain.getHeights();
        mV.mY = h[x];
    }

    /** set turret power. */
    public void setPower(int val) {
        mV.mPower = val;
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
        mV.mAngleDeg = angleDeg;
        mAngleRad = (float)Math.toRadians(angleDeg);
    }

    /** Move turret left by one degree */
    public void turretLeft() {
        setAngleDeg(mV.mAngleDeg + 1);
    }

    /** Move turret right by one degree */
    public void turretRight() {
        setAngleDeg(mV.mAngleDeg - 1);
    }

    public void powerUp() {
        mV.mPower += 1;
        if (mV.mPower > MAX_POWER) {
            mV.mPower = MAX_POWER;
        }
    }

    public void powerDown() {
        mV.mPower -= 1;
        if (mV.mPower < MIN_POWER) {
            mV.mPower = MIN_POWER;
        }
    }

    /*================= Save =================*/
    public void saveState(int index, Bundle map) {
        AutoPack.autoPack(map, Util.indexToString(index), mV);
        mBrain.saveState(index, map);
    }

    /*================= Lifecycle =================*/
    public static Player fromBundle(int index, Bundle map) {
        MyVars v = (MyVars)AutoPack.autoUnpack(map,
                        Util.indexToString(index), MyVars.class);
        Brain brain = Brain.fromBundle(index, map);
        return new Player(index, v, brain);
    }

    public Player(int index, MyVars v, Brain brain) {
        mV = v;
        mId = index;
        mBrain = brain;

        // update cached value of mAngleRad
        setAngleDeg(mV.mAngleDeg);
    }
}
