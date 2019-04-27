package com.bytezap.wobble.clock;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.preference.CustomListPreference;
import com.bytezap.wobble.utils.CommonUtils;

public final class DayDreamSettings extends AppCompatActivity {

    public static final String CLOCK_STYLE = "prefClockStyle";

    public static final String NIGHT_MODE = "prefNightMode";

    public static final String DREAM_DIGITAL_FONT = "prefDreamFont";

    public static final String NIGHT_BACKGROUND = "prefNightBg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String locale = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(SettingsActivity.LANGUAGE, "default");
        CommonUtils.setLangCode(locale);
        CommonUtils.setLanguage(getApplicationContext().getResources(), locale);
        setContentView(R.layout.daydream_layout);

        if (CommonUtils.isLOrLater()) {
            getWindow().setStatusBarColor(Color.parseColor("#90000000"));
        }

        if (CommonUtils.isLOrLater()) {
            getWindow().setNavigationBarColor(Color.parseColor("#90000000"));
        }

        Toolbar toolbar = findViewById(R.id.daydream_toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class DayDreamFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.dream_settings);
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePrefs();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()){

                case CLOCK_STYLE:
                case DREAM_DIGITAL_FONT:
                case NIGHT_BACKGROUND:
                    final CustomListPreference listPreference = (CustomListPreference) pref;
                    final int index = listPreference.findIndexOfValue((String) newValue);
                    listPreference.setSummary(listPreference.getEntries()[index]);
                    break;
            }
            return true;
        }

        private void updatePrefs() {
            CustomListPreference listPreference = (CustomListPreference) findPreference(CLOCK_STYLE);
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(this);

            listPreference = (CustomListPreference) findPreference(DREAM_DIGITAL_FONT);
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(this);

            listPreference = (CustomListPreference) findPreference(NIGHT_BACKGROUND);
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(this);
        }
    }
}
