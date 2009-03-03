package scorched.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
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
    private Paint mPlayerThinPaint[] = null;
    private Paint mPlayerThickPaint[] = null;

    /** true if the screen needs to be redrawn */
    private volatile boolean mNeedScreenRedraw;

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
    
    /** The player occupies a square area on the screen. This returns 
     * the size of the square. */
    private float getPlayerSize() {
        return slotToScreenX(3);
    }
    
    /*================= Operations =================*/
    public void setNeedScreenRedraw() {
        mNeedScreenRedraw = true;
    }

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
        for (int i = 0; i < mModel.getNumberOfPlayers(); i++) {
            Player p = mModel.getPlayer(i);
            drawPlayer(canvas, p);
        }
    }

    private void drawPlayer(Canvas canvas, Player p) {
        int slot = p.getSlot();
        drawPlayerImpl(canvas,
                    mPlayerThinPaint[p.getId()], mPlayerThickPaint[p.getId()],
                    p.getAngle(), getPlayerSize(),
                    slotToScreenX(slot),
                    heightToScreenHeight(p.getHeight()));
    }

    /** Draws a single player */
    private void drawPlayerImpl(Canvas canvas, 
                            Paint thinPaint, Paint thickPaint, 
                            float turretAngle, float playerSize,
                            float tx,
                            float ty) 
    {
        float halfPlayerSize = playerSize / 2;
        final float t = playerSize;
        float centerX = tx;
        float centerY = ty - halfPlayerSize;

        // draw turret
        canvas.drawLine(centerX, centerY, 
                centerX + (playerSize *
                    (float)Math.cos(Math.toRadians(turretAngle))),
                centerY - (playerSize *
                    (float)Math.sin(Math.toRadians(turretAngle))),
                thickPaint);
        
/*        // draw dome
        Rect oldClip = canvas.getClipBounds();
        canvas.clipRect(centerX - halfPlayerSize,
                                        centerY - halfPlayerSize,
                                        centerX + halfPlayerSize,
                                        centerY + halfPlayerSize,
                                        Region.Op.REPLACE);
        mScratchRect.left = centerX - halfPlayerSize;
        mScratchRect.right = centerX + halfPlayerSize;
        mScratchRect.top = centerY - halfPlayerSize;
        mScratchRect.bottom = centerY + halfPlayerSize + playerSize;
        canvas.drawOval(mScratchRect, thinPaint);
        canvas.clipRect(oldClip);*/
                
        // draw top part
        float x = tx - (playerSize / 2);
        float y = ty - playerSize;
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
        canvas.drawPath(p, thinPaint);

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
        canvas.drawPath(q, thinPaint);
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
        mPlayerThinPaint = new Paint[playerColors.length];
        mPlayerThickPaint = new Paint[playerColors.length];
        for (int i = 0; i < playerColors.length; ++i) {
            Paint pthin = new Paint();
            pthin.setAntiAlias(false);
            pthin.setColor(playerColors[i]);
            mPlayerThinPaint[i] = pthin;

            Paint pthick = new Paint();
            pthick.setAntiAlias(false);
            pthick.setColor(playerColors[i]);
            pthick.setStrokeWidth(7);
            mPlayerThickPaint[i] = pthick;
        }

        mScratchRect = new RectF(0, 0, 0, 0);
        //Resources res = context.getResources();

        mNeedScreenRedraw = false;
    }
}
