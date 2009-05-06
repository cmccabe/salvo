package com.senchas.salvo;
import android.graphics.Color;
import com.senchas.salvo.Background;

/**
 * Represents a game foreground.
 *
 *
 */
public enum Foreground {
    snow(Color.argb(0xff, 0xf0, 0xf0, 0xff), new Background[]
        { Background.berkeley_hills_s,
          Background.bridge_at_sunset_s,
          Background.forest_sunrise_s },
        true),
    olive(Color.argb(0xff, 0x4a, 0x63, 0x42), new Background[] {},
        false),
    brown(Color.argb(0xff, 0x55, 0x43, 0x24), new Background[]
        { Background.cloud_fortress_s },
        false),
    light_grey(Color.argb(0xff,0x6d,0x6d,0x6d), new Background[]
        { Background.berkeley_hills_s},
        false),
    dark_cyan(Color.argb(0xff,0x34,0x58,0x54), new Background[] {},
        false),
    tan(Color.argb(0xff,0xf8,0xd3,0x9c), new Background[]
        { Background.berkeley_hills_s,
          Background.forest_sunrise_s,
          Background.snowy_mountains_s },
        true),
    light_green(Color.argb(0xff,0x57,0xae,0x61), new Background[] {},
        true),
    yellow_green(Color.argb(0xff, 0x8f, 0xbd, 0x2f), new Background[]
        { Background.snowy_mountains_s,
          Background.bridge_at_sunset_s },
        true);

    /*================= Static =================*/
    public static Foreground getRandomForeground(Background curBg) {
        Foreground fg[] = Foreground.values();
        int i;

        while (true) {
            i = Util.mRandom.nextInt(fg.length);
            if (fg[i].isCompatible(curBg))
                break;
        }
        return fg[i];
    }

    /*================= Data =================*/
    private final int mColor;
    private final Background mForbiddenBg[];

    /** True if this foreground is very light. In this case, dark text will
     * be used. */
    private final boolean mIsLight;

    /*================= Access =================*/
    public int getColor() {
        return mColor;
    }

    /** Returns true only if this Foreground is compatible with the
     *  given background. */
    private boolean isCompatible(Background bg) {
        for (Background b : mForbiddenBg) {
            if (b == bg)
                return false;
        }
        return true;
    }

    public boolean isLight() {
        return mIsLight;
    }

    /*================= Lifecycle =================*/
    private Foreground(int color, Background[] forbiddenBg, boolean isLight) {
        mColor = color;
        mForbiddenBg = forbiddenBg;
        mIsLight = isLight;
    }
}
