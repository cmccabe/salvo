package scorched.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
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
    /*================= Operations =================*/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Forward key events to state machine
        Activity runGameAct = (Activity)getContext();
        if (! runGameAct.onKeyDown())
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        // Forward touch events to state machine
        Activity runGameAct = (Activity)getContext();
        runGameAct.gameControlTouchEvent(me);
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
    public GameControlView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setFocusable(false); // make sure we get key events
        enableHardwareAcceleration();
    }
}
