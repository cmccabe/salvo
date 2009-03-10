package scorched.android;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ZoomButton;

import scorched.android.Model;
import scorched.android.SalvoSlider.Listener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

public class Salvo extends Activity {
    /*================= Constants =================*/
    private static final String TAG = "Salvo";

    /*================= Data =================*/
    private GameControlView mGameControl;
    private Model mModel;
    private Graphics mGraphics;
    
    /*================= Utility =================*/

    /*================= Operations =================*/
    /**
     * Invoked when the Activity is created.
     * 
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //mModel.addPlayer(LocalHumanPlayer(0)) ... etc
        Player players[] = new Player[5];
        players[0] = new LocalHumanPlayer(0);
        players[1] = new ComputerPlayer(1);
        players[2] = new ComputerPlayer(2);
        players[3] = new ComputerPlayer(3);
        players[4] = new LocalHumanPlayer(4);
        mModel = new Model(players);

        mGraphics = new Graphics(getBaseContext(), mModel);

        // Create Controller / Window object
        requestWindowFeature(Window.FEATURE_NO_TITLE); // turn off title bar
        Log.w(TAG, "setContentView");
        setContentView(R.layout.main);
        Log.w(TAG, "findViewById");
        mGameControl = (GameControlView)findViewById(R.id.scorched_layout);
        mGameControl.initialize(mModel, mGraphics);

        // Sliders
        final SalvoSlider powerSlider = 
            (SalvoSlider)findViewById(R.id.PowerSlider);
        powerSlider.initialize(Player.MIN_POWER, Player.MAX_POWER,
            new SalvoSlider.Listener() {
				public void onPositionChange(int val) {
                    mGameControl.onPowerChange(val);					
				}
            });
        final SalvoSlider angleSlider = 
            (SalvoSlider)findViewById(R.id.AngleSlider);
        powerSlider.initialize(0, 180,
            new SalvoSlider.Listener() {
				public void onPositionChange(int val) {
                    mGameControl.onAngleChange(val);
				}
            });

        // Buttons
        Button fireButton = (Button)findViewById(R.id.FireButton);
        fireButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mGameControl.onFireButton();
            }
        });
        ZoomButton zoomIn = (ZoomButton)findViewById(R.id.ZoomIn);
        zoomIn.setOnClickListener(new OnClickListener() {
        	public void onClick(View arg0) {
        		mGameControl.onZoomIn();
        	}
        });
        ZoomButton zoomOut = (ZoomButton)findViewById(R.id.ZoomOut);
        zoomOut.setOnClickListener(new OnClickListener() {
        	public void onClick(View arg0) {
        		mGameControl.onZoomOut();
        	}
        });
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mGameControl.getThread().pause(); // pause game when Activity pauses
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     * 
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mGameControl.getThread().saveState(outState);
        Log.w(this.getClass().getName(), "onSaveInstanceState called");
    }
}
