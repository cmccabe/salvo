package com.senchas.salvo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class HelpAct extends Activity {
    /*================= Types =================*/

    /*================= Operations =================*/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.help_act);

        ////////////////// Get pointers to stuff
        final WebView webView = (WebView)findViewById(R.id.webview);
        final Button back = (Button)findViewById(R.id.back);

        WebSettings set = webView.getSettings();
        set.setJavaScriptEnabled(false);
        set.setSavePassword(false);
        set.setSaveFormData(false);
        set.setSupportZoom(false);
        webView.loadUrl("file:///android_asset/help.html");

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
