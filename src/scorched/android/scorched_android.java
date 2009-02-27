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
import scorched.android.ScorchedView.ScorchedThread;
import android.view.ViewGroup;

public class scorched_android extends Activity {
    /*================= Constants =================*/
    private static final String TAG = "scorched_android";

    /*================= Data =================*/
    private ScorchedView mView;
    private ScorchedModel mModel;
    
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

        Log.w(TAG, "starting up2!");

        // turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Log.w(TAG, "setting content view");
        
        setContentView(R.layout.main);

        Log.w(this.getClass().getName(), "creating scorched_model");
        mModel = new ScorchedModel(5);
        //mModel.addPlayer(LocalHumanPlayer(0)) ... etc

        Log.w(this.getClass().getName(), "creating scorched_view");
        mView = (ScorchedView) findViewById(R.id.scorched_layout);

        Log.w(this.getClass().getName(), "initializing scorched_model");
        mView.initialize(mModel);

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
        mView.getThread().pause(); // pause game when Activity pauses
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
        mView.getThread().saveState(outState);
        Log.w(this.getClass().getName(), "onSaveInstanceState called");
    }
}
