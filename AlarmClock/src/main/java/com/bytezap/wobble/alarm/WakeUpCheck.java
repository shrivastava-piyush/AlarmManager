/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.DataManager;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AppWakeLock;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.ToastGaffer;
import com.bytezap.wobble.utils.TtsSpeaker;

import java.util.Calendar;

public class WakeUpCheck extends AppCompatActivity{

    private static final String DARK_BLUE = "#175676";
    private static final String LIGHT_BLUE = "#2196F3";
    private static final String DARK_GREY = "#212121";
    private static final String LIGHT_GREY = "#616161";

    //Intents
    public static final String WAKE_UP_CHECK_UPDATE = "WAKE_UP_CHECK_UPDATE";

    private AlarmInstance instance;
    private SharedPreferences settings;
    public static boolean isWakeUpCheckActive = false;
    private BitmapDrawable background;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {

                final String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case WAKE_UP_CHECK_UPDATE:
                            AlarmAssistant.updateAlarmWithStateNoCheck(getApplicationContext(), instance);
                            break;

                        case AlarmService.ALARM_STATE_CHANGE:
                            NotificationProvider.cancelAlarmNotification(getApplicationContext(), instance.hashCode());
                            if (countDownTimer!=null) {
                                countDownTimer.cancel();
                            }
                            AppWakeLock.releaseCpuLock();
                            finish();
                            break;
                    }
                }
            }
        }
    };

    private CountDownTimer countDownTimer = new CountDownTimer(180000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            NotificationProvider.cancelAlarmNotification(getApplicationContext(), instance.hashCode());
            Calendar checkTime = Calendar.getInstance();
            checkTime.set(Calendar.SECOND, 0);
            checkTime.set(Calendar.MILLISECOND, 0);
            instance.setAlarmTime(checkTime);
            AlarmAssistant.setAlarmState(getApplicationContext(), instance, AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
            Intent serviceIntent = new Intent(getApplicationContext(), AlarmService.class);
            serviceIntent.putExtras(getIntent());
            if (CommonUtils.isOOrLater()) {
                ContextCompat.startForegroundService(WakeUpCheck.this, serviceIntent);
            } else {
                startService(serviceIntent);
            }
            AppWakeLock.releaseCpuLock();
            finish();
        }
    };

    public WakeUpCheck(){
        isWakeUpCheckActive = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppWakeLock.acquireCpuWakeLock(getApplicationContext());

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isAnimation = settings.getBoolean(SettingsActivity.ANIMATION, true);
        BitmapController.setIsAnimation(isAnimation);
        if (isAnimation) {
            overridePendingTransition(R.anim.popup_enter, R.anim.fade_out);
        }

        String locale = settings.getString(SettingsActivity.LANGUAGE, "default");
        boolean isVoice = settings.getBoolean(SettingsActivity.VOICE, true);
        CommonUtils.setLangCode(locale);
        CommonUtils.setLanguage(getApplicationContext().getResources(), locale);

        Window window = getWindow();
        Resources res = getResources();

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        int tNumber = getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
        BitmapController.setThemeNumber(tNumber);

        if (ContextCompat.checkSelfPermission(WakeUpCheck.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
            if (wallpaperDrawable != null) {
                window.setBackgroundDrawable(wallpaperDrawable);
            } else {
                background = BitmapController.getCurrentBackground(res);
                if (background == null) {
                    background = BitmapController.setNewBackground(getApplicationContext(), res);
                }

                if (background!=null) {
                    try {
                        window.setBackgroundDrawable(null);
                        window.setBackgroundDrawable(background);
                    } catch (Throwable e) {
                        window.setBackgroundDrawable(null);
                        window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
                    }
                } else {
                    window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
                }
            }
        } else {
            background = BitmapController.getCurrentBackground(res);
            if (background == null) {
                background = BitmapController.setNewBackground(getApplicationContext(), res);
            }

            if (background!=null) {
                try {
                    window.setBackgroundDrawable(null);
                    window.setBackgroundDrawable(background);
                } catch (Throwable e) {
                    window.setBackgroundDrawable(null);
                    window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
                }
            } else {
                window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
            }
        }

        if (CommonUtils.isLOrLater()) {
            int bgColor = ContextCompat.getColor(getApplicationContext(), R.color.wakeUpCheck);
            window.setStatusBarColor(bgColor);
            window.setNavigationBarColor(bgColor);
        }

        long id = getIntent().getLongExtra(AlarmAssistant.ID, -1);
        final DataManager dbManager = DataManager.getInstance(getApplicationContext());
        instance = dbManager.getInstanceById(id);

        if (instance == null || instance.alarmState!= AlarmInstance.ALARM_STATE_DISMISSED_WITH_CHECK) { // Check must have been started from notification, so cancel it
            finish();
        }

        setContentView(R.layout.wakeup_check_layout);
        NotificationProvider.showWakeupCheckNotification(getApplicationContext(), instance.id, isVoice);
        if (!isVoice) {
            long[] pattern = {0, 300, 300, 300};
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (CommonUtils.isLOrLater()) {
                if (vibrator != null) {
                    vibrator.vibrate(pattern, -1, new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }
            } else {
                if (vibrator != null) {
                    vibrator.vibrate(pattern, -1);
                }
            }
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WAKE_UP_CHECK_UPDATE);
        intentFilter.addAction(AlarmService.ALARM_STATE_CHANGE);
        registerReceiver(mReceiver, intentFilter);

        final Button wake1 = findViewById(R.id.wake1);
        final Button wake2 = findViewById(R.id.wake2);
        final TextView text = findViewById(R.id.wake_text);
        final TextView instruction = findViewById(R.id.wake_up_instruct);
        View topView = findViewById(R.id.wake_dialog_image);
        LinearLayout iconWrapper = findViewById(R.id.wake_icon_wrapper);
        final ImageView icon = findViewById(R.id.wake_icon);

        try {
            topView.setBackgroundColor(ThemeDetails.getThemeAccent(tNumber));
        } catch (NullPointerException e){
            topView.setBackgroundColor(Color.parseColor("#212121"));
        }

        switch (tNumber){
            case ThemeDetails.THEME_RAINY_DAY:
            case ThemeDetails.THEME_BLUE_NIGHT:
            case ThemeDetails.THEME_MOUNTAINS:
            case ThemeDetails.THEME_CYAN:
            case ThemeDetails.THEME_SUNRISE:
            case ThemeDetails.THEME_SHIMMERING_NIGHT:
            case ThemeDetails.THEME_THISTLE_PURPLE:
                iconWrapper.setBackgroundResource(R.drawable.image_rounded_blue);
                text.setTextColor(Color.parseColor(DARK_BLUE));
                instruction.setTextColor(Color.parseColor(LIGHT_BLUE));
                wake1.setBackgroundResource(R.drawable.wake_button_blue);
                wake2.setBackgroundResource(R.drawable.wake_button_blue);
                break;

            case ThemeDetails.THEME_BEACH:
            case ThemeDetails.THEME_FOGGY_FOREST:
            case ThemeDetails.THEME_DARK_COSMOS:
            case ThemeDetails.THEME_OF_TIME:
            case ThemeDetails.THEME_AURORA:
            case ThemeDetails.THEME_CUSTOM:
                iconWrapper.setBackgroundResource(R.drawable.image_rounded_black);
                text.setTextColor(Color.parseColor(DARK_GREY));
                instruction.setTextColor(Color.parseColor(LIGHT_GREY));
                wake1.setBackgroundResource(R.drawable.wake_button_black);
                wake2.setBackgroundResource(R.drawable.wake_button_black);
                break;
        }

        if(isVoice){
            TtsSpeaker.setShouldShutDown(false);
            TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.wake_up_question) : "Are you up yet?", TtsSpeaker.isTtsOff(), true);
        }

        wake1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wake1.setVisibility(View.INVISIBLE);
                wake2.setVisibility(View.VISIBLE);
                text.setText(R.string.wakeup_button_2_text);
                instruction.setText(R.string.wakeup_press_and_hold);
            }
        });

        wake2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotificationProvider.cancelAlarmNotification(getApplicationContext(), instance.hashCode());
                if (countDownTimer!=null) {
                    countDownTimer.cancel();
                }
                updateAlarm();
                TtsSpeaker.setShouldShutDown(true);
                TtsSpeaker.stopTts(getApplicationContext());
                AppWakeLock.releaseCpuLock();
                finish();
                return true;
            }
        });

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BitmapController.isAnimation()){
                icon.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake_and_rotate));}
            }
        });

        countDownTimer.start();
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

    private void updateAlarm(){

        final AsyncTask<Void, Void, Void> updateTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                AlarmAssistant.updateAlarmWithStateNoCheck(getApplicationContext(), instance);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                ToastGaffer.showToast(getApplicationContext(), getString(R.string.wake_up_nice_day));
                if(settings.getBoolean(SettingsActivity.VOICE, true)){
                    TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.wake_up_nice_day) : "Looks like you are awake. Have a great day!", TtsSpeaker.isTtsOff(), true);
                }
                if (countDownTimer!=null) {
                    countDownTimer.cancel();
                }
            }
        };
        updateTask.execute();
    }

    @Override
    public void onBackPressed() {
        //Don't let user exit the test
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (countDownTimer!=null) {
            countDownTimer.cancel();
        }
        if (mReceiver!=null) {
            unregisterReceiver(mReceiver);
        }
        if (background!=null) {
            background.getBitmap().recycle();
            background = null;
        }
        isWakeUpCheckActive = false;
        AppWakeLock.releaseCpuLock();
        super.onDestroy();
    }

}
