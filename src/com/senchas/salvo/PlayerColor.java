package com.senchas.salvo;

import android.graphics.Color;

/**
 * Represents a player color.
 *
 * There are only so many player colors, and they are all represented here.
 */
public enum PlayerColor {
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
