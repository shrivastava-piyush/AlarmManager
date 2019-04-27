package com.bytezap.wobble.stopwatch;

import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.customviews.Chronometer;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.SoundController;
import com.bytezap.wobble.utils.TtsSpeaker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;

public class StopwatchFragment extends Fragment implements View.OnClickListener {

    // Timer interval to refresh
    public static final int STOPWATCH_INTERVAL = 25;
    public static final String TIME_MILLI_SEC = "currentTime";
    public static final String IS_RUNNING = "isWatchRunning";
    //preferences
    private static final String LAST_LAP_TIME = "lastLapTime";
    private static final String LAP_LIST = "lapList";
    private static final String LAP_COUNT = "lapCount";
    private static final String PREVIOUS_SECOND = "previousSecond";
    private static final String LAST_TIME = "lastTime";
    private final ArrayList<Lap> laps = new ArrayList<>();

    private Button start, stop, reset, lap;
    private CustomListView lapListView;
    private LapAdapter lapsAdapter;
    private Chronometer stopwatch;

    private String eachLapTime = "";
    private TextView sTime, sMillis;
    private SoundController controller;
    private SharedPreferences defaultSettings;

    private long lastElapsedTime = 0;
    private long timeInMillis = 0;
    private long lastTime = 0;
    private boolean isRunning = false;
    private int previousSecond = 0;

    private final Handler stopwatchHandler = new Handler();
    private final Runnable chronoRunnable = new Runnable() {
        @Override
        public void run() {

            long currentTime = System.currentTimeMillis();
            if (isRunning) {
                timeInMillis += (currentTime - lastTime);
            } else {
                lastTime = timeInMillis;
            }

            if (timeInMillis < 0) timeInMillis = 0;

            try {
                stopwatch.updateChronometer((int) timeInMillis);
                updateStopwatch(timeInMillis, false);
                updateActiveLap(timeInMillis - lastElapsedTime);

                int currentSecond = (int) timeInMillis / 1000;
                if (currentSecond > previousSecond && isSoundEnabled() && isRunning) {
                    controller.tick();
                }
                previousSecond = currentSecond;
                lastTime = currentTime;
            } catch (Throwable b) {
                Log.e(StopwatchFragment.class.getSimpleName(), b.getMessage());
            }

            stopwatchHandler.postDelayed(chronoRunnable, STOPWATCH_INTERVAL);
        }
    };

    public StopwatchFragment() {
        //Empty Constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.stopwatch, container, false);

        start = rootView.findViewById(R.id.chrono_start);
        stop = rootView.findViewById(R.id.chrono_stop);
        reset = rootView.findViewById(R.id.chrono_reset);
        lap = rootView.findViewById(R.id.chrono_lap);
        stopwatch = rootView.findViewById(R.id.stopwatch);
        sTime = rootView.findViewById(R.id.chrono_time);
        sMillis = rootView.findViewById(R.id.chrono_millisec);
        lapListView = rootView.findViewById(R.id.lapList);

        View emptyView = rootView.findViewById(R.id.lap_emptyView);
        lapListView.setEmptyView(emptyView);

        lapsAdapter = new LapAdapter(getActivity(), laps);
        lapListView.setAdapter(lapsAdapter);

        controller = SoundController.getInstance(getActivity().getApplicationContext());
        defaultSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        updateChronoTheme();
        updateStopwatch(0, false);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        reset.setOnClickListener(this);
        lap.setOnClickListener(this);
        stopwatch.setOnClickListener(this);

        stopwatch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isRunning) {
                    Intent chronoIntent = new Intent(getActivity(), StopwatchFullScreen.class);
                    chronoIntent.putExtra(TIME_MILLI_SEC, timeInMillis);
                    startActivity(chronoIntent);
                    return true;
                }
                return false;
            }
        });

        return rootView;
    }

    private void onChronoStart() {
        toggle();
        if (isSoundEnabled()) {
            controller.playSound(SoundController.SOUND_START);
        }
    }

    private void onChronoStop() {
        toggle();
        if (isSoundEnabled()) {
            controller.playSound(SoundController.SOUND_STOP);
        }
    }

    private void onChronoReset() {
        stopwatchHandler.removeCallbacks(chronoRunnable);

        stopwatch.animateChrono(0, 0, 0, false);
        lastTime = 0;
        isRunning = false;
        timeInMillis = 0;
        updateStopwatch(0, false);
        stop.setVisibility(View.INVISIBLE);
        start.setVisibility(View.VISIBLE);
        lastElapsedTime = 0;
        laps.clear();
        lapsAdapter.notifyDataSetChanged();

        if (isSoundEnabled()) {
            controller.playSound(SoundController.SOUND_RESET);
        }
    }

    private void toggle() {
        if (isRunning) {
            stopChronometer();
        } else {
            startChronometer();
        }
    }

    private void startChronometer() {
        isRunning = true;
        lastTime = System.currentTimeMillis();
        setButtonsState();
        stopwatchHandler.removeCallbacks(chronoRunnable);
        stopwatchHandler.post(chronoRunnable);
    }

    private void stopChronometer() {
        isRunning = false;
        stopwatchHandler.removeCallbacks(chronoRunnable);
        setButtonsState();
    }

    private void addLap() {
        if (isRunning) {
            int listSize = lapsAdapter.getCount();
            long lapTime = timeInMillis - lastElapsedTime;
            updateStopwatch(lapTime, true);
            if (listSize == 0) {
                Lap firstLap = new Lap(listSize + 1, eachLapTime);
                laps.add(0, firstLap);
                //Active lap
                laps.add(0, new Lap(listSize + 2, eachLapTime));
            } else {
                //New active lap
                laps.add(0, new Lap(listSize + 1, eachLapTime));
            }
            lapsAdapter.notifyDataSetChanged();
            lastElapsedTime = timeInMillis;
            if (isSoundEnabled()) {
                controller.playSound(SoundController.SOUND_LAP_TIME);
            }
        } else if (BitmapController.isAnimation()) {
            start.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake_and_rotate));
        }
    }

    private void updateActiveLap(long time) {
        if (lapsAdapter.getCount() > 0) {
            Lap curLap = lapsAdapter.getItem(0);
            updateStopwatch(time, true);
            curLap.setLapTime(eachLapTime);
            curLap.updateView(lapsAdapter, lapListView);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.chrono_start:
                onChronoStart();
                break;

            case R.id.chrono_stop:
                onChronoStop();
                break;

            case R.id.chrono_reset:
                if (isRunning || timeInMillis != 0) {
                    onChronoReset();
                } else if (BitmapController.isAnimation()) {
                    start.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake_and_rotate));
                }
                break;

            case R.id.chrono_lap:
                addLap();
                break;

            case R.id.stopwatch:
                if (isRunning) {
                    onChronoStop();
                } else {
                    onChronoStart();
                }
                break;
        }
    }

    @Override
    public void onPause() {

        SharedPreferences preferences = getActivity().getSharedPreferences(Clock.STOPWATCH_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(IS_RUNNING, isRunning);
        editor.putLong(TIME_MILLI_SEC, timeInMillis);
        editor.putLong(LAST_TIME, lastTime);
        editor.putInt(PREVIOUS_SECOND, previousSecond);
        editor.putLong(LAST_LAP_TIME, lastElapsedTime);
        editor.putInt(LAP_COUNT, lapsAdapter.getCount());
        //saving laplist
        if (!laps.isEmpty()) {
            Gson gson = new Gson();
            String json = gson.toJson(laps);
            editor.putString(LAP_LIST, json);
        }
        editor.apply();
        try {
            if (isRunning && timeInMillis > 0) {
                Intent serviceIntent = new Intent(getActivity(), StopwatchService.class);
                serviceIntent.putExtra(TIME_MILLI_SEC, timeInMillis);
                if (CommonUtils.isOOrLater()) {
                    getActivity().startForegroundService(serviceIntent);
                } else {
                    getActivity().startService(serviceIntent);
                }
            }
        } catch (Exception ex) {
            Log.v("Stopwatch", "Service not started: " + ex.toString());
        }

        stopwatchHandler.removeCallbacks(chronoRunnable);
        super.onPause();
    }

    @Override
    public void onResume() {
        NotificationProvider.cancelNotification(getActivity().getApplicationContext());
        Intent serviceIntent = new Intent(getActivity().getApplicationContext(), StopwatchService.class);
        getActivity().stopService(serviceIntent);

        SharedPreferences preferences = getActivity().getSharedPreferences(Clock.STOPWATCH_PREFS, Context.MODE_PRIVATE);

        isRunning = preferences.getBoolean(IS_RUNNING, false);
        timeInMillis = preferences.getLong(TIME_MILLI_SEC, 0);
        lastTime = preferences.getLong(LAST_TIME, 0);
        previousSecond = preferences.getInt(PREVIOUS_SECOND, 0);
        lastElapsedTime = preferences.getLong(LAST_LAP_TIME, 0);
        int lapCount = preferences.getInt(LAP_COUNT, 0);

        if (isRunning && timeInMillis > 0) {
            startChronometer();
            setButtonsState();
        } else if (!isRunning && timeInMillis > 0) {
            stopwatch.updateChronometer((int) timeInMillis);
            updateStopwatch(timeInMillis, false);
            setButtonsState();
        }

        //retrieve lap list
        if (lapCount > 0 && laps.isEmpty()) {
            try {
                GsonBuilder builder = new GsonBuilder();
                builder.registerTypeAdapter(Lap.class, new LapsInstanceCreator());
                Gson gson = builder.create();
                String json = preferences.getString(LAP_LIST, null);
                Type type = new TypeToken<ArrayList<Lap>>() {
                }.getType();
                if (json != null) {
                    ArrayList<Lap> laps = gson.fromJson(json, type);
                    for (Lap l : laps) {
                        this.laps.add(l);
                    }
                }

                lapsAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onResume();
    }

    private boolean isSoundEnabled() {
        return defaultSettings.getBoolean(SettingsActivity.SOUND_EFFECT, true);
    }

    private void setButtonsState() {
        start.setVisibility(isRunning ? View.INVISIBLE : View.VISIBLE);
        stop.setVisibility(isRunning ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateStopwatch(long time, boolean isLap) {
        if (time < 0) {
            time = 0;
        }

        long seconds = (time / 1000);
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        if (seconds > 10 && (seconds%300 == 0)) { //Let's keep the speak interval 5 minutes for now
            speakTime(hours, minutes, seconds%60);
        }
        seconds = seconds % 60;
        long milliseconds = (time % 1000);

        String hourFormat = hours >= 100 ? "%03d" : "%02d";

        if (isLap) {
            eachLapTime = String.format(Locale.getDefault(), "%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
        } else {
            sTime.setText(String.format(Locale.getDefault(), hourFormat+":%02d:%02d", hours, minutes, seconds));
            sMillis.setText(String.format(Locale.getDefault(), ".%03d", milliseconds));
        }
    }

    private void speakTime(long hours, long minutes, long seconds){
        if(defaultSettings.getBoolean(SettingsActivity.VOICE, true) && !TtsSpeaker.isSpeaking()){
            String timeLeft;
            String hourFormatted = getString(R.string.hours, String.valueOf(hours));
            String minFormatted = getString(R.string.minutes, String.valueOf(minutes));
            String secondsFormat = getString(R.string.seconds, seconds);
            String elapsed = getString(R.string.elapsed_time);
            if (hours > 0) {
                timeLeft = elapsed + " " + hourFormatted + " " + minFormatted;
            } else if (minutes > 0) {
                timeLeft = elapsed + " " + minFormatted + " " + secondsFormat;
            } else {
                timeLeft = elapsed + " " + secondsFormat;
            }
            TtsSpeaker.speak(getActivity().getApplicationContext(), timeLeft, TtsSpeaker.isTtsOff(), false);
        }
    }

    private void updateChronoTheme() {

        if (CommonUtils.isLOrLater() && BitmapController.isAnimation()) {
            stopwatch.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.chrono_animator));
            start.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
            stop.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
            reset.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
            lap.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
        }

        switch (BitmapController.getThemeNumber()){
            case ThemeDetails.THEME_THISTLE_PURPLE:
            case ThemeDetails.THEME_CUSTOM:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                stop.setBackgroundResource(R.drawable.stopwatch_white_bg);
                start.setBackgroundResource(R.drawable.stopwatch_white_bg);
                lap.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.BLACK);
                stop.setTextColor(Color.BLACK);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_DARK_COSMOS:
            case ThemeDetails.THEME_OF_TIME:
                reset.setBackgroundResource(R.drawable.stopwatch_dark_bg);
                start.setBackgroundResource(R.drawable.stopwatch_black_border);
                stop.setBackgroundResource(R.drawable.stopwatch_black_border);
                lap.setBackgroundResource(R.drawable.stopwatch_white_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.BLACK);
                break;

            case ThemeDetails.THEME_SUNRISE:
                sMillis.setTextColor(Color.parseColor("#E8EAF6"));
                reset.setBackgroundResource(R.drawable.stopwatch_maroon_bg);
                start.setBackgroundResource(R.drawable.set_timer_purple);
                stop.setBackgroundResource(R.drawable.set_timer_purple);
                lap.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_FOGGY_FOREST:
                reset.setBackgroundResource(R.drawable.stopwatch_maroon_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                lap.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_MOUNTAINS:
                reset.setBackgroundResource(R.drawable.stopwatch_maroon_bg);
                start.setBackgroundResource(R.drawable.set_timer_purple);
                stop.setBackgroundResource(R.drawable.set_timer_purple);
                lap.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                lap.setShadowLayer(1.5f, -1.5f, 1.5f, Color.GRAY);
                break;

            case ThemeDetails.THEME_RAINY_DAY:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                lap.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_BEACH:
                sMillis.setTextColor(Color.parseColor("#283044"));
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_white_bg);
                stop.setBackgroundResource(R.drawable.stopwatch_white_bg);
                lap.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.BLACK);
                stop.setTextColor(Color.BLACK);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_BLUE_NIGHT:
                reset.setBackgroundResource(R.drawable.stopwatch_blue_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                lap.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_SHIMMERING_NIGHT:
                reset.setBackgroundResource(R.drawable.stopwatch_blue_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                lap.setBackgroundResource(R.drawable.set_timer_blue);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_AURORA:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                lap.setBackgroundResource(R.drawable.stopwatch_cyan_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_CYAN:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                lap.setBackgroundResource(R.drawable.stopwatch_white_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                lap.setTextColor(Color.BLACK);
                break;
        }
    }

}