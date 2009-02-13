package scorched.android;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.drawable.Drawable;

public class scorched_android extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Drawable redDrawable = Resources.getSystem().getDrawable(R.drawable.color_red);
        //TextView tv = (TextView)findViewByID(R.id.text);
        //tv.setBackgroundColor(redDrawable);
    }
}