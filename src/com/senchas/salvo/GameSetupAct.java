package com.senchas.salvo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;

public class GameSetupAct extends Activity {
    /*================= Constants =================*/
    public static final String GAME_SETUP_BUNDLE = "GAME_SETUP_BUNDLE";

    private static final int LAUNCH_PLAYER_SETUP_ACT = 1;

    /*================= Types =================*/

    /*================= Data =================*/
    private ModelFactory mModelFactory;

    private volatile boolean mInitialized = false;

    /*================= Operations =================*/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle map) {
        super.onCreate(map);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.game_setup_act);

        if (map == null) {
            // first-time initialization
            mModelFactory = ModelFactory.fromDefaults();
        }
        else {
            // restoring from saved bundle
            mModelFactory = ModelFactory.fromBundle(map);
        }
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
        final Spinner numRoundsSpinner =
            (Spinner)findViewById(R.id.num_rounds_spinner);
        final Spinner cashAmountSpinner =
            (Spinner)findViewById(R.id.starting_cash_spinner);
        final CheckBox randPlayer =
            (CheckBox)findViewById(R.id.randomize_player_positions);
        final Button choosePlayers =
            (Button)findViewById(R.id.choose_players);

        ////////////////// Initialize stuff
        ArrayAdapter < ModelFactory.NumRounds > numRoundsA =
            new ArrayAdapter < ModelFactory.NumRounds >
                (this,
                R.layout.game_setup_spinner_item,
                R.id.game_setup_spinner_item_text,
                ModelFactory.NumRounds.values());
        numRoundsSpinner.setAdapter(numRoundsA);
        numRoundsSpinner.setOnItemSelectedListener(
            new Spinner.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> parent,
                                    View v, int position, long id) {
                    ModelFactory.NumRounds t[] =
                        ModelFactory.NumRounds.values();
                    ModelFactory.NumRounds ty = t[position];
                    mModelFactory.setNumRounds(ty.toShort());
                }
                public void onNothingSelected(AdapterView<?> arg0) { }
            });
        numRoundsSpinner.setSelection
            (ModelFactory.NumRounds.fromShort
                (mModelFactory.getNumRounds())
                    .ordinal());

        ArrayAdapter < ModelFactory.StartingCash > cashAmountSpinnerA =
            new ArrayAdapter < ModelFactory.StartingCash >
                (this,
                R.layout.game_setup_spinner_item,
                R.id.game_setup_spinner_item_text,
                ModelFactory.StartingCash.values());
        cashAmountSpinner.setAdapter(cashAmountSpinnerA);
        cashAmountSpinner.setOnItemSelectedListener(
            new Spinner.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> parent,
                                    View v, int position, long id) {
                    ModelFactory.StartingCash c[] =
                        ModelFactory.StartingCash.values();
                    ModelFactory.StartingCash cash = c[position];
                    mModelFactory.setStartingCash(cash.toShort());
                }
                public void onNothingSelected(AdapterView<?> arg0) { }
            });
        cashAmountSpinner.setSelection
            (ModelFactory.StartingCash.fromShort
                (mModelFactory.getStartingCash())
                    .ordinal());

        randPlayer.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mModelFactory.modifyRandomPlayerPlacement
                    (randPlayer.isChecked());
            }
        });
        randPlayer.setChecked(mModelFactory.getRandomPlayerPlacement());

        choosePlayers.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent myIntent = new Intent().
                        setClass(GameSetupAct.this, PlayerSetupAct.class);
                Bundle map = new Bundle();
                onSaveInstanceState(map);
                myIntent.putExtra(GameSetupAct.GAME_SETUP_BUNDLE, map);
                startActivityForResult(myIntent, LAUNCH_PLAYER_SETUP_ACT);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        mModelFactory.saveState(b);
    }

    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode != LAUNCH_PLAYER_SETUP_ACT) {
            Log.e(this.getClass().getName(), "can't understand " + 
                        "requestCode " + requestCode);
            return;
        }
        switch (resultCode) {
            case RunGameAct.RESULT_GAME_OVER:
            case Activity.RESULT_CANCELED:
                setResult(resultCode);
                finish();
                break;
            case RunGameAct.RESULT_USER_PRESSED_BACK:
                break;
            default:
            	Log.e(this.getClass().getName(), "can't understand " + 
                        "resultCode " + resultCode);
            	break;
        }
    }
}
