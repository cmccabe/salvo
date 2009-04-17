package scorched.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ZoomButton;

import scorched.android.Model;
import android.view.View.OnClickListener;

public class RunGameAct extends Activity {
    /*================= Constants =================*/

    /*================= Temporary Data =================*/
    /** A view representing the part of the screen where most of the graphics
     * are drawn */
    private GameControlView mGameControlView;

    /** Lock that protects game state */
    private Object mStateLock;

    /** Provides access to RunGameAct internals */
    private RunGameActAccessor myRunGameActAccessor;

    /** Observes changes in the GameControlView */
    private GameControlViewObserver mGameControlViewObserver;

    /** The main thread */
    private RunGameThread mRunGameThread;

    /*================= Permanent Data =================*/
    /** The important game data */
    private Model mModel;

    /** The current game state. */
    private GameState mState;

    /*================= Types =================*/
    /** Provides access to RunGameAct internals.
     *
     * Normally RunGameAct hides its internals pretty well. For some
     * classes, we want to be more generous by providing them with this
     * accessor.
     *
     * If Java allowed "friend" classes, this code would be a lot shorter.
     */
    private class RunGameActAccessor {
        /*================= Access =================*/
        public GameControlView getGameControlView() {
            return mGameControlView;
        }

        public Model getModel() {
            return mModel;
        }

        // TODO: add accessors for angle display / weapons display here
    }

    /** Observes changes in the GameControlView.
     */
    private class GameControlViewObserver implements SurfaceHolder.Callback {
        /*================= Operations =================*/
        /** Callback invoked when the Surface has been created and is
         * ready to be used. */
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO: start the thread
        }

        //public void surfaceChanged(SurfaceHolder holder,
        //                      int format, int width, int height) {
        // Callback invoked when the surface dimensions change.
        //}

        /** Callback invoked when the Surface has been destroyed and must
         * no longer be touched.
         *
         * WARNING: after this method returns, the Surface/Canvas must
         * never be touched again! */
        public void surfaceDestroyed(SurfaceHolder holder) {
            Activity runGameAct = (Activity)getContext();
            runGameAct.gameControlViewDestroyed();
        }
    }

    /** The main game thread.
     *
     *  RunGameThread executes the game state machine.
     */
    public class RunGameThread extends Thread {
        /** The semaphore representing user input during a player's turn */
        private Object mUserInputSem = new Object();

        /** True if the game is running */
        private boolean mRun = true;

        /** Indicate whether or not the game is paused */
        private boolean mPaused = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        /** Handle to the application context;
         *  used to (for example) fetch Drawables. */
        private Context mContext;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** The slider representing power */
        private SalvoSlider mPowerSlider;

        /** The slider representing angle */
        private SalvoSlider mAngleSlider;

        /** Last X coordinate the user touched (in game coordinates) */
        private float mTouchX;

        /** Last Y coordinate the user touched (in game coordinates) */
        private float mTouchY;

        public ScorchedThread(SurfaceHolder surfaceHolder,
                            Context context,
                            Handler handler,
                            SalvoSlider powerSlider,
                            SalvoSlider angleSlider) {
            mState = null;
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            mHandler = handler;
            mPowerSlider = powerSlider;
            mAngleSlider = angleSlider;
        }

        /*================= Operations =================*/
        /** Shut down the thread */
        public void suicide() {
            synchronized (mUserInputSem) {
                mRun = false;
            }

            // interrupt anybody in wait()
            this.interrupt();
        }

        /*================= Main =================*/
        @Override
        public void run() {
            Log.w(this.getClass().getName(), "run(): waiting for surface to be created.");

            mRun = true;
            synchronized (mSurfaceHasBeenCreatedSem) {
                while (!mSurfaceHasBeenCreated) {
                    try {
                        mSurfaceHasBeenCreatedSem.wait();
                    }
                    catch (InterruptedException e) {
                        Log.w(this.getClass().getName(),
                            "interrupted waiting for " +
                            "mSurfaceHasBeenCreatedSem");
                        mRun = false;
                    }
                }
            }
            Log.w(this.getClass().getName(),
                "run(): surface has been created.");

            // main loop
            Log.w(this.getClass().getName(), "run(): entering main loop.");
            synchronized (mUserInputSem) {
                mState = GameState.sTurnStartState;
                while (true) {
                    mState.onEnter(mModel,
                                    mPowerSlider, mAngleSlider,
                                    mPowerAdaptor, mAngleAdaptor);
                    GameState next = null;
                    while (true) {
                        next = mState.main(mModel);
                        if (next != null)
                            break;
                        // redraw whatever needs to be redrawn
                        if (mState.needRedraw()) {
                            Canvas canvas = null;
                            try {
                                canvas = mSurfaceHolder.lockCanvas(null);
                                mState.redraw(canvas, mModel);
                            }
                            finally {
                                if (canvas != null) {
                                    // Don't leave the Surface in an
                                    // inconsistent state
                                    mSurfaceHolder.
                                        unlockCanvasAndPost(canvas);
                                }
                            }
                        }
                        try {
                            if (!mRun) {
                                Log.w(this.getClass().getName(),
                                    "mRun == false. quitting.");
                                return;
                            }
                            mUserInputSem.wait(mState.
                                getBlockingDelay());
                        }
                        catch (InterruptedException e) {
                            if (!mRun) {
                                Log.w(this.getClass().getName(),
                                    "interrupted: quitting.");
                                return;
                            }
                        }
                    }
                    mState.onExit(mPowerSlider, mAngleSlider);
                    Log.w(this.getClass().getName(), "transitioning from " +
                            mState.toString() + " to " +
                            next.toString());
                    mState = next;
                }
            }
        }

        /*================= Callbacks =================*/
        /** Called from GUI thread when the user presses a button.
         */
        public void onButton(GameState.GameButton b) {
            synchronized (mUserInputSem) {
                if (mState.onButton(b))
                    mUserInputSem.notify();
            }
        }

        /** Called (from the GUI thread) when the user moves a slider
         */
        public void onSliderChange(boolean isPowerSlider, int val) {
            synchronized (mUserInputSem) {
                if (mState.onSlider(mModel, isPowerSlider, val))
                    mUserInputSem.notify();
            }
        }

        /** Handles a touchscreen event */
        public boolean onTouchEvent(MotionEvent me) {
            synchronized (mUserInputSem) {
                if (mState.onTouchEvent(me))
                    mUserInputSem.notify();
            }
            return true;
        }
    }


    /*================= Operations =================*/
    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.w(this.getClass().getName(), "onPause called");
        //mGameControl.getThread().pause(); // pause game when
                                                // Activity pauses
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its
     *        state
     */
    @Override
    protected void onSaveInstanceState(Bundle map) {
        super.onSaveInstanceState(map);
        Log.w(this.getClass().getName(), "RunGameAct.onSaveInstanceState");
        mModel.saveState(map);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.w(this.getClass().getName(),
            "onWindowFocusChanged(" + hasFocus + ") called");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                showAreYouSureYouWantToQuit();
                return true;
        }
        return false;
    }

    private void showAreYouSureYouWantToQuit() {
        // display alert that we can't have any more players
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage("Are you sure you want to end this game?\n" +
                    "If you want to switch apps without quitting, " +
                    "press \"HOME\"");
        b.setCancelable(false);
        b.setNegativeButton("End Game",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        b.setPositiveButton("Cancel",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                                    int whichButton) {
                    // Let's just forget we ever had this
                    // conversation, OK?
                }
            });
        b.show();
    };

    /*================= Lifecycle =================*/
    @Override
    public void onCreate(Bundle map) {
        super.onCreate(map);

        if (map == null) {
            // We're starting up the game. Create the Model
            Bundle smap =
                getIntent().getBundleExtra(GameSetupAct.GAME_SETUP_BUNDLE);
            ModelFactory fac = ModelFactory.fromBundle(smap);
            mModel = fac.createModel();
            mState = GameState.createInitialGameState();
        }
        else {
            // Decompress saved state
            mModel = Model.fromBundle(map);
            mState = GameState.fromBundle(map);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.game);

        ////////////////// Get pointers to stuff
        mGameControlView = (GameControlView)
            findViewById(R.id.game_control_view);
        final TextView mAngleView =
            (SalvoSlider)findViewById(R.id.PowerSlider);
        final SalvoSlider angleSlider =
            (SalvoSlider)findViewById(R.id.AngleSlider);
        final Button fireButton = (Button)findViewById(R.id.FireButton);
        final ZoomButton zoomIn = (ZoomButton)findViewById(R.id.ZoomIn);
        final ZoomButton zoomOut = (ZoomButton)findViewById(R.id.ZoomOut);

        ////////////////// Initialize stuff

        mGameControl.initialize(mModel, powerSlider, angleSlider);
        fireButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mGameControl.getThread().onButton(GameState.GameButton.FIRE);
            }
        });
        zoomIn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mGameControl.getThread().onButton(
                    GameState.GameButton.ZOOM_IN);
            }
        });
        zoomOut.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mGameControl.getThread().onButton(
                    GameState.GameButton.ZOOM_OUT);
            }
        });


        mGameControl.getHolder().addCallback(mGameControlViewObserver);
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    /** Hook up the various views to each other.
     * Must be called after all views are finished being constructed-- i.e.
     * in onStart, not onCreate. */
    private void initialize() {
    }

    public RunGameAct() {
        super();
        myRunGameActAccessor = new RunGameActAccessor();
        mGameControlViewObserver = new GameControlViewObserver();
        mRunGameThread = new RunGameThread();
    }
}

/////////////

    /*================= ScorchedThread =================*/

        public void gameControlViewDestroyed() {
            // we have to tell thread to shut down & wait for it to finish,
            // or else it might touch the Surface after we return and explode
            mThread.suicide();

            while (true) {
                try {
                    mThread.join();
                    break;
                }
                catch (InterruptedException e) {
                }
            }
        }

        create thread
        // Create game controller thread
        mThread = new ScorchedThread(holder, getContext(),
            new Handler() {
                @Override
                public void handleMessage(Message m) {
                    //mStatusText.setVisibility(m.getData().getInt("viz"));
                    //mStatusText.setText(m.getData().getString("text"));
                }
            },
            powerSlider,
            angleSlider);

        mPowerAdaptor = new SalvoSlider.Listener() {
            public void onPositionChange(int val) {
                mThread.onSliderChange(true, val);
            }
        };
        mAngleAdaptor = new SalvoSlider.Listener() {
            public void onPositionChange(int val) {
                mThread.onSliderChange(false, val);
            }
        };
