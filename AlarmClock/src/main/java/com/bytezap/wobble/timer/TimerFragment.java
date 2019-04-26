package com.bytezap.wobble.timer;

import android.animation.AnimatorInflater;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.preference.VoiceSettingsActivity;
import com.bytezap.wobble.alarm.AlarmAssistant;
import com.bytezap.wobble.customviews.Chronometer;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.database.PresetManager;
import com.bytezap.wobble.database.PresetObject;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.SoundController;
import com.bytezap.wobble.utils.ToastGaffer;
import com.bytezap.wobble.utils.TtsSpeaker;

import java.util.Locale;

public class TimerFragment extends Fragment implements View.OnClickListener, PresetInterface {

    //preferences
    public static final String REMAINING_TIME = "remaining_time";
    public static final int TIMER_INTERVAL = 25;
    public static final String TIMER_TEXT = "timer_text";
    public static final String IS_RUNNING = "timer_running";
    public static final String IS_SET_TIME = "timer_set_time";
    public static final String TOTAL_TIME = "timer_total_time";
    private static final String FREQUENT_RUNNING_TIME = "frequent_time";
    private static final int TIMER_DEFAULT_TIME = 900000;

    private int mHoursValue = 0;
    private int mMinsValue = 0;
    private int mSecsValue = 0;
    private int previousSecond = 0;
    //countdown picker dialog variables
    private long remainingTime = 0, totalTime;

    private Button start, stop, reset, preset;
    private CustomListView presetListView;
    private PresetAdapter presetAdapter;

    private PresetManager pManager;
    private PresetObject currPreset;

    private TextView tTime, tMillis;
    private SoundController controller;
    private Animation shake;
    private SharedPreferences timerPref;

    private CountDownTimer countDownTimer;
    private SharedPreferences defaultSettings;
    private Chronometer timer;
    private boolean isClickedTimer = false;
    // Variable to hold count down timer's running status
    private boolean isRunning = false;
    // Variable to check if time has been set
    private boolean isSetTime = false;

    public TimerFragment() {
        //Empty Constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.new_timer, container, false);

        tTime = rootView.findViewById(R.id.timer_time);
        tMillis = rootView.findViewById(R.id.timer_millisec);
        start = rootView.findViewById(R.id.timer_start);
        stop = rootView.findViewById(R.id.timer_stop);
        reset = rootView.findViewById(R.id.timer_reset);
        preset = rootView.findViewById(R.id.timer_preset);
        timer = rootView.findViewById(R.id.custom_timer);
        presetListView = rootView.findViewById(R.id.presetList);

        View emptyView = rootView.findViewById(R.id.preset_emptyView);
        presetListView.setEmptyView(emptyView);

        pManager = PresetManager.getInstance(getActivity().getApplicationContext());
        presetAdapter = new PresetAdapter(getActivity().getApplicationContext(), pManager.getPresets(), this);
        presetListView.setAdapter(presetAdapter);

        defaultSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        timerPref = getActivity().getSharedPreferences(Clock.TIMER_PREF, Context.MODE_PRIVATE);
        controller = SoundController.getInstance(getActivity().getApplicationContext());
        shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake_and_rotate);

        updateTimerTheme();

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        reset.setOnClickListener(this);
        timer.setOnClickListener(this);
        preset.setOnClickListener(this);

        timer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isRunning) {
                    Intent timerIntent = new Intent(getActivity(), TimerFullScreen.class);
                    timerIntent.putExtra(REMAINING_TIME, remainingTime);
                    startActivity(timerIntent);
                    return true;
                } else {
                    long time = timerPref.getLong(FREQUENT_RUNNING_TIME, TIMER_DEFAULT_TIME);
                    remainingTime = time>1000 ? time : TIMER_DEFAULT_TIME;
                    totalTime = remainingTime;
                    int secs = (int) (remainingTime / 1000);
                    int minutes = secs / 60;
                    int hours = (minutes / 60);
                    minutes = minutes % 60;
                    isSetTime = true;
                    String toastText;
                    if (hours > 0) {
                        toastText = minutes == 0 ? (hours == 1 ? getString(R.string.start_timer_hour) : getString(R.string.start_timer_hours, hours)) : getString(R.string.start_timer, hours, minutes);
                    } else {
                        toastText = minutes == 1 ? getString(R.string.start_timer_minute) : getString(R.string.start_timer_minutes, minutes);
                    }
                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(40);
                    startTimer();
                    ToastGaffer.showToast(getActivity().getApplicationContext(), toastText);
                }
                return true;
            }
        });

        // Necessary for effective localization
        updateTimerText(0, 0, 0, 0);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.timer_start:
                if (isSetTime && remainingTime != 0 && !isRunning) { //check whether the dialog has been called and time has been set
                    if (isSound()) {
                        controller.playSound(SoundController.SOUND_START);
                    }
                    startTimer();
                } else {
                    isClickedTimer = true;
                    requestTimeDialog();
                }
                break;

            case R.id.timer_stop:
                if (isSound()) {
                    controller.playSound(SoundController.SOUND_STOP);
                }
                if (TtsSpeaker.isSpeaking()) {
                    TtsSpeaker.stopTts(getActivity().getApplicationContext());
                }
                stopTimer();
                break;

            case R.id.timer_reset:
                resetTimer(true);
                if (TtsSpeaker.isSpeaking()) {
                    TtsSpeaker.stopTts(getActivity().getApplicationContext());
                }
                break;

            case R.id.timer_preset:
                PresetObject object = new PresetObject(getString(R.string.default_timer_text) + " " + (presetAdapter.getCount() + 1),  0, 5, 0);
                object.id = pManager.createPreset(object);
                presetAdapter.add(object);
                presetListView.smoothScrollToPosition(presetAdapter.getCount());
                break;

            case R.id.custom_timer:
                if (start.getVisibility() == View.VISIBLE) {
                    if (isSetTime && remainingTime != 0 && !isRunning) { //check whether the dialog has been called and time has been set
                        startTimer();
                        if (isSound()) {
                            controller.playSound(SoundController.SOUND_START);
                        }
                    } else {
                        isClickedTimer = true;
                        requestTimeDialog();
                    }
                } else {
                    stopTimer();
                    if (isSound()) {
                        controller.playSound(SoundController.SOUND_STOP);
                    }
                    if (TtsSpeaker.isSpeaking()) {
                        TtsSpeaker.stopTts(getActivity().getApplicationContext());
                    }
                }
                break;
        }
    }

    @Override
    public void startPreset(int position) {
        PresetObject preset = presetAdapter.getItem(position);
        if (preset != null) {
            currPreset = preset;
            remainingTime = preset.hours * 3600000 + preset.minutes * 60000 + preset.seconds * 1000;
            if (remainingTime >= 1000) {
                totalTime = remainingTime;
                isSetTime = true;
                if (isSound()) {
                    controller.playSound(SoundController.SOUND_START);
                }
                startTimer();
            }
        }
    }

    @Override
    public void editPreset(int position) {
        PresetObject preset = presetAdapter.getItem(position);
        if (preset != null) {
            requestLabelDialog(preset);
        }
    }

    @Override
    public void deletePreset(int position) {
        PresetObject preset = presetAdapter.getItem(position);
        if (preset != null) {
            pManager.deletePresetById(preset.id);
            presetAdapter.remove(preset);
        }
    }

    @Override
    public void setPresetTime(int position) {
        PresetObject preset = presetAdapter.getItem(position);
        if (preset != null) {
            requestPresetTimeDialog(preset);
        }
    }

    @Override
    public void onPause() {
        SharedPreferences.Editor editor = timerPref.edit();
        editor.putBoolean(IS_RUNNING, isRunning);
        editor.putBoolean(IS_SET_TIME, isSetTime);
        editor.putLong(REMAINING_TIME, remainingTime);
        editor.putLong(TOTAL_TIME, totalTime);
        if (currPreset !=null) {
            editor.putString(TIMER_TEXT, currPreset.name);
        }
        editor.apply();

        if (isRunning) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                // Launch service for notification
                Intent serviceIntent = new Intent(getActivity(), TimerService.class);
                serviceIntent.putExtra(REMAINING_TIME, remainingTime);
                serviceIntent.putExtra(TOTAL_TIME, totalTime);
                if (CommonUtils.isOOrLater()) {
                    getActivity().startForegroundService(serviceIntent);
                } else {
                    getActivity().startService(serviceIntent);
                }
            }
        } else {
            stopTimer();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        Intent serviceIntent = new Intent(getActivity(), TimerService.class);
        getActivity().stopService(serviceIntent);

        NotificationProvider.cancelNotification(getActivity().getApplicationContext());//Precautionary stuff, should not be needed
        AlarmAssistant.cancelTimerAlarm(getActivity().getApplicationContext());

        isRunning = timerPref.getBoolean(IS_RUNNING, false);
        isSetTime = timerPref.getBoolean(IS_SET_TIME, false);
        remainingTime = timerPref.getLong(REMAINING_TIME, 0);
        totalTime = timerPref.getLong(TOTAL_TIME, 0);

        // Check the timer state and update it accordingly
        if (isRunning && remainingTime != 0) { // Running
            stop.setVisibility(View.VISIBLE);
            start.setVisibility(View.INVISIBLE);
            startTimer();
        } else if ((isSetTime || !isRunning) && remainingTime != 0) { // Paused state
            stopTimer();
            updateTimer(remainingTime);
        } else {
            resetTimer(false); // Nothing happened, but still call this in case timer alert went off
        }
        super.onResume();
    }

    private void startTimer() {

        if (isRunning && countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(remainingTime, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                isRunning = true;
                //Put countdown timer remaining time in a variable
                remainingTime = millisUntilFinished;
                updateTimer(millisUntilFinished);
                int currentSecond = (int) remainingTime / 1000;
                if (currentSecond < previousSecond && isSound()) {
                    controller.tick();
                }
                previousSecond = currentSecond;
            }

            @Override
            public void onFinish() {
                onFinishTimer();
            }

        };

        countDownTimer.start();
        AlarmAssistant.setTimerAlarm(getActivity().getApplicationContext(), remainingTime);
        stop.setVisibility(View.VISIBLE);
        start.setVisibility(View.INVISIBLE);
    }

    private void stopTimer() {
        AlarmAssistant.cancelTimerAlarm(getActivity().getApplicationContext());
        stop.setVisibility(View.INVISIBLE);
        start.setVisibility(View.VISIBLE);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
    }

    private void resetTimer(boolean playEffect) {
        if (isSetTime) {
            AlarmAssistant.cancelTimerAlarm(getActivity().getApplicationContext());
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            if (controller.isPlaying()) {
                controller.terminate();
            }
            if (isSound() && playEffect) {
                controller.playSound(SoundController.SOUND_RESET);
            }

            remainingTime = 0;
            isSetTime = false;
            if (playEffect) {
                timer.animateChrono(0, 0, 0, false);
            } else {
                timer.updateChronometer(0);
            }
            updateTimerText(0, 0, 0, 0);
            stop.setVisibility(View.INVISIBLE);
            start.setVisibility(View.VISIBLE);

            /* Prevent animation for now, since it seems awkward for a button to rotate
            if (playEffect && BitmapController.isAnimation()) {
                reset.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.rotate));
            }*/
            isRunning = false;
        }
    }

    private void speakTime(int hours, int minutes, int seconds){
        if(defaultSettings.getBoolean(VoiceSettingsActivity.VOICE, true) && !TtsSpeaker.isSpeaking()){
            String timeLeft;
            String hourFormatted = getString(R.string.hours, String.valueOf(hours));
            String minFormatted = getString(R.string.minutes, String.valueOf(minutes));
            String secondsFormat = getString(R.string.seconds, seconds);
            String remaining = getString(R.string.remaining);
            if (hours > 0) {
                timeLeft = (hours == 1 ? getString(R.string.hour) : hourFormatted) + " " + minFormatted + " " + remaining;
            } else if (minutes > 0) {
                timeLeft = (minutes == 1 ? getString(R.string.minute) : minFormatted) + " " + secondsFormat + " " + remaining;
            } else {
                timeLeft = secondsFormat + " " + remaining;
            }
            TtsSpeaker.speak(getActivity().getApplicationContext(), timeLeft, TtsSpeaker.isTtsOff(), false);
        }
    }

    // Called when countdown alarm is on
    private void onFinishTimer() {
        resetTimer(false);
        if (!TimerAlert.isAlertActive) {
            startActivity(new Intent(getActivity().getApplicationContext(), TimerAlert.class));
        }
    }

    private boolean isSound() {
        return defaultSettings.getBoolean(SettingsActivity.SOUND_EFFECT, true);
    }

    private void updateTimer(long time) {
        if (time < 0) {
            time = 0;
        }

        int secs = (int) (time / 1000);
        int mins = secs / 60;
        int hrs = (mins / 60);
        timer.updateChronometer((int) time);
        updateTimerText(hrs, mins, secs, time);
    }

    /*private void setButtonsState() {
        start.setVisibility( (isRunning && !isPaused) || (isPaused && !isRunning) || remainingTime == 0 ? View.VISIBLE : View.INVISIBLE);
        start.setVisibility((isRunning && !isPaused) || (isPaused && !isRunning) || remainingTime == 0 ? View.INVISIBLE : View.VISIBLE);
        setTitle.setEnabled(!(isRunning && !isPaused) || remainingTime == 0);
        setTitle.setEnabled(!(isRunning && !isPaused) || remainingTime==0);
    }*/

    private void updateTimerText(int hours, int minutes, int seconds, long time) {

        minutes = minutes % 60;
        if (seconds > 10) {
            int totalSec = (int) (totalTime/1000);
            if ((seconds == totalSec/4) || (seconds == totalSec/2) || (seconds == 3*totalSec/4)) {
                speakTime(hours, minutes, seconds % 60);
            }
        }

        seconds = seconds % 60;
        time = time % 1000;
        String hourFormat = hours >= 100 ? "%03d" : "%02d";

		/* Setting the timer text to the elapsed time */
        tTime.setText(String.format(Locale.getDefault(), hourFormat+":%02d:%02d", hours, minutes, seconds));
        tMillis.setText(String.format(Locale.getDefault(), ".%03d", time));
    }

    private void requestTimeDialog() {

        if (isSetTime)
            remainingTime = 0;

        int mLastHour = 0;
        if (mHoursValue == 0) mHoursValue = mLastHour;
        int mLastMin = 0;
        if (mMinsValue == 0) mMinsValue = mLastMin;
        int mLastSec = 0;
        if (mSecsValue == 0) mSecsValue = mLastSec;

        ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(), R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View ll = inflater.inflate(R.layout.time_picker, null);

        final NumberPicker tHours = ll.findViewById(R.id.numberPickerHours);
        tHours.setMaxValue(199);
        tHours.setMinValue(0);

        final NumberPicker tMins = ll.findViewById(R.id.numberPickerMins);
        tMins.setMaxValue(59);
        tMins.setMinValue(0);

        final NumberPicker tSecs = ll.findViewById(R.id.numberPickerSecs);
        tSecs.setMaxValue(59);
        tSecs.setMinValue(0);

        AlertDialog mSelectTime = new AlertDialog.Builder(wrapper).create();
        mSelectTime.setView(ll);

        mSelectTime.setTitle(R.string.timer_set_time);
        mSelectTime.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mHoursValue = tHours.getValue();
                        mMinsValue = tMins.getValue();
                        mSecsValue = tSecs.getValue();
                        //set time on textViews here
                        if (mHoursValue != 0 || mMinsValue != 0 || mSecsValue != 0) {
                            if (BitmapController.isAnimation()) {
                                timer.animateChrono(mHoursValue, mMinsValue, mSecsValue, true);
                            }
                            remainingTime = mHoursValue * 3600000 + mMinsValue * 60000 + mSecsValue * 1000;
                            totalTime = remainingTime;
                            isSetTime = true;
                            isRunning = false;
                            timerPref.edit().putLong(FREQUENT_RUNNING_TIME, totalTime).apply();
                            updateTimerText(mHoursValue, mMinsValue, mSecsValue, 0);
                            timer.updateChronometer((int) remainingTime);
                            if (isClickedTimer) {
                                startTimer();
                            }
                        }
                        dialog.dismiss();
                    }
                });
        mSelectTime.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        DialogSupervisor.setDialog(mSelectTime);
        mSelectTime.show();
    }

    private void requestPresetTimeDialog(final PresetObject preset) {

        if (isSetTime)
            remainingTime = 0;

        int mLastHour = 0;
        if (mHoursValue == 0) mHoursValue = mLastHour;
        int mLastMin = 0;
        if (mMinsValue == 0) mMinsValue = mLastMin;
        int mLastSec = 0;
        if (mSecsValue == 0) mSecsValue = mLastSec;

        ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(), R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View ll = inflater.inflate(R.layout.time_picker, null);

        final NumberPicker tHours = ll.findViewById(R.id.numberPickerHours);
        tHours.setMaxValue(199);
        tHours.setMinValue(0);

        final NumberPicker tMins = ll.findViewById(R.id.numberPickerMins);
        tMins.setMaxValue(59);
        tMins.setMinValue(0);

        final NumberPicker tSecs = ll.findViewById(R.id.numberPickerSecs);
        tSecs.setMaxValue(59);
        tSecs.setMinValue(0);

        AlertDialog mSelectTime = new AlertDialog.Builder(wrapper).create();
        mSelectTime.setView(ll);

        mSelectTime.setTitle(R.string.timer_set_time);
        mSelectTime.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        preset.hours = tHours.getValue();
                        preset.minutes = tMins.getValue();
                        preset.seconds = tSecs.getValue();
                        pManager.updatePreset(preset);
                        presetAdapter.setPresets(pManager.getPresets());
                        dialog.dismiss();
                    }
                });
        mSelectTime.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        DialogSupervisor.setDialog(mSelectTime);
        mSelectTime.show();
    }

    private void requestLabelDialog(final PresetObject preset) {

        ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(), R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View tt = inflater.inflate(R.layout.timer_title, null);

        final EditText timer_title = tt.findViewById(R.id.timer_edittext);
        final String oldTitle = getString(R.string.default_timer_text) + " " + preset.id;
        timer_title.setText(TextUtils.isEmpty(preset.name) ? oldTitle : preset.name);
        timer_title.setSelection(timer_title.getText().length());

        final AlertDialog mSelectText = new AlertDialog.Builder(wrapper).create();
        mSelectText.setView(tt);
        mSelectText.setTitle(R.string.timer_set_label);
        mSelectText.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String title = timer_title.getText().toString();
                        if (!title.equals(oldTitle) && !TextUtils.isEmpty(title)) {
                            preset.name = timer_title.getText().toString();
                            pManager.updatePreset(preset);
                            presetAdapter.setPresets(pManager.getPresets());
                        }
                        mSelectText.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        dialog.dismiss();
                    }
                });
        mSelectText.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectText.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        dialog.dismiss();
                    }
                });

        mSelectText.getWindow().setWindowAnimations(R.style.dialogBottomAnimStyle);
        DialogSupervisor.setDialog(mSelectText);
        timer_title.requestFocus();
        mSelectText.show();

        mSelectText.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mSelectText.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void updateTimerTheme() {
        int lightGrey = Color.parseColor("#B7B6C2");
        int timerColor = ContextCompat.getColor(getActivity(), R.color.timer_button);
        int darkGrey = ContextCompat.getColor(getActivity(), R.color.dark_grey);
        int darkPurple = Color.parseColor("#0A0107");

        if (CommonUtils.isLOrLater() && BitmapController.isAnimation()) {
            timer.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.chrono_animator));
            start.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
            stop.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
            reset.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
            preset.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getActivity(), R.drawable.button_animator));
        }
        switch (BitmapController.getThemeNumber()){
            case ThemeDetails.THEME_THISTLE_PURPLE:
            case ThemeDetails.THEME_CUSTOM:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                stop.setBackgroundResource(R.drawable.stopwatch_white_bg);
                start.setBackgroundResource(R.drawable.stopwatch_white_bg);
                preset.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.BLACK);
                stop.setTextColor(Color.BLACK);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_DARK_COSMOS:
            case ThemeDetails.THEME_OF_TIME:
                reset.setBackgroundResource(R.drawable.stopwatch_dark_bg);
                start.setBackgroundResource(R.drawable.stopwatch_black_border);
                stop.setBackgroundResource(R.drawable.stopwatch_black_border);
                preset.setBackgroundResource(R.drawable.stopwatch_white_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.BLACK);
                break;

            case ThemeDetails.THEME_SUNRISE:
                tMillis.setTextColor(Color.parseColor("#E8EAF6"));
                reset.setBackgroundResource(R.drawable.stopwatch_maroon_bg);
                start.setBackgroundResource(R.drawable.set_timer_purple);
                stop.setBackgroundResource(R.drawable.set_timer_purple);
                preset.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_FOGGY_FOREST:
                reset.setBackgroundResource(R.drawable.stopwatch_maroon_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                preset.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_MOUNTAINS:
                reset.setBackgroundResource(R.drawable.stopwatch_maroon_bg);
                start.setBackgroundResource(R.drawable.set_timer_purple);
                stop.setBackgroundResource(R.drawable.set_timer_purple);
                preset.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                preset.setShadowLayer(1.5f, -1.5f, 1.5f, Color.GRAY);
                break;

            case ThemeDetails.THEME_RAINY_DAY:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                preset.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_BEACH:
                tMillis.setTextColor(Color.parseColor("#283044"));
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_white_bg);
                stop.setBackgroundResource(R.drawable.stopwatch_white_bg);
                preset.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.BLACK);
                stop.setTextColor(Color.BLACK);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_BLUE_NIGHT:
                reset.setBackgroundResource(R.drawable.stopwatch_blue_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                preset.setBackgroundResource(R.drawable.stopwatch_green_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_SHIMMERING_NIGHT:
                reset.setBackgroundResource(R.drawable.stopwatch_blue_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                preset.setBackgroundResource(R.drawable.set_timer_blue);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_AURORA:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                preset.setBackgroundResource(R.drawable.stopwatch_cyan_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.WHITE);
                break;

            case ThemeDetails.THEME_CYAN:
                reset.setBackgroundResource(R.drawable.stopwatch_indigo_bg);
                start.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                stop.setBackgroundResource(R.drawable.stopwatch_maroon_border);
                preset.setBackgroundResource(R.drawable.stopwatch_white_bg);
                start.setTextColor(Color.WHITE);
                stop.setTextColor(Color.WHITE);
                reset.setTextColor(Color.WHITE);
                preset.setTextColor(Color.BLACK);
                break;
        }
    }
}
