package scorched.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

/**
 * Graphics object for the Scorched Android game
 * 
 * The graphics object does all the work of drawing stuff on the screen.
 * This class doesn't know anything about locking. It is up to the caller to
 * ensure that everything is protected properly.
 */
public class ScorchedGraphics {
	/*================= Constants =================*/
	private final String TAG = "ScorchedGraphics";
	
    /*================= Members =================*/
    private RectF mScratchRect;

    /** Current height of the surface/canvas. */
    private int mCanvasHeight = 0;

    /** Current width of the surface/canvas. */ 
    private int mCanvasWidth = 0;

    /** Paint to draw the lines on screen. */
    private Paint mClear, mTerrainPaint;

    /** Paint to draw the players */
    private Paint mPlayerPaint[] = null;

    /** true if the screen needs to be redrawn */
    private boolean mNeedScreenRedraw;

    private ScorchedModel mModel;

    private Context mContext;
    
    /*================= Static =================*/
    private int[] getPlayerColors() {
        String playerColorStr[] = mContext.getResources().
                    getStringArray(R.array.player_colors);
        int playerColors[] = new int[playerColorStr.length];
        for (int i = 0; i < playerColorStr.length; ++i) {
            Log.w(TAG, "trying to parse color " + playerColorStr[i]);
            playerColors[i] = Color.parseColor(playerColorStr[i]);
        }
        return playerColors;
    }

    /*================= Access =================*/
    private float slotToScreenX(int slot) {
        float x = mCanvasWidth;
        x *= slot;
        x /= (ScorchedModel.MAX_HEIGHTS - 1);
        return x;
    }

    private float heightToScreenHeight(float h) {
        return mCanvasHeight - (h * mCanvasHeight); 
    }

    public boolean needScreenUpdate() {
        return mNeedScreenRedraw;
    }
    
    /** The player occupies a square area on the screen. This returns the size of the
     * square. */
    private float getPlayerSize() {
    	return 25;
    }
    
    /*================= Operations =================*/
    public void setSurfaceSize(int width, int height) {
        mCanvasWidth = width;
        mCanvasHeight = height;
        mNeedScreenRedraw = true;
    }

    /** Draws the playing field */
    public void drawScreen(Canvas canvas) {
        mNeedScreenRedraw = false;
        assert(ScorchedModel.MAX_HEIGHTS % 
                ScorchedModel.HEIGHTS_PER_POLY == 0);
        //Log.w(TAG, "running drawScreen with "
        //      "mCanvasWidth = " + mCanvasWidth +
        //      ", mCanvasHeight = " + mCanvasHeight);
        
        mScratchRect.set(0, 0, mCanvasWidth, mCanvasHeight);
        canvas.drawRect(mScratchRect, mClear);

        // Draw the terrain
        float x = 0;
        float dx = slotToScreenX(1);
        float h[] = mModel.getHeights();
        for (int i = 0; 
            i < ScorchedModel.MAX_HEIGHTS - 2;
            i += 2) 
        {
            Path p = new Path();
            p.moveTo(x, heightToScreenHeight(h[i]));
            p.quadTo(x + dx, heightToScreenHeight(h[i+1]),
                     x + dx + dx, heightToScreenHeight(h[i+2]));
            p.lineTo(x + dx + dx, mCanvasHeight);
            p.lineTo(x, mCanvasHeight);
            x += (2 * dx);
            canvas.drawPath(p, mTerrainPaint);
        }

        // Draw the players
        float playerSize = getPlayerSize();
        for (int i = 0; i < mModel.getNumberOfPlayers(); i++) {
            Player p = mModel.getPlayer(i);
            int slot = p.getSlot();
            drawPlayer(canvas, mPlayerPaint[i], 
                        slotToScreenX(slot) - (playerSize / 2),
                        heightToScreenHeight(h[slot]) - playerSize);
        }
    }

    /** Draws a single player */
    private void drawPlayer(Canvas canvas, Paint paint, float x, float y) {
        final float t = getPlayerSize();

        // draw top part
        final float a = t / 7;
        final float b = t / 7;
        final float d = t / 6;
        final float e = t / 5;
        Path p = new Path();
        p.moveTo(x + a, y + d);
        p.lineTo(x + a + b, y);
        p.lineTo(x + t - (a + b), y);
        p.lineTo(x + t - (a), y + d);
        p.lineTo(x + t - (a), y + d + e);
        p.lineTo(x + a, y + d + e);
        p.lineTo(x + a, y + d);
        canvas.drawPath(p, paint);

        // draw bottom part
        final float h = t / 5;
        final float j = t / 5;
        final float k = t / 5;
        final float l = t / 6;
        final float n = t / 6;
        Path q = new Path();
        q.moveTo(x + n, y + d + e);
        q.lineTo(x, y + d + e + h);
        q.lineTo(x, y + d + e + h + j);
        q.lineTo(x + l, y + d + e + h + j + k);
        q.lineTo(x + t - (l), y + d + e + h + j + k);
        q.lineTo(x + t, y + d + e + h + j);
        q.lineTo(x + t, y + d + e + h);
        q.lineTo(x + t - (n), y + d + e);
        q.lineTo(x + n, y + d + e);
        canvas.drawPath(q, paint);
    }

    /*================= Lifecycle =================*/
    public ScorchedGraphics(Context context, ScorchedModel model) {
        mContext = context;
    	mModel = model;
    	
    	// Load Paints
        mClear = new Paint();
        mClear.setAntiAlias(false);
        mClear.setARGB(255, 0, 0, 0);

        mTerrainPaint = new Paint();
        mTerrainPaint.setAntiAlias(false);
        mTerrainPaint.setARGB(255, 0, 255, 0);

        int playerColors[] = getPlayerColors();
        mPlayerPaint = new Paint[playerColors.length];
        for (int i = 0; i < playerColors.length; ++i) {
            Paint p = new Paint();
            p.setAntiAlias(false);
            p.setColor(playerColors[i]);
            mPlayerPaint[i] = p;
        }

        mScratchRect = new RectF(0, 0, 0, 0);
        //Resources res = context.getResources();

        mNeedScreenRedraw = false;
    }
}
