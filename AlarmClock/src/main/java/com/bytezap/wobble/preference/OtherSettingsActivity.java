/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.preference;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.bytezap.wobble.AboutActivity;
import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.CustomPreferenceFragment;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AnimUtils;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.Blur;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;

public class OtherSettingsActivity extends AppCompatActivity {

    private static final String ABOUT = "prefAbout";

    private static final String ANIMATION = "prefAnimation";

    private static final String CLOCK_FONT = "prefClockFont";

    private static final String LANGUAGE = "prefLanguage";

    private static final String BLUR_BG = "prefBlur";

    private static final String CLOCK_BG = "prefClockBg";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        if (BitmapController.isAnimation()) {
            overridePendingTransition( R.anim.fade_in, R.anim.fade_out);
        }
        setContentView(R.layout.other_settings_activity);

        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        try {
            getWindow().setBackgroundDrawable(background);
        }catch (Exception e){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        Toolbar toolbar = findViewById(R.id.other_settings_toolbar);
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
            addPreferencesFromResource(R.xml.other_preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePrefs();
        }

        private void updatePrefs() {

            CustomListPreference listPref = (CustomListPreference) findPreference(CLOCK_FONT);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            listPref = (CustomListPreference) findPreference(LANGUAGE);
            listPref.setSummary(listPref.getEntry());
            listPref.setOnPreferenceChangeListener(this);

            CustomCheckBoxPreference checkBoxPreference = (CustomCheckBoxPreference) findPreference(ANIMATION);
            checkBoxPreference.setOnPreferenceChangeListener(this);

            BlurSeekBarPreference blurPref = (BlurSeekBarPreference) findPreference(BLUR_BG);
            if (CommonUtils.is17OrLater()) {
                blurPref.setOnPreferenceChangeListener(this);
            } else {
                PreferenceCategory mCategory = (PreferenceCategory) findPreference("other");
                mCategory.removePreference(blurPref);
            }

            CustomCheckBoxPreference clockBgPreference = (CustomCheckBoxPreference) findPreference(CLOCK_BG);
            clockBgPreference.setOnPreferenceChangeListener(this);

            Preference preference = findPreference(ABOUT);
            preference.setOnPreferenceClickListener(this);

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {

                case ABOUT:
                    if (BitmapController.isAnimation()) {
                        try{
                            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity());
                            ActivityCompat.startActivity(getActivity(), new Intent(getActivity().getApplicationContext(), AboutActivity.class), options.toBundle());
                        } catch (Exception e){
                            Log.e(SettingsFragment.class.getSimpleName(), "Could not start transition: " + e.getMessage());
                        }
                    } else {
                        startActivity(new Intent(getActivity(), AboutActivity.class));
                    }
                    break;
            }
            return true;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            switch (preference.getKey()) {

                case CLOCK_FONT: {
                    CustomListPreference fontPref = (CustomListPreference) preference;
                    final int fontId = fontPref.findIndexOfValue((String) newValue);
                    fontPref.setSummary(fontPref.getEntries()[fontId]);

                    Intent resultIntent = new Intent();
                    resultIntent.setAction(Clock.RECREATE);
                    getActivity().sendBroadcast(resultIntent);
                    break;
                }

                case ANIMATION: {
                    BitmapController.setIsAnimation((Boolean) newValue);
                    Intent animIntent = new Intent();
                    animIntent.setAction(Clock.RECREATE);
                    getActivity().sendBroadcast(animIntent);
                    break;
                }

                case LANGUAGE: {
                    if (CommonUtils.getLangCode().equals(newValue)) {
                        break;
                    }
                    CommonUtils.setLangCode((String) newValue);
                    CustomListPreference langPref = (CustomListPreference) preference;
                    final int langId = langPref.findIndexOfValue((String) newValue);
                    langPref.setSummary(langPref.getEntries()[langId]);
                    Intent langIntent = new Intent();
                    langIntent.setAction(Clock.RECREATE);
                    getActivity().sendBroadcast(langIntent);
                    langIntent = new Intent(getActivity(), OtherSettingsActivity.class);
                    getActivity().startActivity(langIntent);
                    getActivity().finish();
                    break;
                }

                case BLUR_BG: {
                    BitmapController.nullifyBackground();
                    Intent langIntent = new Intent();
                    langIntent.setAction(Clock.RECREATE);
                    getActivity().sendBroadcast(langIntent);
                    langIntent = new Intent(getActivity(), OtherSettingsActivity.class);
                    getActivity().startActivity(langIntent);
                    getActivity().finish();
                    break;
                }

                case CLOCK_BG: {
                    BitmapController.setIsAnimation((Boolean) newValue);
                    Intent animIntent = new Intent();
                    animIntent.setAction(Clock.RECREATE);
                    getActivity().sendBroadcast(animIntent);
                    break;
                }
            }
            return true;
        }

        private void blurBackground(final int newLevel){

            final AsyncTask<Void, Void, Void>  blurTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Resources res = getResources();
                    BitmapDrawable currentBackground = BitmapController.getCurrentBackground(res);
                    Bitmap bitmap;
                    if (currentBackground != null) {
                        bitmap = Blur.apply(getActivity().getApplicationContext(), currentBackground.getBitmap(), newLevel);
                        BitmapController.setBackground(new BitmapDrawable(res, bitmap), res, false);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                }
            };
            blurTask.execute();
        }

        @Override
        public void onDestroy() {
            DialogSupervisor.cancelDialog();
            super.onDestroy();
        }
    }
}