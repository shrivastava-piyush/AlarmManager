/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.clock;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.ElementalAnalogClock;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ScaleText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NightMode extends AppCompatActivity {

    private final Handler clockHandler = new Handler();
    private final Runnable refreshDigital;
    private final Runnable refreshDate;
    private final int flags;
    private final BroadcastReceiver changeReceiver;
    private boolean isDigital, isColoured;
    private TextView cTime, cSeconds;
    private TextView cDate, cAmpm;
    private Window window;
    private Date previousDate;
    private ElementalAnalogClock elementalAnalogClock;
    private SimpleDateFormat timeFormat, secondsFormat, dateFormat, ampmFormat;
    private View mainFrame;
    private Rect rect;
    private int tNumber;

    public NightMode() {
        isDigital = true;
        refreshDate = new Runnable() {
            @Override
            public void run() {
                Date displayDate = new Date();
                if (displayDate != previousDate) {
                    cDate.setText(dateFormat.format(displayDate));
                    clockHandler.postDelayed(refreshDate, CommonUtils.getDateUpdationTime());
                }
            }
        };
        flags = (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        refreshDigital = new Runnable() {
            public void run() {
                updateDigital();
                clockHandler.postDelayed(this, 1000);
            }
        };
        changeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }

                switch (action) {
                    case Intent.ACTION_TIME_CHANGED:
                    case Intent.ACTION_TIMEZONE_CHANGED:
                        clockHandler.removeCallbacks(refreshDate);
                        clockHandler.post(refreshDate);
                        if (isDigital) {
                            clockHandler.removeCallbacks(refreshDigital);
                            clockHandler.post(refreshDigital);
                        } else {
                            elementalAnalogClock.start();
                        }
                        break;
                }
            }
        };
        rect = new Rect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);

        window = getWindow();
        Resources res = getResources();

        window.addFlags(flags);
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        setContentView(R.layout.night_mode);

        if (CommonUtils.is16OrLater()) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        SharedPreferences themePrefs = getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE);
        tNumber = themePrefs.getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_BLUE_NIGHT);
        isColoured = themePrefs.getBoolean(Clock.IS_DIGITAL_COLORED, false);

        DisplayMetrics metrics = res.getDisplayMetrics();
        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        if (background != null) {
            getWindow().setBackgroundDrawable(background);
        } else {
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(changeReceiver, intentFilter);

        final LinearLayout digital = findViewById(R.id.digital_clock_layout);
        final RelativeLayout analog = findViewById(R.id.analog_clock_layout);
        elementalAnalogClock = findViewById(R.id.analogClock);
        cTime = findViewById(R.id.clock_hour);
        cSeconds = findViewById(R.id.clock_seconds);
        cDate = findViewById(R.id.night_mode_date);
        cAmpm = findViewById(R.id.ampm);
        mainFrame = findViewById(R.id.night_mode_main);

        SharedPreferences clock_prefs = getSharedPreferences(Clock.CLOCK_PREFS, Context.MODE_PRIVATE);
        isDigital = clock_prefs.getBoolean(ClockFragment.IS_DIGITAL, true);
        boolean isPortrait = CommonUtils.isPortrait(res);
        if (isDigital) {
            String fontNumber = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(SettingsActivity.CLOCK_FONT, "0");
            Typeface typeface;
            switch (fontNumber) {
                case "0":
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/NovaCut.ttf");
                    cTime.setTypeface(typeface);
                    cSeconds.setTypeface(typeface);
                    cAmpm.setTypeface(typeface);
                    cTime.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 130 : 170));
                    cSeconds.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    cAmpm.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    break;

                case "1":
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/DigitalNumbers.ttf");
                    cTime.setTypeface(typeface);
                    cSeconds.setTypeface(typeface);
                    cAmpm.setTypeface(typeface);
                    cTime.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 90 : 135));
                    cSeconds.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 30 : 42));
                    cAmpm.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 30 : 42));
                    break;

                case "2":
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
                    cTime.setTypeface(typeface);
                    cSeconds.setTypeface(typeface);
                    cAmpm.setTypeface(typeface);
                    cTime.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 130 : 170));
                    cSeconds.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    cAmpm.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    break;
            }

        }

        if (isColoured) {
            int themeColor = ThemeDetails.getThemeRadiance(tNumber);
            if (isDigital) {
                cSeconds.setTextColor(themeColor);
                cAmpm.setTextColor(themeColor);
                cTime.setTextColor(themeColor);
            }
            cDate.setTextColor(themeColor);
        }

        cDate.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 30 : 32));

        Locale locale = Locale.getDefault();
        boolean isFormat24 = DateFormat.is24HourFormat(getApplicationContext());
        timeFormat = new SimpleDateFormat(isFormat24 ? "H:mm" : "h:mm", locale);
        secondsFormat = new SimpleDateFormat("ss", locale);
        dateFormat = new SimpleDateFormat("EEEE, MMMM dd", locale);
        ampmFormat = new SimpleDateFormat(isFormat24 ? "" : "a", locale);

        if (isDigital) {
            analog.setVisibility(View.GONE);
            digital.setVisibility(View.VISIBLE);
            clockHandler.removeCallbacks(refreshDigital);
            clockHandler.post(refreshDigital);
        } else {
            digital.setVisibility(View.GONE);
            analog.setVisibility(View.VISIBLE);
            elementalAnalogClock.start();
        }

        if (BitmapController.isAnimation()) {
            mainFrame.setAlpha(0);
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mainFrame, "alpha", 0.1f, 1f);
            alphaAnim.setDuration(3500);
            alphaAnim.start();
        }

        previousDate = new Date();
        clockHandler.post(refreshDate);
    }

    private void updateDigital() {
        Date displayDate = new Date();
        cTime.setText(timeFormat.format(displayDate));
        cSeconds.setText(secondsFormat.format(displayDate));
        cAmpm.setText(ampmFormat.format(displayDate));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mainFrame.getHitRect(rect);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (rect.contains((int) event.getX(), (int) event.getY())) {
                int themeColor = ThemeDetails.getThemeRadiance(tNumber);
                isColoured = !isColoured;
                if (isDigital) {
                    cTime.setTextColor(isColoured ? themeColor : Color.WHITE);
                    cSeconds.setTextColor(isColoured ? themeColor : Color.WHITE);
                    cAmpm.setTextColor(isColoured ? themeColor : Color.WHITE);
                }
                cDate.setTextColor(isColoured ? themeColor : Color.WHITE);
            } else {
                finish();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE).edit().putBoolean(Clock.IS_DIGITAL_COLORED, isColoured).apply();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        window.clearFlags(flags);
        if (!isDigital) {
            elementalAnalogClock.stop();
        } else {
            clockHandler.removeCallbacks(refreshDigital);
        }
        clockHandler.removeCallbacks(refreshDate);
        if (changeReceiver != null) {
            unregisterReceiver(changeReceiver);
        }
        super.onDestroy();
    }
}
