/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.Wave;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.GestureObserver;
import com.bytezap.wobble.utils.MessageSpeaker;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.ToastGaffer;
import com.bytezap.wobble.utils.TtsSpeaker;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

public class AlarmScreen extends AppCompatActivity implements AlarmScreenInterface, OnClickListener, Animation.AnimationListener, GestureObserver.SimpleGestureListener {

    public static final String TAG = AlarmScreen.class.getSimpleName();

    private SlidingUpPanelLayout sLayout;
    public static boolean isAlarmActive;
    private AlarmInstance instance;
    private BitmapDrawable alarmBg;
    private View brightnessSetter;
    private RelativeLayout alarmLayout;
    private LinearLayout waveDrawable;
    private TextView screenTime, screenDate;
    private Bitmap alarmImage = null;
    private SharedPreferences settings;
    private boolean isFragmentAttached = false, isDismissFrag = false, isTaskFinished = false,
            isPreview = false;
    private CharSequence format;

    private Handler timeHanlder = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            Calendar currentTime = Calendar.getInstance();
            screenTime.setText(DateFormat.format(format,currentTime));
            screenDate.setText(CommonUtils.getFormattedDate(currentTime));
            timeHanlder.postDelayed(this, 30000);
        }
    };

    private CountDownTimer safetyTimer, autoDismissTimer;
    private TelephonyManager telephonyMgr;
    private long mLastClickTime;

    public AlarmScreen() {
        isAlarmActive = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long id = getIntent().getLongExtra(AlarmAssistant.ID, -1);
        isPreview = getIntent().getBooleanExtra(AlarmAssistant.PREVIEW, false);
        instance = AlarmAssistant.getInstance(getApplicationContext(), id);

        if (instance == null) {
            // Alarm exists but could not be retrieved, so create it
            instance = new AlarmInstance();
            instance.id = 999;
            Calendar calendar = Calendar.getInstance();
            instance.date = calendar.get(Calendar.DATE);
            instance.month = calendar.get(Calendar.MONTH);
            instance.year = calendar.get(Calendar.YEAR);
            instance.hour = calendar.get(Calendar.HOUR_OF_DAY);
            instance.minutes = calendar.get(Calendar.MINUTE);
            Log.v(TAG, "Alarm for id " + id + "was null");
            //finish();
            //return;
        } else if (instance.alarmState!=AlarmInstance.ALARM_STATE_TRIGGERED && !isPreview) {
            Log.v(TAG, "False instance for id " + id);
            NotificationProvider.cancelAlarmNotification(getApplicationContext(), instance.hashCode());
            finish();
            return;
        }

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Window window = getWindow();
        Resources res = getResources();

        final boolean isAnimation = settings.getBoolean(SettingsActivity.ANIMATION, true);
        BitmapController.setIsAnimation(isAnimation);
        if (isAnimation) {
            overridePendingTransition(R.anim.open_enter, R.anim.open_exit);
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        String locale = settings.getString(SettingsActivity.LANGUAGE, "default");
        CommonUtils.setLangCode(locale);
        CommonUtils.setLanguage(res, locale);

        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        String bgId = settings.getString(SettingsActivity.ALARM_BACKGROUND, "0");
        switch (bgId) {

            case "0":
                setDefaultBg(res, window);
                break;

            case "1":
                int tNumber = getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
                BitmapController.setThemeNumber(tNumber);
                alarmBg = BitmapController.getCurrentBackground(res);
                if (alarmBg == null) {
                    alarmBg = BitmapController.setNewBackground(getApplicationContext(), res);
                }
                if (alarmBg != null) {
                    window.setBackgroundDrawable(alarmBg);
                } else {
                    window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
                }
                break;

            case "2":
                SharedPreferences alarmPrefs = getSharedPreferences(Clock.MAIN_PREF, Context.MODE_PRIVATE);
                String path = alarmPrefs.getString(Clock.ALARM_BG, "Null");
                // The user might have deleted the file
                if (new File(path).exists()) {
                    try {
                        alarmBg = (BitmapDrawable) BitmapDrawable.createFromPath(path);
                        window.setBackgroundDrawable(alarmBg);
                    } catch (Exception ex) {
                        //set the default random bg if this fails
                        setDefaultBg(res, window);
                    }
                } else {
                    setDefaultBg(res, window);
                }
                break;

            case "3":
                try{
                    setTheme(R.style.Theme_Wallpaper);
                } catch (Throwable b) {
                    try{
                        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                        if (wallpaperDrawable != null) {
                            window.setBackgroundDrawable(wallpaperDrawable);
                        } else {
                            setDefaultBg(res, window);
                        }
                        b.printStackTrace();
                    } catch (Throwable c) {
                        c.printStackTrace();
                    }
                }
                break;
        }

        //Setup layout
        setContentView(R.layout.alarm_screen);
        sLayout = findViewById(R.id.sliding_layout);
        sLayout.setDragView(R.id.alarm_drawer);

        if (CommonUtils.isLOrLater()) {
            window.setStatusBarColor(Color.parseColor("#80000000"));
            window.setNavigationBarColor(Color.parseColor("#80000000"));
        }

        if (CommonUtils.is16OrLater()) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        Button snoozeButton = findViewById(R.id.alarm_snooze_button);
        Button dismissButton = findViewById(R.id.alarm_dismiss_button);
        alarmLayout = findViewById(R.id.alarmScreen_layout);
        brightnessSetter = findViewById(R.id.brightness_setter);
        // Alarm name and quotes section
        TextView screenText = findViewById(R.id.alarm_screen_name);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
        screenText.setTypeface(typeface);
        snoozeButton.setTypeface(typeface);
        dismissButton.setTypeface(typeface);

        snoozeButton.setOnClickListener(this);

        final boolean isLongDismiss = settings.getBoolean(SettingsActivity.LONG_DISMISS, false);
        if (isLongDismiss) {
            dismissButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    dismissAlarm();
                    return true;
                }
            });
        } else {
            dismissButton.setOnClickListener(this);
        }

        int i = instance.snoozeTimes;
        if (i != 0 && (i % 3 == 0)) {
            screenText.setText(getString(R.string.snooze_warning, i));
        } else {
            if (!TextUtils.isEmpty(instance.name)) {
                screenText.setText(instance.name);
            }
        }

        if (isPreview) {
            screenText.append("\n\n" + getString(R.string.preview_message));
        }

        int maxSnoozeTime = res.getIntArray(R.array.final_snooze_values)[instance.snoozeTimeIndex];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        boolean isSnoozeDisabled = CommonUtils.shouldDisableSnooze(maxSnoozeTime, calendar, instance);
        if (maxSnoozeTime > 0 && isSnoozeDisabled) {
            snoozeButton.setEnabled(false);
            snoozeButton.setText(R.string.no_snooze);
        }

        int brightnessDuration = Integer.parseInt(settings.getString(SettingsActivity.INCREASING_BRIGHTNESS, "0"));
        final float currBrightness = window.getAttributes().screenBrightness;

        int amPm = calendar.get(Calendar.AM_PM);
        int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
        boolean isFormat24 = DateFormat.is24HourFormat(getApplicationContext());
        int hour = isFormat24 ? hourDay : CommonUtils.getFormatted12Hour(hourDay);
        int minute = calendar.get(Calendar.MINUTE);

        if (brightnessDuration!=0 && (instance.snoozeTimes == 0)) {
            //First check if the instance is not started after the actual time
            int mainGap = (instance.hour*60 + instance.minutes) - (hourDay*60 + minute);
            if (mainGap < brightnessDuration/60) {
                brightnessDuration = mainGap*60;
            }
        }

        safetyTimer = new CountDownTimer(120000 + brightnessDuration*1000L, 10000) {

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (AlarmTone.isToneNotRunning() && instance.dismissMethod != AlarmObject.DISMISS_METHOD_SHAKE && !MessageSpeaker.isSpeaking() && !TtsSpeaker.isSpeaking()) { // Something has gone wrong and the instance tone didn't start. Precautionary stuff.
                    AlarmTone.start(getApplicationContext(), instance);
                }
                int brightnessDur = Integer.parseInt(settings.getString(SettingsActivity.INCREASING_BRIGHTNESS, "0"));
                try{
                    if (brightnessDur!=0) { //Set the brightness back to original after here for battery optimization
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.screenBrightness = currBrightness;
                        getWindow().setAttributes(lp);
                    }
                } catch (Throwable e){
                    e.printStackTrace();
                }

            }
        };
        safetyTimer.start();

        screenTime = findViewById(R.id.alarm_screen_time);
        screenDate = findViewById(R.id.alarm_screen_date);
        TextView screenType = findViewById(R.id.alarm_type);

        int amPmSize = Math.round(2 * screenTime.getTextSize() / 5);
        format = CommonUtils.getTimeFormat(DateFormat.is24HourFormat(getApplicationContext()), amPmSize);
        screenTime.setText(DateFormat.format(format, isPreview ? instance.getAlarmTime() : calendar));
        screenDate.setText(CommonUtils.getFormattedDate(isPreview ? instance.getAlarmTime() : calendar));

        if (hourDay <= 13 && hourDay >= 4) {
            screenType.setText(R.string.good_morning);
        } else if (hourDay >= 16 && hourDay <= 20) {
            screenType.setText(R.string.good_evening);
        } else {
            screenType.setText(R.string.alarm_item_name_default);
        }

        TextView friendlyAlert = findViewById(R.id.launch_app_alert);

        telephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Check latest call state
        telephonyMgr.listen(new CallStateListener(),
                PhoneStateListener.LISTEN_CALL_STATE);

        ImageView upIndicator = findViewById(R.id.up_indicator);
        final LinearLayout indicatorLayout = findViewById(R.id.up_indicator_layout);
        final AnimationDrawable frameAnimation = (AnimationDrawable) upIndicator.getBackground();
        frameAnimation.start();

        final PackageManager pm = getApplicationContext().getPackageManager();
        if (instance.isLaunchApp) {
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(instance.launchAppPkg, 0);
            } catch (final Exception e) {
                ai = null;
            }
            if (ai != null) {
                friendlyAlert.setText(getString(R.string.app_launch_alert, pm.getApplicationLabel(ai)));
                friendlyAlert.setVisibility(View.VISIBLE);
            }

        } else if (instance.dismissMethod == AlarmObject.DISMISS_METHOD_MATH) {
            friendlyAlert.setText(instance.mathDismissProb > 1 ? R.string.math_mutliple_friendly_alert : R.string.math_friendly_alert);
            friendlyAlert.setVisibility(View.VISIBLE);
        } /*else if (instance.dismissMethod == AlarmObject.DISMISS_METHOD_PICTURE) {
            alarmImage = BitmapController.getBitmapFromPath(instance.imagePath);
            if (alarmImage!=null) {
                FrameLayout imageFrame = (FrameLayout) findViewById(R.id.frame_dismiss_image);
                ImageView dismissImage = (ImageView) imageFrame.findViewById(R.id.dismiss_image);
                screenText.setText("");
                imageFrame.setVisibility(View.VISIBLE);
                dismissImage.setImageBitmap(alarmImage);
            }
        }*/

        sLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                indicatorLayout.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    indicatorLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        int dismissDuration = Integer.parseInt(settings.getString(SettingsActivity.AUTO_DISMISS, "0"));
        if (dismissDuration != 0) {
            autoDismissTimer = new CountDownTimer(dismissDuration * 60000L, 100) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    CommonUtils.sendAlarmBroadcast(getApplicationContext(), instance.id, AlarmService.ALARM_SNOOZE);
                    finish();
                }
            }.start();
        }

        waveDrawable = findViewById(R.id.waveDrawable);
        final GestureObserver detector = new GestureObserver(this, this);
        waveDrawable.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (brightnessSetter.getVisibility() == View.VISIBLE) {
                    brightnessSetter.clearAnimation();
                }
                detector.onTouchEvent(event);
                return true;
            }
        });

        if (brightnessDuration != 0 && instance.snoozeTimes == 0) { //Let the brightness be gradually increased only the first time
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            try{
                WindowManager.LayoutParams params = window.getAttributes();
                params.screenBrightness = 1f;
                getWindow().setAttributes(params);
            }catch (Throwable e){
             e.printStackTrace();
            }
            if (CommonUtils.is16OrLater()) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            } else {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
            brightnessSetter.setVisibility(View.VISIBLE);
            Animation animation =  new AlphaAnimation(1f, 0f);
            animation.setDuration(brightnessDuration * 1000);
            animation.setAnimationListener(this);
            brightnessSetter.startAnimation(animation);
        } else {
            if (BitmapController.isAnimation()) {
                startWaveAnimation(getResources());
            }
        }

        if (!isPreview) {
            timeHanlder.postDelayed(timeRunnable, 30000);
        }
    }

    private void startWaveAnimation(Resources res){
        //WaveDrawable Animation
        Wave drawable = new Wave(Color.parseColor("#70000000"), res.getDimensionPixelSize(R.dimen.wave_drawable_layout), 2000);
        if (CommonUtils.is16OrLater()) {
            waveDrawable.setBackground(drawable);
        } else {
            waveDrawable.setBackgroundDrawable(drawable);
        }
        drawable.setWaveInterpolator(new OvershootInterpolator());
        drawable.startAnimation();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.alarm_snooze_button:
                snoozeAlarm();
                break;

            case R.id.alarm_dismiss_button:
                dismissAlarm();
                break;
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // TODO: 26/06/2016 Find a better way to bring the activity front
        if (!isTaskFinished) { //Finish hasn't been manually called. So bring the user back to the activity
            CommonUtils.sendAlarmBroadcast(AlarmScreen.this, instance.id, AlarmService.ALARM_RESTART);
        }
        if (isPreview) {
            CommonUtils.sendAlarmBroadcast(getApplicationContext(), instance.id, AlarmService.ALARM_DISMISS);
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.preview_dismissed));
            finish();
        }
    }

    private void setDefaultBg(Resources res, Window window) {
        // Generate randomness for default settings
        final String[] alarmImages = CommonUtils.isPortrait(res) ? res.getStringArray(R.array.backgroundAlarm) : res.getStringArray(R.array.backgroundAlarmLand);
        final Random r = new Random();
        int randomInt = r.nextInt(alarmImages.length);

        SharedPreferences alarmPrefs = getSharedPreferences(Clock.MAIN_PREF, Context.MODE_PRIVATE);
        int randOld = alarmPrefs.getInt(Clock.ALARM_BG_RAND, -1);

        if (randOld == randomInt) {
            randomInt = r.nextInt(alarmImages.length);
        }

        alarmBg = new BitmapDrawable(res, BitmapController.getImageFromAssets(res, alarmImages[randomInt]));
        if (alarmBg.getBitmap()!=null) {
            window.setBackgroundDrawable(alarmBg);
        } else {
            Log.v("setDefaultBg", "Background from assets was null");
            alarmBg = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.bga3));
            window.setBackgroundDrawable(alarmBg);
        }

        alarmPrefs.edit().putInt(Clock.ALARM_BG_RAND, randomInt).apply();
    }

    private void snoozeAlarm() {
        switch (instance.snoozeMethod){
            case AlarmObject.SNOOZE_METHOD_DEFAULT:
                finalSnooze();
                break;

            case AlarmObject.SNOOZE_METHOD_MATH:
                isDismissFrag = false;
                startMathProblem();
                break;

            case AlarmObject.SNOOZE_METHOD_SHAKE:
                isDismissFrag = false;
                launchShakeFragment();
                break;
        }
    }

    private void dismissAlarm() {
        switch (instance.dismissMethod){
            case AlarmObject.DISMISS_METHOD_DEFAULT:
                finalDismissWithLaunch();
                break;

            case AlarmObject.DISMISS_METHOD_MATH:
                isDismissFrag = true;
                startMathProblem();
                break;

            /*case AlarmObject.DISMISS_METHOD_PICTURE:
                isDismissFrag = true;
                launchImageMatcher();
                break;*/

            case AlarmObject.DISMISS_METHOD_BARCODE:
                if (instance.barcodeText != null) {
                    try {
                        startBarcodeScanner();
                    } catch (Throwable throwable) {
                        Log.v("AlarmScreen", "Barcode Scanner could not be started: " + throwable.getMessage());
                    }
                }
                break;

            case AlarmObject.DISMISS_METHOD_SHAKE:
                isDismissFrag = true;
                launchShakeFragment();
                break;
        }
    }

    private void startMathProblem() {
        if (alarmLayout.getVisibility() == View.VISIBLE) {
            getFragmentManager().beginTransaction().setCustomAnimations(R.animator.fade_in_later, 0)
                    .add(android.R.id.content, MathProblem.newInstance(), "MathProblem").addToBackStack("MathProblem").commit();
            sLayout.setVisibility(View.GONE);
            alarmLayout.setVisibility(View.INVISIBLE);
            isFragmentAttached = true;
        }
    }

    private void startBarcodeScanner() {
        if (alarmLayout.getVisibility() == View.VISIBLE) {
            getFragmentManager().beginTransaction().add(android.R.id.content, BarcodeScannerFragment.newInstance(), "BarcodeScanner").addToBackStack("BarcodeScanner").commit();
            sLayout.setVisibility(View.GONE);
            isFragmentAttached = true;
        }
    }

    private void launchShakeFragment() {
        if (alarmLayout.getVisibility() == View.VISIBLE) {
            getFragmentManager().beginTransaction().setCustomAnimations(R.animator.fade_in_later, 0)
                    .add(android.R.id.content, ShakeFragment.newInstance(), "ShakeFragment").addToBackStack("ShakeFragment").commit();
            sLayout.setVisibility(View.GONE);
            alarmLayout.setVisibility(View.INVISIBLE);
            isFragmentAttached = true;
        }
    }

    private void launchImageMatcher() {
        /*if (alarmLayout.getVisibility() == View.VISIBLE) {
            getFragmentManager().beginTransaction().add(android.R.id.content, CommonUtils.isLOrLater() ? ImageMatcher.newInstance(alarmImage) : ImageMatcherPreL.newInstance(alarmImage), "ImageMatcher").addToBackStack("ImageMatcher").commit();
            sLayout.setVisibility(View.GONE);
            isFragmentAttached = true;
        }*/
    }

    private void finalSnooze() {
        CommonUtils.sendAlarmBroadcast(getApplicationContext(), instance.id, AlarmService.ALARM_SNOOZE);
        finish();
    }

    private void finalDismissWithLaunch() {
        CommonUtils.sendAlarmBroadcast(getApplicationContext(), instance.id, AlarmService.ALARM_DISMISS);
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        switch (event.getKeyCode()) {

            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (isAlarmActive && event.getAction() == KeyEvent.ACTION_UP) {
                    switch (settings.getString(SettingsActivity.VOLUME_KEYS, "0")) {
                        case "1":
                            snoozeAlarm();
                            break;

                        case "2":
                            dismissAlarm();
                            break;

                        default:
                            break;
                    }
                }
                return true;

            default:
                return super.dispatchKeyEvent(event);
        }

    }

    @Override
    public void finish() {
        super.finish();
        isTaskFinished = true;
        if (safetyTimer != null) {
            safetyTimer.cancel();
            safetyTimer = null;
        }
        isAlarmActive = false;
        if (BitmapController.isAnimation()) {
            overridePendingTransition(R.anim.close_enter, R.anim.close_exit);
        } else {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

    @Override
    protected void onDestroy() {
        if (autoDismissTimer != null) {
            autoDismissTimer.cancel();
        }
        if (telephonyMgr != null) {
            telephonyMgr.listen(new CallStateListener(), PhoneStateListener.LISTEN_NONE);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
        if (alarmBg!=null) {
            alarmBg.getBitmap().recycle();
            alarmBg = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (isFragmentAttached) {
            if (alarmLayout.getVisibility() == View.INVISIBLE) {
                getFragmentManager().popBackStackImmediate();
                sLayout.setVisibility(View.VISIBLE);
                sLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                alarmLayout.setVisibility(View.VISIBLE);
            }
            isFragmentAttached = false;
        } else if (sLayout != null &&
                (sLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || sLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            sLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else if (isPreview){
            CommonUtils.sendAlarmBroadcast(getApplicationContext(), instance.id,  AlarmService.ALARM_DISMISS);
            finish();
        }
    }

    public boolean isDismissFrag() {
        return isDismissFrag;
    }

    @Override
    public void onSwipe(int direction) {
        if (System.currentTimeMillis() - mLastClickTime < 400) {
            // in order to avoid user clicking too quickly
            return;
        }

        switch (direction) {
            case GestureObserver.SWIPE_RIGHT:
            case GestureObserver.SWIPE_LEFT:
                switch (instance.snoozeMethod) {
                    case AlarmObject.SNOOZE_METHOD_MATH:
                        isDismissFrag = false;
                        startMathProblem();
                        break;

                    case AlarmObject.SNOOZE_METHOD_SHAKE:
                        isDismissFrag = false;
                        launchShakeFragment();
                        break;
                }
                break;

            case GestureObserver.SWIPE_UP:
                if(sLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED){
                    sLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
                break;

            case GestureObserver.SWIPE_DOWN:
                if(sLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED){
                    sLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
                break;
        }

        mLastClickTime = System.currentTimeMillis();
    }

    @Override
    public void onDoubleTap() {

    }

    @Override
    public void onLongPress() {

    }

    @Override
    public void onAnimationStart(Animation animation) {
        //Do nothing
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        timeHanlder.removeCallbacks(timeRunnable);
        brightnessSetter.setVisibility(View.GONE);
        if (BitmapController.isAnimation()) {
            startWaveAnimation(getResources());
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        //Do nothing
    }

    private class CallStateListener extends PhoneStateListener {
        public void onCallStateChanged(int state, String incomingNumber) {
            if (isAlarmActive && state != TelephonyManager.CALL_STATE_IDLE) {
                CommonUtils.sendAlarmBroadcast(getApplicationContext(), instance.id, AlarmService.ALARM_SNOOZE);
                finish();
            }
        }
    }

}
