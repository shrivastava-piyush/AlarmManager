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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.customviews.ElementalAnalogClock;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ScaleText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockDreamService extends DreamService {

    private final Handler clockHandler = new Handler();
    private final Runnable refreshDigital;
    private final Runnable refreshDate;
    private boolean isDigital;
    private TextView cTime, cSeconds;
    private TextView cDate, cAmPm;
    private Date previousDate;
    private ElementalAnalogClock elementalAnalogClock;
    private SimpleDateFormat timeFormat, secondsFormat, dateFormat, amPmFormat;
    private final BroadcastReceiver changeReceiver;

    public ClockDreamService() {
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

                switch (action){
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
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Locale locale = Locale.getDefault();
        boolean isFormat24 = DateFormat.is24HourFormat(getApplicationContext());
        timeFormat = new SimpleDateFormat(isFormat24 ? "H:mm" : "h:mm", locale);
        secondsFormat = new SimpleDateFormat("ss", locale);
        dateFormat = new SimpleDateFormat("EEEE, MMMM dd", locale);
        amPmFormat = new SimpleDateFormat(isFormat24 ? "" : "a", locale);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setInteractive(false);
        setFullscreen(true);
        setupLayout();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(changeReceiver, filter);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getWindow() != null) {
            setupLayout();
        }
    }

    private void setupLayout(){
        Resources res = getResources();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String locale = settings.getString(SettingsActivity.LANGUAGE, "default");
        CommonUtils.setLangCode(locale);
        CommonUtils.setLanguage(res, locale);

        setContentView(R.layout.night_mode);

        boolean isTheme = settings.getString(DayDreamSettings.NIGHT_BACKGROUND, "0").equals("0");
        if (isTheme) {
            int tNumber = getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_BLUE_NIGHT);
            BitmapController.setThemeNumber(tNumber);
            BitmapDrawable background = BitmapController.getCurrentBackground(res);
            if (background == null || background.getBitmap().isRecycled()) {
                background = BitmapController.setNewBackground(getApplicationContext(), res);
            }
            if (background != null) {
                getWindow().setBackgroundDrawable(background);
            } else {
                getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
            }
        } else {
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        }

        final LinearLayout digital = findViewById(R.id.digital_clock_layout);
        final RelativeLayout analog = findViewById(R.id.analog_clock_layout);
        elementalAnalogClock = findViewById(R.id.analogClock);
        View mainFrame = findViewById(R.id.night_mode_main);
        cTime = findViewById(R.id.clock_hour);
        cSeconds = findViewById(R.id.clock_seconds);
        cDate = findViewById(R.id.night_mode_date);
        cAmPm = findViewById(R.id.ampm);

        isDigital = settings.getString(DayDreamSettings.CLOCK_STYLE, "0").equals("0");
        if (isDigital) {
            DisplayMetrics metrics = res.getDisplayMetrics();
            String fontNumber = settings.getString(DayDreamSettings.DREAM_DIGITAL_FONT, "0");
            Typeface typeface;
            boolean isPortrait = CommonUtils.isPortrait(res);
            switch (fontNumber) {
                case "0":
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/NovaCut.ttf");
                    cTime.setTypeface(typeface);
                    cSeconds.setTypeface(typeface);
                    cAmPm.setTypeface(typeface);
                    cTime.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 130 : 170));
                    cSeconds.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    cAmPm.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    break;

                case "1":
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/DigitalNumbers.ttf");
                    cTime.setTypeface(typeface);
                    cSeconds.setTypeface(typeface);
                    cAmPm.setTypeface(typeface);
                    cTime.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 90 : 135));
                    cSeconds.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 30 : 42));
                    cAmPm.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 30 : 42));
                    break;

                case "2":
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
                    cTime.setTypeface(typeface);
                    cSeconds.setTypeface(typeface);
                    cAmPm.setTypeface(typeface);
                    cTime.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 130 : 170));
                    cSeconds.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    cAmPm.setTextSize((int) ScaleText.scale(metrics, isPortrait ? 32 : 55));
                    break;
            }
        }

        boolean isDimDisplay = settings.getBoolean(DayDreamSettings.NIGHT_MODE, true);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter(
                (isDimDisplay ? 0x40FFFFFF : 0xC0FFFFFF),
                PorterDuff.Mode.MULTIPLY));
        mainFrame.setLayerType(View.LAYER_TYPE_HARDWARE, paint);

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

        mainFrame.setAlpha(0);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mainFrame, "alpha", 0.0f, 1f);
        alphaAnim.setDuration(3500);
        alphaAnim.start();

        previousDate = new Date();
        clockHandler.post(refreshDate);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (isDigital) {
            clockHandler.removeCallbacks(refreshDigital);
        } else {
            elementalAnalogClock.stop();
        }
        clockHandler.removeCallbacks(refreshDate);
        unregisterReceiver(changeReceiver);
    }

    private void updateDigital() {
        Date displayDate = new Date();
        cTime.setText(timeFormat.format(displayDate));
        cSeconds.setText(secondsFormat.format(displayDate));
        cAmPm.setText(amPmFormat.format(displayDate));
    }

}
