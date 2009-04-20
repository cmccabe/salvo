package scorched.android;


/**
 * Represents a game background.
 *
 *
 */
public enum Background {
    foggy_buffalo_s(R.drawable.foggy_buffalo_s),
    bridge_at_sunset_s(R.drawable.bridge_at_sunset_s),
    cloud_fortress_s(R.drawable.cloud_fortress_s),
    cumulus_s(R.drawable.cumulus_s),
    forest_sunrise_s(R.drawable.forest_sunrise_s),
    snowy_mountains_s(R.drawable.snowy_mountains_s),
    berkeley_hills_s(R.drawable.berkeley_hills_s),
    stars_s(R.drawable.stars_s);

    /*================= Static =================*/
    public static Background getRandomBackground() {
        Background bg[] = Background.values();
        int i = Util.mRandom.nextInt(bg.length);
        return bg[i];
    }

    /*================= Data =================*/
    private final int mResId;

    /*================= Access =================*/
    public int getResId() {
        return mResId;
    }

    /*================= Lifecycle =================*/
    private Background(int resId) {
        mResId = resId;
    }
}
