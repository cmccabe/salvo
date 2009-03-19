package scorched.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ZoomButton;

import scorched.android.Model;
import android.view.View.OnClickListener;

public class Salvo extends Activity {
    /*================= Constants =================*/
    private static final String TAG = "Salvo";

    /*================= Data =================*/

    /*================= Utility =================*/

    /*================= Operations =================*/
    /**
     * Invoked when the Activity is created.
     *
     * @param savedInstanceState a Bundle containing state saved from a
     *        previous execution, or null if this is a new execution
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Sound.instance.init(this);

        ////////////////// setContentView
        setContentView(R.layout.title);

        ////////////////// Get pointers to stuff
        final ImageView myImage =
            (ImageView)findViewById(R.id.title_picture);
        final Button newGame = (Button)findViewById(R.id.new_game);
        final Button about = (Button)findViewById(R.id.about);
        final Button buyGame = (Button)findViewById(R.id.buy_game);
        final Button help = (Button)findViewById(R.id.help);

        ////////////////// Initialize stuff
        final Activity titleActivity = this;
        newGame.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent setupIntent = new Intent().
                    setClass(titleActivity, GameSetupActivity.class);
                startActivity(setupIntent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) { }
}
