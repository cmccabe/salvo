package scorched.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
 * View (and Controller) for the Scorched Android game
 * 
 * The view displays stuff on the screen and otherwise presents game state to
 * the viewer. Ideally, this file would be mostly user interface stuff.
 * 
 * 'SurfaceView' in Android seems to double as a controller. It gets keystroke
 * and touchpad callbacks. This is ok for now. If it gets too messy, we can
 * always create a separate controller class later.
 */
class ScorchedView extends SurfaceView implements SurfaceHolder.Callback {
    /*================= Constants =================*/
    private static final String TAG = "ScorchedView";

    /*================= ScorchedThread =================*/
    class ScorchedThread extends Thread {
        /** The drawable to use as the background of the animation canvas */
        //private Bitmap mBackground;

        /**
         * Current height of the surface/canvas.
         * 
         * @see #setSurfaceSize
         */
        private int mCanvasHeight = 1;

        /**
         * Current width of the surface/canvas.
         * 
         * @see #setSurfaceSize
         */
        private int mCanvasWidth = 1;

        /** Indicate whether the surface has been created & is ready
         *  to draw 
         */
        private boolean mRun = false;

        /** Indicate whether or not the game is paused */
        private boolean mPaused = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** Handle to the application context
         *  used to e.g. fetch Drawables. */
        private Context mContext;

        /** Paint to draw the lines on screen. */
        private Paint mPaint, mClear, mTerrainPaint;

        /** Scratch rect object. */
        private RectF mScratchRect;
        
        public ScorchedThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            Resources res = context.getResources();

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setARGB(255, 0, 255, 0);
            
            mClear = new Paint();
            mClear.setAntiAlias(false);
            mClear.setARGB(255, 0, 0, 0);

            mTerrainPaint = new Paint();
            mTerrainPaint.setAntiAlias(false);
            mTerrainPaint.setARGB(255, 0, 255, 255);

            mScratchRect = new RectF(0, 0, 0, 0);
        }

        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                mPaused = false;
            }
        }

        /**
         * Pauses the physics update & animation.
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

        @Override
        public void run() {
            /* wait for the Surface to be ready */
            Log.w(TAG, "run(): waiting for surface to be created.");
            synchronized (mSurfaceHasBeenCreatedLock) {
                if (!mSurfaceHasBeenCreated) {
                    while (true) {
                        try {
                            mSurfaceHasBeenCreatedLock.wait();
                            break;
                        }
                        catch (InterruptedException e) {
                            Log.w(TAG, "run(): interrupted");
                            // continue to wait
                        }
                    }
                }
            }
            Log.w(TAG, "run(): surface has been created.");

            while (mRun) {
                /* Draw stuff */
                Canvas c = null;
                try {
                    synchronized (mSurfaceHolder) {
                        if (mNeedScreenRedraw) {
                            c = mSurfaceHolder.lockCanvas(null);
                            drawScreen(c);
                            mNeedScreenRedraw = false;
                        }
                    }

                    // if (mNeedBallisticsDraw) ...
                }
                finally {
                    // Don't leave the Surface in an inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

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
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
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
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         * 
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            synchronized (mSurfaceHolder) {
                mRun = b;
            }
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
                mNeedScreenRedraw = true;
                // don't forget to resize the background image
                //mBackgroundImage = mBackgroundImage.createScaledBitmap(
                        //mBackgroundImage, width, height, true);
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
            synchronized (mSurfaceHolder) {
                Log.w(TAG, "Pressin dat key");
                switch (keyCode)
                {
                case KeyEvent.KEYCODE_DPAD_UP:
                    //rectY--;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    //rectY++;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    //rectX--;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    //rectX++;
                    break;
                default:
                    Log.w(TAG, "Fuckin fake ass key");    
                }
            }
            return true;
        }

        /**
         * Handles a key-up event.
         * 
         * @param keyCode the key that was pressed
         * @param msg the original event object
         * @return true if the key was handled and consumed, or else false
         */
        boolean doKeyUp(int keyCode, KeyEvent msg) {
            boolean handled = false;

            synchronized (mSurfaceHolder) {
                // foo
            }

            return handled;
        }

        /**
         * Draws everything
         */
        private void drawScreen(Canvas canvas) {
            assert(ScorchedModel.MAX_HEIGHTS % 
                    ScorchedModel.HEIGHTS_PER_POLY == 0);
            Log.w(TAG, "running drawScreen with mCanvasWidth = " + mCanvasWidth +
            		", mCanvasHeight = " + mCanvasHeight);
            
            mScratchRect.set(0, 0, mCanvasWidth, mCanvasHeight);
            canvas.drawRect(mScratchRect, mClear);

            mScratchRect.set(0,0,50,100);
            canvas.drawRect(mScratchRect, mPaint);

            float x = 0;
            float dx = mCanvasWidth / (ScorchedModel.MAX_HEIGHTS - 1);
            float h[] = mModel.getHeights();
            for (int i = 0; 
                i < ScorchedModel.MAX_HEIGHTS - 2;
                i += 2) 
            {
                Path p = new Path();
                p.moveTo(x, heightToScreenHeight(h[i]));
                p.lineTo(x + dx, heightToScreenHeight(h[i+1]));
                p.lineTo(x + dx + dx, heightToScreenHeight(h[i+2]));
                p.lineTo(x + dx + dx, mCanvasHeight);
                p.lineTo(x, mCanvasHeight);
                x += (2 * dx);
                canvas.drawPath(p, mTerrainPaint);
            }
        }

        private float heightToScreenHeight(float h) {
            return mCanvasHeight - (h * mCanvasHeight); 
        }
    }

    /*================= Members =================*/
    /** The thread that draws the animation */
    private ScorchedThread mThread;

    /** True only once the Surface has been created and is ready to be used */
    private Object mSurfaceHasBeenCreatedLock = new Object();
    private boolean mSurfaceHasBeenCreated = false;

    /** Pointer to the model */
    public ScorchedModel mModel;

    /** true if the screen needs to be redrawn */
    public boolean mNeedScreenRedraw;

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
        synchronized (mSurfaceHasBeenCreatedLock) {
            // Wake up mThread.run() if it's waiting for the surface to have
            // ben created
            mSurfaceHasBeenCreated = true;
            mSurfaceHasBeenCreatedLock.notify();
        }
        Log.w(TAG, "surfaceCreated(): set mSurfaceHasBeenCreated");
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        mThread.setRunning(false);
        while (retry) {
            try {
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    /*================= Lifecycle =================*/
    public ScorchedView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // Create animation thread
        mThread = new ScorchedThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                //mStatusText.setVisibility(m.getData().getInt("viz"));
                //mStatusText.setText(m.getData().getString("text"));
            }
        });

        setFocusable(true); // make sure we get key events
    }

    public void initialize(ScorchedModel model)
    {
        mModel = model;
        mNeedScreenRedraw = true;
        
        // Start the animation thread
        mThread.setRunning(true);
        mThread.start();
    }
}
