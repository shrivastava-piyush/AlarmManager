package com.bytezap.wobble.timer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.preference.VoiceSettingsActivity;
import com.bytezap.wobble.customviews.Chronometer;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.SoundController;
import com.bytezap.wobble.utils.TtsSpeaker;
import com.google.android.gms.ads.AdView;

import java.util.Locale;

public class TimerAlert extends AppCompatActivity {

    private static final String TIME_ELAPSED = "timeElapsed";
    public static boolean isAlertActive = false;
    private final Handler handler;
    private SoundController controller;
    private int timeElapsed;
    private long startTime;
    private TextView tHours;
    private TextView tMinutes;
    private TextView tSeconds;
    private Chronometer timerAlert;
    private TextView tMilliseconds;
    private Vibrator vibrator;
    private TextToSpeech speaker;
    private boolean isAlert;
    private Runnable runnable;
    private AdView adView;

    public TimerAlert() {
        handler = new Handler();
        timeElapsed = 0;
        startTime = 0;
        isAlertActive = true;
        speaker = null;
        runnable = new Runnable() {
            int secs;
            int mins;
            int hrs;

            @Override
            public void run() {
                long currTime = System.currentTimeMillis();
                timerAlert.updateChronometer(timeElapsed);
                hrs = (timeElapsed / 3600000) % 60;
                mins = (timeElapsed / 60000) % 60;
                secs = timeElapsed / 1000 % 60;

                if (Locale.getDefault().getLanguage().contains("en")) {

                    SpannableString spannableHour = new SpannableString(String.format(Locale.getDefault(), "%02d", hrs) + "H");
                    spannableHour.setSpan(new RelativeSizeSpan(0.5f), 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tHours.setText(spannableHour);

                    SpannableString spannableMinute = new SpannableString(String.format(Locale.getDefault(), "%02d", mins) + "M");
                    spannableMinute.setSpan(new RelativeSizeSpan(0.5f), 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tMinutes.setText(spannableMinute);

                    SpannableString spannableSecond = new SpannableString(String.format(Locale.getDefault(), "%02d", secs) + "S");
                    spannableSecond.setSpan(new RelativeSizeSpan(0.5f), 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tSeconds.setText(spannableSecond);

                    SpannableString spannableMilliSecond = new SpannableString(String.format(Locale.getDefault(), "%03d", timeElapsed % 1000) + "MS");
                    spannableMilliSecond.setSpan(new RelativeSizeSpan(0.4f), 3, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tMilliseconds.setText(spannableMilliSecond);

                } else {

                    tHours.setText(String.format(Locale.getDefault(), "%02d", hrs));
                    tMinutes.setText(String.format(Locale.getDefault(), "%02d", mins));
                    tSeconds.setText(String.format(Locale.getDefault(), "%02d", secs));
                    tMilliseconds.setText(String.format(Locale.getDefault(), "%03d", timeElapsed % 1000));
                }

                timeElapsed += currTime - startTime;
                startTime = currTime;
                if (!isAlert && timeElapsed >= 15000) {
                    finishAlert();
                } else {
                    handler.postDelayed(this, TimerFragment.TIMER_INTERVAL);
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        Resources res = getApplicationContext().getResources();

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        if (CommonUtils.isLOrLater()) {
            window.setStatusBarColor(Color.parseColor("#80000000"));
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String locale = settings.getString(SettingsActivity.LANGUAGE, "default");
        CommonUtils.setLangCode(locale);
        CommonUtils.setLanguage(res, locale);

        int tNumber = getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_BLUE_NIGHT);
        BitmapController.setThemeNumber(tNumber);

        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        window.setBackgroundDrawable(background);
        setContentView(R.layout.timer_alert);

        controller = SoundController.getInstance(getApplicationContext());
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        isAlert = settings.getBoolean(SettingsActivity.COUNTDOWN_ALARM, true);
        if (settings.getBoolean(VoiceSettingsActivity.VOICE, true) && savedInstanceState == null) {
            TtsSpeaker.setShouldShutDown(false);
            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.time_up) : "Time's Up", TtsSpeaker.isTtsOff(), BitmapController.isAppNotRunning());
            voiceAlert();
        } else {
            //Start the tone
            if (isAlert) {
                controller.activateAlert();
            } else {
                vibrator.vibrate(new long[]{1000, 300}, 0);
            }
        }

        timerAlert = findViewById(R.id.timer_alert);
        final FloatingActionButton stop = findViewById(R.id.timer_alert_stop);
        final TextView tText = findViewById(R.id.timer_alert_warning);
        tText.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_infinite));
        tHours = findViewById(R.id.timer_alert_hour);
        tMinutes = findViewById(R.id.timer_alert_minute);
        tSeconds = findViewById(R.id.timer_alert_second);
        tMilliseconds = findViewById(R.id.timer_alert_millisec);

        switch (BitmapController.getThemeNumber()){
            case ThemeDetails.THEME_MOUNTAINS:
            case ThemeDetails.THEME_SUNRISE:
            case ThemeDetails.THEME_AURORA:
            case ThemeDetails.THEME_BEACH:
            case ThemeDetails.THEME_FOGGY_FOREST:
            case ThemeDetails.THEME_CYAN:
            case ThemeDetails.THEME_THISTLE_PURPLE:
                tHours.setBackgroundResource(R.drawable.block_dark_opaque_left_border);
                tMinutes.setBackgroundResource(R.drawable.block_dark_opaque);
                tSeconds.setBackgroundResource(R.drawable.block_dark_opaque);
                tMilliseconds.setBackgroundResource(R.drawable.block_dark_opaque_right_border);
                break;

            case ThemeDetails.THEME_RAINY_DAY:
            case ThemeDetails.THEME_CUSTOM:
            case ThemeDetails.THEME_SHIMMERING_NIGHT:
            case ThemeDetails.THEME_BLUE_NIGHT:
            case ThemeDetails.THEME_DARK_COSMOS:
            case ThemeDetails.THEME_OF_TIME:
                tHours.setBackgroundResource(R.drawable.block_white_opaque_left_border);
                tMinutes.setBackgroundResource(R.drawable.block_white_opaque);
                tSeconds.setBackgroundResource(R.drawable.block_white_opaque);
                tMilliseconds.setBackgroundResource(R.drawable.block_white_opaque_right_border);
                break;

        }

        timerAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake_and_rotate));
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAlert();
            }
        });

        NotificationProvider.cancelNotification(getApplicationContext());
        NotificationProvider.showTimerAlertNotification(getApplicationContext());
        startTime = System.currentTimeMillis();
        handler.removeCallbacks(runnable);
        handler.post(runnable);

        //Ads
        /*if (!LicenceController.checkLicense(getApplicationContext())) {
            LinkDetector detector = new LinkDetector(getApplicationContext());
            if (detector.isNetworkAvailable()) {
                adView = (AdView) findViewById(R.id.timer_alert_adView);
                adView.setVisibility(View.VISIBLE);
                adView.setDrawingCacheEnabled(true);
                adView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build());
            }
        }*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(TIME_ELAPSED, timeElapsed);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        timeElapsed = savedInstanceState.getInt(TIME_ELAPSED);
    }

    private void voiceAlert() {

        if (CommonUtils.is15OrLater()) {
            speaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
                    if (isAlert) {
                        controller.activateAlert();
                    } else {
                        vibrator.vibrate(new long[]{1000, 300}, 0);
                    }
                    speaker.setOnUtteranceProgressListener(null);
                    if (BitmapController.isAppNotRunning()) {
                        TtsSpeaker.shutDown(getApplicationContext());
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    if (isAlert) {
                        controller.activateAlert();
                    } else {
                        vibrator.vibrate(new long[]{1000, 300}, 0);
                    }
                    speaker.setOnUtteranceProgressListener(null);
                    if (BitmapController.isAppNotRunning()) {
                        TtsSpeaker.shutDown(getApplicationContext());
                    }
                }
            });
        } else {
            speaker.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    if (isAlert) {
                        controller.activateAlert();
                    } else {
                        vibrator.vibrate(new long[]{1000, 300}, 0);
                    }
                    speaker.setOnUtteranceProgressListener(null);
                    if (BitmapController.isAppNotRunning()) {
                        TtsSpeaker.shutDown(getApplicationContext());
                    }
                }
            });
        }
    }

    private void finishAlert() {
        if (speaker != null) {
            speaker.stop();
            if (CommonUtils.is15OrLater()) {
                speaker.setOnUtteranceProgressListener(null);
            } else {
                speaker.setOnUtteranceCompletedListener(null);
            }
        }
        controller.terminate();
        handler.removeCallbacks(runnable);
        vibrator.cancel();
        TtsSpeaker.setShouldShutDown(true);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(R.layout.timer_alert);
        finish();
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onBackPressed() {
        finishAlert();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        vibrator.cancel();
        if (adView!=null) {
            adView.destroy();
        }
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(R.layout.timer_alert); // Precautionary stuff
        isAlertActive = false;
        super.onDestroy();
    }

}
