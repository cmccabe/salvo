package scorched.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;


/**
 * Controller for the Scorched Android game
 * 
 * ScorchedView gets input from the user, as well as events from other
 * parts of the system, and presents them to mGraphics and mModel.
 *
 * The nomenclature is unfortunate; despite its name, this is *not* a View
 * in the model-view-controller sense. In the MVC sense, the View would be
 * ScorchedGraphics.
 */
class ScorchedView extends SurfaceView implements SurfaceHolder.Callback {
    /*================= Constants =================*/
    private static final String TAG = "ScorchedView";

    private enum GameState {
        IDLE,
        PLAYER_MOVE,
        BALLISTICS,
        EXPLOSION,
        QUIT,
    };

    /*================= Types =================*/

    /** Represents player input gathered from various functions. 
     *  This class must be used as a singleton because of locking issues.
     */
    private static class PlayerInput {
        /*================= Constants =================*/
        public enum PlayerInputAction {
            /** No player input */
            NONE,
            /** Key (or button) was pressed */
            KEY_DOWN,
            /** Key (or button) was released */
            KEY_UP,
            /** Slider was set to a particular value */  
            SET_SLIDER,
        };

        public enum PlayerKeys {
            TURRET_LEFT,
            TURRET_RIGHT,
            POWER_UP,
            POWER_DOWN,
            FIRE,
            QUIT,
        };

        public enum Slider {
            TURRET_SLIDER,
            POWER_SLIDER,
        };

        /*================= Members =================*/
        PlayerInputAction mAct;
        PlayerKeys mKey;
        Slider mSlider;
        int mVal;
        boolean mIgnoreInput;

        /*================= Access =================*/
        /** Returns the current action the player wishes to take, or NONE if
         *  'timeout' milliseconds elapsed without user input. */
        PlayerInputAction poll(int timeout) throws InterruptedException {
            assert Thread.holdsLock(this);
            if (mAct == PlayerInputAction.NONE) {
                this.wait(timeout);
            }
            return mAct;
        }

        PlayerKeys getKey() {
            assert Thread.holdsLock(this);
            assert((mAct == PlayerInputAction.KEY_UP) ||
            		(mAct == PlayerInputAction.KEY_DOWN));
            return mKey;
        }

        Slider getSlider() {
            assert Thread.holdsLock(this);
            assert(mAct == PlayerInputAction.SET_SLIDER);
            return mSlider;
        }

        int getVal() {
            assert Thread.holdsLock(this);
            assert(mAct == PlayerInputAction.SET_SLIDER);
            return mVal;
        }

        /*================= Operations =================*/
        // These functions handle user input.
        // Basically, the goal is to serialize user input so that
        // it is easier for the main thread to process.

        // It might seem easier to move some things out of the main thread
        // and into doKeyDown, etc. However, doKeyDown is purely a callback
        // and cannot do things like continue to move the turret until the
        // user lets go of the key. In order to do that, you need a thread.

        /** Handle key press events. 
         * Returns false if the key press was ignored. */
        boolean pressKey(PlayerKeys key) throws InterruptedException {
            synchronized (this) {
                while (mAct != PlayerInputAction.NONE) {
                    if (mIgnoreInput) {
                        return false;
                    }
                    this.wait();
                }
                mAct = PlayerInputAction.KEY_DOWN;
                mKey = key;
                this.notifyAll();
            }
            return true;
        }

        /** Handle key release events. 
         * Returns false if the key release was ignored. */
        boolean releaseKey(PlayerKeys key) throws InterruptedException {
            synchronized (this) {
                while (mAct != PlayerInputAction.NONE) {
                    if (mIgnoreInput) {
                        return false;
                    }
                    this.wait();
                }
                mAct = PlayerInputAction.KEY_UP;
                mKey = key;
                this.notifyAll();
            }
            return true;
        }

        /** Handle slider events. */
        void setSlider(Slider slider, int val) throws InterruptedException {
            synchronized (this) {
                while (mAct != PlayerInputAction.NONE) {
                    if (mIgnoreInput) {
                        return;
                    }
                    this.wait();
                }
                mAct = PlayerInputAction.SET_SLIDER;
                mSlider = slider;
                mVal = val;
                this.notifyAll();
            }
        }

        void clearInput() {
            assert Thread.holdsLock(this);
            mAct = PlayerInputAction.NONE; 
            this.notifyAll();
        }

        /* Controls whether the player input should be ignored or not. */
        void changeIgnoreInput(boolean ignoreInput) {
            synchronized (this) {
                mIgnoreInput = ignoreInput;

                // If anyone is waiting to put his event into the object,
                // tell him to wake up
                this.notifyAll();
            }
        }

        /*================= Lifecycle =================*/
        PlayerInput() {
            mAct = PlayerInputAction.NONE;
            mIgnoreInput = true;
        }
    };


    /*================= ScorchedThread =================*/
    class ScorchedThread extends Thread {
        // TODO: have the thread stop routines etc. give ScorchedThread an
        // interrupt()

        /** Represents the current controller state */
        volatile private GameState mGameState;

        /** Represents the current user input */
        private PlayerInput mCurPlayerInput;

        /** Indicate whether or not the game is paused */
        private boolean mPaused = false;

        /** Pointer to the view */
        public ScorchedGraphics mGraphics = null;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** Handle to the application context
         *  used to e.g. fetch Drawables. */
        private Context mContext;

        public ScorchedThread(ScorchedGraphics graphics,
                            SurfaceHolder surfaceHolder, 
                            Context context,
                            Handler handler) {
            mGraphics = graphics;
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            mGameState = GameState.PLAYER_MOVE;
            mCurPlayerInput = new PlayerInput();
        }

        /*================= Operations =================*/
        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                mPaused = false;
            }
        }

        /**
         * Pauses the physics update and animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                mPaused = true;
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mPaused = false;
            }
        }

        /** Shut down the thread */
        public void suicide() {
            mGameState = GameState.QUIT;

            // interrupt anybody in wait()
            this.interrupt();
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mGraphics.setSurfaceSize(width, height);
            }
        }

        /*================= Main =================*/
        @Override
        public void run() {
            mGameState = GameState.IDLE;

            Log.w(TAG, "run(): waiting for surface to be created.");
            synchronized (mSurfaceHasBeenCreatedSem) {
                while (!mSurfaceHasBeenCreated) {
                    try {
                        mSurfaceHasBeenCreatedSem.wait();
                    }
                    catch (InterruptedException e) {
                        Log.w(TAG, "interrupted waiting for " +
                                        "mSurfaceHasBeenCreatedSem");
                        mGameState = GameState.QUIT;
                    }
                }
            }
            Log.w(TAG, "run(): surface has been created.");

            // main loop
            try 
            {
                mGameState = GameState.PLAYER_MOVE;
                while (true) {
                    GameState nextState = mGameState;
                    switch (mGameState) {
                        case IDLE:
                            // Should not get here
                            throw new RuntimeException(
                                "Got mGameState = IDLE in main loop");
    
                        case PLAYER_MOVE:
                            // The player is moving around his 
                            // turret, etc. 
                            Player curPlayer = mModel.getCurPlayer();
                            if (curPlayer.isHuman()) {
                                mGameState = runHumanMove(curPlayer);
                            }
                            else {
                                throw new RuntimeException("unimplemented");
                            }
    
                        case BALLISTICS:
                            // The projectile is moving through the sky
                            //mGameState = runBallistics();
                            break;
    
                        case EXPLOSION:
                            // The projectile is exploding onscreen
                            //mGameState = runExplosion();
                            break;
    
                        case QUIT:
                            // The user has requested QUIT
                            return;
                    }
                }
            }
            catch (InterruptedException e) {
                Log.w(TAG, "interrupted: quitting.");
                mGameState = GameState.QUIT;
                return;
            }
        }

        /** Implements a human move */
        private GameState runHumanMove(Player curPlayer) 
            throws InterruptedException
        {
            // TODO: implement 'quick tweak' followed by delay, and then 
            // more motion
            PlayerInput.PlayerKeys curKey = 
                PlayerInput.PlayerKeys.TURRET_RIGHT;
            boolean keyDown = false;
            mCurPlayerInput.changeIgnoreInput(false);

            while (true) {
                runScreenRefresh();
                synchronized (mCurPlayerInput) {
                    PlayerInput.PlayerInputAction act = 
                        mCurPlayerInput.poll(10);
                    switch (act) {
                        case NONE:
                            break;

                        case KEY_DOWN:
                            keyDown = true;
                            curKey = mCurPlayerInput.getKey();
                            mCurPlayerInput.clearInput();
                            break;

                        case KEY_UP:
                            keyDown = false;
                            mCurPlayerInput.clearInput();
                            break;

                        case SET_SLIDER:
                            switch (mCurPlayerInput.getSlider()) {
                                case TURRET_SLIDER:
                                    curPlayer.setAngle(
                                        mCurPlayerInput.getVal());
                                    break;
                                case POWER_SLIDER:
                                    curPlayer.setPower(
                                        mCurPlayerInput.getVal());
                                    break;
                            }
                            mCurPlayerInput.clearInput();
                            mGraphics.setNeedScreenRedraw();
                            break;
                    }
                }

                if (keyDown) {
	                switch (curKey) {
	                    case TURRET_LEFT:
	                        curPlayer.turretLeft();
	                        mGraphics.setNeedScreenRedraw();
	                        break;
	                    case TURRET_RIGHT:
	                        curPlayer.turretRight();
	                        mGraphics.setNeedScreenRedraw();
	                        break;
	                    case POWER_UP:
	                        curPlayer.powerUp();
	                        mGraphics.setNeedScreenRedraw();
	                        break;
	                    case POWER_DOWN:
	                        curPlayer.powerDown();
	                        mGraphics.setNeedScreenRedraw();
	                        break;
	                    case FIRE:
	                        mCurPlayerInput.changeIgnoreInput(true);
	                        return GameState.BALLISTICS;
	                    case QUIT:
	                        mCurPlayerInput.changeIgnoreInput(true);
	                        return GameState.QUIT;
	                }
                }
                keyDown = false;
            }
        }

        private void runScreenRefresh()
        {
            // redraw canvas if necessary
            Canvas canvas = null;
            try {
                if (mGraphics.needScreenUpdate()) {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    mGraphics.drawScreen(canvas);
                }
            }
            finally {
                if (canvas != null) {
                    // Don't leave the Surface in an inconsistent state
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
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

        /**
         * Handles a key-down event.
         * 
         * @param keyCode the key that was pressed
         * @param msg the original event object
         * @return true
         */
        boolean doKeyDown(int keyCode, KeyEvent msg) {
            Log.w(TAG, "doKeyDown");
        	try {
	        	switch (keyCode) {
	                case KeyEvent.KEYCODE_DPAD_UP:
	                    return mCurPlayerInput.pressKey(
	                            PlayerInput.PlayerKeys.POWER_UP);
	                case KeyEvent.KEYCODE_DPAD_DOWN:
	                    return mCurPlayerInput.pressKey(
	                            PlayerInput.PlayerKeys.POWER_DOWN);
	                case KeyEvent.KEYCODE_DPAD_LEFT:
	                    return mCurPlayerInput.pressKey(
	                            PlayerInput.PlayerKeys.TURRET_LEFT);
	                case KeyEvent.KEYCODE_DPAD_RIGHT:
	                    return mCurPlayerInput.pressKey(
	                            PlayerInput.PlayerKeys.TURRET_RIGHT);
	                default:
	                    break;
	            }
        	}
        	catch (InterruptedException e) {
        		return false;
        	}
            return false;
        }

        /**
         * Handles a key-up event.
         * 
         * @param keyCode the key that was released
         * @param msg the original event object
         * @return true if the key was handled and consumed, or else false
         */
        boolean doKeyUp(int keyCode, KeyEvent msg) {
            Log.w(TAG, "doKeyUp");
        	try {
	        	switch (keyCode) {
	                case KeyEvent.KEYCODE_DPAD_UP:
	                    return mCurPlayerInput.releaseKey(
                                        PlayerInput.PlayerKeys.POWER_UP);
	                case KeyEvent.KEYCODE_DPAD_DOWN:
	                    return mCurPlayerInput.releaseKey(
                                        PlayerInput.PlayerKeys.POWER_DOWN);
	                case KeyEvent.KEYCODE_DPAD_LEFT:
	                    return mCurPlayerInput.releaseKey(
                                        PlayerInput.PlayerKeys.TURRET_LEFT);
	                case KeyEvent.KEYCODE_DPAD_RIGHT:
	                    return mCurPlayerInput.releaseKey(
                                        PlayerInput.PlayerKeys.TURRET_RIGHT);
	                default:
	                    break;
	            }
	            return false;
	        }
        	catch (InterruptedException e) {
        		return false;
        	}
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
    public ScorchedModel mModel = null;

    /** Pointer to the view */
    public ScorchedGraphics mGraphics = null;

    /*================= Accessors =================*/
    /** Fetches the animation thread for this ScorchedView. */
    public ScorchedThread getThread() {
        return mThread;
    }

    /*================= User Input Operations =================*/
    /** Standard override to get key-press events. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        return mThread.doKeyDown(keyCode, msg);
    }

    /** Standard override to get key-up (released) events. */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent msg) {
        return mThread.doKeyUp(keyCode, msg);
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus)
            mThread.pause();
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        mThread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (mSurfaceHasBeenCreatedSem) {
            // Wake up mThread.run() if it's waiting for the surface to have
            // ben created
            mSurfaceHasBeenCreated = true;
            mSurfaceHasBeenCreatedSem.notify();
        }
        Log.w(TAG, "surfaceCreated(): set mSurfaceHasBeenCreated");
    }

    /*
     * Callback invoked when the Surface has been destroyed and must 
     * no longer be touched. 
     * WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
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
    public ScorchedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(ScorchedModel model, ScorchedGraphics graphics)
    {
        mModel = model;
        mGraphics = graphics;

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // Create animation thread
        mThread = new ScorchedThread(mGraphics, holder, getContext(), 
            new Handler() {
                @Override
                public void handleMessage(Message m) {
                    //mStatusText.setVisibility(m.getData().getInt("viz"));
                    //mStatusText.setText(m.getData().getString("text"));
                }
        });

        setFocusable(true); // make sure we get key events
        
        // Start the animation thread
        mThread.start();
    }
}
