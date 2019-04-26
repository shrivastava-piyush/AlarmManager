/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.database.DataManager;
import com.bytezap.wobble.timer.TimerAlert;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.ToastGaffer;

import java.util.Calendar;
import java.util.List;

public class AlarmAssistant {

    private static final String TAG = AlarmAssistant.class.getSimpleName();
    private static final String INTENT_CATEGORY = "ALARM_ASSISTANT";
    public static final String ID = "alarm_id";
    public static final String PREVIEW = "alarm_preview";

    //intents
    private static final String ACTION = "com.bytezap.alarmclock.action.";
    static final String INDICATOR_ACTION = "indicator";
    static final String SKIP = ACTION + "skip";
    static final String PRE_DISMISS = ACTION + "pre_dismiss";
    static final String UPCOMING_NOTIF = ACTION + "upcoming_notif";
    public static final String DISMISS_NOW = ACTION + "dismiss_now";
    public static final String DISMISS_AFTER_SNOOZE = ACTION + "dismiss_snooze";

    static void checkAndSetAlarms(Context context, DataManager dataManager) {

        //Handler prefers to pass dataManager for optimization
        if (dataManager == null) {
            dataManager = DataManager.getInstance(context);
        }
        List<AlarmInstance> alarmInstances = dataManager.getInstances();
        if (alarmInstances != null) {
            for (AlarmInstance instance : alarmInstances) {
                checkAndSetAlarm(context, instance);
            }
        }
        updateClosestAlarm(context, alarmInstances);
    }

    private static void setAlarm(Context context, long timeInMillis, PendingIntent pIntent) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (CommonUtils.isMOrLater()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pIntent);
        } else if (CommonUtils.isKOrLater()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pIntent);
        }

    }

    public static void setTimerAlarm(Context context, long timeInMillis) {

        Intent intent = new Intent(context, TimerAlert.class);
        intent.setClass(context, TimerAlert.class);
        intent.addCategory(INTENT_CATEGORY);
        PendingIntent pIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (CommonUtils.isMOrLater()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInMillis, pIntent);
        } else if (CommonUtils.isKOrLater()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInMillis, pIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInMillis, pIntent);
        }
    }

    private static void indicateAlarmSet(Context context, Calendar calendar, boolean isSet) {
        if (CommonUtils.isLOrLater()) {
            indicateForLollipop(context, calendar, isSet);
        }
    }

    @TargetApi(21)
    private static void indicateForLollipop(Context context, Calendar calendar, boolean shouldSet) {

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int flags = !shouldSet ? PendingIntent.FLAG_NO_CREATE : 0;
        PendingIntent indicator = PendingIntent.getBroadcast(context, 0,
                createIndicatingIntent(context), flags);

        if (shouldSet) {
            Intent vIntent = new Intent(context, Clock.class);
            vIntent.setAction(Clock.TAB_ALARM);
            vIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent viewIntent = PendingIntent.getActivity(context, 0,
                    vIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager.AlarmClockInfo info =
                    new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), viewIntent);

            alarmMgr.setAlarmClock(info, indicator);

        } else if (indicator != null) {
            alarmMgr.cancel(indicator);
        }

    }

    @SuppressWarnings("deprecation")
    private static void indicateForPreLollipop(Context context, Calendar calendar, boolean isSet) {
        String time = isSet ? CommonUtils.getFormattedTimeWithDay(context, calendar) : "";
        Settings.System.putString(context.getContentResolver(),
                    Settings.System.NEXT_ALARM_FORMATTED, time);
    }

    @Nullable
    public static AlarmObject getAlarm(Context context, long id) {
        if (id != -1) {
            DataManager dataManager = DataManager.getInstance(context);
            return dataManager.getAlarmById(id);
        }
        return null;
    }

    @Nullable
    public static AlarmInstance getInstance(Context context, long id) {
        if (id != -1) {
            DataManager dataManager = DataManager.getInstance(context);
            return dataManager.getInstanceById(id);
        }
        return null;
    }

    public static int getHashCode(long alarmId) {
        return Long.valueOf(alarmId).hashCode();
    }

    public static void setAlarmState(Context context, AlarmInstance instance, int state) {
        DataManager dataManager = DataManager.getInstance(context);

        if (instance != null) {
            Log.v(TAG, "A state change occurred from " + instance.getStateName(instance.alarmState) + " to " + instance.getStateName(state));
            instance.alarmState = state;
            dataManager.updateInstance(instance);
        }
    }

    static void rescheduleInstances(Context context){

        DataManager dataManager = DataManager.getInstance(context);
        List<AlarmInstance> alarmInstances = dataManager.getInstances();

        if (alarmInstances != null) {
            for (AlarmInstance instance : alarmInstances) {
                AlarmObject alarm = dataManager.getAlarmById(instance.id);
                if (alarm == null) {
                    cancelAlarm(context, instance);
                    dataManager.deleteInstanceById(instance.id);
                    continue;
                }

                long timeDiff = Math.abs(Calendar.getInstance().getTimeInMillis() - instance.getAlarmTime().getTimeInMillis());
                if (timeDiff  > 30000) {
                    NotificationProvider.cancelAlarmNotification(context, alarm.hashCode());
                    cancelAlarm(context, instance);
                    dataManager.deleteInstanceById(instance.id);
                    checkAndSetAlarm(context, dataManager.createInstance(alarm));
                } else {
                    checkAndSetAlarm(context, instance);
                }
            }
        }
    }

    static void rescheduleInstancesAfterBoot(Context context){

        DataManager dataManager = DataManager.getInstance(context);
        List<AlarmInstance> alarmInstances = dataManager.getInstances();

        if (alarmInstances != null) {
            for (AlarmInstance instance : alarmInstances) {
                AlarmObject alarm = dataManager.getAlarmById(instance.id);
                if (alarm == null) {
                    cancelAlarm(context, instance);
                    dataManager.deleteInstanceById(instance.id);
                    continue;
                }
                if ((Calendar.getInstance().getTimeInMillis() - instance.getAlarmTime().getTimeInMillis() < 1800000)
                        && instance.alarmState == AlarmInstance.ALARM_STATE_TRIGGERED) { //Phone has been restarted
                    Intent serviceIntent = new Intent(context, AlarmService.class);
                    serviceIntent.putExtra(ID, instance.id);
                    serviceIntent.setClass(context, AlarmService.class);
                    serviceIntent.addCategory(INTENT_CATEGORY);
                    if (CommonUtils.is16OrLater()) {
                        serviceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    }
                    if (CommonUtils.isOOrLater()) {
                        ContextCompat.startForegroundService(context, serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } else {
                    checkAndSetAlarm(context, instance);
                }
            }
        }
    }

    static void cancelAlarms(Context context, DataManager dataManager) {
        if (dataManager == null) {
            dataManager = DataManager.getInstance(context);
        }
        List<AlarmInstance> alarms = dataManager.getInstances();

        if (alarms != null) {
            for (AlarmInstance alarm : alarms) {
                cancelAlarm(context, alarm);
            }
        }
    }

    static void cancelAlarm(Context context, AlarmInstance instance) {
        if (instance == null) {
            Log.e(TAG, "Alarm could not be cancelled");
            return;
        }
        NotificationProvider.cancelAlarmNotification(context, instance.hashCode());
        boolean isNotifEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.UPCOMING_NOTIFICATIONS, true);
        if (isNotifEnabled && !instance.isWithin2Hours()) {
            cancelNotifAlarm(context, instance);
        }
        Intent intent = new Intent(context, AlarmService.class);
        intent.putExtra(ID, instance.id);
        intent.setClass(context, AlarmService.class);
        intent.addCategory(INTENT_CATEGORY);
        PendingIntent pIntent;

        if (CommonUtils.isOOrLater()) {
            pIntent = PendingIntent.getForegroundService(context, instance.hashCode(),
                    intent, PendingIntent.FLAG_NO_CREATE);
        } else {
            pIntent = PendingIntent.getService(context, instance.hashCode(),
                    intent, PendingIntent.FLAG_NO_CREATE);
        }

        if (pIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            indicateAlarmSet(context, null, false);
            alarmManager.cancel(pIntent);
            pIntent.cancel();
        }
    }

    private static void cancelNotifAlarm(Context context, AlarmInstance instance) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(ID, instance.id);
        intent.setAction(UPCOMING_NOTIF);
        intent.addCategory(INTENT_CATEGORY);
        intent.setClass(context, AlarmReceiver.class);

        PendingIntent pIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                intent, PendingIntent.FLAG_NO_CREATE);

        if (pIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pIntent);
            pIntent.cancel();
        }
    }

    public static void cancelTimerAlarm(Context context) {
        Intent intent = new Intent(context, TimerAlert.class);
        intent.setClass(context, TimerAlert.class);
        intent.addCategory(INTENT_CATEGORY);
        PendingIntent pIntent = PendingIntent.getActivity(context, 1,
                intent, PendingIntent.FLAG_NO_CREATE);

        if (pIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pIntent);
            pIntent.cancel();
        }
    }

    public static void updateClosestAlarm(Context context, List<AlarmInstance> alarmInstances) {
        AlarmInstance nextAlarm = null;
        boolean isNoSnoozeOrCheck;
        //Handler prefers to supply list of alarms for optimization
        if (alarmInstances == null) {
            DataManager manager = DataManager.getInstance(context);
            alarmInstances = manager.getInstances();
        }
        if (alarmInstances == null) {
            return;
        }
        for (AlarmInstance instance : alarmInstances) {
            isNoSnoozeOrCheck = (instance.alarmState == AlarmInstance.ALARM_STATE_FRESHLY_STARTED) || (instance.alarmState == AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
            if (isNoSnoozeOrCheck) {
                if (nextAlarm == null || instance.getNextAlarmTime().before(nextAlarm.getNextAlarmTime())) {
                    nextAlarm = instance;
                }
            }
        }
        if (nextAlarm!=null) {
            indicateAlarmSet(context, nextAlarm.getNextAlarmTime(), true);
        }
    }

    private static void scheduleFreshAlarm(Context context, AlarmInstance instance){
        AlarmObject alarm = getAlarm(context, instance.id);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        PendingIntent intent = createPendingIntent(context, instance);
        if (alarm != null) {
            instance.hour = alarm.hour;
            instance.minutes = alarm.minutes;
        }
        Calendar alarmTime = instance.getNextAlarmTime();

        int brightnessDuration = Integer.parseInt(settings.getString(SettingsActivity.INCREASING_BRIGHTNESS, "0"));
        if (brightnessDuration !=0) {
            alarmTime.add(Calendar.MILLISECOND, - brightnessDuration * 1000);
        }
        setAlarm(context, alarmTime.getTimeInMillis(), intent);
        indicateAlarmSet(context, alarmTime, true);
        if (settings.getBoolean(SettingsActivity.UPCOMING_NOTIFICATIONS, true)) {
            scheduleUpcomingNotif(context, alarmTime, instance);
        }
    }

    public static void rescheduleAlarm(Context context, AlarmInstance instance){
        cancelAlarm(context, instance);
        checkAndSetAlarm(context, instance);
        updateClosestAlarm(context, null);
    }

    public static void checkAndSetAlarm(Context context, AlarmInstance instance) {
        switch (instance.alarmState) {
            case AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK:
            case AlarmInstance.ALARM_STATE_FRESHLY_STARTED:
                scheduleFreshAlarm(context, instance);
                break;

            case AlarmInstance.ALARM_STATE_SNOOZED:
                updateSnoozeAlarm(context, Calendar.getInstance(), instance);
                break;

            case AlarmInstance.ALARM_STATE_DISMISSED_WITH_CHECK:
                updateWakeupCheck(context, instance, Calendar.getInstance());
                break;

            case AlarmInstance.ALARM_STATE_SKIPPED:
                skipAlarm(context, instance.id, false);
                break;

            case AlarmInstance.ALARM_STATE_PRE_DISMISS:
                setPreDismissAlarm(context, instance);
                break;

            case AlarmInstance.ALARM_STATE_TRIGGERED:
                Log.v(TAG, "Alarm updated during triggered state");
                break;
        }
    }

    static void disableAlarmRemoveInstance(Context context, AlarmInstance instance){
        DataManager manager = DataManager.getInstance(context);
        AlarmObject object = manager.getAlarmById(instance.id);
        if (object != null) {
            object.isEnabled = false;
            manager.updateAlarm(object);
        }
        manager.deleteInstanceById(instance.id);
    }

    public static void dismissAlarm(Context context, AlarmInstance instance) {
        cancelAlarm(context, instance);
        if (instance.isOneTimeAlarm()) {
            disableAlarmRemoveInstance(context, instance);
        } else {
            setPreDismissAlarm(context, instance);
        }
        indicateAlarmSet(context, instance.getNextAlarmTime(), false);
        updateClosestAlarm(context, null);
    }

    private static void setPreDismissAlarm(Context context, AlarmInstance instance){
        setAlarmState(context, instance, AlarmInstance.ALARM_STATE_PRE_DISMISS);
        PendingIntent intent = createDismissPendingIntent(context, instance);
        Calendar alarmTime = instance.getNextAlarmTime();
        setAlarm(context, alarmTime.getTimeInMillis(), intent);
    }

    public static void updateSetAlarms(Context context) {
        DataManager dataManager = DataManager.getInstance(context);
        List<AlarmInstance> instances = dataManager.getInstances();
        AlarmInstance nextAlarm = null;
        boolean isNoSnoozeOrCheck;
        if (instances == null) {
            return;
        }
        for (AlarmInstance instance : instances) {
            isNoSnoozeOrCheck = (instance.alarmState == AlarmInstance.ALARM_STATE_FRESHLY_STARTED) || (instance.alarmState == AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
            if (isNoSnoozeOrCheck) {
                checkAndSetAlarm(context, instance);
                if (nextAlarm == null || instance.getNextAlarmTime().before(nextAlarm.getNextAlarmTime())) {
                    nextAlarm = instance;
                }
            }
        }

        if (nextAlarm != null) {
            indicateAlarmSet(context, nextAlarm.getNextAlarmTime(), true);
        }
    }

    private static void scheduleUpcomingNotif(Context context, Calendar calendar, AlarmInstance instance) {
        NotificationProvider.cancelAlarmNotification(context, instance.hashCode());
        if (instance.isWithin2Hours()) {
            NotificationProvider.showUpcomingAlarmNotification(context, instance);
        } else {
            calendar.add(Calendar.HOUR_OF_DAY, -2); //Let's keep hour offset 2 for now
            setAlarm(context, calendar.getTimeInMillis(), createNotifPendingIntent(context, instance));
        }
    }

    public static void rescheduleNotifications(Context context, boolean isNotifEnabled) {
        DataManager manager = DataManager.getInstance(context);
        List<AlarmInstance> list = manager.getInstances();
        if (list == null) {
            return;
        }
        for (AlarmInstance alarm : list) {
            if (isNotifEnabled) {
                scheduleUpcomingNotif(context, alarm.getNextAlarmTime(), alarm);
            } else {
                if (alarm.isWithin2Hours()) {
                    NotificationProvider.cancelAlarmNotification(context, alarm.hashCode());
                } else {
                    cancelNotifAlarm(context, alarm);
                }
            }
        }
    }

    static void updateDismissState(Context context, AlarmInstance instance) {
        instance.snoozeTimes = 0;
        if (instance.wakeupCheck) {
            setWakeupCheck(context, instance, Calendar.getInstance());
        } else {
            updateAlarmWithStateNoCheck(context, instance);
        }
    }

    static void updateAlarmWithStateNoCheck(Context context, AlarmInstance instance) {
        if (instance!=null) {
            cancelAlarm(context, instance);
            if (instance.isOneTimeAlarm()) {
                disableAlarmRemoveInstance(context, instance);
                if (!BitmapController.isAppNotRunning()) {
                    context.sendBroadcast(new Intent(Clock.REFRESH_LIST));
                }
            } else {
                setAlarmState(context, instance, AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
            }
            checkAndSetAlarm(context, instance);
        }
        updateClosestAlarm(context, null);
    }

    static void snoozeAlarm(Context context, Calendar snoozeTime, AlarmInstance instance) {

        if (instance != null) {
            PendingIntent intent = createPendingIntent(context, instance);
            instance.snoozeTimes++;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            int snoozeDuration = Integer.parseInt(settings.getString(SettingsActivity.SNOOZE_DURATION, "5"));

            snoozeTime.add(Calendar.MINUTE, snoozeDuration);
            snoozeTime.set(Calendar.SECOND, 0);
            snoozeTime.set(Calendar.MILLISECOND, 0);
            instance.setAlarmTime(snoozeTime);
            setAlarmState(context, instance, AlarmInstance.ALARM_STATE_SNOOZED);
            setAlarm(context, snoozeTime.getTimeInMillis(), intent);
            indicateAlarmSet(context, snoozeTime, true);

            NotificationProvider.cancelAlarmNotification(context, instance.hashCode());
            NotificationProvider.showSnoozeNotification(context, instance, snoozeTime);

            ToastGaffer.showToast(context, context.getString(R.string.snooze_toast, snoozeDuration));
        } else {
            Log.e("Snooze Alarm", "Invalid instance id");
        }

    }

    private static void updateSnoozeAlarm(Context context, Calendar currentTime, AlarmInstance instance) {

        if (instance != null) {
            PendingIntent intent = createPendingIntent(context, instance);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            int snoozeDuration = Integer.parseInt(settings.getString(SettingsActivity.SNOOZE_DURATION, "5"));

            int alarmMinute = (int) ((instance.getAlarmTime().getTimeInMillis() - currentTime.getTimeInMillis())/60000);
            alarmMinute = snoozeDuration - alarmMinute;

            if (alarmMinute == 0) { // Maybe less than 1 minute remains
                alarmMinute = 1;
            }

            if (alarmMinute < 0 || alarmMinute > snoozeDuration) {
                alarmMinute = snoozeDuration;
            }
            currentTime.add(Calendar.MINUTE, alarmMinute);
            currentTime.set(Calendar.SECOND, 0);
            currentTime.set(Calendar.MILLISECOND, 0);
            instance.setAlarmTime(currentTime);
            setAlarm(context, currentTime.getTimeInMillis(), intent);
            indicateAlarmSet(context, currentTime, true);

            NotificationProvider.cancelAlarmNotification(context, instance.hashCode());
            NotificationProvider.showSnoozeNotification(context, instance, currentTime);

        } else {
            Log.e("Snooze Alarm", "Invalid alarm id");
        }

    }

    static void setWakeupCheck(Context context, AlarmInstance instance, Calendar checkTime) {

        if (instance != null) {
            PendingIntent pendingIntent = createPendingIntent(context, instance);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            int wakeupInterval = Integer.parseInt(settings.getString(SettingsActivity.WAKE_UP_DURATION, "10"));

            checkTime.add(Calendar.MINUTE, wakeupInterval);
            checkTime.set(Calendar.SECOND, 0);
            checkTime.set(Calendar.MILLISECOND, 0);
            instance.setAlarmTime(checkTime);
            setAlarmState(context, instance, AlarmInstance.ALARM_STATE_DISMISSED_WITH_CHECK);
            setAlarm(context, checkTime.getTimeInMillis(), pendingIntent);
            indicateAlarmSet(context, checkTime, true);

            ToastGaffer.showToast(context, context.getString(R.string.wakeup_toast, wakeupInterval));
        } else {
            Log.e("WakeUp Check", "Invalid alarm id");
        }
    }

    private static void updateWakeupCheck(Context context, AlarmInstance instance, Calendar currentTime) {

        if (instance != null) {
            PendingIntent pendingIntent = createPendingIntent(context, instance);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            int wakeDuration = Integer.parseInt(settings.getString(SettingsActivity.WAKE_UP_DURATION, "10"));

            Calendar alarmTime = instance.getAlarmTime();
            int wakeUpInterval = (int) (alarmTime.getTimeInMillis() - currentTime.getTimeInMillis())/60000;

            wakeUpInterval = wakeDuration - wakeUpInterval;

            if (wakeUpInterval == 0) { // Maybe less than 1 minute remains
                wakeUpInterval = 1;
            }

            if (wakeUpInterval < 0 || wakeUpInterval > wakeDuration) {
                wakeUpInterval = wakeDuration;
            }
            currentTime.add(Calendar.MINUTE, wakeUpInterval);
            currentTime.set(Calendar.SECOND, 0);
            currentTime.set(Calendar.MILLISECOND, 0);
            setAlarm(context, currentTime.getTimeInMillis(), pendingIntent);
            indicateAlarmSet(context, currentTime, true);
        } else {
            Log.e("WakeUp Check", "Invalid alarm id");
        }
    }

    static void skipAlarm(Context context, long id, boolean showToast) {
        AlarmInstance instance = getInstance(context, id);
        if (instance != null) {
            cancelAlarm(context, instance);

            PendingIntent intent = createSkipPendingIntent(context, instance);
            Calendar calendar = instance.getNextAlarmTime();
            indicateAlarmSet(context, calendar, false);

            setAlarmState(context, instance, AlarmInstance.ALARM_STATE_SKIPPED);
            setAlarm(context, calendar.getTimeInMillis(), intent);

            updateClosestAlarm(context, null);
        }

        if (showToast) {
            ToastGaffer.showToast(context, context.getString(R.string.skip_alarm));
        }

    }

    static void unSkipAlarm(Context context, AlarmObject alarm) {
        AlarmInstance instance = getInstance(context, alarm.id);
        if (instance != null) {
            cancelAlarm(context, instance);

            PendingIntent intent = createPendingIntent(context, instance);
            Calendar calendar = instance.getNextAlarmTime();
            setAlarmState(context, instance, AlarmInstance.ALARM_STATE_FRESHLY_STARTED);
            setAlarm(context, calendar.getTimeInMillis(), intent);
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.UPCOMING_NOTIFICATIONS, true)) {
                scheduleUpcomingNotif(context, calendar, instance);
            }
            updateClosestAlarm(context, null);
            CommonUtils.showAlarmToast(context, calendar.getTimeInMillis());
        }
    }

    private static PendingIntent createPendingIntent(Context context, AlarmInstance instance) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.putExtra(ID, instance.id);
        intent.setClass(context, AlarmService.class);
        intent.addCategory(INTENT_CATEGORY);
        if (CommonUtils.is16OrLater()) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }

        if (CommonUtils.isOOrLater()) {
            return PendingIntent.getForegroundService(context, instance.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            return PendingIntent.getService(context, instance.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private static PendingIntent createNotifPendingIntent(Context context, AlarmInstance instance) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(UPCOMING_NOTIF);
        intent.putExtra(ID, instance.id);
        intent.addCategory(INTENT_CATEGORY);
        intent.setClass(context, AlarmReceiver.class);
        if (CommonUtils.is16OrLater()) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }

        return PendingIntent.getBroadcast(context, instance.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent createSkipPendingIntent(Context context, AlarmInstance instance) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(SKIP);
        intent.putExtra(ID, instance.id);
        intent.addCategory(INTENT_CATEGORY);
        if (CommonUtils.is16OrLater()) {
            intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        intent.setClass(context, AlarmReceiver.class);

        return PendingIntent.getBroadcast(context, instance.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent createDismissPendingIntent(Context context, AlarmInstance instance) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(PRE_DISMISS);
        intent.putExtra(ID, instance.id);
        intent.addCategory(INTENT_CATEGORY);
        if (CommonUtils.is16OrLater()) {
            intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        intent.setClass(context, AlarmReceiver.class);

        return PendingIntent.getBroadcast(context, instance.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Intent createIndicatingIntent(Context context) {
        return new Intent(context, AlarmInstigator.class).setAction(INDICATOR_ACTION);
    }

}
