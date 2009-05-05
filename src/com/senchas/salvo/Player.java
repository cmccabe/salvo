package com.senchas.salvo;

import com.senchas.salvo.WeaponType.Armory;

import java.util.SortedMap;

import com.senchas.salvo.ModelFactory.MyVars;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

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

        /** Returns the color, using 'alpha' as the new alpha channel value */
        public int toInt(byte alpha) {
            int ret = mColor & 0x00ffffff;
            return ret | (alpha << 24);
        }

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

    public static final int INVALID_POWER = -1;
    public static final int MIN_POWER = 50;
    public static final int MAX_POWER = 1000;

    public static final int MIN_TURRET_ANGLE = 0;
    public static final int MAX_TURRET_ANGLE = 180;

    public static final int INVALID_PLAYER_ID = -1;

    public static final int PLAYER_X_SIZE = 30;
    public static final int PLAYER_Y_SIZE = 21;
    public static final int COLLISION_RADIUS = 10;
    public static final int BORDER_SIZE = 1;
    public static final int TURRET_LENGTH= 20;

    public static final byte DESELECTED_AURA_ALPHA = (byte)0;
    public static final byte SELECTED_AURA_ALPHA = (byte)0x55;
    private static final int WHITENED_AURA_COLOR =
        Color.argb(0xcc, 0xdd, 0xdd, 0xdd);

    /*================= Members =================*/
    public static class MyVars {
        /** How much life we have. If this is 0 then we're dead. */
        public int mLife;

        /** The horizontal 'slot' that the tank occupies on the playing
         * field. */
        public int mX;

        /** Current y-position of the bottom of the tank. */
        public int mY;

        /** Current turret angle, in degrees. Turret angles are represented
         * like this:
         *            90
         *       135  |   45
         *         \  |  /
         *          \ | /
         *   180 =========== 0
         */
        public int mAngleDeg;

        /** Player name */
        public String mName;

        /** The currently selected weapon */
        public WeaponType mCurWeaponType;

        /** Our color */
        public PlayerColor mColor;
    }
    private MyVars mV;

    /** The brain that controls this player */
    public Brain mBrain;

    /** The weapons that this player owns */
    Armory mArmory;

    /** The index of this player in the players array */
    public int mId;

    /** Cached value of the current turret angle in radians.
     * You always need the angle in radians for doing math */
    private float mAngleRad;

    /** The player's body color */
    private int mBodyColor;

    /** The color of this player's outline. The player's outline changes
     * color in response to how much life the player has left. */
    private int mOutlineColor;

    /** The color of this player's "aura". A player's aura is the
     * semi-transparent circle drawn around him in drawPlayer when he is the
     * selected player.
     */
    private int mAuraColor;

    /** The alpha value we should use for our aura. */
    private byte mAuraAlpha;

    /** How "whitened" our aura should be */
    private int mAuraWhitening;

    /*================= Static =================*/

    /*================= Access =================*/
    public String getName() {
        return mV.mName;
    }

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

    /** Get the y-coordinate of the center of the tank */
    public int getY() {
        return mV.mY;
    }

    /** Get the y-coordinate of the center of the turret.
     *
     * Note: the x-coordinate of the center of the turret is the same as
     * that of the tank itself. */
    public int getTurretCenterY() {
        return (int)(mV.mY - (PLAYER_Y_SIZE / 4));
    }

    public int getAngleDeg() {
        return mV.mAngleDeg;
    }

    public float getAngleRad() {
        return mAngleRad;
    }

    public WeaponType getCurWeaponType () {
        return mV.mCurWeaponType;
    }

    public Armory getArmory () {
        return mArmory;
    }

    public Player.PlayerColor getBaseColor() {
        return mV.mColor;
    }

    public int getBodyColor() {
        return mBodyColor;
    }

    public int getAuraColor() {
        return mAuraColor;
    }

    public int getOutlineColor() {
        return mOutlineColor;
    }

    public boolean isAlive() {
        return mV.mLife > 0;
    }

    public Brain getBrain() {
        return mBrain;
    }

    /** Initialize the Weapon singleton with what we're firing */
    /*public void fireWeapon() {
        float dx = (float)Math.cos(mAngleRad);
        dx = (dx * mV.mPower) / 10000.0f;
        float dy = (float)Math.sin(mAngleRad);
        dy = (dy * mV.mPower) / 10000.0f;
        Weapon.instance.initialize(getTurretX(), getTurretY(), dx, dy,
                                   WeaponType.sBabyMissile);
    }*/

    /** Get the height that we would be at when resting comfortably on the
      * ground. */
    public int getCorrectHeight(Terrain terrain) {
        // TODO: use averaging mechanism here to set tank height
        short h[] = terrain.getBoard();
        return h[mV.mX];
    }

    public byte getAuraAlpha() {
        return mAuraAlpha;
    }

    /*================= Operations =================*/
    public void setX(int x, Terrain terrain) {
        mV.mX = x;
        mV.mY = getCorrectHeight(terrain);
    }

    /** Drop the player down to the current height of the terrain.
     *
     * If the player has to fall, apply fall damage.
     * If the player is already at that height, do nothing.
     */
    public void doFalling(Terrain terrain) {
        int cy = getCorrectHeight(terrain);
        if (mV.mY < cy) {
            Log.e(this.getClass().getName(), "can't understand " +
                "why the player is lower than expected");
            mV.mY = cy;
        }
        else if (mV.mY > cy) {
            int fallDist = mV.mY - cy;
            takeDamage(fallDist); // TODO: determine best
                                  // multiplier setting here
            mV.mY = cy;
        }
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

    public void setCurWeaponType(WeaponType type) {
        mV.mCurWeaponType = type;
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new RuntimeException("takeDamage: damage cannot be " +
                                       "less than 0");
        }
        mV.mLife -= damage;
        if (mV.mLife < 0)
            mV.mLife = 0;
        // Player color depends on the current amount of life
        cachePlayerColor(0);
    }

    /** Set the current aura alpha. 0 will disable the aura */
    public void setAuraAlpha(byte auraAlpha) {
        mAuraAlpha = auraAlpha;
        cacheAuraColor();
    }

    public void setAuraWhitening(int auraWhitening) {
        mAuraWhitening = auraWhitening;
        cacheAuraColor();
    }

    private void cacheAuraColor() {
        int c0 = mV.mColor.toInt((byte)mAuraAlpha);
        int cW = WHITENED_AURA_COLOR;

        int auraA = Util.linearInterpolation(
                            Color.alpha(c0), Color.alpha(cW),
                            0, 100,
                            mAuraWhitening);
        int auraR = Util.linearInterpolation(
                            Color.red(c0), Color.red(cW),
                            0, 100,
                            mAuraWhitening);
        int auraG = Util.linearInterpolation(
                            Color.green(c0), Color.green(cW),
                            0, 100,
                            mAuraWhitening);
        int auraB = Util.linearInterpolation(
                            Color.blue(c0), Color.blue(cW),
                            0, 100,
                            mAuraWhitening);
        mAuraColor = Color.argb(auraA, auraR, auraG, auraB);
    }

    public void setFadeAmount(int fadeAmount) {
        cachePlayerColor(fadeAmount);
    }

    private void cachePlayerColor(int fadeAmount) {
        int alpha = Util.linearInterpolation(0xff, 0,
                                          0, 100,
                                          fadeAmount);
        mBodyColor = mV.mColor.toInt((byte)alpha);
        mOutlineColor = computeOutlineColor(alpha);
    }

    private int computeOutlineColor(int alpha) {
        int l = mV.mLife;
        if (l <= 100) {
            int whiteness = (l * 0xff) / 100;
            return Color.argb(alpha, 0xff, whiteness, whiteness);
        }
        else {
            if (l > MAX_LIFE) {
                throw new RuntimeException("getOutlineColor: can't " +
                        "handle life > MAXLIFE (" + MAX_LIFE + ")");
            }
            int blueness = ((l - 100) * 0xff) / 200;
            return Color.argb(alpha, 0xff - blueness, 0xff - blueness, 0xff);
        }
    }

    /*================= Save =================*/
    public void saveState(int index, Bundle map) {
        AutoPack.autoPack(map, Util.indexToString(index), mV);
        mBrain.saveState(index, map);
        mArmory.saveState(index, map);
    }

    /*================= Lifecycle =================*/
    public static Player fromBundle(int index, Bundle map) {
        MyVars v = (MyVars)AutoPack.autoUnpack(map,
                        Util.indexToString(index), MyVars.class);
        Brain brain = Brain.fromBundle(index, map);
        Armory armory = Armory.fromBundle(index, map);
        return new Player(index, v, brain, armory);
    }

    public Player(int index, MyVars v, Brain brain, Armory armory) {
        mV = v;
        mId = index;
        mBrain = brain;
        mArmory = armory;

        mAuraAlpha = 0;
        mAuraWhitening = 0;

        cacheAuraColor();
        cachePlayerColor(0);

        // update cached value of mAngleRad
        setAngleDeg(mV.mAngleDeg);
    }
}
