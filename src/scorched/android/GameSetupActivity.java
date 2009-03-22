package scorched.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

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
        final Button play = (Button)findViewById(R.id.play);
        final Button addPlayer = (Button)findViewById(R.id.add_player);
        final ListView playerList = (ListView)findViewById(R.id.player_list);

        ////////////////// Initialize stuff
        final Activity titleActivity = this;
        play.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent setupIntent =
                    new Intent().setClass(titleActivity,
                        RunGameActivity.class);
                    startActivity(setupIntent);
            }
        });
        String arr[] = { "ahoy", "matey", "want", "some", "grog",
                        "now", "or", "what", "do", "you", "want",
                        "scurvy", "landlubber" };
        ArrayAdapter<String> myArrayAdaptor =
            new ArrayAdapter<String>(getBaseContext(),
                R.layout.new_player_list_item, R.id.row_text, arr);
        playerList.setAdapter(myArrayAdaptor);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) { }
}

//public SharedPreferences getPreferences(int mode)

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
