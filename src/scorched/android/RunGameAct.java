package scorched.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ZoomButton;

import scorched.android.Model;
import android.view.View.OnClickListener;

public class RunGameAct extends Activity {
    /*================= Constants =================*/

    /*================= Handles to Views =================*/
    /** A view representing the part of the screen where most of the graphics
     * are drawn */
    private GameControlView mGameControlView;

    /** Displays the current angle */
    private TextView mAngleText;

    /** The button you press to choose the previous weapon in your armory */
    private Button mArmoryLeftButton;

    /** The button you press to choose the next weapon in your armory */
    private Button mArmoryRightButton;

    /** The button you press to choose the previous weapon in your armory */
    private Button mWeapSelLeftButton

    /** The button the user presses to fire */
    private Button mFireButton;

    /*================= Temporary Data =================*/
    /** Lock that protects game state */
    private Object mStateLock;

    /** Provides access to RunGameAct internals */
    private RunGameActAccessor mAcc;

    /** Observes changes in the GameControlView */
    private GameControlViewObserver mGameControlViewObserver;

    /** The main thread */
    private RunGameThread mThread;

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
     *
     * This class contains no locking
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
     *
     * This class does its own locking with mStateLock
     */
    private class GameControlViewObserver implements SurfaceHolder.Callback {
        /*================= Operations =================*/
        /** Callback invoked when the Surface has been created and is
         * ready to be used. */
        public void surfaceCreated(SurfaceHolder holder) {
            synchronized (mStateLock) {
                mThread.getStateController().setSurfaceAvailable();
            }
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
        //public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO: figure out what to do here, if anything.

            // The confusing thing is that before this callback, we should
            // receive onPause() or similar, which should already have led us
            // to pause the thread. So what remains to do here anyway?
        //}
    }

    /** Controls the state of the RunGameThread.
     *
     * This class contains no locking
     */
    private class RunGameThreadStateController {
        /*================= Data =================*/
        /** GameControlView has been created; we can access its
         * data without crashing and burning. */
        private boolean mSurfaceAvailable;

        /** RunGameAct is done initializing itself. */
        private boolean mInitializationComplete;

        /** mRunGameThread has been told to stop; it either is
         * blocking on mStateLock or is about to do so. */
        private boolean mStopRequested;

        /** mRunGameThread has been told to terminate itself */
        private boolean mTerminateRequested;

        /*================= Access =================*/
        public boolean getStopRequested() {
            return mStopRequested;
        }

        public boolean getTerminateRequested() {
            return mTerminateRequested;
        }

        /*================= Operations =================*/
        public void setInitializationComplete() {
            assert (Thread.holdsLock(mStateLock));
            if (mInitializationComplete)
                return;
            mInitializationComplete = true;
            if (mSurfaceAvailable)
                mRunGameThread.start();
        }

        public void setSurfaceAvailable() {
            assert (Thread.holdsLock(mStateLock));
            if (mSurfaceAvailable)
                return;
            mSurfaceAvailable = true;
            if (mInitializationComplete)
                mRunGameThread.start();
        }

        public void changeStopRequested(boolean stopRequested) {
            assert (Thread.holdsLock(mStateLock));
            mStopRequested = stopRequested;
        }

        public void changeTerminateRequested(boolean terminateRequested) {
            assert (Thread.holdsLock(mStateLock));
            mTerminateRequested = terminateRequested;
        }

        /*================= Lifecycle =================*/
        public RunGameThreadStateController() {
            mSurfaceAvailable = false;
            mInitializationComplete = false;
        }
    }

    /** The main game thread.
     *
     * RunGameThread executes the game state machine.
     *
     * State transitions can only happen in the main thread.
     *
     * The main thread also draws everything on the screen.
     *
     * If you have done anything that might require a screen redraw, or
     * cause a transition to another game state, be sure to use notify() to
     * wake up the main thread.
     */
    public class RunGameThread extends Thread {
        /*================= Data =================*/
        private RunGameThreadStateController mStateController;

        /*================= Access =================*/
        public RunGameThreadStateController getStateController() {
            return mStateController;
        }

        /*================= Operations =================*/
        @Override
        public void run() {
            Log.w(this.getClass().getName(), "Starting RunGameThread...");

            while (true) {
                // Enter the state
                synchronized (mStateLock) {
                    stateLog("onEnter", mState);
                    mState.onEnter(mAcc);
                }

                // Execute the state's main loop
                GameState next = null;
                synchronized (mStateLock) {
                    stateLog("starting main", mState);
                    while (true) {
                        if (doCancellationPoint())
                            return;
                        next = mState.main(mAcc);
                        if (next != null)
                            break;
                        // Delay until the next call to main()
                        // If getBlockingDelay == 0, then we delay until
                        // someone calls notify() on mStateLock
                        mStateLock.wait(mState.getBlockingDelay());
                    }
                    if (doCancellationPoint())
                        return;
                }

                synchronized (mStateLock) {
                    stateLog("onExit", mState);
                    mState.onExit();
                    mState = next;
                }
            }
        }

        /** Print out state logging information */
        private void stateLog(String text, GameState state) {
            StringBuilder b = new StringBuilder(80);
            b.append("onEnter(").append(mState.toString());
            b.append(")");
            Log.w(this.getClass().getName(), b.toString());
        }

        /** Process requests that the main thread sleep or exit.
         *
         * This function checks to see if someone has requested that the
         * main thread go to sleep or terminate itself.
         *
         * In the event that someone has requested sleep, we sleep using
         * wait().
         * In the event that someone has requested termination, we return
         * true.
         *
         * @return      true if we should exit run(), false otherwise
         */
        private boolean doCancellationPoint() {
            assert (Thread.holdsLock(mStateLock));
            if (mStateController.getTerminateRequested())
                return true;
            if (mStateController.getStopRequested()) {
                mStateLock.wait();
            }
            return false;
        }

        /*================= Lifecycle =================*/
        public RunGameThread() {
            mStateController = new RunGameThreadStateController();
        }
    }

    /*================= Operations =================*/
    /** Called from GameControlView to handle keystrokes */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                showAreYouSureYouWantToQuit();
                return true;
        }
        return false;
    }

    /** Called from GameControlView to handle touch events */
    public void onTouchEvent(MotionEvent me) {
        synchronized (mStateLock) {
            if (mState.onTouchEvent(mAcc, me)) {
                mStateLock.notify();
            }
        }
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
        mAngleText = (TextView)findViewById(R.id.angle_text);
        mArmoryLeftButton = (Button)findViewById(R.id.armory_left_button);
        mArmoryMainText = (TextView)findViewById(R.id.armory_main_text);
        mArmorySecondaryText = (TextView)
            findViewById(R.id.armory_secondary_text);
        mArmoryRightButton = (Button)findViewById(R.id.armory_right_button);
        mFireButton = (Button)findViewById(R.id.fire_button);

        ////////////////// Initialize stuff
        mArmoryLeftButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                synchronized (mStateLock) {
                    if (mState.onButton(mAcc,
                            GameState.GameButton.ARMORY_LEFT)) {
                        mStateLock.notify();
                    }
                }
            }
        });

        mArmoryRightButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                synchronized (mStateLock) {
                    if (mState.onButton(mAcc,
                            GameState.GameButton.ARMORY_RIGHT)) {
                        mStateLock.notify();
                    }
                }
            }
        });

        fireButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int act = event.getAction();
                if (act == MotionEvent.ACTION_DOWN) {
                    synchronized (mStateLock) {
                        if (mState.onButton(mAcc,
                                GameState.GameButton.PRESS_FIRE)) {
                            mStateLock.notify();
                        }
                    }
                }
                else if (act == MotionEvent.ACTION_UP) {
                    synchronized (mStateLock) {
                        if (mState.onButton(mAcc,
                                GameState.GameButton.RELEASE_FIRE)) {
                            mStateLock.notify();
                        }
                    }
                }
                return true;
            }
        });

        mGameControlView.getHolder().addCallback(mGameControlViewObserver);
        synchronized (mStateLock) {
            mThread.getStateController().setInitializationComplete();
        }
    }

    @Override
    protected void onPause() {
        // The game is no longer in the foreground
        super.onPause();
        synchronized (mStateLock) {
            Log.w(this.getClass().getName(), "onPause called");
            mThread.getStateController().changeStopRequested(true);
            mState.notify();
        }
    }

    @Override
    public void onResume() {
        // The game is back in the foreground
        super.onResume();
        synchronized (mStateLock) {
            Log.w(this.getClass().getName(), "onResume called");
            mThread.getStateController().changeStopRequested(false);
            mState.notify();
        }
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
        synchronized (mStateLock) {
            Log.w(this.getClass().getName(),
                    "RunGameAct.onSaveInstanceState");
            mModel.saveState(map);
            mState.saveState(map);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy(map);
        synchronized (mStateLock) {
            Log.w(this.getClass().getName(),
                    "RunGameAct.onDestroy");
            mThread.getStateController().changeTerminateRequested(true);
            mStateLock.notify()
        }
    }

    public RunGameAct() {
        super();
        mStateLock = new Object();
        mAcc = new RunGameActAccessor();
        mGameControlViewObserver = new GameControlViewObserver();
        mThread = new RunGameThread();
    }
}
