/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 * authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.alarm.camera.BarcodeScanner;
import com.bytezap.wobble.alarm.media.MediaMultiSelectActivity;
import com.bytezap.wobble.alarm.media.MediaPickerActivity;
import com.bytezap.wobble.alarm.media.Recorder;
import com.bytezap.wobble.alarm.media.RingtonePickerActivity;
import com.bytezap.wobble.customviews.ExpandableLayout;
import com.bytezap.wobble.customviews.TweakedSwitch;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.database.DataManager;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;
import com.bytezap.wobble.utils.LicenceController;
import com.bytezap.wobble.utils.LinkDetector;
import com.bytezap.wobble.utils.ToastGaffer;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormatSymbols;
import java.util.Locale;

public class AlarmDetails extends AppCompatActivity implements OnClickListener {

    private static final String TAG = AlarmDetails.class.getSimpleName();
    public final static String IS_REPLICATE = "details_isReplicate";
    public final static String DETAILS_VOCAL_MESSAGE_PLACE = "details_vocal_message_place";
    public final static String LAUNCH_APP_NAME = "launch_app_name";
    public final static String LAUNCH_APP_PACKAGE = "launch_app_package";
    public final static String DETAILS_TONE_TITLE = "details_tone_title";
    public final static String SONG_IDS = "song_ids";
    private final static String DETAILS_ALARM_OBJECT = "details_alarm_object";

    private AlarmObject alarm;
    private TimePicker timePicker;
    private TweakedSwitch[] detailsDays;
    private CheckBox repeat, vibrate;
    private TweakedSwitch wakeupCheck, launchApp;
    private FrameLayout headerDays;
    private String[] snoozeTime, dismissMethods, snoozeMethods;
    private int mSelectSource;
    private TextView snoozeDisableIndicator, alarmName, toneIndicator, vocalMessage, dismissIndicator, snoozeIndicator;
    private ExpandableLayout repeatDays;
    private DataManager dataManager;
    private boolean isReplicate = false;

    private AdView adView;
    private final Handler adsHandler = new Handler();
    private final Runnable adsRunnable = new Runnable() {
        boolean isFirstTime = true;

        @Override
        public void run() {
            /*NativeContentAd contentAd = new NativeContentAd() {
                @Override
                public CharSequence getHeadline() {
                    return "lalallala";
                }

                @Override
                public List<Image> getImages() {
                    return null;
                }

                @Override
                public CharSequence getBody() {
                    return "ipsum dolor gendit bolor";
                }

                @Override
                public Image getLogo() {
                    return new Image() {
                        @Override
                        public Drawable getDrawable() {
                            return ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_action_checked);
                        }

                        @Override
                        public Uri getUri() {
                            return null;
                        }

                        @Override
                        public double getScale() {
                            return 0;
                        }
                    };
                }

                @Override
                public CharSequence getCallToAction() {
                    return null;
                }

                @Override
                public CharSequence getAdvertiser() {
                    return "Google Play";
                }

                @Override
                public Bundle getExtras() {
                    return null;
                }

                @Override
                public void destroy() {

                }

                @Override
                protected Object zzdg() {
                    return null;
                }
            };
            final NativeContentAdView nativeView = (NativeContentAdView) findViewById(R.id.native_ad);
            TextView headlineView = (TextView) nativeView.findViewById(R.id.native_headline);
            headlineView.setText(contentAd.getHeadline());
            nativeView.setHeadlineView(headlineView);
            TextView subheadlineView = (TextView) nativeView.findViewById(R.id.native_sub_headline);
            subheadlineView.setText(contentAd.getBody());
            nativeView.setHeadlineView(subheadlineView);
            ImageView imageView = (ImageView) nativeView.findViewById(R.id.native_ad_image);
            imageView.setImageDrawable(contentAd.getLogo().getDrawable());
            nativeView.setLogoView(imageView);
            AdLoader adLoader = new AdLoader.Builder(getApplicationContext(), "ca-app-pub-3940256099942544/2247696110")
                    .forAppInstallAd(new NativeAppInstallAd.OnAppInstallAdLoadedListener() {
                        @Override
                        public void onAppInstallAdLoaded(NativeAppInstallAd appInstallAd) {
                            // Show the app install ad.
                        }
                    })
                    .forContentAd(new NativeContentAd.OnContentAdLoadedListener() {
                        @Override
                        public void onContentAdLoaded(NativeContentAd contentAd) {
                            // Show the content ad.
                        }
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            // Handle the failure by logging, altering the UI, etc.
                        }
                    })
                    .withNativeAdOptions(new NativeAdOptions.Builder()
                            // Methods in the NativeAdOptions.Builder class can be
                            // used here to specify individual options settings.
                            .build())
                    .build();
            nativeView.setVisibility(View.VISIBLE);
            adLoader.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build());*/
            if (LicenceController.isIsAdFirstTime()) {
                if (isFirstTime) {
                    adView = findViewById(R.id.details_adView);
                    adView.setAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            LicenceController.setIsAdFirstTime(false);
                            adView.setVisibility(View.VISIBLE);
                        }
                    });
                    isFirstTime = false;
                    adsHandler.postDelayed(adsRunnable, 300);
                } else {
                    adView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                            .build());
                }
            } else {
                adView = findViewById(R.id.details_adView);
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        adView.setVisibility(View.VISIBLE);
                    }
                });
                adView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build());
            }
        }
    };

    private void createAlarm() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateAlarmFromLayout();
                alarm.id = dataManager.createAlarm(alarm);
                AlarmInstance instance = dataManager.createInstance(alarm);
                if (alarm.id>-1) {
                    AlarmAssistant.checkAndSetAlarm(getApplicationContext(), instance); // Update all the alarms since we do no have the newly created alarm id
                } else {
                    AlarmAssistant.cancelAlarms(getApplicationContext(), dataManager);
                    AlarmAssistant.checkAndSetAlarms(getApplicationContext(), dataManager); // Update all the alarms since we do no have the newly created alarm id
                }
                AlarmAssistant.updateClosestAlarm(getApplicationContext(), dataManager.getInstances());
                CommonUtils.showAlarmToast(getApplicationContext(), instance.getNextAlarmTime().getTimeInMillis());
            }
        });
    }

    private void updateAlarm() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateAlarmFromLayout();
                dataManager.updateAlarm(alarm);
                AlarmInstance instance = dataManager.doesInstanceExist(alarm.id) ? dataManager.updateInstanceFromAlarm(alarm) : dataManager.createInstance(alarm);
                if (instance != null) {
                    AlarmAssistant.cancelAlarm(getApplicationContext(), instance);
                    CommonUtils.sendAlarmBroadcast(AlarmDetails.this, instance.id, AlarmService.ALARM_STATE_CHANGE);
                    AlarmAssistant.checkAndSetAlarm(getApplicationContext(), instance);
                    AlarmAssistant.updateClosestAlarm(getApplicationContext(), dataManager.getInstances());
                    CommonUtils.showAlarmToast(getApplicationContext(), instance.getNextAlarmTime().getTimeInMillis());
                }
            }
        });
    }

    private void setRingtoneTitle(final Uri tone, final int toneType, boolean isThreadPoolExecutor) {
        final AsyncTask<Void, Void, String> ringtoneTask = new AsyncTask<Void, Void, String>() {

            @Override
            protected void onPreExecute() {
                toneIndicator.setText(R.string.loading_tone);
            }

            @Override
            protected String doInBackground(Void... params) {
                if (tone == null) {
                    return "";
                }

                if (CommonUtils.isKOrLater()) {
                    return getRingToneTitle(tone, toneType);
                } else {
                    return icsGetRingToneTitle(getApplicationContext(), tone, toneType);
                }
            }

            @Override
            protected void onPostExecute(String title) {
                super.onPostExecute(title);
                if (!TextUtils.isEmpty(title)) {
                    toneIndicator.setText(title);
                } else {
                    //The user might have forgot to set the tone or the tone might have been deleted or moved
                    alarm.alarmTone = RingtoneManager.getDefaultUri(AudioManager.STREAM_ALARM);
                    toneIndicator.setText(R.string.default_ringtone);
                }
            }
        };
        if (isThreadPoolExecutor) {
            ringtoneTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            ringtoneTask.execute();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Resources res = getResources();
        Window window = getWindow();
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        int tNumber = getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, 0);
        BitmapDrawable background = BitmapController.getDetailsBitmap(res, tNumber);
        if (background != null) {
            try {
                window.setBackgroundDrawable(null);
                window.setBackgroundDrawable(background);
            } catch (Throwable e) {
                Log.e(TAG, e.toString());
                window.setBackgroundDrawable(null);
                window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
            }
        } else {
            window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        if (BitmapController.isAnimation()) {
            overridePendingTransition( R.anim.fade_in, R.anim.fade_out);
        }

        if (CommonUtils.is16OrLater()) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        setContentView(R.layout.activity_details);
        dataManager = DataManager.getInstance(getApplicationContext());

        final LinearLayout done = findViewById(R.id.alarm_details_done);
        final LinearLayout cancel = findViewById(R.id.alarm_details_cancel);
        final LinearLayout nameLayout = findViewById(R.id.alarm_details_name_layout);
        final LinearLayout voiceLayout = findViewById(R.id.alarm_details_voice_layout);
        alarmName = findViewById(R.id.alarm_details_name);
        vocalMessage = findViewById(R.id.alarm_details_voice);
        timePicker = findViewById(R.id.alarm_details_time_picker);
        final LinearLayout dismissLayout = findViewById(R.id.dismiss_layout);
        final LinearLayout snoozeLayout = findViewById(R.id.snooze_layout);
        timePicker.setIs24HourView(DateFormat.is24HourFormat(getApplicationContext()));

        detailsDays = new TweakedSwitch[]{findViewById(R.id.alarm_details_repeat_sunday),
                findViewById(R.id.alarm_details_repeat_monday),
                findViewById(R.id.alarm_details_repeat_tuesday),
                findViewById(R.id.alarm_details_repeat_wednesday),
                findViewById(R.id.alarm_details_repeat_thursday),
                findViewById(R.id.alarm_details_repeat_friday),
                findViewById(R.id.alarm_details_repeat_saturday)};

        toneIndicator = findViewById(R.id.alarm_label_tone_selection);
        repeatDays = findViewById(R.id.expand);
        headerDays = repeatDays.getHeaderLayout();
        repeat = findViewById(R.id.repeatButton);
        vibrate = findViewById(R.id.vibrateButton);
        wakeupCheck = findViewById(R.id.wakeup_check);
        snoozeIndicator = findViewById(R.id.snooze_indicator);
        dismissIndicator = findViewById(R.id.dismiss_indicator);
        snoozeDisableIndicator = findViewById(R.id.snooze_disable_indicator);
        final LinearLayout disableLayout = findViewById(R.id.disable_snooze_layout);
        launchApp = findViewById(R.id.app_selected);
        final ImageView wakeUpInfo = findViewById(R.id.wakeup_info);
        final ImageView launchInfo = findViewById(R.id.app_launch_info);
        final ImageView snoozeInfo = findViewById(R.id.snooze_disable_info);
        LinearLayout ringToneContainer = findViewById(R.id.alarm_ringtone_container);
        LinearLayout vibrationLayout = findViewById(R.id.details_vibration_layout);

        done.setOnClickListener(this);
        cancel.setOnClickListener(this);
        ringToneContainer.setOnClickListener(this);
        nameLayout.setOnClickListener(this);
        voiceLayout.setOnClickListener(this);
        vibrationLayout.setOnClickListener(this);
        wakeupCheck.setOnClickListener(this);
        wakeUpInfo.setOnClickListener(this);
        launchInfo.setOnClickListener(this);
        snoozeInfo.setOnClickListener(this);
        dismissLayout.setOnClickListener(this);
        snoozeLayout.setOnClickListener(this);
        disableLayout.setOnClickListener(this);
        launchApp.setOnClickListener(this);

        launchApp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (alarm.launchAppPkg != null && !launchApp.isChecked()) {
                    if (alarm.launchAppPkg.contains(".")) {
                        launchApp.setChecked(true);
                        return true;
                    }
                }
                return false;
            }
        });

        dismissMethods = res.getStringArray(R.array.dismissMethodArray);
        snoozeMethods = res.getStringArray(R.array.snoozeMethodArray);
        snoozeTime = res.getStringArray(R.array.finalSnoozeTime);
        Bundle bundle =  getIntent().getExtras();
        final long id = bundle.getLong(AlarmAssistant.ID);
        isReplicate = bundle.getBoolean(IS_REPLICATE);

        if (savedInstanceState == null) {
            if (id == -1) {
                if (CommonUtils.isMOrLater()) {
                    alarm = new AlarmObject(timePicker.getHour(), timePicker.getMinute());
                } else {
                    alarm = new AlarmObject(timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                }
                headerDays.setEnabled(false);
            } else {
                setupOldDetails(id);
            }
        } else {

            if (id == -1) {
                if (CommonUtils.isMOrLater()) {
                    alarm = new AlarmObject(timePicker.getHour(), timePicker.getMinute());
                } else {
                    alarm = new AlarmObject(timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                }
                headerDays.setEnabled(false);
            } else {
                alarm = savedInstanceState.getParcelable(DETAILS_ALARM_OBJECT);
                if (alarm == null) {
                    Log.v(TAG, "Alarm null for id: " + id);
                    alarm = dataManager.getAlarmById(id);
                    if (alarm == null) {
                        // Alarm can't be found in the database itself. Finish the activity here
                        finish();
                    }
                }
            }

            repeatDays.hide();
            alarmName.setText(!TextUtils.isEmpty(alarm.name) ? alarm.name : getString(R.string.details_set_alarm_name));
            if (alarm.vocalMessagePlace != AlarmObject.ALARM_VOCAL_NOT_CALLED) {
                vocalMessage.setText(alarm.vocalMessageType == AlarmObject.VOCAL_TYPE_TEXT ? R.string.custom_message : R.string.recorded_message);
            }
            if (alarm.alarmTone != null) {
                toneIndicator.setText(savedInstanceState.getString(DETAILS_TONE_TITLE, getString(R.string.details_alarm_tone_default)));
            }
            dismissIndicator.setText(dismissMethods[alarm.dismissMethod]);
            snoozeDisableIndicator.setText(snoozeTime[alarm.snoozeTimeIndex]);
        }

        repeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    headerDays.setEnabled(true);
                    if (!repeatDays.isOpened()) {
                        repeatDays.show();
                        setAlarmDays(id);
                    }
                } else {
                    repeatDays.hide();
                    headerDays.setEnabled(false);
                }

            }
        });

        DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
        String[] weekDays = symbols.getWeekdays();

        for (int i = 0; i <= 6; i++) {
            detailsDays[i].setText(weekDays[i + 1]);
        }

        for (int day = 0; day <= 6; day++) {
            final int finalDay = day;
            detailsDays[day].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    detailsDays[finalDay].toggle();
                    boolean isDaySet = detailsDays[finalDay].isChecked();
                    alarm.setRepeatingDay(finalDay, isDaySet);
                    if (alarm.isOneTimeAlarm()) {
                        repeatDays.hide();
                        repeat.setChecked(false);
                    }
                }
            });
        }

        //Ads
        if (!LicenceController.checkLicense(getApplicationContext())) {
            LinkDetector detector = new LinkDetector(getApplicationContext());
            if (detector.isNetworkAvailable()) {
                runOnUiThread(adsRunnable);
            }
        }
    }

    private void setupOldDetails(final long id) {

        alarm = dataManager.getAlarmById(id);
        if (alarm == null || alarm.id != id) {
            Log.v(TAG, "Alarm null for id: " + id);
            finish();
        }

        if (Build.VERSION.SDK_INT >= 23) {
            timePicker.setMinute(alarm.minutes);
            timePicker.setHour(alarm.hour);
        } else {
            timePicker.setCurrentMinute(alarm.minutes);
            timePicker.setCurrentHour(alarm.hour);
        }

        if(alarm.toneType==AlarmObject.TONE_TYPE_SILENT){
            toneIndicator.setText(R.string.default_silent);
        } else if(alarm.toneType==AlarmObject.TONE_TYPE_LOUD_RINGTONE){
            toneIndicator.setText(R.string.loud_ring);
        } else if(alarm.toneType==AlarmObject.TONE_TYPE_SHUFFLE){
            toneIndicator.setText(R.string.shuffle_songs);
        } else {
            setRingtoneTitle(alarm.alarmTone, alarm.toneType, false);
        }

        for (int day = AlarmObject.SUNDAY; day <= AlarmObject.SATURDAY; day++) {
            detailsDays[day].setChecked(alarm.getRepeatingDay(AlarmObject.SUNDAY));
        }

        vibrate.setChecked(alarm.isVibrate);

        repeat.setChecked(!alarm.isOneTimeAlarm());
        headerDays.setEnabled(!alarm.isOneTimeAlarm());

        alarmName.setText(!TextUtils.isEmpty(alarm.name) ? alarm.name : getString(R.string.details_set_alarm_name));
        if (alarm.vocalMessagePlace != AlarmObject.ALARM_VOCAL_NOT_CALLED) {
            vocalMessage.setText(alarm.vocalMessageType == AlarmObject.VOCAL_TYPE_AUDIO ? R.string.recorded_message : R.string.custom_message);
        }
        wakeupCheck.setChecked(alarm.wakeupCheck);
        snoozeIndicator.setText(snoozeMethods[alarm.snoozeMethod]);
        dismissIndicator.setText(dismissMethods[alarm.dismissMethod]);
        snoozeDisableIndicator.setText(snoozeTime[alarm.snoozeTimeIndex]);

        for (int day = AlarmObject.SUNDAY; day <= AlarmObject.SATURDAY; day++) {
            detailsDays[day].setChecked(alarm.getRepeatingDay(day));
        }

        launchApp.setChecked(alarm.isLaunchApp);
        if (alarm.launchAppPkg != null) {
            if (alarm.launchAppPkg.contains(".")) {
                final PackageManager pm = getApplicationContext().getPackageManager();
                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(alarm.launchAppPkg, 0);
                } catch (final Exception e) {
                    ai = null;
                }
                final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : getString(R.string.launch_app_not_found));
                launchApp.setSummary(applicationName);
            }
        } else {
            launchApp.setSummary(getString(R.string.default_none));
        }
    }

    private void setAlarmDays(long id) {
        if (id == -1 || alarm.isOneTimeAlarm()) {
            for (int day = AlarmObject.SUNDAY; day <= AlarmObject.SATURDAY; day++) {
                detailsDays[day].setChecked(true);
                alarm.setRepeatingDay(day, true);
            }
        } else {
            for (int day = AlarmObject.SUNDAY; day <= AlarmObject.SATURDAY; day++) {
                detailsDays[day].setChecked(alarm.getRepeatingDay(day));
            }
        }
    }

    private void launchTonePicker() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
        builder.setTitle(R.string.details_alarm_tone_default).setSingleChoiceItems(R.array.ringtoneType, alarm.toneType, new selectRingtone());

        AlertDialog dialog = builder.create();
        DialogSupervisor.setDialog(dialog);
        dialog.show();
    }

    private void pickIntent() {
        if (CommonUtils.isMOrLater() && this.mSelectSource > 2) {
            // Reinforce the permission check here
            if (ContextCompat.checkSelfPermission(AlarmDetails.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(AlarmDetails.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Clock.REQUEST_PERMISSION_STORAGE_DOC);
            } else {
                accessMusic();
            }
        } else {
            accessMusic();
        }
    }

    private void accessMusic() {
        switch (this.mSelectSource) {

            case 0:
                alarm.toneType = AlarmObject.TONE_TYPE_SILENT;
                toneIndicator.setText(R.string.default_silent);
                break;

            case 1:
                try {
                    startActivityForResult(new Intent(this, RingtonePickerActivity.class), 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case 2:
                alarm.toneType = AlarmObject.TONE_TYPE_LOUD_RINGTONE;
                toneIndicator.setText(R.string.loud_ring);
                break;

            case 3:
                try {
                    startActivityForResult(new Intent(this, MediaPickerActivity.class), 2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case 4:
                try {
                    Intent intent = new Intent(this, MediaMultiSelectActivity.class);
                    if (alarm.toneType == AlarmObject.TONE_TYPE_SHUFFLE) {
                        intent.putExtra(SONG_IDS, alarm.uriIds);
                    }
                    startActivityForResult(intent, 3);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void finishSetup() {
        if (alarm.id < 0 || (alarm.id > 0 && isReplicate)) {
            createAlarm();
        } else {
            updateAlarm();
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.fade_out);
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(DETAILS_ALARM_OBJECT, alarm);
        outState.putString(DETAILS_TONE_TITLE, toneIndicator.getText().toString());
        super.onSaveInstanceState(outState);
    }

    private void startBarcodeScanner() {
        Intent barCodeIntent = new Intent(this, BarcodeScanner.class);
        try {
            startActivityForResult(barCodeIntent, 4);
        } catch (Throwable throwable) {
            Log.v("AlarmDetails", "Barcode Scanner could not be started: " + throwable.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.alarm_ringtone_container:
                launchTonePicker();
                break;

            case R.id.alarm_details_name_layout:
                setTitleDialog();
                break;

            case R.id.alarm_details_voice_layout:
                setVocalDialog();
                break;

            case R.id.alarm_details_done:
                if (CommonUtils.isMOrLater()) {
                    if (ContextCompat.checkSelfPermission(AlarmDetails.this,
                            Manifest.permission.READ_PHONE_STATE)
                            != PackageManager.PERMISSION_GRANTED) {

                        ToastGaffer.showToast(getApplicationContext(), getString(R.string.m_perm_telephone), true);
                        //Called on activity
                        ActivityCompat.requestPermissions(AlarmDetails.this,
                                new String[]{Manifest.permission.READ_PHONE_STATE},
                                Clock.REQUEST_PERMISSION_PHONE_STATE);
                    } else {
                        finishSetup();
                    }
                } else {
                    finishSetup();
                }
                break;

            case R.id.alarm_details_cancel:
                updateForComparison();
                ContextThemeWrapper cancelWrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
                AlertDialog.Builder cancelBuilder = new AlertDialog.Builder(cancelWrapper);
                cancelBuilder.setTitle(R.string.discard)
                        .setMessage(R.string.discard_message);
                cancelBuilder.setPositiveButton(R.string.default_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                cancelBuilder.setNegativeButton(R.string.cancel, null);
                AlertDialog cancelDialog = cancelBuilder.create();
                DialogSupervisor.setDialog(cancelDialog);
                cancelDialog.show();
                break;

            case R.id.details_vibration_layout:
                boolean hasVibrator = ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator();
                if (hasVibrator) {
                    vibrate.toggle();
                } else {
                    ToastGaffer.showToast(getApplicationContext(), "Sorry, vibration is not available on this device.");
                }
                break;

            case R.id.wakeup_check:
                wakeupCheck.toggle();
                break;

            case R.id.dismiss_layout:
                ContextThemeWrapper dismissWrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
                AlertDialog.Builder dismissBuilder = new AlertDialog.Builder(dismissWrapper);
                dismissBuilder.setTitle(R.string.details_dismiss_method)
                        .setSingleChoiceItems(dismissMethods, alarm.dismissMethod, new selectDismissMethod());
                AlertDialog dismissDialog = dismissBuilder.create();
                DialogSupervisor.setDialog(dismissDialog);
                dismissDialog.show();
                break;

            case R.id.snooze_layout:
                ContextThemeWrapper themeSnooze = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
                AlertDialog.Builder snoozeBuilder = new AlertDialog.Builder(themeSnooze);
                snoozeBuilder.setTitle(R.string.details_snooze_method)
                        .setSingleChoiceItems(snoozeMethods, alarm.snoozeMethod, new selectSnoozeMethod());
                AlertDialog snoozeDialog = snoozeBuilder.create();
                DialogSupervisor.setDialog(snoozeDialog);
                snoozeDialog.show();
                break;

            case R.id.disable_snooze_layout:
                ContextThemeWrapper themeWrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
                AlertDialog.Builder disableBuilder = new AlertDialog.Builder(themeWrapper);
                disableBuilder.setTitle(R.string.disable_snoozing)
                        .setSingleChoiceItems(snoozeTime, alarm.snoozeTimeIndex, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                alarm.snoozeTimeIndex = which;
                                snoozeDisableIndicator.setText(snoozeTime[which]);
                                dialog.dismiss();
                            }
                        });
                AlertDialog disableDialog = disableBuilder.create();
                DialogSupervisor.setDialog(disableDialog);
                disableDialog.show();
                break;

            case R.id.app_selected:
                if (launchApp.isChecked()) {
                    launchApp.toggle();
                } else {
                    Intent appIntent = new Intent(this, ApplicationList.class);
                    startActivityForResult(appIntent, 5);
                }
                break;

            case R.id.wakeup_info:
                showInfoDialog(AlarmDetails.this, getString(R.string.wakeup_info), getString(R.string.wakeup_info_title));
                break;

            case R.id.app_launch_info:
                showInfoDialog(AlarmDetails.this, getString(R.string.launch_app_info), getString(R.string.details_launch_app));
                break;

            case R.id.snooze_disable_info:
                showInfoDialog(AlarmDetails.this, getString(R.string.snooze_disable_info), getString(R.string.snooze_disable_info_title));
                break;
        }
    }

    private void updateForComparison(){

        if (Build.VERSION.SDK_INT >= 23) {
            alarm.hour = timePicker.getHour();
            alarm.minutes = timePicker.getMinute();
        } else {
            alarm.hour = timePicker.getCurrentHour();
            alarm.minutes = timePicker.getCurrentMinute();
        }

        alarm.isVibrate = vibrate.isChecked();
        alarm.isLaunchApp = launchApp.isChecked();
        alarm.wakeupCheck = wakeupCheck.isChecked();
    }

    private void showInfoDialog(Context context, String info, String title) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, R.style.AlertDialogStyle);
        AlertDialog dialog = new AlertDialog.Builder(wrapper).create();
        dialog.setTitle(title);
        dialog.setMessage(info);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.default_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        DialogSupervisor.setDialog(dialog);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri uri;
            switch (requestCode) {
                case 1:
                    uri = data.getData();
                    if (uri != null) {
                        alarm.alarmTone = uri;
                        alarm.toneType = AlarmObject.TONE_TYPE_RINGTONE;
                        String title = data.getStringExtra(DETAILS_TONE_TITLE);
                        if (title != null) {
                            toneIndicator.setText(title);
                        } else {
                            setRingtoneTitle(uri, alarm.toneType, true);
                        }
                    }
                    break;

                case 2:
                    uri = data.getData();
                    if (uri != null) {
                        alarm.alarmTone = uri;
                        alarm.toneType = AlarmObject.TONE_TYPE_MUSIC;
                        String title = data.getStringExtra(DETAILS_TONE_TITLE);
                        if (title != null) {
                            toneIndicator.setText(title);
                        } else {
                            setRingtoneTitle(uri, alarm.toneType, true);
                        }
                    }
                    break;

                case 3:
                    long[] ids = data.getLongArrayExtra(SONG_IDS);
                    if (ids != null) {
                        alarm.uriIds = ids;
                        alarm.toneType = AlarmObject.TONE_TYPE_SHUFFLE;
                        toneIndicator.setText(R.string.shuffle_songs);
                    }
                    break;

                case 4:
                    String barText = data.getStringExtra(BarcodeScanner.BARCODE_TEXT);
                    if (!TextUtils.isEmpty(barText)) {
                        alarm.dismissMethod = AlarmObject.DISMISS_METHOD_BARCODE;
                        alarm.barcodeText = barText;
                        dismissIndicator.setText(dismissMethods[alarm.dismissMethod]);
                        ToastGaffer.showToast(getApplicationContext(), getString(R.string.default_barcode_selected, barText));
                    }
                    break;

                case 5:
                    String appName = data.getStringExtra(LAUNCH_APP_NAME);
                    String appPackage = data.getStringExtra(LAUNCH_APP_PACKAGE);
                    if (!TextUtils.isEmpty(appPackage)) {
                        alarm.launchAppPkg = appPackage;
                        launchApp.setChecked(true);
                        if (!TextUtils.isEmpty(appName)) {
                            launchApp.setSummary(appName);
                        }
                    }
                    break;

                case 6:
                    if (data != null) {
                        alarm.vocalMessageType = AlarmObject.VOCAL_TYPE_AUDIO;
                        alarm.vocalMessagePlace = data.getIntExtra(DETAILS_VOCAL_MESSAGE_PLACE, 1);
                        alarm.vocalMessage = data.getStringExtra(Recorder.FILE_PATH);
                        vocalMessage.setText(R.string.recorded_message);
                    }
                    break;

                /*case 7:
                    if (data != null) {
                        alarm.imagePath = data.getStringExtra(CameraActivity.IMAGE_PATH);
                        alarm.dismissMethod = AlarmObject.DISMISS_METHOD_PICTURE;
                        dismissIndicator.setText(dismissMethods[alarm.dismissMethod]);
                    }
                    break;*/

                default:
                    Log.v(TAG, "Unhandled request code in onActivityResult: " + requestCode);
            }
        }
    }

    private String icsGetRingToneTitle(Context context, Uri tone, int toneType) {
        String title = "";

        switch (toneType) {
            case AlarmObject.TONE_TYPE_RINGTONE:
                if (tone != null) {
                    Ringtone ringTone = RingtoneManager.getRingtone(context, tone);
                    if (ringTone != null) {
                        title = ringTone.getTitle(context);
                    }
                }
                break;

            case AlarmObject.TONE_TYPE_MUSIC:
                String scheme = tone.getScheme();
                if (CommonUtils.isUriRingtoneValid(context, tone) && Build.VERSION.SDK_INT < 19) {
                    switch (scheme) {
                        case "file":
                            title = tone.getLastPathSegment();
                        case "content":
                            String[] icsTitle = {MediaStore.Audio.Media.TITLE};
                            Cursor cursor = null;
                            try {
                                cursor = context.getContentResolver().query(tone, icsTitle, null, null, null);
                                if (cursor != null && cursor.getCount() != 0) {
                                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                                    cursor.moveToFirst();
                                    title = cursor.getString(columnIndex);
                                }
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                            break;
                    }
                }
                break;

            case AlarmObject.TONE_TYPE_SHUFFLE:
                title = getString(R.string.shuffle_songs);
                break;
        }

        return title;
    }

    private String getRingToneTitle(Uri tone, int toneType) {
        String title = "";

        switch (toneType) {
            case AlarmObject.TONE_TYPE_RINGTONE:
                if (tone != null) {
                    Ringtone ringTone = RingtoneManager.getRingtone(getApplicationContext(), tone);
                    if (ringTone != null) {
                        title = ringTone.getTitle(getApplicationContext());
                    }
                }
                break;

            case AlarmObject.TONE_TYPE_MUSIC:
                if (CommonUtils.isUriRingtoneValid(getApplicationContext(), tone)) {
                    title = getTitleFromDatabase(tone);
                }
                break;

            case AlarmObject.TONE_TYPE_SHUFFLE:
                title = getString(R.string.shuffle_songs);
                break;
        }

        return title;
    }

    private String getTitleFromDatabase(Uri uri) {
        String selection = null;
        String[] selectionArgs = null;
        String title = "";
        if (CommonUtils.isKOrLater()) {
            if (uri.getAuthority().equals("com.android.providers.downloads.documents")) {
                uri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(DocumentsContract.getDocumentId(uri)));
            } else if (uri.getAuthority().equals("com.android.providers.media.documents")) {
                String[] split = DocumentsContract.getDocumentId(uri).split(":");
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
            }
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, new String[]{CommonUtils.getUriTitleColumnName(uri)}, selection, selectionArgs, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    title = cursor.getString(0);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                try {
                    Log.e(TAG, "Get ringtone uri Exception: " + e.toString());
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return title;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Clock.REQUEST_PERMISSION_STORAGE_DOC:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessMusic();
                } else {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.permission_denied));
                }
                break;

            case Clock.REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent barCodeIntent = new Intent(this, BarcodeScanner.class);
                    try {
                        startActivityForResult(barCodeIntent, 4);
                    } catch (Throwable throwable) {
                        Log.v(TAG, "Barcode Scanner could not be started: " + throwable.getMessage());
                    }
                } else {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.permission_denied));
                }
                break;

            case Clock.REQUEST_PERMISSION_PHONE_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    finishSetup();
                } else {
                    finishSetup();
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.telephone_warning));
                }
                break;

            case Clock.REQUEST_PERMISSION_STORAGE_MICRO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchRecorder();
                } else {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.permission_denied));
                }
                break;
        }

    }

    @Override
    protected void onDestroy() {
        DialogSupervisor.cancelDialog();
        if (adView!=null) {
            adView.destroy();
        }
        adsHandler.removeCallbacks(adsRunnable);
        super.onDestroy();
    }

    private void launchDismissDialog(final boolean isDismiss){
        ContextThemeWrapper wrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vM = inflater.inflate(R.layout.math_setup_layout, null);

        final RadioGroup group = vM.findViewById(R.id.difficulty_group);
        final TextView probExample = vM.findViewById(R.id.prob_example);
        final TextView problems = vM.findViewById(R.id.problem_number);
        final SeekBar problemBar = vM.findViewById(R.id.problem_seekbar);
        final CheckBox skip = vM.findViewById(R.id.skip_checkbox);

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.problem_easy:
                        probExample.setText(String.format(Locale.getDefault(), "%d - %d = ?", 27, 9));
                        break;

                    case R.id.problem_medium:
                        probExample.setText(String.format(Locale.getDefault(), "%d + %d = ?", 137, 94));
                        break;

                    case R.id.problem_hard:
                        probExample.setText(String.format(Locale.getDefault(), "%d %s %d = ?", 399, "%",227));
                        break;

                    case R.id.problem_very_hard:
                        probExample.setText(String.format(Locale.getDefault(), "%d x %d = ?", 482, 742));
                        break;
                }
            }
        });

        problemBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (value == 0) {
                    value = 1;
                }
                problems.setText(String.valueOf(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        setGroupLevel(group, isDismiss ? alarm.dismissLevel : alarm.snoozeLevel);
        setProbExample(isDismiss ? alarm.dismissLevel : alarm.snoozeLevel, probExample);
        skip.setChecked(isDismiss ? alarm.dismissSkip : alarm.snoozeSkip);
        problemBar.setProgress(isDismiss ? alarm.mathDismissProb : alarm.mathSnoozeProb);

        final AlertDialog mDismissDialog = new AlertDialog.Builder(wrapper).create();
        mDismissDialog.setView(vM);
        mDismissDialog.setTitle(R.string.snooze_info_title);
        mDismissDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.default_ok) , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setAlarmLevel(group.getCheckedRadioButtonId(), isDismiss);
                if (isDismiss) {
                    alarm.dismissMethod = AlarmObject.DISMISS_METHOD_MATH;
                    alarm.mathDismissProb = problemBar.getProgress();
                    alarm.dismissSkip = skip.isChecked();
                    dismissIndicator.setText(dismissMethods[alarm.dismissMethod]);
                } else {
                    alarm.snoozeMethod = AlarmObject.SNOOZE_METHOD_MATH;
                    alarm.mathSnoozeProb = problemBar.getProgress();
                    alarm.snoozeSkip = skip.isChecked();
                    snoozeIndicator.setText(snoozeMethods[alarm.snoozeMethod]);
                }
                dialog.dismiss();
            }
        });

        mDismissDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        DialogSupervisor.setDialog(mDismissDialog);
        mDismissDialog.show();
    }

    private void launchShakeDialog(final boolean isDismiss){

        ContextThemeWrapper wrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vM = inflater.inflate(R.layout.shake_dialog, null);

        final NumberPicker picker = vM.findViewById(R.id.picker_shake);
        NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.valueOf(value * 5);
            }
        };
        picker.setFormatter(formatter);
        picker.setMinValue(1);
        picker.setMaxValue(400);
        picker.setValue(alarm.dismissShake /5);

        //Taking a necessary risk here. Shouldn't impact user if doesn't work
        try{
            Field f = NumberPicker.class.getDeclaredField("mInputText");
            f.setAccessible(true);
            EditText inputText = (EditText) f.get(picker);
            inputText.setFilters(new InputFilter[0]);
        } catch (Exception e){
            try {
                Method method = picker.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
                method.setAccessible(true);
                method.invoke(picker, true);
            } catch (Exception ignored){}
        }

        final AlertDialog mShakeDialog = new AlertDialog.Builder(wrapper).create();
        mShakeDialog.setView(vM);
        mShakeDialog.setTitle(R.string.times_shake);
        mShakeDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.default_ok) , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                    if (isDismiss) {
                        alarm.dismissMethod = AlarmObject.DISMISS_METHOD_SHAKE;
                        alarm.dismissShake = picker.getValue()*5;
                        dismissIndicator.setText(dismissMethods[alarm.dismissMethod]);
                    } else {
                        alarm.snoozeMethod = AlarmObject.SNOOZE_METHOD_SHAKE;
                        alarm.snoozeShake = picker.getValue()*5;
                        snoozeIndicator.setText(snoozeMethods[alarm.snoozeMethod]);
                    }
                } else {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.no_accelerometer));
                }
            }
        });
        mShakeDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        DialogSupervisor.setDialog(mShakeDialog);
        mShakeDialog.show();
    }

    private void setTitleDialog() {

        ContextThemeWrapper wrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View aN = inflater.inflate(R.layout.alarm_title, null);

        final EditText alarmTitle = aN.findViewById(R.id.alarm_details_name_holder);
        String s = alarmName.getText().toString();
        if (!s.equals(getString(R.string.details_set_alarm_name))) {
            alarmTitle.setText(s);
        }
        alarmTitle.setSelection(alarmTitle.getText().length());
        final AlertDialog mAlarmName = new AlertDialog.Builder(wrapper).create();
        mAlarmName.setView(aN);
        mAlarmName.setTitle(R.string.details_alarm_name);
        mAlarmName.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String s = alarmTitle.getText().toString();
                        if (!TextUtils.isEmpty(s)) {
                            alarmName.setText(s);
                            alarm.name = !s.equals(getString(R.string.details_set_alarm_name)) ? s : "";
                        }
                        mAlarmName.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        dialog.dismiss();
                    }
                });
        mAlarmName.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAlarmName.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        dialog.dismiss();
                    }
                });

        mAlarmName.getWindow().setWindowAnimations(R.style.dialogBottomAnimStyle);
        DialogSupervisor.setDialog(mAlarmName);
        alarmTitle.requestFocus();
        mAlarmName.show();

        mAlarmName.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mAlarmName.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

    }

    private void setVocalDialog() {

        String[] newItems = new String[]{getString(R.string.recorded_message), getString(R.string.custom_message)};
        String[] oldItems = new String[]{getString(R.string.recorded_message), getString(R.string.custom_message), getString(R.string.default_none)};
        ContextThemeWrapper wrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
        builder.setTitle(R.string.alarm_details_vocal_message).setItems(alarm.vocalMessagePlace == AlarmObject.ALARM_VOCAL_NOT_CALLED ? newItems : oldItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        if (CommonUtils.isMOrLater()) {
                            if (ContextCompat.checkSelfPermission(AlarmDetails.this,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {

                                ToastGaffer.showToast(getApplicationContext(), getString(R.string.m_perm_audio), true);

                                //Called on activity
                                ActivityCompat.requestPermissions(AlarmDetails.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        Clock.REQUEST_PERMISSION_STORAGE_MICRO);
                            } else {
                                launchRecorder();
                                dialog.dismiss();
                            }
                        } else {
                            launchRecorder();
                            dialog.dismiss();
                        }
                        break;

                    case 1:
                        vocalTextDialog();
                        dialog.dismiss();
                        break;

                    case 2:
                        alarm.vocalMessagePlace = AlarmObject.ALARM_VOCAL_NOT_CALLED;
                        vocalMessage.setText(R.string.alarm_details_message);
                        break;
                }
            }
        });

        AlertDialog dialog = builder.create();
        DialogSupervisor.setDialog(dialog);
        dialog.show();
    }

    private void launchRecorder() {
        Intent audioIntent = new Intent(getApplicationContext(), Recorder.class);
        audioIntent.putExtra(DETAILS_VOCAL_MESSAGE_PLACE, alarm.vocalMessagePlace);
        startActivityForResult(audioIntent, 6);
    }

    private void vocalTextDialog() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vM = inflater.inflate(R.layout.custom_message_layout, null);

        final EditText vocalTitle = vM.findViewById(R.id.alarm_details_vocal_holder);
        final RadioGroup group = vM.findViewById(R.id.radio_group);
        String s = alarm.vocalMessage;
        if (s != null && (alarm.vocalMessagePlace != AlarmObject.ALARM_VOCAL_NOT_CALLED) && alarm.vocalMessageType == AlarmObject.VOCAL_TYPE_TEXT) {
            vocalTitle.setText(s);
            setVocalMessageID(group, alarm.vocalMessagePlace);
        }
        vocalTitle.setSelection(vocalTitle.getText().length());
        final AlertDialog mVocalMessage = new AlertDialog.Builder(wrapper).create();
        mVocalMessage.setView(vM);
        mVocalMessage.setTitle(R.string.alarm_details_vocal_message);

        mVocalMessage.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String s = vocalTitle.getText().toString();
                        if (!TextUtils.isEmpty(s)) {
                            setVocalMessagePlace(group.getCheckedRadioButtonId());
                            vocalMessage.setText(R.string.custom_message);
                            alarm.vocalMessage = s;
                            alarm.vocalMessageType = AlarmObject.VOCAL_TYPE_TEXT;
                        }
                        dialog.dismiss();
                    }
                });
        mVocalMessage.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        boolean isVoice = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.VOICE, true);
        if (!isVoice) {
            TextView alert = vM.findViewById(R.id.voice_warning);
            alert.setVisibility(View.VISIBLE);
        }

        DialogSupervisor.setDialog(mVocalMessage);
        mVocalMessage.show();
    }

    private void setVocalMessagePlace(int checkedId) {

        switch (checkedId) {
            case R.id.vocal_after_time:
                alarm.vocalMessagePlace = AlarmObject.ALARM_VOCAL_AFTER_TIME;
                break;

            case R.id.vocal_before_time:
                alarm.vocalMessagePlace = AlarmObject.ALARM_VOCAL_BEFORE_TIME;
                break;

            case R.id.vocal_after_dismiss:
                alarm.vocalMessagePlace = AlarmObject.ALARM_VOCAL_AFTER_DISMISS;
                break;

            case R.id.vocal_after_snoozing:
                alarm.vocalMessagePlace = AlarmObject.ALARM_VOCAL_AFTER_SNOOZE;
                break;

            case R.id.vocal_replace_time:
                alarm.vocalMessagePlace = AlarmObject.ALARM_VOCAL_REPLACE_TIME;
                break;
        }
    }

    private void setAlarmLevel(int checkedId, boolean isDismiss){

        switch (checkedId){

            case R.id.problem_easy:
                if (isDismiss) {
                    alarm.dismissLevel = 0;
                } else {
                    alarm.snoozeLevel = 0;
                }
                break;

            case R.id.problem_medium:
                if (isDismiss) {
                    alarm.dismissLevel = 1;
                } else {
                    alarm.snoozeLevel = 1;
                }
                break;

            case R.id.problem_hard:
                if (isDismiss) {
                    alarm.dismissLevel = 2;
                } else {
                    alarm.snoozeLevel = 2;
                }
                break;

            case R.id.problem_very_hard:
                if (isDismiss) {
                    alarm.dismissLevel = 3;
                } else {
                    alarm.snoozeLevel = 3;
                }
                break;
        }
    }

    private void setGroupLevel(RadioGroup group, int level){
        switch (level){
            case 0:
                group.check(R.id.problem_easy);
                break;

            case 1:
                group.check(R.id.problem_medium);
                break;

            case 2:
                group.check(R.id.problem_hard);
                break;

            case 3:
                group.check(R.id.problem_very_hard);
                break;
        }
    }

    private void setProbExample(int level, TextView probExample){
        switch (level){
            case 0:
                probExample.setText(String.format(Locale.getDefault(), "%d - %d = ?", 27, 9));
                break;

            case 1:
                probExample.setText(String.format(Locale.getDefault(), "%d + %d = ?", 137, 94));
                break;

            case 2:
                probExample.setText(String.format(Locale.getDefault(), "%d %s %d = ?", 399, "%",227));
                break;

            case 3:
                probExample.setText(String.format(Locale.getDefault(), "%d x %d = ?", 482, 742));
                break;
        }
    }

    private void setVocalMessageID(RadioGroup group, int place) {

        switch (place) {
            case AlarmObject.ALARM_VOCAL_AFTER_TIME:
                group.check(R.id.vocal_after_time);
                break;

            case AlarmObject.ALARM_VOCAL_BEFORE_TIME:
                group.check(R.id.vocal_before_time);
                break;

            case AlarmObject.ALARM_VOCAL_AFTER_DISMISS:
                group.check(R.id.vocal_after_dismiss);
                break;

            case AlarmObject.ALARM_VOCAL_AFTER_SNOOZE:
                group.check(R.id.vocal_after_snoozing);
                break;

            case AlarmObject.ALARM_VOCAL_REPLACE_TIME:
                group.check(R.id.vocal_replace_time);
                break;
        }
    }

    @SuppressWarnings("deprecation")
    // Save details for the current alarm
    private void updateAlarmFromLayout() {
        if (Build.VERSION.SDK_INT >= 23) {
            alarm.hour = timePicker.getHour();
            alarm.minutes = timePicker.getMinute();
        } else {
            alarm.hour = timePicker.getCurrentHour();
            alarm.minutes = timePicker.getCurrentMinute();
        }

        if (!repeat.isChecked()) {
            alarm.setRepeatingDays(false);
        }

        alarm.isVibrate = vibrate.isChecked();
        alarm.isEnabled = true;
        alarm.isLaunchApp = launchApp.isChecked();
        alarm.wakeupCheck = wakeupCheck.isChecked();
    }

    @Override
    public void onBackPressed() {
        updateForComparison();
        ContextThemeWrapper wrapper = new ContextThemeWrapper(AlarmDetails.this, R.style.AlertDialogStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
        builder.setTitle("Discard")
                .setMessage("Discard all changes?");
        builder.setPositiveButton(R.string.default_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AlarmDetails.super.onBackPressed();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        DialogSupervisor.setDialog(dialog);
        dialog.show();
    }

    private class selectRingtone implements DialogInterface.OnClickListener {

        public void onClick(DialogInterface dialog, int which) {
            mSelectSource = which;
            pickIntent();
            dialog.dismiss();
        }
    }

    private class selectDismissMethod implements DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case 0:
                    alarm.dismissMethod = AlarmObject.DISMISS_METHOD_DEFAULT;
                    dismissIndicator.setText(dismissMethods[alarm.dismissMethod]);
                    dialog.dismiss();
                    break;

                case 1:
                    launchDismissDialog(true);
                    dialog.dismiss();
                    break;

                /*case 2:
                    //Start CameraActivity/Select Picture from here
                    PackageManager pm = getPackageManager();
                    if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                        startActivityForResult(intent, 7);
                        dialog.dismiss();
                    } else {
                        ToastGaffer.showToast(getApplicationContext(), getString(R.string.cam_unavailable));
                    }
                    break;*/

                case 2:
                    if (CommonUtils.isMOrLater()) {
                        if (ContextCompat.checkSelfPermission(AlarmDetails.this,
                                Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {

                            //Called on activity
                            ActivityCompat.requestPermissions(AlarmDetails.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    Clock.REQUEST_PERMISSION_CAMERA);
                        } else {
                            startBarcodeScanner();
                        }
                    } else {
                        startBarcodeScanner();
                    }
                    dialog.dismiss();
                    break;

                case 3:
                    launchShakeDialog(true);
                    dialog.dismiss();
                    break;
            }
        }
    }

    private class selectSnoozeMethod implements DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case 0:
                    alarm.snoozeMethod = AlarmObject.SNOOZE_METHOD_DEFAULT;
                    snoozeIndicator.setText(snoozeMethods[alarm.snoozeMethod]);
                    dialog.dismiss();
                    break;

                case 1:
                    launchDismissDialog(false);
                    dialog.dismiss();
                    break;

                case 2:
                    launchShakeDialog(false);
                    dialog.dismiss();
                    break;
            }
        }
    }

}
