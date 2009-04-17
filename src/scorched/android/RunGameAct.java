package scorched.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ZoomButton;

import scorched.android.Model;
import android.view.View.OnClickListener;

public class RunGameAct extends Activity {
    /*================= Constants =================*/

    /*================= Data =================*/
    private Object mRunGameState = new Object();
    private boolean mNeedInitialization = true;
    private GameControlView mGameControl;
    private Model mModel;

    /*================= Utility =================*/

    /*================= Operations =================*/
    @Override
    public void onCreate(Bundle map) {
        super.onCreate(map);

        if (map == null) {
            // We're starting up the game. Create the Model
            Bundle smap =
                getIntent().getBundleExtra(GameSetupAct.GAME_SETUP_BUNDLE);
            ModelFactory fac = ModelFactory.fromBundle(smap);
            mModel = fac.createModel();
        }
        else {
            // Decompress saved state
            mModel = Model.fromBundle(map);
        }

        Graphics.instance.initialize(getApplicationContext(), mModel);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.game);
    }

    @Override
    public void onStart() {
        super.onStart();

        synchronized (mRunGameState) {
            if (mNeedInitialization == true) {
                initialize();
            }
        }
    }

    /** Hook up the various views to each other.
     * Must be called after all views are finished being constructed-- i.e.
     * in onStart, not onCreate. */
    private void initialize() {
        ////////////////// Get pointers to stuff
        mGameControl = (GameControlView)findViewById(R.id.game_control_view);
        final SalvoSlider powerSlider = null;
        final SalvoSlider angleSlider = null;
        final Button fireButton = null; 
        final ZoomButton zoomIn = null;
        final ZoomButton zoomOut = null;

        ////////////////// Initialize stuff
        mGameControl.initialize(mModel, powerSlider, angleSlider);
        fireButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mGameControl.getThread().onButton(GameState.GameButton.FIRE);
            }
        });
        zoomIn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mGameControl.getThread().onButton(
                    GameState.GameButton.ZOOM_IN);
            }
        });
        zoomOut.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mGameControl.getThread().onButton(
                    GameState.GameButton.ZOOM_OUT);
            }
        });
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.w(this.getClass().getName(), "onPause called");
        //mGameControl.getThread().pause(); // pause game when
                                                // Activity pauses
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its
     *        state
     */
    @Override
    protected void onSaveInstanceState(Bundle map) {
        super.onSaveInstanceState(map);
        Log.w(this.getClass().getName(), "RunGameAct.onSaveInstanceState");
        mModel.saveState(map);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.w(this.getClass().getName(),
            "onWindowFocusChanged(" + hasFocus + ") called");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                showAreYouSureYouWantToQuit();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showAreYouSureYouWantToQuit() {
        // display alert that we can't have any more players
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage("Are you sure you want to end this game?\n" +
                    "If you want to switch apps without quitting, " +
                    "press \"HOME\"");
        b.setCancelable(false);
        b.setNegativeButton("End Game",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        b.setPositiveButton("Cancel",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    // Let's just forget we ever had this
                    // conversation, OK?
                }
            });
        b.show();
    };
}
