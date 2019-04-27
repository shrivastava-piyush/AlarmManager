/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.preference;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.CropActivity;
import com.bytezap.wobble.R;
import com.bytezap.wobble.alarm.AlarmAssistant;
import com.bytezap.wobble.customviews.CustomPreferenceFragment;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AnimUtils;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;
import com.bytezap.wobble.utils.ToastGaffer;

public class AlarmSettingsActivity extends AppCompatActivity {

    private static final String AUTO_DISMISS = "prefAutoDismiss";

    private static final String SNOOZE_DURATION = "prefSnoozeDuration";

    private static final String WAKE_UP_DURATION = "prefWakeUpInterval";

    private static final String VOLUME_KEYS = "prefVolumeKeys";

    private static final String PLAY_IN_SILENT_MODE = "prefSilentMode";

    private static final String ALARM_VOLUME = "prefAlarmVolume";

    private static final String INCREASING_VOLUME = "prefIncreasingVolume";

    private static final String ALARM_BACKGROUND = "prefAlarmBg";

    private static final String UPCOMING_NOTIFICATIONS = "prefUpcomingNotif";

    private static final String LONG_DISMISS = "prefLongDismiss";

    private static final String INCREASING_BRIGHTNESS = "prefIncreasingBrightness";

    public static final int IMAGE_MAX_SIZE = 1024;
    public static final int IMAGE_MAX_SIZE_TABLET = 1200;
    public static final String DEFAULT_WALLPAPER_EXTENSION = ".jpg";
    public static final String DIR_WALLPAPER_ALARM = "/Wobble/Wallpapers/Alarm";
    public static final String DIR_WALLPAPER_THEME = "/Wobble/Wallpapers/Theme";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        if (BitmapController.isAnimation()) {
            overridePendingTransition( R.anim.fade_in, R.anim.fade_out);
        }
        setContentView(R.layout.alarm_settings_activity);

        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        try {
            getWindow().setBackgroundDrawable(background);
        }catch (Exception e){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        Toolbar toolbar = findViewById(R.id.alarm_settings_toolbar);
        setSupportActionBar(toolbar);

        try {
            CustomListView listView = findViewById(android.R.id.list);
            if (listView != null && ThemeDetails.shouldMaskTheme(BitmapController.getThemeNumber())) {
                listView.setBackgroundColor(Color.parseColor("#20000000"));
            }
            if (BitmapController.isAnimation()) {
                AnimUtils.startListAnimation(getApplicationContext(), listView);
            }
        } catch (Exception ignored){}

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

    public static class SettingsFragment extends CustomPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        CustomListPreference alarmBgPref;
        private String prevBg = "0";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.alarm_preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePrefs();
        }

        private void updatePrefs() {

            CustomListPreference listPref = (CustomListPreference) findPreference(INCREASING_VOLUME);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            listPref = (CustomListPreference) findPreference(INCREASING_BRIGHTNESS);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            listPref = (CustomListPreference) findPreference(AUTO_DISMISS);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            listPref = (CustomListPreference) findPreference(SNOOZE_DURATION);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            listPref = (CustomListPreference) findPreference(WAKE_UP_DURATION);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            listPref = (CustomListPreference) findPreference(VOLUME_KEYS);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            alarmBgPref = (CustomListPreference) findPreference(ALARM_BACKGROUND);
            alarmBgPref.setSummary(alarmBgPref.getEntry());
            alarmBgPref.setOnPreferenceChangeListener(this);
            alarmBgPref.setOnPreferenceClickListener(this);

            CustomCheckBoxPreference checkBoxPreference = (CustomCheckBoxPreference) findPreference(UPCOMING_NOTIFICATIONS);
            checkBoxPreference.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {

                case ALARM_BACKGROUND:
                    if (!alarmBgPref.getValue().equals("2")) {
                        prevBg = alarmBgPref.getValue();
                    }
                    break;
            }
            return true;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            switch (preference.getKey()) {

                case INCREASING_VOLUME:
                case AUTO_DISMISS:
                case SNOOZE_DURATION:
                case WAKE_UP_DURATION:
                case VOLUME_KEYS: {
                    CustomListPreference listPref = (CustomListPreference) preference;
                    final int id = listPref.findIndexOfValue((String) newValue);
                    listPref.setSummary(listPref.getEntries()[id]);
                    break;
                }

                case INCREASING_BRIGHTNESS: {
                    SharedPreferences settings = getPreferenceManager().getSharedPreferences();
                    if (settings.getBoolean("firstTime", true) && !newValue.equals("0")) {
                        settings.edit().putBoolean("firstTime", false).apply();
                        ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(), R.style.AlertDialogStyle);
                        AlertDialog dialog = new AlertDialog.Builder(wrapper).create();
                        dialog.setTitle(R.string.pref_brightness);
                        dialog.setMessage(getString(R.string.brightness_friendly_alert));
                        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok_gotit), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        DialogSupervisor.setDialog(dialog);
                        dialog.show();
                    }
                    CustomListPreference brightnessPref = (CustomListPreference) preference;
                    final int prefId = brightnessPref.findIndexOfValue((String) newValue);
                    brightnessPref.setSummary(brightnessPref.getEntries()[prefId]);
                    break;
                }

                case ALARM_BACKGROUND: {
                    CustomListPreference bgPref = (CustomListPreference) preference;
                    final int bgId = bgPref.findIndexOfValue((String) newValue);
                    if (bgId == 2) {
                        if (CommonUtils.isMOrLater()) {
                            // Reinforce the permission check here
                            if (ContextCompat.checkSelfPermission(getActivity(),
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {

                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        Clock.REQUEST_PERMISSION_STORAGE_DOC);
                            } else {
                                accessStorage();
                            }
                        } else {
                            accessStorage();
                        }
                    } else {
                        bgPref.setSummary(bgPref.getEntries()[bgId]);
                    }
                    break;
                }

                case UPCOMING_NOTIFICATIONS: {
                    updateNotifications(getActivity().getApplicationContext(), (Boolean) newValue);
                    break;
                }
            }
            return true;
        }

        private void accessStorage() {
            Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
            imageIntent.setType("image/*");
            imageIntent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(imageIntent, 0);
            } catch (Exception e) {
                ToastGaffer.showToast(getActivity(), getString(R.string.no_file_manager));
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == Clock.REQUEST_PERMISSION_STORAGE_DOC) {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessStorage();
                } else {
                    alarmBgPref = (CustomListPreference) findPreference(ALARM_BACKGROUND);
                    alarmBgPref.setValue(prevBg);
                    alarmBgPref.setSummary(alarmBgPref.getEntry());
                    ToastGaffer.showToast(getActivity().getApplicationContext(), getString(R.string.permission_denied));
                }
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == RESULT_OK && requestCode == 0 && data != null) {
                Uri uri = data.getData();
                String s = getActivity().getContentResolver().getType(uri);
                if (s != null) {
                    if (s.contains("image")) {
                        Intent cropIntent = new Intent(getActivity(), CropActivity.class);
                        cropIntent.setData(data.getData());
                        startActivityForResult(cropIntent, 1);
                    } else {
                        ToastGaffer.showToast(getActivity().getApplicationContext(), getString(R.string.wrong_file_type));
                    }
                }
            } else if (resultCode == RESULT_CANCELED && (requestCode == 0 || requestCode == 1)) {
                alarmBgPref = (CustomListPreference) findPreference(ALARM_BACKGROUND);
                alarmBgPref.setValue(prevBg);
                alarmBgPref.setSummary(alarmBgPref.getEntry());
            }
        }

        private void updateNotifications(final Context context, final boolean isEnabled){
            final AsyncTask<Void, Void, Void>  notifTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    AlarmAssistant.rescheduleNotifications(context, isEnabled);
                    return null;
                }
            };
            notifTask.execute();
        }

        @Override
        public void onDestroy() {
            DialogSupervisor.cancelDialog();
            super.onDestroy();
        }
    }
}