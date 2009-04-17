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
 * GameControlView takes input from the user and forwards it to
 * parts of the system, and presents them to Graphics and mModel.
 */
class GameControlView extends SurfaceView  {
    /*================= Operations =================*/
    //@Override
    //protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    //}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Activity runGameAct = (Activity)getContext();
        if (! runGameAct.onKeyDown())
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        Activity runGameAct = (Activity)getContext();
        return runGameAct.gameControlTouchEvent(me);
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // TODO: figure out what we should do here, if anything
    }

    /*================= Lifecycle =================*/
    public GameControlView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setFocusable(false); // make sure we get key events

        // Try to get hardware acceleration
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
}
