/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.interaction;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.alarm.AlarmAssistant;
import com.bytezap.wobble.alarm.AlarmScreen;
import com.bytezap.wobble.alarm.AlarmService;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.database.DataManager;
import com.bytezap.wobble.timer.TimerFragment;
import com.bytezap.wobble.timer.TimerService;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ToastGaffer;
import com.bytezap.wobble.utils.VoiceNotifier;

import java.util.ArrayList;
import java.util.List;

public class InteractionHandler extends Activity {

    private static final long TIMER_MIN_TIME = 1000;
    private static final long TIMER_MAX_TIME = 24 * 60 * 60 * 1000;
    private static final String TAG = InteractionHandler.class.getSimpleName();

    private DataManager dataManager;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);
            mContext = getApplicationContext();
            final Intent intent = getIntent();
            final String action = intent == null ? null : intent.getAction();
            dataManager = DataManager.getInstance(mContext);
            if (action == null) {
                return;
            }

            switch (action) {
                case AlarmClock.ACTION_SET_ALARM:
                    setInteractiveAlarm(intent);
                    break;
                case AlarmClock.ACTION_SHOW_ALARMS:
                    showAlarms();
                    break;
                case AlarmClock.ACTION_SET_TIMER:
                    setInteractiveTimer(intent);
                    break;
                case AlarmClock.ACTION_DISMISS_ALARM:
                    dismissInteractiveAlarms();
                    break;
                case AlarmClock.ACTION_SNOOZE_ALARM:
                    snoozeInteractiveAlarms();
                    break;
            }

        } catch (Exception e) {
            Log.v(TAG, e.toString());
        } finally {
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setInteractiveAlarm(Intent intent) {
        final int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1);

        // If not provided, use zero. If it is provided, make sure it's valid, otherwise, show UI
        final int minutes;
        if (intent.hasExtra(AlarmClock.EXTRA_MINUTES)) {
            minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1);
        } else {
            minutes = 0;
        }

        //Invalid time
        if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
            // Invalid time, open the alarms UI
            showAlarms();
            Log.e(TAG, "Alarm has invalid time");
            VoiceNotifier.notifyFailure(this, getString(R.string.invalid_time));
            return;
        }

        AlarmObject alarm = new AlarmObject(hour, minutes);
        alarm.isVibrate = intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, false);
        final String tone = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE);

        if (tone != null) {
            if (!AlarmClock.VALUE_RINGTONE_SILENT.equals(tone) || !TextUtils.isEmpty(tone)) {
                alarm.alarmTone = Uri.parse(tone);
            }
        }

        final ArrayList<Integer> days = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
        if (days != null) {
            for (int i = 0; i < days.size(); i++) {
                alarm.setRepeatingDay(days.get(i) - 1, true);
            }
        } else {
            // API says to use an ArrayList<Integer> but we allow the user to use a int[] too.
            final int[] daysArray = intent.getIntArrayExtra(AlarmClock.EXTRA_DAYS);
            if (daysArray != null) {
                for (int day : daysArray) {
                    alarm.setRepeatingDay(day - 1, true);
                }
            }
        }

        String message = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        if (message == null) {
            message = "";
        }
        alarm.name = message;

        dataManager.createAlarm(alarm);
        AlarmInstance instance = dataManager.createInstance(alarm);
        AlarmAssistant.checkAndSetAlarm(mContext, instance);
        AlarmAssistant.updateClosestAlarm(mContext, dataManager.getInstances());
        CommonUtils.showAlarmToast(mContext, alarm.getNextAlarmTime().getTimeInMillis());
        if (!BitmapController.isAppNotRunning()) {
            sendBroadcast(new Intent(Clock.REFRESH_LIST));
        }

        boolean notShowUI = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (!notShowUI) {
            showAlarms();
        }
        VoiceNotifier.notifySuccess(this, getString(R.string.alarm_created));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setInteractiveTimer(Intent intent) {
        if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
            showTimer();
            Log.e(TAG, "Timer no time specified");
            VoiceNotifier.notifyFailure(this, getString(R.string.invalid_time));
            return;
        }

        final long time = 1000L * intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0);
        if (time < TIMER_MIN_TIME || time > TIMER_MAX_TIME) {
            Log.e(TAG, "Timer has invalid time");
            VoiceNotifier.notifyFailure(this, getString(R.string.invalid_time));
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Clock.TIMER_PREF, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(TimerFragment.IS_RUNNING, false)) {
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.cancel_old_timer));
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(TimerFragment.IS_RUNNING, true);

        String message = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        if (message != null) {
            //editor.putString(TimerFragment.TIMER_TEXT, message);
        }
        editor.putBoolean(TimerFragment.IS_SET_TIME, true);
        editor.putLong(TimerFragment.REMAINING_TIME, time);
        editor.putLong(TimerFragment.TOTAL_TIME, time);
        editor.apply();

        boolean notShowUI = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        if (notShowUI) {
            Intent serviceIntent = new Intent(mContext, TimerService.class);
            serviceIntent.putExtra(TimerFragment.REMAINING_TIME, time);
            startService(serviceIntent);
        } else {
            showTimer();
        }
        VoiceNotifier.notifySuccess(this, getString(R.string.timer_created));
    }

    private void showAlarms() {
        Intent showAlarm = new Intent(this, Clock.class);
        showAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        showAlarm.setAction(Clock.TAB_ALARM);
        startActivity(showAlarm);
    }

    private void showTimer() {
        Intent showTimer = new Intent(this, Clock.class);
        showTimer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        showTimer.setAction(Clock.TAB_TIMER);
        startActivity(showTimer);
    }

    private void dismissInteractiveAlarms() {
        Intent intent = getIntent();
        boolean notShowUI = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (!notShowUI) {
            showAlarms();
        }
        new asyncDismissAlarm(mContext, this, intent, dataManager).execute();
    }

    private void snoozeInteractiveAlarms() {

        List<AlarmInstance> firedAlarms = dataManager.getFiredAlarms();
        if (firedAlarms.isEmpty()) {
            Log.e(TAG, "No alarm to be snoozed");
            VoiceNotifier.notifyFailure(this, "No alarm to be snoozed");
            return;
        }
        for (AlarmInstance instance : firedAlarms) {
            if (AlarmScreen.isAlarmActive) {
                if (instance.snoozeMethod == AlarmObject.SNOOZE_METHOD_DEFAULT) {
                    Intent intent = new Intent(mContext, AlarmScreen.class);
                    intent.setAction(AlarmService.ALARM_SNOOZE);
                    intent.putExtra(AlarmAssistant.ID, instance.id);
                    sendBroadcast(intent);
                    VoiceNotifier.notifySuccess(this, getString(R.string.alarm_snoozed));
                } else {
                    // Notify failure here for alarm with snooze problem
                    Log.e(TAG, "Failed snoozing alarm");
                    VoiceNotifier.notifyFailure(this, getString(R.string.math_friendly_alert));
                }
            }
        }
    }

    private static class asyncDismissAlarm extends AsyncTask<Void, Void, Void> {

        private Activity activity;
        private final Context context;
        private DataManager dbManager;
        private Intent dismissIntent;

        public asyncDismissAlarm(Context appContext,Activity dismissActivity, Intent intent, DataManager manager) {
            activity = dismissActivity;
            context = appContext;
            dbManager = manager;
            dismissIntent = intent;
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        protected Void doInBackground(Void... params) {
            List<AlarmInstance> alarmInstances = dbManager.getInstances();
            if (alarmInstances.isEmpty()) {
                Log.e(TAG, "No alarm to dismiss");
                VoiceNotifier.notifyFailure(activity, context.getString(R.string.alarm_not_found));
                return null;
            }

            //Dismiss alarms in pre-dismissed state
            for(AlarmInstance instance : alarmInstances){
                if (instance.alarmState == AlarmInstance.ALARM_STATE_PRE_DISMISS) {
                    AlarmAssistant.setAlarmState(context, instance, AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
                    AlarmAssistant.rescheduleAlarm(context, instance);
                }
            }

            final String searchMode = dismissIntent.getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE);
            if (searchMode == null && alarmInstances.size() > 1) {
                // Add selected alarms here
                Intent showAlarm = new Intent(context, MiscellanyActivity.class);
                showAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                showAlarm.putExtra(MiscellanyActivity.SELECTED_ALARMS, alarmInstances.toArray(new Parcelable[alarmInstances.size()]));
                context.startActivity(showAlarm);
                VoiceNotifier.notifySuccess(activity, context.getString(R.string.pick_alarm));
                return null;
            }

            final List<AlarmInstance> selectedAlarms = new ArrayList<>();

            if (searchMode == null) {
                selectedAlarms.addAll(alarmInstances);
            } else {
                switch (searchMode) {
                    case AlarmClock.ALARM_SEARCH_MODE_TIME:
                        int hour = dismissIntent.getIntExtra(AlarmClock.EXTRA_HOUR, -1);
                        final int minutes = dismissIntent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
                        final Boolean isPm = (Boolean) dismissIntent.getExtras().get(AlarmClock.EXTRA_IS_PM);
                        boolean isInvalidTime = isPm != null && hour > 12 && isPm;
                        if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
                            isInvalidTime = true;
                        }

                        if (isInvalidTime) {
                            Log.e(TAG, "Alarm has invalid time");
                            VoiceNotifier.notifyFailure(activity, context.getString(R.string.invalid_time));
                            return null;
                        }

                        hour = Boolean.TRUE.equals(isPm) && hour < 12 ? (hour + 12) : hour; //Convert into 24-hour format
                        for (AlarmInstance instance : alarmInstances) {
                            if (instance.hour == hour && instance.minutes == minutes) {
                                selectedAlarms.add(instance);
                            }
                        }

                        if (selectedAlarms.isEmpty()) {
                            Log.e(TAG, "No alarm to dismiss");
                            VoiceNotifier.notifyFailure(activity, context.getString(R.string.alarm_not_found));
                            return null;
                        }
                        break;

                    case AlarmClock.ALARM_SEARCH_MODE_NEXT:
                        AlarmInstance nextAlarm = null;
                        for (AlarmInstance instance : alarmInstances) {
                            if (nextAlarm == null || instance.getNextAlarmTime().before(nextAlarm.getNextAlarmTime())) {
                                nextAlarm = instance;
                            }
                        }
                        if (nextAlarm == null) {
                            Log.e(TAG, "No alarm to dismiss");
                            VoiceNotifier.notifyFailure(activity, context.getString(R.string.alarm_not_found));
                            return null;
                        }

                        for (AlarmInstance instance : alarmInstances) {
                            // For alarms which fire at same time
                            if (instance.hour == nextAlarm.hour && instance.minutes == nextAlarm.minutes) {
                                selectedAlarms.add(instance);
                            }
                        }
                        break;

                    case AlarmClock.ALARM_SEARCH_MODE_ALL:
                        selectedAlarms.addAll(alarmInstances);
                        break;

                    case AlarmClock.ALARM_SEARCH_MODE_LABEL:
                        final String name = dismissIntent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
                        if (name == null) {
                            Log.e(TAG, "No alarm to dismiss");
                            VoiceNotifier.notifyFailure(activity, context.getString(R.string.alarm_not_found));
                            return null;
                        }

                        for (AlarmInstance instance : alarmInstances) {
                            if (instance.name.contains(name)) {
                                selectedAlarms.add(instance);
                            }
                        }

                        if (selectedAlarms.isEmpty()) {
                            Log.e(TAG, "No alarm to dismiss");
                            VoiceNotifier.notifyFailure(activity, context.getString(R.string.alarm_not_found));
                            return null;
                        }
                        break;
                }
            }

            if (!AlarmClock.ALARM_SEARCH_MODE_ALL.equals(searchMode) && selectedAlarms.size() > 1) {
                // Add selected alarms here
                Intent showAlarm = new Intent(context, MiscellanyActivity.class);
                showAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                showAlarm.putExtra(MiscellanyActivity.SELECTED_ALARMS, alarmInstances.toArray(new Parcelable[alarmInstances.size()]));
                context.startActivity(showAlarm);
                VoiceNotifier.notifySuccess(activity, context.getString(R.string.pick_alarm));
                return null;
            }

            for (AlarmInstance instance : selectedAlarms) {
                if (instance.getNextAlarmTime().getTimeInMillis() - System.currentTimeMillis() <= DateUtils.DAY_IN_MILLIS) { //Check if alarm is within 24 hours
                    AlarmAssistant.dismissAlarm(context, instance);
                    VoiceNotifier.notifySuccess(activity, context.getString(R.string.pick_alarm));
                } else {
                    // Notify failure here for alarm not within 24 hours
                    Log.e(TAG, "Alarm not within 24 hours");
                    VoiceNotifier.notifyFailure(activity, context.getString(R.string.alarm_not_24));
                }
            }
            if (!BitmapController.isAppNotRunning()) {
                context.sendBroadcast(new Intent(Clock.REFRESH_LIST));
            }
            return null;
        }
    }

}
