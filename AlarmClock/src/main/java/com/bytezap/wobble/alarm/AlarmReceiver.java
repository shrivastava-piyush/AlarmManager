/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.NotificationProvider;

import java.util.Calendar;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String intentAction = intent.getAction();
        if (intentAction != null) {
            AlarmInstance alarm;
            long alarmId;

            switch (intentAction) {
                case AlarmAssistant.DISMISS_AFTER_SNOOZE:
                    alarmId = intent.getLongExtra(AlarmAssistant.ID, -1);
                    alarm = AlarmAssistant.getInstance(context, alarmId);
                    if (alarm!=null) {
                        AlarmAssistant.updateDismissState(context, alarm);
                        NotificationProvider.cancelAlarmNotification(context, alarm.hashCode());
                    } else {
                        Log.e("AlarmReceiver", "Invalid alarm for action:" + AlarmAssistant.DISMISS_AFTER_SNOOZE);
                    }
                    break;

                case AlarmAssistant.SKIP:
                    alarmId = intent.getLongExtra(AlarmAssistant.ID, -1);
                    alarm = AlarmAssistant.getInstance(context, alarmId);
                    if (alarm != null) {
                        AlarmAssistant.setAlarmState(context, alarm, AlarmInstance.ALARM_STATE_FRESHLY_STARTED);
                        AlarmAssistant.rescheduleAlarm(context, alarm);
                        if (!BitmapController.isAppNotRunning()) {
                            context.sendBroadcast(new Intent(Clock.REFRESH_LIST));
                        }
                        Log.v("AlarmReceiver", intentAction + ": AlarmInsigator updated the skipped alarm: " + alarm.id);
                    }
                    break;

                case AlarmAssistant.UPCOMING_NOTIF:
                    alarmId = intent.getLongExtra(AlarmAssistant.ID, -1);
                    alarm = AlarmAssistant.getInstance(context, alarmId);
                    if (alarm != null) {
                        if (Calendar.getInstance().getTimeInMillis() - 30000 < alarm.getAlarmTime().getTimeInMillis()) {
                            NotificationProvider.cancelAlarmNotification(context, alarm.hashCode());
                            NotificationProvider.showUpcomingAlarmNotification(context, alarm);
                        }
                    } else {
                        Log.e("AlarmReceiver", "Invalid alarm for action: " + AlarmAssistant.UPCOMING_NOTIF);
                    }
                    break;

                case AlarmAssistant.DISMISS_NOW:
                    alarmId = intent.getLongExtra(AlarmAssistant.ID, -1);
                    alarm = AlarmAssistant.getInstance(context, alarmId);
                    if (alarm!=null) {
                        AlarmAssistant.dismissAlarm(context, alarm);
                        NotificationProvider.cancelAlarmNotification(context, alarm.hashCode());
                    } else {
                        Log.e("AlarmReceiver", "Invalid alarm for " + AlarmAssistant.DISMISS_NOW);
                    }
                    break;

                case AlarmAssistant.PRE_DISMISS:
                    alarmId = intent.getLongExtra(AlarmAssistant.ID, -1);
                    alarm = AlarmAssistant.getInstance(context, alarmId);
                    if (alarm!=null) {
                        AlarmAssistant.setAlarmState(context, alarm, AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
                        AlarmAssistant.rescheduleAlarm(context, alarm);
                    } else {
                        Log.e("AlarmReceiver", "Invalid alarm for " + AlarmAssistant.PRE_DISMISS);
                    }
                    break;
            }
        }
    }
}
