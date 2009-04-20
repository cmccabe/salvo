package scorched.android;

import java.util.Iterator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private final static int PURE_RED = Color.argb(255,255,0,0);

    private final static int PURE_ORANGE = Color.argb(255,255,140,0);

    /** size of mLineTemp */
    private static final int LINE_TEMP_SIZE = 80;

    /** number of coordinates needed to describe a line */
    private static final int COORDS_PER_LINE = 4;
    /*================= Types =================*/

    /*================= Members =================*/
    private Bitmap mBackgroundImage;

    private Background mBackground;

    private Foreground mForeground;
    
    private RectF mScratchRect;

    /** Current height of the surface/canvas. */
    private int mCanvasHeight = 0;

    /** Current width of the surface/canvas. */
    private int mCanvasWidth = 0;

    /** Paint to draw the lines on screen. */
    private Paint mClear, mForegroundPaint;

    /** Thin paint to draw the players */
    private Paint mPlayerThinPaint[];

    /** Thick paint to draw the players */
    private Paint mPlayerThickPaint[];

    /** The paint for explosions */
    private Paint mExplosionPaint;

    private Path mTempPath;

    /** temporary storage for line values */
    float mLineTemp[];

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
        else if (slot > (Terrain.MAX_X - 2))
            return Terrain.MAX_X - 2;
        else
            return slot;
    }

    /*================= Access =================*/

    /*================= Operations =================*/
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
        //canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mBackgroundImage, 0, 0, null);

        short h[] = model.getHeights();
        for (int x = 0; x < Terrain.MAX_X; x += LINE_TEMP_SIZE) {
            int j = 0;
            for (int i = 0; i < LINE_TEMP_SIZE; i++) {
                mLineTemp[j] = x + i;
                j++;
                mLineTemp[j] = h[x + i];
                j++;
                mLineTemp[j] = x + i;
                j++;
                mLineTemp[j] = Terrain.MAX_Y;
                j++;
            }
            canvas.drawLines(mLineTemp, 0, LINE_TEMP_SIZE * COORDS_PER_LINE,
                             mForegroundPaint);
        }

//        for (Player p : model.getPlayers()) {
//            drawPlayer(canvas, p);
//        }
    }

//    private void drawPlayer(Canvas canvas, Player p) {
//        drawPlayerImpl(canvas,
//                mPlayerThinPaint[p.getId()], mPlayerThickPaint[p.getId()],
//                p.getAngleRad(),
//                gameXtoOnscreenX(p.getX()),
//                gameYtoOnscreenY(p.getY()));
//    }
//
//    /** Draws a single player */
//    private void drawPlayerImpl(Canvas canvas,
//                            Paint thinPaint, Paint thickPaint,
//                            float turretAngle,
//                            float tx,
//                            float ty)
//    {
//        final float ps = Model.PLAYER_SIZE * mV.mZoom;
//        final float tl = Model.TURRET_LENGTH * mV.mZoom;
//        final float t = Model.PLAYER_SIZE * mV.mZoom;
//        float centerX = tx;
//        float centerY = ty - (ps/2);
//
//        // draw turret
//        canvas.drawLine(centerX, centerY,
//                centerX + (tl * (float)Math.cos(turretAngle)),
//                centerY - (tl * (float)Math.sin(turretAngle)),
//                thickPaint);
//
///*        // draw dome
//        Rect oldClip = canvas.getClipBounds();
//        canvas.clipRect(centerX - (ps/2),
//                                    centerY - (ps/2),
//                                    centerX + (ps/2),
//                                    centerY + (ps/2),
//                                    Region.Op.REPLACE);
//        mScratchRect.left = centerX - (ps/2);
//        mScratchRect.right = centerX + (ps/2);
//        mScratchRect.top = centerY - (ps/2);
//        mScratchRect.bottom = centerY + (ps/2) + ps;
//        canvas.drawOval(mScratchRect, thinPaint);
//        canvas.clipRect(oldClip);*/
//
//        // draw top part
//        float x = tx - (ps / 2);
//        float y = ty - ps;
//        final float a = t / 7;
//        final float b = t / 7;
//        final float d = t / 6;
//        final float e = t / 5;
//        mTempPath.moveTo(x + a, y + d);
//        mTempPath.lineTo(x + a + b, y);
//        mTempPath.lineTo(x + t - (a + b), y);
//        mTempPath.lineTo(x + t - (a), y + d);
//        mTempPath.lineTo(x + t - (a), y + d + e);
//        // canvas.drawPath(mTempPath, thinPaint);
//
//        // draw bottom part
//        final float h = t / 5;
//        final float j = t / 5;
//        //final float k = t / 5;
//        //final float l = t / 6;
//        final float n = t / 6;
//        mTempPath.lineTo(x + n, y + d + e);
//        mTempPath.lineTo(x, y + d + e + h);
//        mTempPath.lineTo(x, y + d + e + h + j);
//        //mTempPath.lineTo(x, y + d + e + h + j + k);
//        //mTempPath.lineTo(x + t, y + d + e + h + j + k);
//        mTempPath.lineTo(x + t, y + d + e + h + j);
//        mTempPath.lineTo(x + t, y + d + e + h);
//        mTempPath.lineTo(x + t - (n), y + d + e);
//        // mTempPath.lineTo(x + n, y + d + e);
//
//        // finish top part
//        mTempPath.lineTo(x + a, y + d + e);
//        mTempPath.lineTo(x + a, y + d);
//
//
//        canvas.drawPath(mTempPath, thinPaint);
//        mTempPath.rewind();
//        canvas.drawCircle(x+n, y+d+e+h+j, a, thinPaint);
//        canvas.drawCircle(x+3*n, y+d+e+h+j, a, thinPaint);
//        canvas.drawCircle(x+5*n, y+d+e+h+j, a, thinPaint);
//    }

    /*================= Lifecycle =================*/
    /** Initialize the Graphics singleton.
     * NOTE: Context must be an Application Context,
     *       else you will leak memory.
     * NOTE: We don't hang on to any references to the model
     */
    public void initialize(Context context, Model model) {
        // Load Paints
        mClear = new Paint();
        mClear.setAntiAlias(false);
        mClear.setARGB(255, 0, 0, 0);

        // get player colors
        Player players[] = model.getPlayers();
        int playerColors[] = new int[players.length];
        for (int i = 0; i < playerColors.length; i++)
            playerColors[i] = players[i].getColor().toInt();

        // calculate player paints
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

        mExplosionPaint = new Paint();
        mTempPath = new Path();
        mScratchRect = new RectF(0, 0, 0, 0);
        //Resources res = context.getResources();

        // allocate some temporary space for weapon trajectories
        mTrajTemp = new float[Weapon.MAX_SAMPLES * 4];

        mLineTemp = new float[LINE_TEMP_SIZE * COORDS_PER_LINE];

        mBackground = Background.getRandomBackground();
        mBackgroundImage = BitmapFactory.decodeResource
            (context.getResources(), mBackground.getResId());

        mForeground = Foreground.getRandomForeground(mBackground);
        mForegroundPaint = new Paint();
        mForegroundPaint.setColor(mForeground.getColor());
        mForegroundPaint.setAntiAlias(false);
    }
}
