package scorched.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * A slider widget which the user can slide back and forth.
 *
 * There are arrows on the ends for fine adjustments.
 *
 * Most functions are synchronized on mState to prevent wackiness. The mState
 * mutex should generally be held a pretty short amount of time.
 */
public class SalvoSlider extends View {
    /* ================= Types ================= */
    /** Describes the state of this slider */
    public enum SliderState {
        /**
         * The slider is not in use. Only a blank space will be drawn. Touch
         * will be disabled.
         */
        DISABLED,

        /**
         * The slider will be drawn using a bar graphic. Touch will be enabled.
         */
        BAR,

        /**
         * The slider will be drawn using the angle graphic. Touch will be
         * enabled.
         */
        ANGLE,
    };

    /** Used to hook up Listeners */
    public static interface Listener {
        void onPositionChange(int val);
    }

    /* ================= Constants ================= */
    private static final String TAG = "SalvoSlider";

    private static final int BUTTON_PERCENT = 20;

    /* ================= Members ================= */
    private SliderState mState;

    // //// User input stuff
    /** Listener to notify when slider value changes */
    private Listener mListener;

    /** Minimum slider value */
    private int mMin;

    /** Maximum slider value. */
    private int mMax;

    /** True if the slider's value increases left-to-right rather
     *  than left-to-right */
    private boolean mReversed;

    /** Current slider value */
    private int mVal;

    // //// Current configuration
    /**
     * Current slider color. If mColor == Color.WHITE then the slider is
     * disabled.
     */
    private int mColor;

    /** Current slider width */
    private int mWidth;

    /** Current slider height */
    private int mHeight;

    // //// Things computed by cacheStuff()
    /** Current left boundary of slidable area */
    private int mLeftBound;

    /** Current right boundary of slidable area */
    private int mRightBound;

    /** Gradient paint for bar */
    private Paint mBarPaint;

    /** Paint for text that's drawn on bars */
    private Paint mFontPaint;

    /////// Temporaries
    private Paint mTempPaint;

    private Rect mTempRect;

    private Path mTempPath;

    /* ================= Access ================= */

    /* ================= Operations ================= */
    /**
     * Cache a bunch of stuff that we don't want to have to recalculate on each
     * draw().
     */
    private void cacheStuff() {
        assert Thread.holdsLock(mState);
        mLeftBound = (mWidth * BUTTON_PERCENT) / 100;
        mRightBound = (mWidth * (100 - BUTTON_PERCENT)) / 100;

        int colors[] = new int[3];
        colors[0] = Color.WHITE;
        colors[1] = mColor;
        colors[2] = Color.WHITE;
        Shader barShader = new LinearGradient(0, 0, 0, (mHeight * 3) / 2,
                colors, null, Shader.TileMode.REPEAT);
        mBarPaint = new Paint();
        mBarPaint.setShader(barShader);

        mFontPaint = new Paint();
        mFontPaint.setColor(Color.WHITE);
        mFontPaint.setAntiAlias(true);
        adjustTypefaceToFit(mFontPaint,
            (mHeight * 4) / 5, Typeface.SANS_SERIF);
        mFontPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int x;
        synchronized (mState) {
            super.onDraw(canvas);
            switch (mState) {
            case DISABLED:
                canvas.drawColor(Color.BLACK);
                break;

            case BAR:
                canvas.drawColor(Color.BLACK);
                x = mLeftBound
                        + (((mRightBound - mLeftBound) * mVal) /
                            (mMax - mMin));
                mTempRect.set(mLeftBound, 0, x, mHeight);
                canvas.drawRect(mTempRect, mBarPaint);
                drawEndButtons(canvas);
                drawSliderText(canvas);
                break;

            case ANGLE:
                canvas.drawColor(Color.BLACK);

                int totalWidth = mRightBound - mLeftBound;
                int w = totalWidth / 6;
                x = mLeftBound
                        + ((totalWidth * mVal) / (mMax - mMin));
                mTempPath.moveTo(x, 0);
                mTempPath.lineTo(x - w, mHeight);
                mTempPath.lineTo(x + w, mHeight);
                mTempPaint.setColor(Color.argb(255, 236, 189, 62));
                mTempPaint.setAntiAlias(true);
                mTempPaint.setStrokeWidth(1);
                canvas.drawPath(mTempPath, mTempPaint);
                mTempPath.rewind();
                drawEndButtons(canvas);
                drawSliderText(canvas);
                break;
            }
        }
    }

    private void drawSliderText(Canvas canvas) {
        String str;
        if (mReversed) {
            str = "" + (mMax - mVal);
        }
        else {
            str = "" + mVal;
        }
        canvas.drawText(str, mWidth / 2, (mHeight * 4) / 5, mFontPaint);
    }

    private void adjustTypefaceToFit(Paint p, int height, Typeface tf) {
        p.setTypeface(tf);
        Paint.FontMetrics metrics = new Paint.FontMetrics();
        int size = 40;
        p.setTextSize(size);
        p.getFontMetrics(metrics);
        int fontHeight = (int)(metrics.top + metrics.bottom);
        if (Math.abs(fontHeight - height) > 5) {
            size = (size * height) / fontHeight;
        }
        p.setTextSize(size);
    }

    private void drawEndButtons(Canvas canvas) {
        // Draw end buttons
        drawEndButton(canvas, 0, mLeftBound);
        drawEndButton(canvas, mWidth, mRightBound);
    }

    private void drawEndButton(Canvas canvas, int xLeft, int xRight) {
        mTempPaint.setColor(Color.BLACK);
        mTempPaint.setAntiAlias(false);
        mTempRect.set(xLeft, 0, xRight, mHeight);
        canvas.drawRect(mTempRect, mTempPaint);

        mTempPaint.setARGB(255, 236, 189, 62);
        mTempPaint.setAntiAlias(false);
        mTempPaint.setStrokeWidth(mHeight / 20);
        canvas.drawLine(xLeft, 0, xRight, 0, mTempPaint);
        canvas.drawLine(xLeft, mHeight, xRight, mHeight, mTempPaint);
        canvas.drawLine(xLeft, 0, xLeft, mHeight, mTempPaint);
        canvas.drawLine(xRight, 0, xRight, mHeight, mTempPaint);
        mTempPaint.setStrokeWidth(1);

        int w = xRight - xLeft;
        mTempPath.moveTo(xLeft + (w / 5), mHeight / 2);
        mTempPath.lineTo(xLeft + ((4 * w) / 5), mHeight / 5);
        mTempPath.lineTo(xLeft + ((4 * w) / 5), (mHeight * 4) / 5);
        mTempPaint.setAntiAlias(true);
        canvas.drawPath(mTempPath, mTempPaint);
        mTempPath.rewind();
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        boolean invalidate = false;
        synchronized (mState) {
            if (mState == SliderState.DISABLED) {
                return true;
            }
            int action = me.getAction();
            int x = (int)me.getX();
            if (x < mLeftBound) {
                invalidate = downButton(action);
            }
            else if (x > mRightBound) {
                invalidate = upButton(action);
            }
            else {
                int off = x - mLeftBound;
                int newVal = mMin +
                    (off * (mMax - mMin)) /
                        (mRightBound - mLeftBound);
                if (newVal != mVal) {
                    updateVal(newVal);
                    invalidate = true;
                }
            }
        }
        if (invalidate) {
            invalidate();
        }
        return true;
    }

    private boolean downButton(int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            updateVal(mVal - 1);
            return true;
        }
        else {
            return false;
        }
    }

    private boolean upButton(int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            updateVal(mVal + 1);
            return true;
        }
        else {
            return false;
        }
    }

    private void updateVal(int val) {
        assert Thread.holdsLock(mState);
        if (val < mMin) {
            val = mMin;
        }
        else if (val > mMax) {
            val = mMax;
        }
        mVal = val;
        mListener.onPositionChange(mReversed ? (mMax - mVal) : mVal);
    }

    /**
     * Change the slider state to something else. This can be called from non-UI
     * threads.
     */
    public void setState(SliderState state, Listener listener, int min,
            int max, int val, int color) {
        Log.w(TAG, "setState state=" + state + ",min=" + min + ",max=" + max
                + ",color=" + color);
        synchronized (mState) {
            mState = state;
            mListener = listener;

            // user input stuff
            assert (mListener != null);
            if (min < max) {
                mMin = min;
                mMax = max;
                mReversed = false;
                mVal = val;
            }
            else {
                mMin = max;
                mMax = min;
                mReversed = true;
                mVal = mMax - val;
            }

            // configuration
            mColor = color;
            mWidth = getWidth();
            mHeight = getHeight();
            cacheStuff();
        }

        postInvalidate();
    }

    /* ================= Lifecycle ================= */
    private void construct() {
        mState = SliderState.DISABLED;
        setFocusable(true);

        // Temporaries
        mTempPaint = new Paint();
        mTempRect = new Rect();
        mTempPath = new Path();
    }

    /** Constructor for "manual" instantiation */
    public SalvoSlider(Context context) {
        super(context);
        construct();
    }

    /** Contructor for layout file */
    public SalvoSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct();
    }
}
