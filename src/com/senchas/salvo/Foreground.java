package com.senchas.salvo;
import android.graphics.Color;
import com.senchas.salvo.Background;

/**
 * Represents a game foreground.
 *
 *
 */
public enum Foreground {
    olive(Color.argb(0xff, 0x4a, 0x63, 0x42), new Background[] {}),
    brown(Color.argb(0xff, 0x55, 0x43, 0x24), new Background[]
        { Background.cloud_fortress_s }),
    light_grey(Color.argb(0xff,0x6d,0x6d,0x6d), new Background[]
        { Background.berkeley_hills_s}),
    dark_cyan(Color.argb(0xff,0x34,0x58,0x54), new Background[] {}),
    light_green(Color.argb(0xff,0x57,0xae,0x61), new Background[] {}),
    bright_green(Color.argb(0xff, 0x5f, 0xcf, 0x4d), new Background[]
        { Background.snowy_mountains_s,
          Background.bridge_at_sunset_s });

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

    /*================= Lifecycle =================*/
    private Foreground(int color, Background[] forbiddenBg) {
        mColor = color;
        mForbiddenBg = forbiddenBg;
    }
}
