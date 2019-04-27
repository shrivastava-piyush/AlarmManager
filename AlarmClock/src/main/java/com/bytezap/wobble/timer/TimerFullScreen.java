/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.timer;

import android.animation.Animator;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
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

public class TimerFullScreen extends AppCompatActivity{

    private BigChronometer timer;
    private TextView timerText;
    private CountDownTimer countDownTimer;
    private long remainingTime;
    private int previousSecond;
    private SoundController controller;

    private AdView adView;
    private final Runnable adsRunnable = new Runnable() {

        @Override
        public void run() {
            if (!LicenceController.isIsAdFirstTime()) {
                adView = findViewById(R.id.timer_full_adView);
                adView.setVisibility(View.VISIBLE);
                adView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build());
            }
        }
    };

    private final Handler timerHandler = new Handler();

    public TimerFullScreen() {
        remainingTime = 0;
        previousSecond = 0;
    }

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

        setContentView(R.layout.timer_full_screen);

        timer = findViewById(R.id.custom_timer_big);
        timerText = findViewById(R.id.timer_full_text);
        TextView modeText = findViewById(R.id.timer_fullscreen);
        modeText.setTextColor(ThemeDetails.getThemeRadiance(BitmapController.getThemeNumber()));

        controller = SoundController.getInstance(getApplicationContext());
        if (savedInstanceState == null) {
            remainingTime = getIntent().getLongExtra(TimerFragment.REMAINING_TIME, 0);
            if (remainingTime == 0) {
                finish();
            }
        } else {
            remainingTime = savedInstanceState.getLong(TimerFragment.REMAINING_TIME);
        }

        countDownTimer = new CountDownTimer(remainingTime, TimerFragment.TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished;
                updateTimer(millisUntilFinished);
                int currentSecond = (int) remainingTime / 1000;
                if (currentSecond < previousSecond && isSound()) {
                    controller.tick();
                }
                previousSecond = currentSecond;
            }

            @Override
            public void onFinish() {
                updateTimer(0);
            }

        };
        countDownTimer.start();
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

    private void updateTimer(long time) {

        if (time < 0) {
            time = 0;
        }

        timer.updateChronometer((int) time);

        int seconds = (int) (time / 1000);
        int minutes = seconds / 60;
        int hours = (minutes / 60);

        minutes = minutes % 60;
        seconds = seconds % 60;
        time = time % 1000;
        String hourFormat = hours >= 100 ? "%03d" : "%02d";

        timerText.setText(String.format(hourFormat + ":%02d:%02d.%03d", hours, minutes, seconds, time));
    }

    @Override
    public void onUserInteraction() {
        if (countDownTimer!=null) {
            countDownTimer.cancel();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        if (adView!=null) {
            adView.destroy();
        }
        timerHandler.removeCallbacks(adsRunnable);
        super.onDestroy();
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
        super.finish();
        overridePendingTransition(0,0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(TimerFragment.REMAINING_TIME, remainingTime);
        super.onSaveInstanceState(outState);
    }
}
