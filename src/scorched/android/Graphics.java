package scorched.android;

import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;
import android.view.View;

/**
 * The graphics object does all the work of drawing stuff on the screen.
 * This class doesn't know anything about locking. It is up to the caller to
 * ensure that everything is protected properly.
 */
public enum Graphics {
    instance;

    /*================= Constants =================*/
    private final static String TAG = "Graphics";

    private final static int RED = Color.argb(255,255,0,0);

    private final static int ORANGE = Color.argb(255,255,140,0);

    /*================= Types =================*/
    static public class ViewSettings {
        /*================= Constants =================*/
        /** The zoom factor (axes are each multiplied by this when we
         *  zoom in) */
        public final static float USER_ZOOM_FACTOR = 1.8f;

        /** The maximum X coordinate we'll ever show on the screen */
        public final static float MAX_DISPLAYED_X = Model.MAX_X - 2;

        /** The user cannot pan so that the top edge of the screen is greater
         * than this coordinate. However, if the screen is zoomed out, we will
         * display higher Y coordinates than this one.
         */
        public final static float MAX_PAN_Y =
            Model.MAX_ELEVATION +
                2 * (Model.PLAYER_SIZE + Model.TURRET_LENGTH);

        /*================= Members =================*/
        /** The X-offset of the view window */
        public float mViewX;

        /** The Y-offset of the view window */
        public float mViewY;

        /** The zoom factor */
        public float mZoom;

        /*================= Accessor =================*/
        public String toString() {
            String ret = "ViewSettings(mViewX=" + mViewX +
                         " , mViewY=" + mViewY +
                         " , mZoom=" + mZoom;
            return ret;
        }

        /*================= Operations =================*/
        /** Change this ViewSettings object to be the same as 'src'
         */
        public void copyInPlace(ViewSettings src) {
            mViewX = src.mViewX;
            mViewY = src.mViewY;
            mZoom = src.mZoom;
        }

        /** Change the ViewSetting to enclose (x1, y1) and (x2, y2)
         */
        public void encloseCoords(
                    float x1, float y1, float x2, float y2,
                    float slop,
                    float canvasWidth, float canvasHeight) {
            float xMin, xMax, yMin, yMax;
            if (x1 < x2) {
                xMin = x1 - slop;
                xMax = x2 + slop;
            }
            else {
                xMin = x2 - slop;
                xMax = x1 + slop;
            }
            if (y1 < y2) {
                yMin = y1 - slop;
                yMax = y2 + slop;
            }
            else {
                yMin = y2 - slop;
                yMax = y1 + slop;
            }
            float yZ = canvasHeight / (yMax - yMin);
            float xZ = canvasWidth / (xMax - xMin);
            mutate(xMin, yMin, (yZ > xZ) ? xZ : yZ,
                    canvasWidth, canvasHeight);
        }

        /** Changes the zoom to 'newZoom', while changing mViewX and mViewY
         *  so that the view is still centered on the same point (if
         *  possible).
         *
         * @return      true if the ViewSettings were changed
         */
        public boolean zoomAndPreserveCenter(float newZoom,
                                   float canvasWidth, float canvasHeight) {
            float oldCenterX = mViewX + (canvasWidth / (2 * mZoom));
            float oldCenterY = mViewY + (canvasHeight / (2 * mZoom));
            float newCenterX = mViewX + (canvasWidth / (2 * newZoom));
            float newCenterY = mViewY + (canvasHeight / (2 * newZoom));

            return mutate(mViewX + (oldCenterX - newCenterX),
                          mViewY + (oldCenterY - newCenterY),
                          newZoom,
                          canvasWidth, canvasHeight);
        }

        /** Scrolls the ViewSetting by x, y
         *
         * @return      true if the ViewSettings were changed
         */
        public boolean scrollBy(float x, float y,
                                float canvasWidth, float canvasHeight) {
            return mutate(mViewX + x, mViewY + y, mZoom,
                            canvasWidth, canvasHeight);
        }

        /** Sets the ViewSetting. The values will be constrained to be
         * reasonable-- i.e., they won't display offscreen stuff or be zoomed
         * in or out too much.
         *
         * @return      true if the ViewSettings were changed
         */
        public boolean mutate(float nX, float nY, float nZ,
                              float canvasWidth, float canvasHeight) {
            // Make sure we don't zoom in too far
            // For now this is: if the X shows less than the size of a tank
            if (nZ > canvasWidth / Model.PLAYER_SIZE)
                nZ = canvasWidth / Model.PLAYER_SIZE;
            // Determine if new zoom level makes things too tiny
            // For now this is: if the X zoom shows more than the entire field
            else if (nZ < canvasWidth / MAX_DISPLAYED_X )
                nZ = canvasWidth / MAX_DISPLAYED_X;

            // Don't show offscreen stuff
            float newRight = nX + (canvasWidth / nZ);
            float newTop = nY + (canvasHeight /  nZ);
            if (newRight > MAX_DISPLAYED_X)
                nX = MAX_DISPLAYED_X - (canvasWidth / nZ);
            if (newTop > MAX_PAN_Y)
                nY = MAX_PAN_Y - (canvasHeight / nZ);

            // We never want to scroll the game below the bottom edge, so
            // after doing the above check, we do the below check
            if (nX < 0f)
                nX = 0f;
            if (nY < 0f)
                nY = 0f;

            // Return true only if something changed
            if (nX != mViewX || nY != mViewY || nZ != mZoom) {
                mViewX = nX;
                mViewY = nY;
                mZoom = nZ;
                return true;
            }
            else
                return false;
        }

        private float square(float f) {
            return f * f;
        }

        /** Modifies this ViewSettings to be midway between initVs and
         * finalVs. It will be curStep / maxStep of the way along.
         *
         * Note: if initVs and finalVs are 'in bounds,' then the output will be too.
         * So we don't call mutate() here.
         */
        public void interpolate(ViewSettings initVs, ViewSettings finalVs,
            final int curStep, int maxStep)
        {
            final int curStepCmp = maxStep - curStep;
            final float maxStepF = maxStep;
            mViewX = ((initVs.mViewX * curStepCmp) / maxStepF) +
                         ((finalVs.mViewX * curStep) / maxStepF);
            mViewY = ((initVs.mViewY * curStepCmp) / maxStepF) +
                        ((finalVs.mViewY * curStep) / maxStepF);

            // For zoom, don't linearly interpolate.
            // We want to stay zoomed-out (mZoom ~ small) for longer,
            // because that looks better.
            // Use some simple numerical tricks to do that...
            float maxStepSquared = maxStep * maxStep;
            float curZoomStep;
            if (initVs.mZoom > finalVs.mZoom)
                curZoomStep = square(curStep);
            else
                curZoomStep = maxStepSquared - square(curStep - maxStep);
            float curZoomStepCmp = maxStepSquared - curZoomStep;
            mZoom = ((initVs.mZoom * curZoomStepCmp) / maxStepSquared) +
                        ((finalVs.mZoom * curZoomStep) / maxStepSquared);
        }

        /*================= Lifecycle =================*/
        public ViewSettings(float viewX, float viewY, float zoom) {
            mViewX = viewX;
            mViewY = viewY;
            mZoom = zoom;
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
    private int mPlayerColors[];

    /** Thin paint to draw the players */
    private Paint mPlayerThinPaint[];

    /** Thick paint to draw the players */
    private Paint mPlayerThickPaint[];

    /** The paint for explosions */
    private Paint mExplosionPaint;

    private Path mTempPath;

    /** the current view settings */
    private ViewSettings mV;

    /** temporary space used to draw trajectory */
    private static float mTrajTemp[];

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

    public void getEnclosingViewSettings(
            float x1, float y1, float x2, float y2, float slop,
                ViewSettings out) {
        out.encloseCoords(x1, y1, x2, y2, slop, mCanvasWidth, mCanvasHeight);
    }

    public void getViewSettings(ViewSettings out) {
        out.copyInPlace(mV);
    }

    /*================= Operations =================*/
    public void setViewSettings(ViewSettings v) {
        //Log.w(TAG, "setViewSettings(v=" + v.toString() + ")");
        mV.copyInPlace(v);
    }

    public void setSurfaceSize(int width, int height) {
        mCanvasWidth = width;
        mCanvasHeight = height;
        //mNeedRedrawAll = true;
        // TODO Properly center, reposition canvas to be in bounds,
        // possibly zoom in
        // TODO somehow hook into GameState so that we can let it know
        // that we need a redraw
    }

    /** Draws the playing field */
    public void drawScreen(Canvas canvas, Model model) {
        canvas.drawColor(Color.BLACK);

        float maxX = mV.mViewX + (mCanvasWidth / mV.mZoom);
        float slotWidth = mV.mZoom;
        int firstSlot =
            boundaryCheckDrawSlot(roundDownToMultipleOfTwo(mV.mViewX));
        int lastSlot =
            boundaryCheckDrawSlot(roundDownToMultipleOfTwo(maxX) + 2);
        float x = gameXtoOnscreenX(firstSlot);
        float h[] = model.getHeights();
        for (int i = firstSlot; i < lastSlot; i += 2) {
            mTempPath.moveTo(x, gameYtoOnscreenY(h[i]));
            mTempPath.quadTo(x + slotWidth, gameYtoOnscreenY(h[i+1]),
                     x + slotWidth + slotWidth, gameYtoOnscreenY(h[i+2]));
            mTempPath.lineTo(x + slotWidth + slotWidth, mCanvasHeight);
            mTempPath.lineTo(x, mCanvasHeight);
            canvas.drawPath(mTempPath, mTerrainPaint);
            mTempPath.rewind();

            Player p1 = model.slotToPlayer(i);
            if (p1 != null) {
                drawPlayer(canvas, p1);
            }
            Player p2 = model.slotToPlayer(i+1);
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

    /** Draw the weapon's trajectory
     */
    public void drawTrajectory(Canvas canvas, Player player,
                                short curSample) {
        final float x[] = Weapon.instance.getX();
        final float y[] = Weapon.instance.getY();

        final Paint paint = mPlayerThickPaint[player.getId()];

        mTrajTemp[0] = gameXtoOnscreenX(x[0]);
        mTrajTemp[1] = gameYtoOnscreenY(y[0]);
        int i = 2;
        for (int j = 1; j <= curSample; j++) {
            mTrajTemp[i] = mTrajTemp[i+2] = gameXtoOnscreenX(x[j]);
            mTrajTemp[i+1] = mTrajTemp[i+3] = gameYtoOnscreenY(y[j]);
            i+=4;
        }
        canvas.drawLines(mTrajTemp, 0, curSample * 4, paint);
    }

    public void initializeExplosion() {
        final float centerX = Weapon.instance.getFinalX();
        final float centerY = Weapon.instance.getFinalY();
        Shader explosionShader =
            new RadialGradient(gameXtoOnscreenX(centerX),
                               gameYtoOnscreenY(centerY),
                               1, RED, ORANGE, Shader.TileMode.REPEAT);
        mExplosionPaint.setShader(explosionShader);
    }

    /** Draw an explosion
     */
    public void drawExplosion(Canvas canvas,
                              Player player, float curSize) {
        final float centerX = Weapon.instance.getFinalX();
        final float centerY = Weapon.instance.getFinalY();
        final float drawCenterX = gameXtoOnscreenX(centerX);
        final float drawCenterY = gameYtoOnscreenY(centerY);

        final Paint paint = mPlayerThickPaint[player.getId()];
        final float drawCurSize = mV.mZoom * curSize;
        canvas.drawCircle(drawCenterX, drawCenterY, drawCurSize,
                            mExplosionPaint);
    }

    /** Scroll at the user's behest.
     *
     * @return  true if the ViewSettings have been changed
     */
    public boolean userScrollBy(float x, float y) {
        boolean ret = mV.scrollBy(x, y, mCanvasWidth, mCanvasHeight);
        return ret;
    }

    /** Zooms out at the user's behest.
     *
     * @return  true if the ViewSettings have been changed
     */
    public boolean userZoomOut() {
        return mV.zoomAndPreserveCenter
                (mV.mZoom / ViewSettings.USER_ZOOM_FACTOR,
                 mCanvasWidth, mCanvasHeight);
    }

    /** Zooms in at the user's behest.
     *
     * @return  true if the ViewSettings have been changed
     */
    public boolean userZoomIn() {
        return mV.zoomAndPreserveCenter
                (mV.mZoom * ViewSettings.USER_ZOOM_FACTOR,
                 mCanvasWidth, mCanvasHeight);
    }

    /*================= Lifecycle =================*/
    /** Initialize the Graphics singleton.
     * NOTE: Context must be an Application Context,
     *       else you will leak memory.
     */
    public void initialize(Context context) {
        // Load Paints
        mClear = new Paint();
        mClear.setAntiAlias(false);
        mClear.setARGB(255, 0, 0, 0);

        mTerrainPaint = new Paint();
        mTerrainPaint.setAntiAlias(false);
        mTerrainPaint.setARGB(255, 0, 255, 0);

        // get player colors
        String playerColorStr[] = context.getResources().
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

        // calculate explosion paint
        int red = Color.argb(255,255,0,0);
        int orange = Color.argb(255,255,140,0);

        mExplosionPaint = new Paint();
        mTempPath = new Path();
        mScratchRect = new RectF(0, 0, 0, 0);
        //Resources res = context.getResources();

        // Set viewX, viewY, zoom
        mV = new ViewSettings(4, 0.5f, 15f);

        // allocate some temporary space for weapon trajectories
        mTrajTemp = new float[Weapon.MAX_SAMPLES * 4];
    }
}
