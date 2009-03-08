package scorched.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;


/**
 * Controller for the Scorched Android game.
 * 
 * GameControlView gets input from the user, as well as events from other
 * parts of the system, and presents them to mGraphics and mModel.
 */
class GameControlView extends SurfaceView implements SurfaceHolder.Callback {
    /*================= Constants =================*/
    private static final String TAG = "GameControlView";

    private enum GameState {
        IDLE,
        PLAYER_MOVE,
        BALLISTICS,
        EXPLOSION,
        QUIT,
    };

    /*================= Types =================*/

    /*================= ScorchedThread =================*/
    class ScorchedThread extends Thread {
        /** The semaphore representing user input during a player's turn */
        private Object mUserInputSem = new Object();

        /** The game state we should transition to next. Protected by
          * mUserInputSem */
        private GameState mNextGameState;

        /** Represents the current controller state */
        volatile private GameState mGameState;

        /** Indicate whether or not the game is paused */
        private boolean mPaused = false;

        /** Pointer to the view */
        public Graphics mGraphics = null;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** Handle to the application context
         *  used to e.g. fetch Drawables. */
        private Context mContext;

        /** The zoom, pan settings that were in effect when the user started
         * pressing on the screen */ 
        private Graphics.ViewSettings mTouchViewSettings;

        /** Last X coordinate the user touched (in game coordinates) */
        private float mTouchX;

        /** Last Y coordinate the user touched (in game coordinates) */
        private float mTouchY;
        
        public ScorchedThread(Graphics graphics,
                            SurfaceHolder surfaceHolder, 
                            Context context,
                            Handler handler) {
            mGraphics = graphics;
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            mGameState = GameState.PLAYER_MOVE;
            mTouchViewSettings = null;
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
        	synchronized (mSurfaceHolder) {
                mGraphics.setSurfaceSize(width, height);
            }
        	synchronized (mUserInputSem) {
        		mUserInputSem.notify();
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
                    switch (mGameState) {
                        case IDLE:
                            // Should not get here
                            throw new RuntimeException(
                                "Got mGameState = IDLE in main loop");
    
                        case PLAYER_MOVE: {
                            // The player is moving around his 
                            // turret, etc. 
                            Player curPlayer = mModel.getCurPlayer();
                            if (curPlayer.isHuman()) {
                                mGameState = runHumanMove(curPlayer);
                            }
                            else {
                                throw new RuntimeException("unimplemented");
                            }
                        }
                        break;
    
                        case BALLISTICS: {
                            // The projectile is moving through the sky
                            Player curPlayer = mModel.getCurPlayer();
                            mGameState = runBallistics(curPlayer);
                        }
                        break;
    
                        case EXPLOSION: {
                            Log.w(TAG, "entering EXPLOSION state");
                            // The projectile is exploding onscreen
                            Canvas canvas = mSurfaceHolder.lockCanvas(null);
                            Paint p = new Paint();
                            p.setAntiAlias(false);
                            p.setARGB(128, 255, 0, 255);
                            Rect rect = new Rect();
                            rect.set(0, 0, 200, 200);
                            canvas.drawRect(rect, p);
                            //mGameState = runExplosion();
                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                            Thread.sleep(10000);
                        }
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
            synchronized (mUserInputSem) {
                mNextGameState = GameState.PLAYER_MOVE;
            }

            while (true) {
                runScreenRefresh(curPlayer, null);
                synchronized (mUserInputSem) {
                    mUserInputSem.wait();
                    if (mNextGameState != GameState.PLAYER_MOVE) {
                        return mNextGameState;
                    }
                }
            }
        }

        private GameState runBallistics(Player curPlayer)
            throws InterruptedException
        {
            synchronized (mUserInputSem) {
                mNextGameState = GameState.BALLISTICS;
            }

            Weapon weapon = curPlayer.getWeapon();
            while (true) {
                runScreenRefresh(curPlayer, weapon);
                synchronized (mUserInputSem) {
                    //mUserInputSem.wait(1);
                    weapon.nextSample();
                    Weapon.Point collisionPoint = weapon.testCollision();
                    if (collisionPoint != null) {
                    	mNextGameState = GameState.EXPLOSION;
                    }
                    if (mNextGameState != GameState.BALLISTICS) {
                        return mNextGameState;
                    }
                    if (weapon.getNeedsRedraw()) {
                    	mGraphics.setNeedScreenRedraw();
                    }
                }
            }
        }

        private void runScreenRefresh(Player player, Weapon weapon)
        {
            // redraw canvas if necessary
            Canvas canvas = null;
            try {
                if (mGraphics.needScreenUpdate()) {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    mGraphics.drawScreen(canvas);
                    if (weapon != null) {
                        mGraphics.drawWeapon(canvas, weapon, player);
                    }
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
            if (mGameState != GameState.PLAYER_MOVE) {
                Log.w(TAG, "doKeyDown: ignoring");
                return false;
            }

            if (!isKnownKey(keyCode)) {
                return false;
            }

            Log.w(TAG, "doKeyDown");

            synchronized (mUserInputSem) {
                Player p = mModel.getCurPlayer();
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        p.powerUp();
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        p.powerDown();
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        p.turretLeft();
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        p.turretRight();
                        break;
                    case KeyEvent.KEYCODE_H:
                        mGraphics.viewLeft();
                        break;
                    case KeyEvent.KEYCODE_I:
                        mGraphics.zoomIn();
                        break;
                    case KeyEvent.KEYCODE_J:
                        mGraphics.viewDown();
                        break;
                    case KeyEvent.KEYCODE_K:
                        mGraphics.viewUp();
                        break;
                    case KeyEvent.KEYCODE_L:
                        mGraphics.viewRight();
                        break;
                    case KeyEvent.KEYCODE_Q:
                        // quit.
                        mNextGameState = GameState.QUIT;
                        break;
                    case KeyEvent.KEYCODE_SPACE:
                        // launch!
                        mNextGameState = GameState.BALLISTICS; 
                            //GameState.EXPLOSION;
                        break;
                    case KeyEvent.KEYCODE_U:
                        mGraphics.zoomOut();
                        break;
                    default:
                        throw new RuntimeException("can't handle keycode " +
                        		                   keyCode);
                }
                mGraphics.setNeedScreenRedraw();
                mUserInputSem.notify();
            }
            return true;
        }

        private boolean isKnownKey(int keyCode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_H:
                case KeyEvent.KEYCODE_I:
                case KeyEvent.KEYCODE_J:
                case KeyEvent.KEYCODE_K:
                case KeyEvent.KEYCODE_L:
                case KeyEvent.KEYCODE_Q:
                case KeyEvent.KEYCODE_SPACE:
                case KeyEvent.KEYCODE_U:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Handles a key-up event.
         * 
         * @param keyCode the key that was released
         * @param msg the original event object
         * @return true if the key was handled and consumed, or else false
         */
        boolean doKeyUp(int keyCode, KeyEvent msg) {
            return false;
        }

        /** Handles a touchscreen event */
        public boolean onTouchEvent(MotionEvent me) {
            int action = me.getAction();
            if ((action == MotionEvent.ACTION_DOWN) ||
                (action == MotionEvent.ACTION_MOVE) ||
                (action == MotionEvent.ACTION_UP)) 
            {
                if (mTouchViewSettings == null) {
                    mTouchViewSettings = mGraphics.getViewSettings();
                    mTouchX = mGraphics.
                        onscreenXtoGameX(me.getX(), mTouchViewSettings);
                    mTouchY = mGraphics.
                        onscreenYtoGameY(me.getY(), mTouchViewSettings);
                }
                else {
                    float x = mGraphics.
                            onscreenXtoGameX(me.getX(), mTouchViewSettings);
                    float y = mGraphics.
                            onscreenYtoGameY(me.getY(), mTouchViewSettings);
                    mGraphics.scrollBy(mTouchX - x, -(mTouchY - y));
                    synchronized (mUserInputSem) {
                        mUserInputSem.notify();
                    }
                    mTouchX = x;
                    mTouchY = y;
                }
            }
            // TODO: do edgeflags?

            if (action == MotionEvent.ACTION_UP) {
                mTouchViewSettings = null;
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

    /** Pointer to the view */
    public Graphics mGraphics = null;
    
    /*================= Accessors =================*/
    /** Fetches the animation thread for this GameControlView. */
    public ScorchedThread getThread() {
        return mThread;
    }

    /*================= User Input Operations =================*/
    public boolean onTouchEvent(MotionEvent me) {
    	return mThread.onTouchEvent(me);
    }	
        
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
    public GameControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(Model model, Graphics graphics)
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
