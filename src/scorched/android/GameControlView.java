package scorched.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * Controller for the Scorched Android game.
 *
 * GameControlView gets input from the user, as well as events from other
 * parts of the system, and presents them to Graphics and mModel.
 */
class GameControlView extends SurfaceView implements SurfaceHolder.Callback {
    /*================= Constants =================*/
    private static final String TAG = "GameControlView";

    /*================= Types =================*/

    /*================= ScorchedThread =================*/
    class ScorchedThread extends Thread {
        /** The semaphore representing user input during a player's turn */
        private Object mUserInputSem = new Object();

        /** Represents the current controller state */
        private GameState mGameState;

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
        
        /** The object to control sound */
        private Sound mSound;

        public ScorchedThread(SurfaceHolder surfaceHolder,
                            Context context,
                            Handler handler,
                            SalvoSlider powerSlider,
                            SalvoSlider angleSlider) {
            mGameState = null;
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            mHandler = handler;
            mPowerSlider = powerSlider;
            mAngleSlider = angleSlider;
            mSound = new Sound(context);
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

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            synchronized (mUserInputSem) {
                Graphics.instance.setSurfaceSize(width, height);
                mUserInputSem.notify();
            }
        }

        /*================= Main =================*/
        @Override
        public void run() {
            Log.w(TAG, "run(): waiting for surface to be created.");
            mRun = true;
            synchronized (mSurfaceHasBeenCreatedSem) {
                while (!mSurfaceHasBeenCreated) {
                    try {
                        mSurfaceHasBeenCreatedSem.wait();
                    }
                    catch (InterruptedException e) {
                        Log.w(TAG, "interrupted waiting for " +
                                        "mSurfaceHasBeenCreatedSem");
                        mRun = false;
                    }
                }
            }
            Log.w(TAG, "run(): surface has been created.");

            // main loop
            Log.w(TAG, "run(): entering main loop.");
            synchronized (mUserInputSem) {
                mGameState = GameState.sTurnStartState;
                while (true) {
                    mGameState.onEnter(mModel,
                                    mPowerSlider, mAngleSlider,
                                    mPowerAdaptor, mAngleAdaptor);
                    GameState next = null;
                    while (true) {
                        next = mGameState.main(mModel);
                        if (next != null)
                            break;
                        // redraw whatever needs to be redrawn
                        if (mGameState.needRedraw()) {
                            Canvas canvas = null;
                            try {
                                canvas = mSurfaceHolder.lockCanvas(null);
                                mGameState.redraw(canvas, mModel);
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
                                Log.w(TAG, "mRun == false. quitting.");
                                return;
                            }
                            mUserInputSem.wait(mGameState.
                                getBlockingDelay());
                        }
                        catch (InterruptedException e) {
                            if (!mRun) {
                                Log.w(TAG, "interrupted: quitting.");
                                return;
                            }
                        }
                    }
                    mGameState.onExit(mPowerSlider, mAngleSlider);
                    Log.w(TAG, "transitioning from " +
                            mGameState.toString() + " to " +
                            next.toString());
                    mGameState = next;
                }
            }
        }

        /*================= Save / Restore =================*/
        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         *
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                mModel.saveState(map);
            }
            return map;
        }

        /**
         * Restores game state from the indicated Bundle. Typically called
         * when the Activity is being restored after having been previously
         * destroyed.
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle map) {
            synchronized (mSurfaceHolder) {
                mModel.restoreState(map);
                mPaused = false;
            }
        }

        /** Called from GUI thread when the user presses a button.
         */
        public void onButton(GameState.GameButton b) {
            synchronized (mUserInputSem) {
                if (mGameState.onButton(b))
                    mUserInputSem.notify();
            }
        }

        /** Called (from the GUI thread) when the user moves a slider
         */
        public void onSliderChange(boolean isPowerSlider, int val) {
            synchronized (mUserInputSem) {
                if (mGameState.onSlider(mModel, isPowerSlider, val))
                    mUserInputSem.notify();
            }
        }

        /** Handles a touchscreen event */
        public boolean onTouchEvent(MotionEvent me) {
            synchronized (mUserInputSem) {
                if (mGameState.onTouchEvent(me))
                    mUserInputSem.notify();
            }
            return true;
        }

    }

    /*================= Members =================*/
    /** The thread that draws the animation */
    private ScorchedThread mThread;

    private Object mSurfaceHasBeenCreatedSem = new Object();

    /** True only once the Surface has been created and is ready to
     * be used */
    private boolean mSurfaceHasBeenCreated = false;

    /** Pointer to the model */
    public Model mModel = null;

    SalvoSlider.Listener mPowerAdaptor;
    SalvoSlider.Listener mAngleAdaptor;

    /*================= Accessors =================*/
    /** Fetches the animation thread for this GameControlView. */
    public ScorchedThread getThread() {
        return mThread;
    }

    /*================= User Input Operations =================*/
    /** Pan the game board */
    @Override
    public boolean onTouchEvent(MotionEvent me) {
        return mThread.onTouchEvent(me);
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        //if (!hasWindowFocus)
            //mThread.pause();
    }

    /** Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        mThread.setSurfaceSize(width, height);
    }

    /** Callback invoked when the Surface has been created and is
     * ready to be used. */
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (mSurfaceHasBeenCreatedSem) {
            // Wake up mThread.run() if it's waiting for the surface to have
            // ben created
            mSurfaceHasBeenCreated = true;
            mSurfaceHasBeenCreatedSem.notify();
        }
        Log.w(TAG, "surfaceCreated(): set mSurfaceHasBeenCreated");
    }

    /** Callback invoked when the Surface has been destroyed and must
     * no longer be touched.
     * WARNING: after this method returns, the Surface/Canvas must
     * never be touched again! */
    public void surfaceDestroyed(SurfaceHolder holder) {
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

    /*================= Lifecycle =================*/
    public GameControlView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Try to get hardware acceleration
        try {
            getHolder().setType(
                android.view.SurfaceHolder.SURFACE_TYPE_HARDWARE);
            Log.w(TAG, "GameControlView: activated hardware acceleration");
        }
        catch(Exception e2) {
            getHolder().setType(
                android.view.SurfaceHolder.SURFACE_TYPE_NORMAL);
            Log.w(TAG, "GameControlView: no acceleration");
        }
    }

    public void initialize(Model model,
                        SalvoSlider powerSlider, SalvoSlider angleSlider)
    {
        mModel = model;

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

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
        setFocusable(false); // make sure we get key events

        // Start the animation thread
        mThread.start();
    }
}
