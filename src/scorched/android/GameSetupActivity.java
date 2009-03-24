package scorched.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;

public class GameSetupActivity extends Activity {
    /*================= Constants =================*/
    public static final String PREFS_NAME = "MyPrefsFile";

    /*================= Types =================*/
    private class NewPlayerListener implements View.OnClickListener {
        public void onClick(View v) {
            if (mModelFactory.canAddPlayer()) {
                // TODO
            }
            else {
                // display alert that we can't have any more players
                AlertDialog.Builder b = new AlertDialog.
                    Builder(GameSetupActivity.this);
                b.setMessage("You already have " + Model.MAX_PLAYERS +
                            " players. You can't add any more.");
                b.setCancelable(true);
                b.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            setResult(RESULT_OK);
                        }
                    });

                b.show();
            }
        }

    };

    /*================= Data =================*/
    private ModelFactory mModelFactory;

    private volatile boolean mInitialized = false;

    /*================= Operations =================*/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.game_setup);

        mModelFactory = new ModelFactory(); //(savedInstanceState);
    }

    /** Called when the views are ready to be displayed */
    @Override
    public void onStart() {
        super.onStart();
        if (mInitialized)
            return;
        else
            mInitialized = true;

        ////////////////// Get pointers to stuff
        final Button play = (Button)findViewById(R.id.play);
        final Button addPlayer = (Button)findViewById(R.id.add_player);
        final CheckBox randPlayer =
            (CheckBox)findViewById(R.id.randomize_player_positions);
        final Spinner terrainSpinner =
            (Spinner)findViewById(R.id.terrain_spinner);
        final ListView playerList = (ListView)findViewById(R.id.player_list);

        ////////////////// Initialize stuff]
        final Activity titleActivity = this;
        play.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent setupIntent =
                    new Intent().setClass(titleActivity,
                        RunGameActivity.class);
                    startActivity(setupIntent);
            }
        });

        addPlayer.setOnClickListener(new NewPlayerListener());


        randPlayer.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mModelFactory.modifyRandomPlayerPlacement
                    (randPlayer.isChecked());
            }
        });

        ArrayAdapter<String> spinnerAdapter =
            new ArrayAdapter < String >(getBaseContext(),
                R.layout.terrain_spinner_item, R.id.terrain_type,
                Model.TerrainType.getStrings());
        terrainSpinner.setAdapter(spinnerAdapter);

        // initialize ListView
        playerList.setAdapter(mModelFactory.getPlayerListAdapter());
        playerList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
              public void onItemClick(AdapterView<?> parent, View view,
                  int position, long id) {
                int selectedPosition = parent.getSelectedItemPosition();
                Log.i("SampleApp",
                        "Click on position"+selectedPosition +
                        "position=" + position + "id=" + id);
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

/*
public class SeparatedListAdapter extends BaseAdapter {
    public final Map<String,Adapter> sections = new LinkedHashMap<String,Adapter>();
    public final ArrayAdapter<String> headers;
    public final static int TYPE_SECTION_HEADER = 0;

    public SeparatedListAdapter(Context context) {
        headers = new ArrayAdapter<String>(context, R.layout.list_header);
    }

    public Object getItem(int position) {
        for(Object section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0) return section;
            if(position < size) return adapter.getItem(position - 1);

            // otherwise jump into next section
            position -= size;
        }
        return null;
    }

    public int getCount() {
        // total together all sections, plus one for each section header
        int total = 0;
        for(Adapter adapter : this.sections.values())
            total += adapter.getCount() + 1;
        return total;
    }

    public int getViewTypeCount() {
        // assume that headers count as one, then total all sections
        int total = 1;
        for(Adapter adapter : this.sections.values())
            total += adapter.getViewTypeCount();
        return total;
    }

    public int getItemViewType(int position) {
        int type = 1;
        for(Object section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0) return TYPE_SECTION_HEADER;
            if(position < size) return type + adapter.getItemViewType(position - 1);

            // otherwise jump into next section
            position -= size;
            type += adapter.getViewTypeCount();
        }
        return -1;
    }

    public boolean areAllItemsSelectable() {
        return false;
    }

    public boolean isEnabled(int position) {
        return (getItemViewType(position) != TYPE_SECTION_HEADER);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int sectionnum = 0;
        for(Object section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0) return headers.getView(sectionnum, convertView, parent);
            if(position < size) return adapter.getView(position - 1, convertView, parent);

            // otherwise jump into next section
            position -= size;
            sectionnum++;
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}

*/

/*
        String arr[] = { "ahoy", "matey", "want", "some", "grog",
                        "now", "or", "what", "do", "you", "want",
                        "scurvy", "landlubber" };
        ArrayAdapter<String> myArrayAdaptor =
            new ArrayAdapter<String>(getBaseContext(),
                R.layout.new_player_list_item, R.id.player_name, arr);
        playerList.setAdapter(myArrayAdaptor);
*/
