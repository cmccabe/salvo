package com.senchas.salvo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.view.View.OnClickListener;

public class TitleScreenAct extends Activity {
    /*================= Constants =================*/

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

        ////////////////// setContentView
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.title);

        ////////////////// Get pointers to stuff
        final Button newGame = (Button)findViewById(R.id.new_game);
        final Button help = (Button)findViewById(R.id.help);

        ////////////////// Initialize stuff
        final Activity titleActivity = this;
        newGame.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent setupIntent = new Intent().
                    setClass(titleActivity, GameSetupAct.class);
                startActivity(setupIntent);
            }
        });
        help.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent setupIntent = new Intent().
                    setClass(titleActivity, HelpAct.class);
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
