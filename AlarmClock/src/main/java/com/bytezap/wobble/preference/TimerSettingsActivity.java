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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.CustomPreferenceFragment;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AnimUtils;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;
import com.bytezap.wobble.utils.ToastGaffer;

public class TimerSettingsActivity extends AppCompatActivity {

    private static final String KEEP_SCREEN_ON = "prefScreenOn";

    private static final String SOUND_EFFECT = "prefSoundEffects";

    private static final String COUNTDOWN_ALARM = "prefTimerAlarm";

    private static final String TIMER_VOLUME = "prefTimerVolume";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        if (BitmapController.isAnimation()) {
            overridePendingTransition( R.anim.fade_in, R.anim.fade_out);
        }
        setContentView(R.layout.timer_settings_activity);

        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        try {
            getWindow().setBackgroundDrawable(background);
        }catch (Exception e){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        Toolbar toolbar = findViewById(R.id.timer_settings_toolbar);
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

    public static class SettingsFragment extends CustomPreferenceFragment {

        CustomListPreference alarmBgPref;
        private String prevBg = "0";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.timer_preferences);
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
        public void onDestroy() {
            DialogSupervisor.cancelDialog();
            super.onDestroy();
        }
    }
}