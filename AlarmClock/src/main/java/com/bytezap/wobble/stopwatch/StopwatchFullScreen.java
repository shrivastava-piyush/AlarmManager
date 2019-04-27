/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.stopwatch;

import android.animation.Animator;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.customviews.BigChronometer;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.LicenceController;
import com.bytezap.wobble.utils.LinkDetector;
import com.bytezap.wobble.utils.SoundController;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class StopwatchFullScreen extends AppCompatActivity{

    private static final String LAST_TIME = "last_time";
    private static final String PREVIOUS_SECOND = "previous_second";
    private long timeInMillis = 0, lastTime = 0;
    private int previousSecond = 0;
    private SoundController controller;
    private BigChronometer stopwatch;
    private TextView chronoText;

    private final Handler stopwatchHandler = new Handler();
    private final Runnable chronoRunnable = new Runnable() {
        @Override
        public void run() {

            long currentTime = System.currentTimeMillis();
            timeInMillis += (currentTime - lastTime);

            if (timeInMillis < 0) timeInMillis = 0;

            try {
                stopwatch.updateChronometer((int) timeInMillis);
                updateStopwatch(timeInMillis);

                int currentSecond = (int) timeInMillis / 1000;
                if (currentSecond > previousSecond && isSound()) {
                    controller.tick();
                }
                previousSecond = currentSecond;
                lastTime = currentTime;
            } catch (Throwable b) {
                Log.e(StopwatchFullScreen.class.getSimpleName(), b.getMessage());
            }

            stopwatchHandler.postDelayed(chronoRunnable, StopwatchFragment.STOPWATCH_INTERVAL);
        }
    };

    private AdView adView;
    private final Runnable adsRunnable = new Runnable() {

        @Override
        public void run() {
            if (!LicenceController.isIsAdFirstTime()) {
                adView = findViewById(R.id.stopwatch_full_adView);
                adView.setVisibility(View.VISIBLE);
                adView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);

        Window window = getWindow();
        Resources res = getResources();

        window.addFlags((WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN));

        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        if (CommonUtils.is16OrLater()) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }

        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        if (background!=null) {
            getWindow().setBackgroundDrawable(background);
        } else {
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        setContentView(R.layout.stopwatch_full_screen);

        stopwatch = findViewById(R.id.custom_stopwatch_big);
        chronoText = findViewById(R.id.stopwatch_full_text);
        controller = SoundController.getInstance(getApplicationContext());

        if (savedInstanceState == null) {
            timeInMillis = getIntent().getLongExtra(StopwatchFragment.TIME_MILLI_SEC, 0);
            if (timeInMillis == 0) {
                finish();
            }
            lastTime = System.currentTimeMillis();
        } else {
            timeInMillis = savedInstanceState.getLong(StopwatchFragment.TIME_MILLI_SEC);
            lastTime = savedInstanceState.getLong(LAST_TIME);
            previousSecond = savedInstanceState.getInt(PREVIOUS_SECOND);
        }

        TextView modeText = findViewById(R.id.stopwatch_fullscreen);
        modeText.setTextColor(ThemeDetails.getThemeRadiance(BitmapController.getThemeNumber()));

        stopwatchHandler.removeCallbacks(chronoRunnable);
        stopwatchHandler.post(chronoRunnable);
        showModeText(modeText);

        //Ads
        if (!LicenceController.checkLicense(getApplicationContext())) {
            LinkDetector detector = new LinkDetector(getApplicationContext());
            if (detector.isNetworkAvailable()) {
                //adsHandler.post(adsRunnable);
                runOnUiThread(adsRunnable);
            }
        }
    }

    private boolean isSound() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.SOUND_EFFECT, true);
    }

    @Override
    public void onUserInteraction() {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (adView!=null) {
            adView.destroy();
        }
        stopwatchHandler.removeCallbacks(adsRunnable);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(StopwatchFragment.TIME_MILLI_SEC, timeInMillis);
        outState.putLong(LAST_TIME, lastTime);
        outState.putInt(PREVIOUS_SECOND, previousSecond);
        super.onSaveInstanceState(outState);
    }

    private void updateStopwatch(long time) {
        if (time < 0) {
            time = 0;
        }

        long seconds = (time / 1000);
        long minutes = seconds / 60;
        long hours = minutes / 60;

        minutes = minutes % 60;
        seconds = seconds % 60;
        long milliseconds = (time % 1000);

        String hourFormat = hours >= 100 ? "%03d" : "%02d";

        chronoText.setText(String.format(hourFormat + ":%02d:%02d.%03d", hours, minutes, seconds, milliseconds));
    }

    private void showModeText(final TextView modeText) {
        final ViewPropertyAnimator modeAnimator = modeText.animate();

        modeAnimator
                .alpha(1)
                .setStartDelay(300)
                .setDuration(1000)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        modeText.animate();
                        modeAnimator
                                .alpha(0)
                                .setStartDelay(300)
                                .setDuration(1000)
                                .start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                })
                .start();
    }

    @Override
    public void finish() {
        stopwatchHandler.removeCallbacks(chronoRunnable);
        super.finish();
        overridePendingTransition(0,0);
    }
}
