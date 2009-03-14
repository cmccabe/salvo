package scorched.android;

import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

/**
 * The graphics object does all the work of drawing stuff on the screen.
 * This class doesn't know anything about locking. It is up to the caller to
 * ensure that everything is protected properly.
 */
public class Graphics {
    /*================= Constants =================*/
    private final String TAG = "Graphics";

    /*================= Types =================*/
    static public class ViewSettings implements Cloneable {
        /** The zoom factor (axes are each multiplied by this when we 
         *  zoom in) */
        public static final float ZOOM_FACTOR = 2f;

        /*================= Members =================*/
        /** The X-offset of the view window */
        public float mViewX;

        /** The Y-offset of the view window */
        public float mViewY;

        /** The zoom factor */
        public float mZoom;

        /*================= Lifecycle =================*/
        public ViewSettings(float viewX, float viewY, float zoom) {
            mViewX = viewX;
            mViewY = viewY;
            mZoom = zoom;
        }

        public ViewSettings clone() {
            return new ViewSettings(mViewX, mViewY, mZoom);
        }
    }

    /*================= Members =================*/
    private RectF mScratchRect;

    /** Current height of the surface/canvas. */
    private int mCanvasHeight = 0;

    /** Current width of the surface/canvas. */
    private int mCanvasWidth = 0;

    /** Paint to draw the lines on screen. */
    private Paint mClear, mTerrainPaint;

    /** Player colors */
    int mPlayerColors[];

    /** Thin paint to draw the players */
    private Paint mPlayerThinPaint[];

    /** Thick paint to draw the players */
    private Paint mPlayerThickPaint[];

    private Path mTempPath;

    /** true if we need to redraw the whole screen */
    private boolean mNeedRedrawAll;

    private Model mModel;

    private Context mContext;

    private ViewSettings mV;

    /*================= Static =================*/
    private static final int roundDownToMultipleOfTwo(float x) {
        int ret = (int)(x / 2);
        ret *= 2;
        return ret;
    }

    private static final int boundaryCheckDrawSlot(int slot) {
        if (slot < 0)
            return 0;
        else if (slot > (Model.MAX_X - 2))
            return Model.MAX_X - 2;
        else
            return slot;
    }

    /*================= Access =================*/
    public int getPlayerColor(int playerId) {
        return mPlayerColors[playerId];
    }

    public boolean getNeedRedrawAll() {
        return mNeedRedrawAll;
    }

    /** Give the onscreen coordinate corresponding to x */
    public float gameXtoOnscreenX(float x) {
        return (x - mV.mViewX) * mV.mZoom;
    }

    /** Give the onscreen coordinate corresponding to y */
    public float gameYtoOnscreenY(float y) {
        return mCanvasHeight - ((y - mV.mViewY) * mV.mZoom);
    }

    /** Give the game coordinate corresponding to an onscreen x */
    public float onscreenXtoGameX(float x, ViewSettings v) {
        return (x / v.mZoom) + v.mViewX;
    }

    /** Give the game coordinate corresponding to an onscreen y */
    public float onscreenYtoGameY(float y, ViewSettings v) {
        return (y / v.mZoom) + v.mViewY;
    }

    /** Get the X game coordinate of the ccenter of the viewport */
    private float getGameCenterX() {
        return mV.mViewX + (mCanvasWidth / (2 * mV.mZoom));
    }

    /** Get the Y game coordinate of the ccenter of the viewport */
    private float getGameCenterY() {
        return mV.mViewY + (mCanvasHeight / (2 * mV.mZoom));
    }

    /** Get the X game coordinate of the right edge of the viewport */
    private float getGameRightEdge() {
        return mV.mViewX + (mCanvasWidth / mV.mZoom);
    }

    /** Get the Y game coordinate of the top edge of the viewport */
    private float getGameTopEdge() {
        return mV.mViewY + (mCanvasHeight /  mV.mZoom);
    }

    public ViewSettings getViewSettings() {
        return mV.clone();
    }

    /*================= Operations =================*/
    public void scrollBy(float x, float y) {
        // Make sure we don't scroll off the edge
        float newX = mV.mViewX + x;
        float newY = mV.mViewY + y;
        float newRight = getGameRightEdge() + x;
        float newTop = getGameTopEdge() + y;
        float MaxX = Model.MAX_X - 2;
        if (newRight > MaxX)
            newX = MaxX - (mCanvasWidth / mV.mZoom);
        float MaxY = Model.MAX_ELEVATION + 
            2*(Model.PLAYER_SIZE + Model.TURRET_LENGTH);
        if (newTop > MaxY)
            newY = MaxY - (mCanvasHeight / mV.mZoom);

        // We never want to scroll the game below the bottom edge, so 
        // after doing the above check, we do the below check
        if (newX < 0f)
            newX = 0f;
        if (newY < 0f)
            newY = 0f;
        if (newX != mV.mViewX || newY != mV.mViewY) {
            mNeedRedrawAll = true;
            mV.mViewX = newX;
            mV.mViewY = newY;
        }
    }

    public void setNeedRedrawAll() {
        mNeedRedrawAll = true;
    }

    public void clearNeedRedrawAll() {
        mNeedRedrawAll = false;
    }

    public void setSurfaceSize(int width, int height) {
        mCanvasWidth = width;
        mCanvasHeight = height;
        mNeedRedrawAll = true;
        // TODO Properly center, reposition canvas to be in bounds, 
        // possibly zoom in
    }

    /** Draws the playing field */
    public void drawScreen(Canvas canvas) {
        // Only redraw if we need to
        if (!mNeedRedrawAll) {
            return;
        }

        canvas.drawColor(Color.BLACK);

        float maxX = mV.mViewX + (mCanvasWidth / mV.mZoom);
        float slotWidth = mV.mZoom;
        int firstSlot =
            boundaryCheckDrawSlot(roundDownToMultipleOfTwo(mV.mViewX));
        int lastSlot =
            boundaryCheckDrawSlot(roundDownToMultipleOfTwo(maxX) + 2);
        float x = gameXtoOnscreenX(firstSlot);
        float h[] = mModel.getHeights();
        for (int i = firstSlot; i < lastSlot; i += 2) {
            mTempPath.moveTo(x, gameYtoOnscreenY(h[i]));
            mTempPath.quadTo(x + slotWidth, gameYtoOnscreenY(h[i+1]),
                     x + slotWidth + slotWidth, gameYtoOnscreenY(h[i+2]));
            mTempPath.lineTo(x + slotWidth + slotWidth, mCanvasHeight);
            mTempPath.lineTo(x, mCanvasHeight);
            canvas.drawPath(mTempPath, mTerrainPaint);
            mTempPath.rewind();

            Player p1 = mModel.slotToPlayer(i);
            if (p1 != null) {
                drawPlayer(canvas, p1);
            }
            Player p2 = mModel.slotToPlayer(i+1);
            if (p2 != null ) {
                drawPlayer(canvas, p2);
            }
            x += (slotWidth + slotWidth);
        }
    }

    private void drawPlayer(Canvas canvas, Player p) {
        drawPlayerImpl(canvas,
                mPlayerThinPaint[p.getId()], mPlayerThickPaint[p.getId()],
                p.getAngleRad(),
                gameXtoOnscreenX(p.getX()),
                gameYtoOnscreenY(p.getY()));
    }

    /** Draws a single player */
    private void drawPlayerImpl(Canvas canvas,
                            Paint thinPaint, Paint thickPaint,
                            float turretAngle,
                            float tx,
                            float ty)
    {
        final float ps = Model.PLAYER_SIZE * mV.mZoom;
        final float tl = Model.TURRET_LENGTH * mV.mZoom;
        final float t = Model.PLAYER_SIZE * mV.mZoom;
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
        //final float k = t / 5;
        //final float l = t / 6;
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

    public void drawTrajectory(Canvas canvas, Player player,
                                float x[], float y[], short numSamples) {
        Paint paint = mPlayerThickPaint[player.getId()];
        int start;
        if (mNeedRedrawAll)
            start = 0;
        else {
            start = numSamples - 2;
            if (start < 0)
                return;
        }

        float prevX = gameXtoOnscreenX(x[start]);
        float prevY = gameYtoOnscreenY(y[start]);
        for (int i = start + 1; i < numSamples; ++i) {
            mTempPath.moveTo(prevX, prevY);
            float curX = gameXtoOnscreenX(x[i]);
            float curY = gameYtoOnscreenY(y[i]);
            mTempPath.lineTo(curX, curY);
            canvas.drawPath(mTempPath, paint);
            mTempPath.rewind();
            prevX = curX;
            prevY = curY;
        }
    }

    private void applyZoom(float newZoom) {
        float oldCenterX = getGameCenterX();
        float oldCenterY = getGameCenterY();
        mV.mZoom = newZoom;
        float newCenterX = getGameCenterX();
        float newCenterY = getGameCenterY();

        mNeedRedrawAll = true;
        scrollBy(oldCenterX - newCenterX, oldCenterY - newCenterY);
    }

    public void zoomOut() {
        // Determine if new zoom level makes things too tiny
        // For now this is: if the X zoom shows more than the entire field
        float newZoom = mV.mZoom / ViewSettings.ZOOM_FACTOR;
        if (mCanvasWidth / newZoom > Model.MAX_X - 2) return;
        applyZoom(newZoom);
    }

    public void zoomIn() {
        // Make sure we don't zoom in too far
        // For now this is: if the X shows less than the size of a tank
        float newZoom = mV.mZoom * ViewSettings.ZOOM_FACTOR;
        if (mCanvasWidth / newZoom < Model.PLAYER_SIZE) return;
        applyZoom(newZoom);
    }

    /*================= Lifecycle =================*/
    public Graphics(Context context, Model model) {
        mContext = context;
        mModel = model;

        // Load Paints
        mClear = new Paint();
        mClear.setAntiAlias(false);
        mClear.setARGB(255, 0, 0, 0);

        mTerrainPaint = new Paint();
        mTerrainPaint.setAntiAlias(false);
        mTerrainPaint.setARGB(255, 0, 255, 0);

        // get player colors
        String playerColorStr[] = mContext.getResources().
                    getStringArray(R.array.player_colors);
        mPlayerColors = new int[playerColorStr.length];
        for (int i = 0; i < playerColorStr.length; ++i) {
            mPlayerColors[i] = Color.parseColor(playerColorStr[i]);
        }

        // calculate player paints
        mPlayerThinPaint = new Paint[mPlayerColors.length];
        mPlayerThickPaint = new Paint[mPlayerColors.length];
        for (int i = 0; i < mPlayerColors.length; ++i) {
            Paint pthin = new Paint();
            pthin.setAntiAlias(true);
            pthin.setColor(mPlayerColors[i]);
            mPlayerThinPaint[i] = pthin;

            Paint pthick = new Paint();
            pthick.setAntiAlias(true);
            pthick.setColor(mPlayerColors[i]);
            pthick.setStrokeWidth(3);
            mPlayerThickPaint[i] = pthick;
        }

        mTempPath = new Path();
        mScratchRect = new RectF(0, 0, 0, 0);
        //Resources res = context.getResources();

        mNeedRedrawAll = false;

        // Set viewX, viewY, zoom
        mV = new ViewSettings(4, 0.5f, 15f);
    }
}
