package scorched.android;

import java.util.Iterator;

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

    /** The X-offset of the view window */
    private float mViewX;

    /** The Y-offset of the view window */
    private float mViewY;

    /** The zoom factor */
    private float mZoom;

    /** Paint to draw the lines on screen. */
    private Paint mClear, mTerrainPaint;

    /** Paint to draw the players */
    private Paint mPlayerThinPaint[] = null;
    private Paint mPlayerThickPaint[] = null;

    private Path mTempPath;

    /** true if the screen needs to be redrawn */
    private volatile boolean mNeedScreenRedraw;

    private ScorchedModel mModel;

    private Context mContext;
    
    /*================= Static =================*/
    private static final int roundDownToMultipleOfTwo(float x) {
        int ret = (int)(x / 2);
        ret *= 2;
        return ret;
    }

    private static final int boundaryCheckDrawSlot(int slot) {
        if (slot < 0)
            return 0;
        else if (slot > (ScorchedModel.MAX_X - 2))
            return ScorchedModel.MAX_X - 2;
        else
            return slot;
    }

    /*================= Access =================*/
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


    public boolean needScreenUpdate() {
        return mNeedScreenRedraw;
    }
    
    /** Give the onscreen coordinate corresponding to x */
    private float gameXtoViewX(float x) {
        return (x - mViewX) / mZoom;
    }

    /** Give the onscreen coordinate corresponding to y */
    private float gameYtoViewY(float y) {
        return mCanvasHeight - ((y - mViewY) / mZoom);
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
        
        // Clear canvas
        mScratchRect.set(0, 0, mCanvasWidth, mCanvasHeight);
        canvas.drawRect(mScratchRect, mClear);

        // Draw the terrain
        float maxX = mViewX + (mCanvasWidth * mZoom);
        float maxY = mViewY + (mCanvasHeight * mZoom);
        float slotWidth = 1.0f / mZoom;
        int firstSlot =
            boundaryCheckDrawSlot(roundDownToMultipleOfTwo(mViewX));
        int lastSlot = 
            boundaryCheckDrawSlot(roundDownToMultipleOfTwo(maxX) + 2);

        float x = gameXtoViewX(firstSlot);
        //Log.w(TAG, "canvasWidth=" + mCanvasWidth + 
    	//		",firstSlot=" + firstSlot + 
    	//		",lastSlot=" + lastSlot +
    	//		",slotWidth=" + slotWidth +
    	//		",x="+x);
        float h[] = mModel.getHeights();
        for (int i = firstSlot; i < lastSlot; i += 2) {
            mTempPath.moveTo(x, gameYtoViewY(h[i]));
            mTempPath.quadTo(x + slotWidth, gameYtoViewY(h[i+1]),
                     x + slotWidth + slotWidth, gameYtoViewY(h[i+2]));
            mTempPath.lineTo(x + slotWidth + slotWidth, mCanvasHeight);
            mTempPath.lineTo(x, mCanvasHeight);
            canvas.drawPath(mTempPath, mTerrainPaint);
            mTempPath.rewind();
            x += (slotWidth + slotWidth);
        }
        Log.w(TAG, "finalX=" + x); 
        		
        // Draw the players
        for (int i = 0; i < mModel.getNumberOfPlayers(); i++) {
            Player p = mModel.getPlayer(i);
            drawPlayer(canvas, p);
        }
    }

    private void drawPlayer(Canvas canvas, Player p) {
        drawPlayerImpl(canvas,
                mPlayerThinPaint[p.getId()], mPlayerThickPaint[p.getId()],
                p.getAngle(),
                gameXtoViewX(p.getX()),
                gameYtoViewY(p.getY()));
    }

    /** Draws a single player */
    private void drawPlayerImpl(Canvas canvas, 
                            Paint thinPaint, Paint thickPaint, 
                            float turretAngle,
                            float tx,
                            float ty) 
    {
        final float ps = ScorchedModel.PLAYER_SIZE / mZoom;
        final float tl = ScorchedModel.TURRET_LENGTH / mZoom;
        final float t = ScorchedModel.PLAYER_SIZE / mZoom;
        float centerX = tx;
        float centerY = ty - (ps/2);

        // draw turret
        canvas.drawLine(centerX, centerY, 
                centerX + (tl * (float)Math.cos(turretAngle)),
                centerY - (tl * (float)Math.sin(turretAngle)),
                thickPaint);
        
/*        // draw dome
        Rect oldClip = canvas.getClipBounds();
        canvas.clipRect(centerX - (ps/2),
                                    centerY - (ps/2),
                                    centerX + (ps/2),
                                    centerY + (ps/2),
                                    Region.Op.REPLACE);
        mScratchRect.left = centerX - (ps/2);
        mScratchRect.right = centerX + (ps/2);
        mScratchRect.top = centerY - (ps/2);
        mScratchRect.bottom = centerY + (ps/2) + ps;
        canvas.drawOval(mScratchRect, thinPaint);
        canvas.clipRect(oldClip);*/
                
        // draw top part
        float x = tx - (ps / 2);
        float y = ty - ps;
        final float a = t / 7;
        final float b = t / 7;
        final float d = t / 6;
        final float e = t / 5;
        mTempPath.moveTo(x + a, y + d);
        mTempPath.lineTo(x + a + b, y);
        mTempPath.lineTo(x + t - (a + b), y);
        mTempPath.lineTo(x + t - (a), y + d);
        mTempPath.lineTo(x + t - (a), y + d + e);
        // canvas.drawPath(mTempPath, thinPaint);

        // draw bottom part
        final float h = t / 5;
        final float j = t / 5;
        final float k = t / 5;
        final float l = t / 6;
        final float n = t / 6;
        mTempPath.lineTo(x + n, y + d + e);
        mTempPath.lineTo(x, y + d + e + h);
        mTempPath.lineTo(x, y + d + e + h + j);
        //mTempPath.lineTo(x, y + d + e + h + j + k);
        //mTempPath.lineTo(x + t, y + d + e + h + j + k);
        mTempPath.lineTo(x + t, y + d + e + h + j);
        mTempPath.lineTo(x + t, y + d + e + h);
        mTempPath.lineTo(x + t - (n), y + d + e);
        // mTempPath.lineTo(x + n, y + d + e);
        
        // finish top part
        mTempPath.lineTo(x + a, y + d + e);
        mTempPath.lineTo(x + a, y + d);

        
        canvas.drawPath(mTempPath, thinPaint);
        mTempPath.rewind();
        canvas.drawCircle(x+n, y+d+e+h+j, a, thinPaint);
        canvas.drawCircle(x+3*n, y+d+e+h+j, a, thinPaint);
        canvas.drawCircle(x+5*n, y+d+e+h+j, a, thinPaint);
    }

    public void drawWeapon(Canvas canvas, Weapon weapon, Player player)
    {
        Iterator<Weapon.Point> iter = weapon.getPoints();
        assert (iter.hasNext());
        Weapon.Point firstPoint = (Weapon.Point)iter.next();
        Paint paint = mPlayerThickPaint[2];//player.getId()];
        float x = gameXtoViewX(firstPoint.getX());
        float y = gameYtoViewY(firstPoint.getY());
        canvas.drawCircle(x, y, 2, paint);
        //Log.w(TAG, "firstX=" + x + ",y=" + y);
        while (iter.hasNext()) {
            Weapon.Point point = (Weapon.Point)iter.next();
            x = gameXtoViewX(point.getX());
            y = gameYtoViewY(point.getY());
//            mTempPath.lineTo(x, y);
            canvas.drawCircle(x, y, 2, paint);
//            Log.w(TAG, "x=" + x + ",y=" + y);
        }
    }

    public void zoomOut() {
        mZoom = mZoom * 2f;
        mNeedScreenRedraw = true;
    }

    public void zoomIn() {
        mZoom = mZoom / 2f;
        mNeedScreenRedraw = true;
    }

    public void viewLeft() {
        mViewX -= 0.1;
        mNeedScreenRedraw = true;
    }

    public void viewRight() {
        mViewX += 0.1;
        mNeedScreenRedraw = true;
    }

    public void viewUp() {
        mViewY += 0.1;
        mNeedScreenRedraw = true;
    }

    public void viewDown() {
        mViewY -= 0.1;
        mNeedScreenRedraw = true;
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
            pthin.setAntiAlias(true);
            pthin.setColor(playerColors[i]);
            mPlayerThinPaint[i] = pthin;

            Paint pthick = new Paint();
            pthick.setAntiAlias(true);
            pthick.setColor(playerColors[i]);
            pthick.setStrokeWidth(3);
            mPlayerThickPaint[i] = pthick;
        }

        mTempPath = new Path();
        mScratchRect = new RectF(0, 0, 0, 0);
        //Resources res = context.getResources();

        mNeedScreenRedraw = false;

        // Set pan/zoom values
        mViewX = 4;
        mViewY = 0.5f;
        mZoom = 0.020f;
    }
}
