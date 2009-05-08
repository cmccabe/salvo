package com.senchas.salvo;

import java.util.HashSet;
import java.util.LinkedList;

import com.senchas.salvo.ModelFactory.PlayerFactory;
import com.senchas.salvo.Player.PlayerColor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

public class PlayerSetupAct extends Activity {
    /*================= Constants =================*/

    /*================= Types =================*/

    /*================= Data =================*/
    private ModelFactory mModelFactory;

    private volatile PlayerFactory mCurPlayer;

    private volatile boolean mInitialized;

    private ListView mPlayerList;
    private Button mAddPlayerButton;
    private Button mDeletePlayerButton;

    private EditText mPlayerName;
    private ArrayAdapter < PlayerColor > mPlayerColorSpinnerA;
    private Spinner mPlayerColorSpinner;
    private Spinner mPlayerTypeSpinner;
    private EditText mLife;

    /*================= Operations =================*/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle map) {
        super.onCreate(map);

        Log.w(this.getClass().getName(), "onCreate");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.player_setup_act);

        mInitialized = false;
        if (map == null)
            map = getIntent().getBundleExtra(GameSetupAct.GAME_SETUP_BUNDLE);
        mModelFactory = ModelFactory.fromBundle(map);
        mCurPlayer = mModelFactory.getPlayerFactory(0);
        // Wait until onStart to hook up the callbacks and listeners.
        // We want to wait for everything to settle down.
    }

    /** Called when the views are ready to be displayed */
    @Override
    public void onStart() {
        super.onStart();
        Log.w(this.getClass().getName(),
            "onStart (mInitialized = " + mInitialized + ")");
        if (mInitialized)
            return;
        else
            mInitialized = true;

        ////////////////// Get pointers to stuff
        mPlayerList = (ListView)findViewById(R.id.player_list);
        mAddPlayerButton = (Button)findViewById(R.id.add_player);
        mDeletePlayerButton = (Button)findViewById(R.id.delete_player);

        mPlayerName = (EditText)findViewById(R.id.player_name);
        mPlayerColorSpinner =
            (Spinner)findViewById(R.id.player_color_spinner);
        mPlayerTypeSpinner =
            (Spinner)findViewById(R.id.player_type_spinner);
        final Button lifeMinus =
            (Button)findViewById(R.id.player_life_minus);
        mLife = (EditText)findViewById(R.id.player_life);
        final Button lifePlus =
            (Button)findViewById(R.id.player_life_plus);
        final Button play = (Button)findViewById(R.id.play);

        ////////////////// Initialize stuff
        mPlayerList.setAdapter(mModelFactory.getPlayerListAdapter());
        mPlayerList.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //parent.getSelectedItemPosition();
                try {
                    selectPlayer(mModelFactory.getPlayerFactory(position));
                }
                catch (Exception e) {
                    StringBuilder b = new StringBuilder(80);
                    b.append("mPlayerList.OnItemClickListener: ");
                    b.append("got exception: ");
                    b.append(e.toString());
                    Log.e(this.getClass().getName(), b.toString());
                }
            }
        });

        mAddPlayerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mModelFactory.canAddPlayer())
                    selectPlayer(mModelFactory.addPlayerFactory());
                else {
                    // display alert that we can't have any more players
                    StringBuilder b = new StringBuilder(80);
                    b.append("You already have ").append(Model.MAX_PLAYERS);
                    b.append(" players. You can't add any more.");
                    doDialog_noChoices(b.toString());
                }
            }
        });

        mPlayerName.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s,
                                        int start, int before, int count) {
            }

            public void afterTextChanged(Editable arg0) {
                String newName = mPlayerName.getText().toString();
                if (newName.length() != 0) {
                    mCurPlayer.setName(newName);
                    mModelFactory.notifyDataSetChanged();
                }
            }

            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) { }
        });

        mPlayerColorSpinnerA =
            new ArrayAdapter < PlayerColor >(this,
                R.layout.player_spinner_item,
                R.id.player_spinner_item_text);
        mPlayerColorSpinner.setAdapter(mPlayerColorSpinnerA);
        mPlayerColorSpinner.setOnItemSelectedListener(
            new Spinner.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> parent,
                                    View v, int position, long id) {
                    PlayerColor color =
                        mPlayerColorSpinnerA.getItem(position);

                    // Strictly speaking, this check shouldn't be necessary
                    // because the spinner shouldn't contain colors that
                    // are in use. However, double check just in case
                    // there is a race condition or something.
                    if (mModelFactory.colorInUse(color)) {
                        mPlayerColorSpinner.setSelection(0);
                    }
                    else {
                        mCurPlayer.setColor(color);
                        mModelFactory.notifyDataSetChanged();
                    }
                }
                public void onNothingSelected(AdapterView<?> arg0) { }
            });

        ArrayAdapter < BrainFactory > playerTypeSpinnerA =
            new ArrayAdapter < BrainFactory >(this,
                R.layout.player_spinner_item,
                R.id.player_spinner_item_text,
                BrainFactory.values());
        mPlayerTypeSpinner.setAdapter(playerTypeSpinnerA);
        mPlayerTypeSpinner.setOnItemSelectedListener(
            new Spinner.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> parent,
                                    View v, int position, long id) {
                    BrainFactory t[] = BrainFactory.values();
                    BrainFactory ty = t[position];
                    mCurPlayer.setBrainFactory(ty);
                    mModelFactory.notifyDataSetChanged();
                }
                public void onNothingSelected(AdapterView<?> arg0) { }
            });

        lifeMinus.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                setLifeField((short) (mCurPlayer.getLife() - 25), true);
                mModelFactory.notifyDataSetChanged();
            }
        });
        lifePlus.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                setLifeField((short) (mCurPlayer.getLife() + 25), true);
                mModelFactory.notifyDataSetChanged();
            }
        });

        mDeletePlayerButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                try {
                    mCurPlayer = mModelFactory.
                        deletePlayerFactory(mCurPlayer);
                    mModelFactory.notifyDataSetChanged();
                    selectPlayer(mCurPlayer);
                }
                catch (ModelFactory.TooFewPlayers e) {
                    StringBuilder b = new StringBuilder(160);
                    b.append("Can't delete player: you can't play with ");
                    b.append("fewer than ");
                    b.append(Model.MIN_PLAYERS);
                    b.append(" players");
                    doDialog_noChoices(b.toString());
                }
                catch (Exception e) {
                    StringBuilder b = new StringBuilder(160);
                    b.append("deletePlayer: exception: ");
                    b.append(e.toString());
                    Log.e(this.getClass().getName(), b.toString());
                }
            }
        });

        play.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (mModelFactory.everyoneIsAComputer())
                    doDialog_warnAllCpu();
                else
                    launchRunGameActivity();
            }
        });

        selectPlayer(mModelFactory.getPlayerFactory(0));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        mModelFactory.saveState(b);
    }

    /** Set the current selected player's life
     *
     * @param life              The new life amount. Will be bounds checked.
     * @param setTextView       If true, we'll change the play life text
    */
    private void setLifeField(short life, boolean setTextView) {
        if (life < Player.MIN_STARTING_LIFE) {
            life = Player.MIN_STARTING_LIFE;
            setTextView = true;
        }
        else if (life > Player.MAX_STARTING_LIFE) {
            life = Player.MAX_STARTING_LIFE;
            setTextView = true;
        }
        mCurPlayer.setLife(life);
        mModelFactory.notifyDataSetChanged();
        if (setTextView) {
            StringBuilder b = new StringBuilder(80);
            b.append(life);
            b.append("% life");
            mLife.setText(b.toString());
        }
    }

    /** Change the selected player to playerNum */
    private void selectPlayer(PlayerFactory p) {
        int oldPos = mModelFactory.getPlayerPosition(mCurPlayer);
        mPlayerList.setItemChecked(oldPos, false);

        mCurPlayer = p;

        // Set the display on the right side of the screen
        mPlayerTypeSpinner.setSelection
            (mCurPlayer.getBrainFactory().ordinal());
        setLifeField(mCurPlayer.getLife(), true);
        LinkedList < PlayerColor > availColors =
            mModelFactory.getAvailableColors();
        mPlayerColorSpinnerA.clear();
        mPlayerColorSpinnerA.add(mCurPlayer.getColor());
        for (PlayerColor color: availColors) {
            mPlayerColorSpinnerA.add(color);
        }
        mPlayerColorSpinner.setAdapter(mPlayerColorSpinnerA);
        mPlayerColorSpinner.setSelection(0);

        mPlayerName.setText(mCurPlayer.getName());
        int pos = mModelFactory.getPlayerPosition(p);
        mPlayerList.setSelection(pos);
        mPlayerList.setItemChecked(pos, true);
        mAddPlayerButton.setEnabled(mModelFactory.canAddPlayer());
        mDeletePlayerButton.setEnabled(mModelFactory.canDeletePlayer());
    }

    /** Warn about the dangers of an all-CPU world */
    private void doDialog_warnAllCpu() {
        // display alert that we can't have any more players
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage("You have not created any human players.\n" +
                    "Are you sure you want to watch the computer fight " +
                    "itself?");
        b.setCancelable(true);
        b.setNegativeButton("Edit Players",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                }
            });
        b.setPositiveButton("Begin Game",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    launchRunGameActivity();
                }
            });
        b.show();
    };

    /** Displays a simple modal alert dialog box with no choices */
    private void doDialog_noChoices(String msg) {
        AlertDialog.Builder b = new AlertDialog.
            Builder(PlayerSetupAct.this);
        b.setMessage(msg);
        b.setCancelable(true);
        b.setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    // just dismiss the box
                }
            });
        b.show();
    }

    /** Starts the RunGameActivity */
    private void launchRunGameActivity() {
        Intent myIntent = new Intent().
                setClass(PlayerSetupAct.this, RunGameAct.class);
        Bundle map = new Bundle();
        onSaveInstanceState(map);
        myIntent.putExtra(GameSetupAct.GAME_SETUP_BUNDLE, map);
        startActivity(myIntent);
    }
}
