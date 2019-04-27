/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.clock;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.customviews.ElementalAnalogClock;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AnimUtils;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.FlipAnimation;
import com.bytezap.wobble.utils.GestureObserver;
import com.bytezap.wobble.utils.ScaleText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockFragment extends Fragment implements GestureObserver.SimpleGestureListener {

    public static final String IS_DIGITAL = "isDigital";
    public static final String IS_ELEGANT = "isElegant";
    private static final String TAG = "ClockFragment";

    private final Handler clockHandler = new Handler();
    private TextView cHour, tSeconds;
    private TextView cDate, cDay, cMonth, cAmPm, cSimpleDate, cSimpleDateBg, cSimpleDateBgAnalog;
    private long mLastClickTime;

    private FrameLayout layout,clockLayout, dateFrame;
    private TypedArray myImages;
    private BitmapDrawable clockBackground;
    private boolean isDigital = true;
    private boolean isElegant = true;
    private ElementalAnalogClock elementalAnalogClock;
    private RelativeLayout analog;
    private LinearLayout digital;
    private LinearLayout elegantDate, dateFrameLayout;

    private SimpleDateFormat hourMinFormat, secondsFormat, monthFormat, ampmFormat;
    private SimpleDateFormat simpleDateFormat, dateFormat, dayFormat;
    private SharedPreferences settings;

    private BroadcastReceiver changeReceiver;

    private final Runnable refreshDate = new Runnable() {
        @Override
        public void run() {
            updateDate();
            clockHandler.postDelayed(refreshDate, CommonUtils.getDateUpdationTime());
        }
    };
    private final Runnable refreshDigital = new Runnable() {
        public void run() {
            updateDigital();
            clockHandler.postDelayed(this, 1000);
        }
    };

    public ClockFragment(){
        // Empty Constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.clock, container, false);

        digital = rootView.findViewById(R.id.digital_clock_layout);
        analog = rootView.findViewById(R.id.analog_clock_layout);
        elementalAnalogClock = rootView.findViewById(R.id.analogClock);
        cHour = rootView.findViewById(R.id.clock_hour);
        tSeconds = rootView.findViewById(R.id.clock_seconds);
        cSimpleDate = rootView.findViewById(R.id.clock_date_simple);
        cSimpleDateBg = rootView.findViewById(R.id.clock_date_simple_bg);
        cSimpleDateBgAnalog = rootView.findViewById(R.id.clock_date_simple_bg_analog);
        elegantDate = rootView.findViewById(R.id.elegant_layout);
        cDay = rootView.findViewById(R.id.elegant_day);
        dateFrame = rootView.findViewById(R.id.date_frame);
        dateFrameLayout = rootView.findViewById(R.id.date_frame_layout);
        cDate = rootView.findViewById(R.id.elegant_date);
        cMonth = rootView.findViewById(R.id.elegant_month);
        cAmPm = rootView.findViewById(R.id.ampm);

        settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        Resources res = getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();

        String fontNumber = settings.getString(SettingsActivity.CLOCK_FONT, "0");
        Typeface typeface;
        switch (fontNumber){
            case "0":
                typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/NovaCut.ttf");
                cHour.setTypeface(typeface);
                tSeconds.setTypeface(typeface);
                cAmPm.setTypeface(typeface);
                cHour.setTextSize((int) ScaleText.scale(metrics, 105));
                tSeconds.setTextSize((int) ScaleText.scale(metrics, 32));
                cAmPm.setTextSize((int) ScaleText.scale(metrics, 32));
                break;

            case "1":
                typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/DigitalNumbers.ttf");
                cHour.setTypeface(typeface);
                tSeconds.setTypeface(typeface);
                cAmPm.setTypeface(typeface);
                cHour.setTextSize((int) ScaleText.scale(metrics, 80));
                tSeconds.setTextSize((int) ScaleText.scale(metrics, 25));
                cAmPm.setTextSize((int) ScaleText.scale(metrics, 25));
                break;

            case "2":
                typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf");
                cHour.setTypeface(typeface);
                tSeconds.setTypeface(typeface);
                cAmPm.setTypeface(typeface);
                cHour.setTextSize((int) ScaleText.scale(metrics, 105));
                tSeconds.setTextSize((int) ScaleText.scale(metrics, 32));
                cAmPm.setTextSize((int) ScaleText.scale(metrics, 32));
                break;
        }

        int textSize = (int) ScaleText.scale(metrics, 28);
        cDate.setTextSize(textSize);
        cDay.setTextSize(textSize);
        cMonth.setTextSize(textSize);
        cSimpleDate.setTextSize(textSize);
        cSimpleDateBg.setTextSize(textSize);
        cSimpleDateBgAnalog.setTextSize(textSize);

        myImages = res.obtainTypedArray(R.array.backgroundClock);
        layout = rootView.findViewById(R.id.clock_frame);
        clockLayout = rootView.findViewById(R.id.clock_flip_view);

        //Read Theme from shared preferences and update clock background
        updateClockBg(rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initFormats();

        SharedPreferences clock_prefs = getActivity().getSharedPreferences(Clock.CLOCK_PREFS, Context.MODE_PRIVATE);
        isDigital = clock_prefs.getBoolean(IS_DIGITAL, true);
        isElegant = clock_prefs.getBoolean(IS_ELEGANT, true);

        // Set date visibility
        elegantDate.setVisibility(isElegant ? View.VISIBLE : View.INVISIBLE);
        cSimpleDate.setVisibility(isElegant ? View.INVISIBLE : View.VISIBLE);

        final GestureObserver detector = new GestureObserver(getActivity(), ClockFragment.this);
        ImageButton switchButton = getActivity().findViewById(R.id.switchButton);
        ViewPager pager = getActivity().findViewById(R.id.viewpager);

        if (BitmapController.isAnimation() && (savedInstanceState == null) && (pager.getCurrentItem() == 1)) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(clockLayout, View.ALPHA, 0.0f, 1.0f);
            animator.setDuration(700);
            animator.setStartDelay(300);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    //Make this visible only after the start delay
                    clockLayout.setVisibility(View.VISIBLE);
                    // Set clock visibility
                    analog.setVisibility(isDigital ? View.INVISIBLE : View.VISIBLE);
                    digital.setVisibility(isDigital ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    clockLayout.setVisibility(View.VISIBLE);
                    // Set clock visibility
                    analog.setVisibility(isDigital ? View.INVISIBLE : View.VISIBLE);
                    digital.setVisibility(isDigital ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        } else {
            clockLayout.setVisibility(View.VISIBLE);
            // Set clock visibility
            analog.setVisibility(isDigital ? View.INVISIBLE : View.VISIBLE);
            digital.setVisibility(isDigital ? View.VISIBLE : View.INVISIBLE);
        }
        clockLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);
                return true;
            }
        });

        boolean isClockBgDisabled = settings.getBoolean(SettingsActivity.CLOCK_BG, false);
        if (isClockBgDisabled) {
            switchButton.setVisibility(View.GONE);
            dateFrame.setVisibility(View.GONE);
            if (!CommonUtils.isPortrait(getResources())) {
                dateFrameLayout.setVisibility(View.GONE);
            }
            cSimpleDateBg.setVisibility(View.VISIBLE);
            cSimpleDateBgAnalog.setVisibility(View.VISIBLE);
        }

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchClock();
            }
        });

        dateFrame.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switchDate();
                return true;
            }
        });

    }

    private void initFormats(){
        Locale locale = Locale.getDefault();
        boolean isFormat24 = DateFormat.is24HourFormat(getActivity().getApplicationContext());
        simpleDateFormat = new SimpleDateFormat("EEEE, MMMM dd", locale);
        hourMinFormat = new SimpleDateFormat(isFormat24 ? "H:mm" : "h:mm", locale);
        secondsFormat = new SimpleDateFormat("ss", locale);
        dateFormat = new SimpleDateFormat("dd", locale);
        monthFormat = new SimpleDateFormat("MMMM", locale);
        ampmFormat = new SimpleDateFormat(isFormat24 ? "" : "a", locale);
        dayFormat = new SimpleDateFormat("EEEE", locale);
    }

    private void switchClock() {
        FlipAnimation flipAnimation = new FlipAnimation(digital, analog, true);
        if (isDigital) {
            elementalAnalogClock.start();
            clockHandler.removeCallbacks(refreshDigital);
        } else {
            flipAnimation.reverseWithAnimation();
            elementalAnalogClock.stop();
            clockHandler.removeCallbacks(refreshDigital);
            clockHandler.post(refreshDigital);
        }
        clockLayout.startAnimation(flipAnimation);
        isDigital = !isDigital;
    }

    private void switchDate() {
        Animation animation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_out);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                elegantDate.setVisibility(isElegant ? View.VISIBLE : View.INVISIBLE);
                cSimpleDate.setVisibility(isElegant ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        dateFrame.startAnimation(animation);
        clockHandler.removeCallbacks(refreshDate);
        clockHandler.post(refreshDate);
        isElegant = !isElegant;
    }

    private void swipeClock(boolean isDown) {
        FlipAnimation swipeAnimation = new FlipAnimation(digital, analog, isDown);
        if (isDigital) {
            elementalAnalogClock.start();
            clockHandler.removeCallbacks(refreshDigital);
        } else {
            swipeAnimation.reverseView();
            elementalAnalogClock.stop();
            clockHandler.post(refreshDigital);
        }
        clockLayout.startAnimation(swipeAnimation);
        isDigital = !isDigital;
    }

    @Override
    public void onSwipe(int direction) {
        if (System.currentTimeMillis() - mLastClickTime < 400) {
            // in order to avoid user clicking too quickly
            return;
        }

        if (BitmapController.isAnimation()) {
            switch (direction) {
                case GestureObserver.SWIPE_DOWN:
                    swipeClock(true);
                    break;
                case GestureObserver.SWIPE_UP:
                    swipeClock(false);
                    break;
            }
        }

        mLastClickTime = System.currentTimeMillis();
    }

    @Override
    public void onLongPress() {
        startActivity(new Intent(ClockFragment.this.getActivity().getApplicationContext(), NightMode.class));
    }

    @Override
    public void onDoubleTap() {
        if (System.currentTimeMillis() - mLastClickTime < 400) {
            // in order to avoid user clicking too quickly
            return;
        }
        if (BitmapController.isAnimation()) {
            ObjectAnimator animator = AnimUtils.getPulseAnimator(isDigital ? digital : analog, 0.85f, 1.1f);
            animator.start();
        }

        mLastClickTime = System.currentTimeMillis();
    }

    @Override
    public void onPause() {
        SharedPreferences clock_prefs = getActivity().getSharedPreferences(Clock.CLOCK_PREFS, Context.MODE_PRIVATE);
        clock_prefs.edit().putBoolean(IS_DIGITAL, isDigital).apply();
        clock_prefs.edit().putBoolean(IS_ELEGANT, isElegant).apply();
        if (isDigital) {
            clockHandler.removeCallbacks(refreshDigital);
        } else {
            elementalAnalogClock.stop();
        }
        clockHandler.removeCallbacks(refreshDate);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateDate();
        clockHandler.removeCallbacks(refreshDate);
        clockHandler.post(refreshDate);

        if (isDigital) {
            updateDigital();
            clockHandler.removeCallbacks(refreshDigital);
            clockHandler.post(refreshDigital);
        } else {
            elementalAnalogClock.start();
        }

        if (changeReceiver == null) {
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
            intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getActivity().registerReceiver(changeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action == null) {
                        return;
                    }

                    switch (action) {
                        case Intent.ACTION_TIME_CHANGED:
                        case Intent.ACTION_TIMEZONE_CHANGED:
                            initFormats();
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
            }, intentFilter);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("Animation", "Prevent clock animation");
        super.onSaveInstanceState(outState);
    }

    private void updateDigital() {
        Date displayDate = new Date();
        cHour.setText(hourMinFormat.format(displayDate));
        tSeconds.setText(secondsFormat.format(displayDate));
        cAmPm.setText(ampmFormat.format(displayDate));
    }

    private void updateDate(){
        Date displayDate = new Date();
        cSimpleDateBg.setText(simpleDateFormat.format(displayDate));
        cSimpleDateBgAnalog.setText(simpleDateFormat.format(displayDate));
        if (isElegant) {
            cDate.setText(dateFormat.format(displayDate));
            cDay.setText(dayFormat.format(displayDate));
            cMonth.setText(monthFormat.format(displayDate));
        } else {
            cSimpleDate.setText(simpleDateFormat.format(displayDate));
        }
    }

    @Override
    public void onDestroy() {
        myImages.recycle();
        if (changeReceiver !=null) {
            getActivity().unregisterReceiver(changeReceiver);
        }
        if (clockBackground != null) {
            clockBackground.getBitmap().recycle();
            clockBackground = null;
        }
        super.onDestroy();
    }

    @SuppressWarnings("ResourceType")
    private void updateClockBg(View rootView) {
        Resources res = getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();
        DisplayMetrics dm = res.getDisplayMetrics();
        int minDim = Math.min(dm.widthPixels, dm.heightPixels);
        boolean isClockBgDisabled = settings.getBoolean(SettingsActivity.CLOCK_BG, false);

        if (minDim < 600) {
            options.inSampleSize = 2;
        }
        switch (BitmapController.getThemeNumber()) {
            case ThemeDetails.THEME_BLUE_NIGHT:
            case ThemeDetails.THEME_SUNRISE:
            case ThemeDetails.THEME_RAINY_DAY:
                if (!isClockBgDisabled) {
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_GRID_RED, -1), options));
                }
                break;
            case ThemeDetails.THEME_MOUNTAINS:
                setDividerColor(rootView, Color.parseColor("#1F1A38"));
                if (!isClockBgDisabled) {
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_GRID_BROWN, -1), options));
                }
                break;
            case ThemeDetails.THEME_BEACH:
                setDividerColor(rootView, Color.parseColor("#00897B"));
                if (!isClockBgDisabled) {
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_GRID_GREEN, -1), options));
                }
                break;
            case ThemeDetails.THEME_FOGGY_FOREST:
                if (!isClockBgDisabled)
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_GRID_GREEN, -1), options));
                break;
            case ThemeDetails.THEME_SHIMMERING_NIGHT:
                if (!isClockBgDisabled) {
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_BLUE, -1), options));
                }
                rootView.findViewById(R.id.switchButton).setBackgroundResource(R.drawable.clock_switch_blue);
                break;
            case ThemeDetails.THEME_DARK_COSMOS:
            case ThemeDetails.THEME_OF_TIME:
                if (!isClockBgDisabled) {
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_BLACK, -1), options));
                }
                rootView.findViewById(R.id.switchButton).setBackgroundResource(R.drawable.clock_switch_black);
                break;
            case ThemeDetails.THEME_AURORA:
            case ThemeDetails.THEME_CYAN:
                if (!isClockBgDisabled) {
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_GREEN, -1), options));
                }
                rootView.findViewById(R.id.switchButton).setBackgroundResource(R.drawable.clock_switch_cyan);
                break;
            case ThemeDetails.THEME_THISTLE_PURPLE:
                if (!isClockBgDisabled)
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_RED, -1), options));
                break;
            case ThemeDetails.THEME_CUSTOM:
                if (!isClockBgDisabled)
                    clockBackground = new BitmapDrawable(res, BitmapFactory.decodeResource(res, myImages.getResourceId(ThemeDetails.PATTERN_GRID_BROWN, -1), options));
                break;
            default:
                Log.v(TAG, "Unknown theme");
                break;
        }

        if (!isClockBgDisabled) {
            if (CommonUtils.is16OrLater()) {
                layout.setBackground(clockBackground);
            } else {
                layout.setBackgroundDrawable(clockBackground);
            }
        }
    }

    private void setDividerColor(View rootView, int color){
        cDate.setTextColor(color);
        rootView.findViewById(R.id.elegant_divider1).setBackgroundColor(color);
        rootView.findViewById(R.id.elegant_divider2).setBackgroundColor(color);
        cDay.setTextColor(color);
        cMonth.setTextColor(color);
    }

}


