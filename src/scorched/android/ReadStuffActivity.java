package scorched.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ReadStuffActivity extends Activity {
    /*================= Types =================*/
    public enum StuffText {
        /*================= Values =================*/
        ABOUT_BOX(
"in no event unless required by applicable law or agreed to in writing " +
"will any copyright holder, or any other party who may modify and/or " +
"redistribute the program as permitted above, be liable to you for " +
"damages, including any general, special, incidental or consequential " +
"damages arising out of the use or inability to use the program " +
"(including but not limited to loss of data or data being rendered " +
"inaccurate or losses sustained by you or third parties or a failure of " +
"the program to operate with any other programs), even if such holder " +
"or other party has been advised of the possibility of such damages."),
        HELP_BOX("Foobah");

        /*================= Static =================*/
        static StuffText fromCode(int code) {
            StuffText[] stuffs = StuffText.class.getEnumConstants();
            return stuffs[code];
        }

        /*================= Access =================*/
        public int toCode() {
            return ordinal();
        }

        public String getText() {
            return mText;
        }

        /*================= Lifecycle =================*/
        private StuffText(String text) {
            mText = text;
        }

        /*================= Data =================*/
        final String mText;
    }

    /*================= Constants =================*/
    final static String STUFF_TYPE_EXTRA = "STUFF_TYPE_EXTRA";

    /*================= Operations =================*/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StuffText[] stuffs = StuffText.class.getEnumConstants();
        int code = getIntent().getIntExtra(STUFF_TYPE_EXTRA,
                                            stuffs[0].ordinal());

        ////////////////// setContentView
        setContentView(R.layout.read_stuff);

        ////////////////// Get pointers to stuff
        final TextView stuff = (TextView)findViewById(R.id.stuff);
        final Button back = (Button)findViewById(R.id.back);

        ////////////////// Initialize stuff
        StuffText myText = StuffText.fromCode(code);
        stuff.setText(myText.getText());
        Log.w(this.getClass().getName(),
            "StuffText(" + myText.ordinal() + ") = " + myText.getText());

        back.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) { }
}
