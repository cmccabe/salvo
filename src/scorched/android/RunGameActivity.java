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

public class RunGameActivity extends Activity {
    /*================= Constants =================*/
    private static final String TAG = "RunGameActivity";

    /*================= Data =================*/
    private Object mRunGameState = new Object();
    private boolean mNeedInitialization = true;
    private GameControlView mGameControl;
    private Model mModel;

    /*================= Utility =================*/

    /*================= Operations =================*/
    /**
     * Invoked when the Activity is created.
     *
     * @param savedInstanceState a Bundle containing state saved from
     *        a previous execution, or null if this is a new execution
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.w(this.getClass().getName(), "onCreate called");

        Player players[] = new Player[5];
        players[0] = new LocalHumanPlayer(0, "a", Player.PlayerColor.BLUE);
        players[1] = new ComputerPlayer(1, "b", Player.PlayerColor.GREEN);
        players[2] = new ComputerPlayer(2, "c", Player.PlayerColor.PURPLE);
        players[3] = new ComputerPlayer(3, "d", Player.PlayerColor.RED);
        players[4] = new LocalHumanPlayer(4, "e", Player.PlayerColor.YELLOW);
        mModel = new Model(players);
        Graphics.instance.initialize(getApplicationContext(), mModel);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.w(TAG, "setContentView");
        setContentView(R.layout.game);

        // todo: create modelFactory or model out of savedInstanceState
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
        final SalvoSlider powerSlider =
            (SalvoSlider)findViewById(R.id.PowerSlider);
        final SalvoSlider angleSlider =
            (SalvoSlider)findViewById(R.id.AngleSlider);
        final Button fireButton = (Button)findViewById(R.id.FireButton);
        final ZoomButton zoomIn = (ZoomButton)findViewById(R.id.ZoomIn);
        final ZoomButton zoomOut = (ZoomButton)findViewById(R.id.ZoomOut);

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
    protected void onSaveInstanceState(Bundle b) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(b);
        mGameControl.getThread().saveState(b);
        Log.w(this.getClass().getName(), "onSaveInstanceState called");
    }

    @Override
    protected void onRestoreInstanceState(Bundle b) {
        Log.w(this.getClass().getName(), "onRestoreInstanceState called");
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
