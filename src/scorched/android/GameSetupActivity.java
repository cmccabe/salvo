package scorched.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GameSetupActivity extends Activity {
    /*================= Constants =================*/
    private static final String TAG = "GameSetupActivity";
    public static final String PREFS_NAME = "MyPrefsFile";

    /*================= Operations =================*/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ////////////////// setContentView
        setContentView(R.layout.game_setup);

        ////////////////// Get pointers to stuff
        final Button startGame = (Button)findViewById(R.id.start_game);

        ////////////////// Initialize stuff
        final Activity titleActivity = this;
        startGame.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent setupIntent =
                    new Intent().setClass(titleActivity, RunGameActivity.class);
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



//import android.app.Activity;
//import android.content.SharedPreferences;
//
//public class Calc extends Activity {
//public static final String PREFS_NAME = "MyPrefsFile";
//    . . .
//
//    @Override
//    protected void onCreate(Bundle state){
//       super.onCreate(state);
//
//    . . .
//
//       // Restore preferences
//       SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//       boolean silent = settings.getBoolean("silentMode", false);
//       setSilent(silent);
//    }
//
//    @Override
//    protected void onStop(){
//       super.onStop();
//
//      // Save user preferences. We need an Editor object to
//      // make changes. All objects are from android.context.Context
//      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//      SharedPreferences.Editor editor = settings.edit();
//      editor.putBoolean("silentMode", mSilentMode);
//
//      // Don't forget to commit your edits!!!
//      editor.commit();
//    }
//}
