package scorched.android;

import scorched.android.RunGameAct.RunGameActAccessor;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * Controller for the Scorched Android game.
 *
 * GameControlView is the biggest view on the main game screen. The game
 * control view is the big display that contains almost all game graphics.
 *
 * This class contains mostly graphics code.
 * We forward all user input and important events up to RunGame.java and the
 * state machine so that they can be handled in a centralized and consistent
 * way.
 */
class GameControlView extends SurfaceView  {
    /*================= Constants =================*/
    /** size of mLineTemp */
    private static final int LINE_TEMP_SIZE = 80;

    /** number of coordinates needed to describe a line */
    private static final int COORDS_PER_LINE = 4;

    private static final int TURRET_STROKE_WIDTH = 3;

    private static final byte SELECTION_CIRCLE_ALPHA= (byte)0x55;

    private static final int SELECTION_CIRCLE_RADIUS = 32;

    /*================= Data =================*/
    /** temporary storage for line values */
    private float mLineTemp[];

    private Paint mTempPlayerPaint;

    private RectF mScratchRectF;

    private Path mPathTmp, mPathTmp2;

    private Bitmap mBackgroundImage;

    private Paint mForegroundPaint;

    /*================= Operations =================*/
    public void drawScreen(RunGameActAccessor acc) {
        // TODO: draw this stuff into a bitmap to speed things up?
        Canvas canvas = null;
        SurfaceHolder holder = getHolder();
        Model model = acc.getModel();
        try {
            canvas = holder.lockCanvas(null);
            drawTerrain(canvas, acc.getModel().getTerrain());
             for (Player player : model.getPlayers()) {
                 drawPlayer(canvas, model.getCurPlayerId(), player);
             }
        }
        finally {
            if (canvas != null) {
                // Don't leave the Surface in an inconsistent state
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /** Draws the terrain */
    private void drawTerrain(Canvas canvas, Terrain terrain) {
        //canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mBackgroundImage, 0, 0, null);

        short h[] = terrain.getHeights();
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
    }

    private void drawPlayer(Canvas canvas, int curPlayerId,
                            Player player) {
        final int x = player.getX();
        final int y = player.getY();
        final int sx = Player.PLAYER_X_SIZE;
        final int sy = Player.PLAYER_Y_SIZE;
        final int playerColor = player.getColor().toInt();
        final int playerBlendColor =
            player.getColor().toInt(SELECTION_CIRCLE_ALPHA);

        if (curPlayerId == player.getId()) {
            mTempPlayerPaint.setColor(playerBlendColor);
            // Draw selection circle
            canvas.drawCircle(x, y, SELECTION_CIRCLE_RADIUS,
                                mTempPlayerPaint);
        }

        // Draw turret
        mTempPlayerPaint.setColor(Color.WHITE);
        mTempPlayerPaint.setStrokeWidth(TURRET_STROKE_WIDTH +
                                        Player.BORDER_SIZE);
        final int ty = (int)(y - (sy / 4));
        float angle = player.getAngleRad();
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);
        canvas.drawLine(x, ty,
            x + ((Player.TURRET_LENGTH + Player.BORDER_SIZE) * cos),
            ty - ((Player.TURRET_LENGTH + Player.BORDER_SIZE) * sin),
            mTempPlayerPaint);

        mTempPlayerPaint.setColor(playerColor);
        mTempPlayerPaint.setStrokeWidth(TURRET_STROKE_WIDTH);
        canvas.drawLine(x, ty,
            x + (Player.TURRET_LENGTH * cos),
            ty - (Player.TURRET_LENGTH * sin),
            mTempPlayerPaint);
        mTempPlayerPaint.setStrokeWidth(1);

        // Draw tank body
        mPathTmp.moveTo(x - (sx / 2), y);
        mPathTmp.lineTo(x - (sx / 4), y - (sy / 5.5f));
        mPathTmp.lineTo(x + (sx / 4), y - (sy / 5.5f));
        mPathTmp.lineTo(x + (sx / 2), y);
        mPathTmp.lineTo(x + ((sx * 3) / 10), y + (sy / 2));
        mPathTmp.lineTo(x - ((sx * 3) / 10), y + (sy / 2));
        mPathTmp.lineTo(x - (sx / 2), y);

        mPathTmp2.moveTo(x - (sx / 4), y - (sy / 5.5f));
        mPathTmp2.lineTo(x - (sx / 4), y - (sy / 2f));
        mPathTmp2.lineTo(x + (sx / 4), y - (sy / 2f));
        mPathTmp2.lineTo(x + (sx / 4), y - (sy / 5.5f));
        mPathTmp2.lineTo(x - (sx / 4), y - (sy / 5.5f));

        mTempPlayerPaint.setStyle(Paint.Style.FILL);
        mTempPlayerPaint.setColor(playerColor);
        canvas.drawPath(mPathTmp, mTempPlayerPaint);
        canvas.drawPath(mPathTmp2, mTempPlayerPaint);

        mTempPlayerPaint.setStyle(Paint.Style.STROKE);
        mTempPlayerPaint.setColor(Color.WHITE);
        canvas.drawPath(mPathTmp, mTempPlayerPaint);
        canvas.drawPath(mPathTmp2, mTempPlayerPaint);

        mPathTmp.reset();
        mPathTmp2.reset();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Forward key events to state machine
        Activity runGameAct = (Activity)getContext();
        boolean ret = runGameAct.onKeyDown(keyCode, event);
        if (!ret)
            ret = super.onKeyDown(keyCode, event);
        return ret;
}

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        // Forward touch events to state machine
        Activity runGameAct = (Activity)getContext();
        runGameAct.onTouchEvent(me);
        return true;
    }

    /** Try to enable hardware acceleration */
    private void enableHardwareAcceleration() {
        try {
            getHolder().setType(
                android.view.SurfaceHolder.SURFACE_TYPE_HARDWARE);
            Log.w(this.getClass().getName(),
                "GameControlView: activated hardware acceleration");
        }
        catch(Exception e) {
            getHolder().setType(
                android.view.SurfaceHolder.SURFACE_TYPE_NORMAL);
            StringBuilder b = new StringBuilder(160);
            b.append("GameControlView: no acceleration ");
            b.append("(error: ").append(e.toString()).append(")");
            Log.w(this.getClass().getName(), b.toString());
        }
    }

    //@Override
    //protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    //}

    //@Override
    //public void onWindowFocusChanged(boolean hasWindowFocus) {
        // Standard window-focus override. Notice focus lost so we can pause on
        // focus lost. e.g. user switches to take a call.
        // TODO: figure out what we should do here, if anything
    //}

    /*================= Lifecycle =================*/
    /** Initialize this GameControlView.
     *
     * Must be called before trying to draw anything.
     */
    public void initialize(Background bg, Foreground fg) {
        mBackgroundImage = BitmapFactory.decodeResource
            (getContext().getResources(), bg.getResId());

        mForegroundPaint = new Paint();
        mForegroundPaint.setColor(fg.getColor());
        mForegroundPaint.setAntiAlias(false);
    }

    public GameControlView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setFocusable(false); // make sure we get key events
        enableHardwareAcceleration();

        mLineTemp = new float[LINE_TEMP_SIZE * COORDS_PER_LINE];
        mTempPlayerPaint = new Paint();
        mTempPlayerPaint.setAntiAlias(true);
        mScratchRectF = new RectF();
        mPathTmp = new Path();
        mPathTmp2 = new Path();
    }
}
