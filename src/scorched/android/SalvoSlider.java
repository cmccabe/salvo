package scorched.android;

import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A slider widget which the user can slide back and forth.
 * 
 * There are arrows on the ends for fine adjustments.
 */
public class SalvoSlider extends View {
    /*================= Types =================*/
    /** Used to hook up Listeners */
    public static interface Listener {
        void onPositionChange(int val);
    }

    /*================= Constants =================*/
    private static final String TAG = "SalvoSlider";

    private static final int BUTTON_PERCENT = 20;
    
    /*================= Members =================*/
    /** Listener to notify when slider value changes */
    private Listener mListener;

    /** Minimum slider value */
    public int mMin;
    
    /** Maximum slider value. */
    public int mMax;

    public int mLen;

    /** Current slider value */
    private int mVal;

    /** Background drawable element */
    public Drawable background;
    
    private Paint mTempPaint; 

    private Rect mTempRect;
    
    /*================= Access =================*/
    private int getSliderLeftBound() {
        return (getWidth() * BUTTON_PERCENT) / 100;
    }

    private int getSliderRightBound() {
        return (getWidth() * (100 - BUTTON_PERCENT)) / 100;
    }

    /*================= Operations =================*/
    @Override
    protected void onDraw(Canvas canvas) {
    	if (mLen == 0) {
    		// we haven't initialized this object yet...
    		return;
    	}
        super.onDraw(canvas);
        int sl = getSliderLeftBound();
        int sr = getSliderRightBound();
        int w = getWidth();
        int h = getHeight();

        mTempPaint.setColor(Color.GRAY);
        mTempRect.set(0, 0, sl, h);
        canvas.drawRect(mTempRect, mTempPaint);

        mTempPaint.setColor(Color.CYAN);
        mTempRect.set(sr, 0, w, h);
        canvas.drawRect(mTempRect, mTempPaint);

        int x = sl + (((sr - sl) * mVal) / mLen);
        mTempPaint.setColor(Color.RED);
        mTempRect.set(sl, 0, x, h);
        canvas.drawRect(mTempRect, mTempPaint);
        mTempPaint.setColor(Color.BLACK);
        mTempRect.set(sl, h + 1, x, sr);
        canvas.drawRect(mTempRect, mTempPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        /*int action = me.getAction();
        if ((action == MotionEvent.ACTION_DOWN) ||
            (action == MotionEvent.ACTION_MOVE) ||
            (action == MotionEvent.ACTION_UP)) 
        {
            int newVal = me.getX();
        }*/
    	return true;
    }

    /*================= Lifecycle =================*/
    private void construct() {
        mListener = null;
        mMin = 0;
        mMax = 0;
        mLen = 0;
        mVal = 0;
        background = null;
        mTempPaint = new Paint();
        mTempRect = new Rect();
        setFocusable(true);
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

    public void initialize(int min, int max, Listener listener) {
        mListener = listener;
        mMin = min;
        mMax = max;
        mLen = Math.abs(max - min);
        mVal = (mLen * 3) / 4;
        invalidate();
    }
}
