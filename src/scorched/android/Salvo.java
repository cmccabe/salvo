package scorched.android;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import scorched.android.Model;
import scorched.android.Slider.OnPositionChangedListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

public class Salvo extends Activity {
    /*================= Constants =================*/
    private static final String TAG = "Salvo";

    /*================= Data =================*/
    private GameControlView mGameControl;
    private Model mModel;
    private Graphics mGraphics;
    
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
        mModel = new Model(players);

        // Create View
        mGraphics = new Graphics(getBaseContext(), mModel);

        // Create Controller / Window object
        requestWindowFeature(Window.FEATURE_NO_TITLE); // turn off title bar
        Log.w(TAG, "setContentView");
        setContentView(R.layout.main);
        Log.w(TAG, "findViewById");
        mGameControl = (GameControlView) findViewById(R.id.scorched_layout);
        mGameControl.initialize(mModel, mGraphics);

        //Drawable redDrawable = 
            //Resources.getSystem().getDrawable(R.drawable.color_red);
        //TextView tv = (TextView)findViewByID(R.id.text);
        //tv.setBackgroundColor(redDrawable);
        Spinner spinner = (Spinner)findViewById(R.id.WeaponSpinner);
        ArrayAdapter<String> adapterForSpinner = 
            new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
                                adapterForSpinner.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
                                spinner.setAdapter(adapterForSpinner);
        adapterForSpinner.add("Mini Missile");
        adapterForSpinner.add("Mega Missile");
        adapterForSpinner.add("Flying Monkey");
        final Slider powerSlider = (Slider)findViewById(R.id.PowerSlider);
        powerSlider.max = 999;
        final TextView powerValue = (TextView)findViewById(R.id.PowerValue);
        powerSlider.setOnPositionChangedListener(
            new OnPositionChangedListener() {

            public void onPositionChangeCompleted() {
                // TODO Auto-generated method stub
                
            }

            public void onPositionChanged(Slider slider, int oldPosition,
                    int newPosition) {
                powerValue.setText(Integer.
                    toString(newPosition+1000).substring(1));
            }
        });
        Button powerMinus = (Button)findViewById(R.id.PowerMinus);
        powerMinus.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                if (powerSlider.pos > powerSlider.min) {
                    powerSlider.setPosition(powerSlider.pos - 1);
                    powerValue.setText(Integer.toString(powerSlider.pos+1000).
                                        substring(1));
                }
            }
            
        });
        Button powerPlus = (Button)findViewById(R.id.PowerPlus);
        powerPlus.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                if (powerSlider.pos < powerSlider.max) {
                    powerSlider.setPosition(powerSlider.pos + 1);
                    powerValue.setText(Integer.toString(powerSlider.pos+1000).
                                        substring(1));
                }
            }
            
        });
        final Slider angleSlider = (Slider)findViewById(R.id.AngleSlider);
        angleSlider.max = 180;
        final TextView angleValue = (TextView)findViewById(R.id.AngleValue);
        angleSlider.setOnPositionChangedListener(
            new OnPositionChangedListener() {

            public void onPositionChangeCompleted() {
                // TODO Auto-generated method stub
                
            }

            public void onPositionChanged(Slider slider, int oldPosition,
                    int newPosition) {
                angleValue.setText(Integer.toString(newPosition+1000).
                                        substring(1));
                
            }
            
        });
        Button angleMinus = (Button)findViewById(R.id.AngleMinus);
        angleMinus.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                if (angleSlider.pos > angleSlider.min)
                {
                    angleSlider.setPosition(angleSlider.pos - 1);
                    angleValue.setText(Integer.toString(angleSlider.pos+1000).
                                            substring(1));
                }
            }
            
        });
        Button anglePlus = (Button)findViewById(R.id.AnglePlus);
        anglePlus.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                if (angleSlider.pos < angleSlider.max)
                {
                    angleSlider.setPosition(angleSlider.pos + 1);
                    angleValue.setText(Integer.toString(angleSlider.pos+1000).
                                            substring(1));
                }
            }
            
        });

    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mGameControl.getThread().pause(); // pause game when Activity pauses
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
        mGameControl.getThread().saveState(outState);
        Log.w(this.getClass().getName(), "onSaveInstanceState called");
    }
}
