package com.jamal.aegistest2;

/**
 * Created by Jamal on 15-Feb-16.
 */
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

public class Splash extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 7400;
    private static int PROGRESS_TIME_OUT = 5400;
    private static int PROGRESS_res = 2500;
    private static int PROGRESS_init = 3500;
    private static int PROGRESS_final = 5000;

    CircularProgressView progressView;
    TextView status_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        progressView = (CircularProgressView) findViewById(R.id.progress_view);
        status_text = (TextView) findViewById(R.id.status);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status_text.setText("Loading Resources...");
            }
        }, PROGRESS_res);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status_text.setText("Initialising Parameters...");
            }
        }, PROGRESS_init);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status_text.setText("Finalising Views...");
            }
        }, PROGRESS_final);

        //----------------------------------------------------
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status_text.setText("Welcome to AEGIS");
                progressView.setIndeterminate(false);
                progressView.setProgress(100);
            }
        }, PROGRESS_TIME_OUT);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Splash.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}