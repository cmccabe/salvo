package scorched.android;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import scorched.android.ScorchedModel;
import android.view.ViewGroup;

public class scorched_android extends Activity {
    /*================= Constants =================*/
    private static final String TAG = "scorched_android";

    /*================= Data =================*/
    private ScorchedView mWin;
    private ScorchedModel mModel;
    private ScorchedGraphics mGraphics;
    
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

        // Create Model
        //mModel.addPlayer(LocalHumanPlayer(0)) ... etc
        Player players[] = new Player[5];
        players[0] = new LocalHumanPlayer(0);
        players[1] = new ComputerPlayer(1);
        players[2] = new ComputerPlayer(2);
        players[3] = new ComputerPlayer(3);
        players[4] = new LocalHumanPlayer(4);
        mModel = new ScorchedModel(players);

        // Create View
        mGraphics = new ScorchedGraphics(getBaseContext(), mModel);

        // Create Controller / Window object
        requestWindowFeature(Window.FEATURE_NO_TITLE); // turn off title bar
        Log.w(TAG, "setContentView");
        setContentView(R.layout.main);
        Log.w(TAG, "findViewById");
        mWin = (ScorchedView) findViewById(R.id.scorched_layout);
        mWin.initialize(mModel, mGraphics);

        //Drawable redDrawable = 
            //Resources.getSystem().getDrawable(R.drawable.color_red);
        //TextView tv = (TextView)findViewByID(R.id.text);
        //tv.setBackgroundColor(redDrawable);
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mWin.getThread().pause(); // pause game when Activity pauses
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
        mWin.getThread().saveState(outState);
        Log.w(this.getClass().getName(), "onSaveInstanceState called");
    }
}
