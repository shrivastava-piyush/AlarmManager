/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.preference;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AnimUtils;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    public static final String AUTO_DISMISS = "prefAutoDismiss";

    public static final String SNOOZE_DURATION = "prefSnoozeDuration";

    public static final String WAKE_UP_DURATION = "prefWakeUpInterval";

    public static final String VOLUME_KEYS = "prefVolumeKeys";

    public static final String PLAY_IN_SILENT_MODE = "prefSilentMode";

    public static final String KEEP_SCREEN_ON = "prefScreenOn";

    public static final String SOUND_EFFECT = "prefSoundEffects";

    public static final String COUNTDOWN_ALARM = "prefTimerAlarm";

    public static final String ABOUT = "prefAbout";

    public static final String ALARM_VOLUME = "prefAlarmVolume";

    public static final String INCREASING_VOLUME = "prefIncreasingVolume";

    public static final String TIMER_VOLUME = "prefTimerVolume";

    public static final String ANIMATION = "prefAnimation";

    public static final String VOICE = "prefVoice";

    public static final String ALARM_BACKGROUND = "prefAlarmBg";

    public static final String VOICE_ALERT_VOLUME = "prefAlertVolume";

    public static final String CLOCK_FONT = "prefClockFont";

    public static final String LANGUAGE = "prefLanguage";

    public static final String LONG_DISMISS = "prefLongDismiss";

    public static final String UPCOMING_NOTIFICATIONS = "prefUpcomingNotif";

    public static final String INCREASING_BRIGHTNESS = "prefIncreasingBrightness";

    public static final String BLUR_BG = "prefBlur";

    public static final String CLOCK_BG = "prefClockBg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        if (BitmapController.isAnimation()) {
            overridePendingTransition( R.anim.fade_in, R.anim.fade_out);
        }
        setContentView(R.layout.settings_activity);

        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        try {
            getWindow().setBackgroundDrawable(background);
        }catch (Exception e){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        CustomListView listView = findViewById(R.id.settings_list);
        ArrayList<PrefName> list = new ArrayList<>();
        list.add(new PrefName(getString(R.string.preference_category_alarm), ContextCompat.getDrawable(getApplicationContext(), R.drawable.tab_alarm)));
        list.add(new PrefName(getString(R.string.preference_category_timer), ContextCompat.getDrawable(getApplicationContext(), R.drawable.tab_timer)));
        list.add(new PrefName(getString(R.string.pref_voice), ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_volume)));
        list.add(new PrefName(getString(R.string.preference_category_other), ContextCompat.getDrawable(getApplicationContext(), R.drawable.tab_alarm)));
        SettingsListAdapter adapter = new SettingsListAdapter(this, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        try {
            if (ThemeDetails.shouldMaskTheme(BitmapController.getThemeNumber())) {
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent;
        switch (i){
            case 0:
                intent = new Intent(SettingsActivity.this, AlarmSettingsActivity.class);
                startActivity(intent);
                break;

            case 1:
                intent = new Intent(SettingsActivity.this, TimerSettingsActivity.class);
                startActivity(intent);
                break;

            case 2:
                intent = new Intent(SettingsActivity.this, VoiceSettingsActivity.class);
                startActivity(intent);
                break;

            case 3:
                intent = new Intent(SettingsActivity.this, OtherSettingsActivity.class);
                startActivity(intent);
                break;
        }
    }
}