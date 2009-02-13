package scorched.android;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.drawable.Drawable;

public class scorched_android extends Activity {
    /**
     * Invoked when the Activity is created.
     * 
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.scorched_layout);

        Log.w(this.getClass().getName(), "creating scorched_view");

        mView = (ScorchedView) findViewById(R.id.scorched_view);
        mThread = mView.getThread();

        mLunarThread.setState(LunarThread.STATE_READY);

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
        mLunarThread.saveState(outState);
        Log.w(this.getClass().getName(), "onSaveInstanceState called");
    }
}
