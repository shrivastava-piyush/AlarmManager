package com.bytezap.wobble.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.alarm.AlarmAssistant;
import com.bytezap.wobble.alarm.AlarmReceiver;
import com.bytezap.wobble.alarm.AlarmScreen;
import com.bytezap.wobble.alarm.AlarmService;
import com.bytezap.wobble.alarm.WakeUpCheck;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.timer.TimerAlert;
import com.bytezap.wobble.timer.TimerFragment;

import java.util.Calendar;
import java.util.Locale;

public class NotificationProvider {

    public static Notification showAlarmNotification(Context context, AlarmInstance instance, boolean isSnoozeDisabled) {
        // The PendingIntent to launch the activity if the user selects this notification
        Intent launcher = new Intent(context, AlarmScreen.class);
        launcher.putExtra(AlarmAssistant.ID, instance.id);
        launcher.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent snooze = new Intent(AlarmService.ALARM_SNOOZE);
        snooze.putExtra(AlarmAssistant.ID, instance.id);
        snooze.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent dismiss = new Intent(AlarmService.ALARM_DISMISS);
        dismiss.putExtra(AlarmAssistant.ID, instance.id);
        dismiss.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int hashcode = instance.hashCode();
        String channelId = "WobbleAlarmService";

        PendingIntent contentIntent = PendingIntent.getActivity(context, hashcode, launcher, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent snoozeIntent = PendingIntent.getBroadcast(context, hashcode, snooze, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent dismissIntent = PendingIntent.getBroadcast(context, hashcode, dismiss, PendingIntent.FLAG_UPDATE_CURRENT);

        String contentText = CommonUtils.getFormattedTimeWithDay(context, Calendar.getInstance(Locale.getDefault()));
        int tNumber = context.getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(TextUtils.isEmpty(instance.name) ? context.getString(R.string.alarm_item_name_default) : instance.name)
                .setOngoing(true)
                .setWhen(0)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentText(contentText)
                .setColor(ThemeDetails.getThemeAccent((tNumber)))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.tab_alarm)
                .setContentIntent(contentIntent);

        if (instance.dismissMethod == AlarmObject.DISMISS_METHOD_DEFAULT) {
            builder.addAction(R.drawable.ic_dismiss, context.getString(R.string.alarm_dismiss), dismissIntent);
        }

        if (!(instance.snoozeMethod != AlarmObject.SNOOZE_METHOD_DEFAULT || isSnoozeDisabled)) {
            builder.addAction(R.drawable.ic_snooze, context.getString(R.string.alarm_snooze), snoozeIntent);
        }

        Notification notification = builder.build();
        if (CommonUtils.isOOrLater()) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel mNotificationChannel = CommonUtils.createNotificationChannel(channelId, "WobbleAlarm");
                mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mNotificationChannel.setSound(null, null);
                manager.createNotificationChannel(mNotificationChannel);
                manager.notify(hashcode, notification);
            }
        } else {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(hashcode, notification);
        }

        return notification;
    }

    public static void showSnoozeNotification(Context context, AlarmInstance instance, Calendar alarmTime){
        Intent dismissIntent = new Intent(context, AlarmReceiver.class);
        dismissIntent.setAction(AlarmAssistant.DISMISS_AFTER_SNOOZE);
        dismissIntent.putExtra(AlarmAssistant.ID, instance.id);
        dismissIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent actionIntent = PendingIntent.getBroadcast(context, instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent launcher = new Intent(context, Clock.class);
        launcher.setAction(Clock.TAB_ALARM);
        launcher.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(context, instance.hashCode(), launcher, PendingIntent.FLAG_UPDATE_CURRENT);

        String contentTitle = TextUtils.isEmpty(instance.name) ? context.getString(R.string.alarm_item_name_default) : instance.name;
        int tNumber = context.getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
        String channelId = "WobbleSnoozeNotif";

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(contentTitle)
                .setContentText(context.getString(R.string.snooze_until, CommonUtils.getFormattedTime(context, alarmTime)))
                .setSmallIcon(R.drawable.tab_alarm)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setGroup(String.valueOf(instance.id))
                .setGroupSummary(true)
                .setOngoing(true)
                .setSound(null)
                .addAction(CommonUtils.isLOrLater() ? R.drawable.ic_dismiss : R.drawable.ic_action_unchecked, context.getString(R.string.alarm_dismiss), actionIntent)
                .setColor(ThemeDetails.getThemeAccent(tNumber))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(false)
                .setLocalOnly(true)
                .build();

        if (CommonUtils.isOOrLater()) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel mNotificationChannel = CommonUtils.createNotificationChannel(channelId, "WobbleSnooze");
                mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mNotificationChannel.setSound(null, null);
                manager.createNotificationChannel(mNotificationChannel);
                manager.notify(instance.hashCode(), notification);
            }
        } else {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(instance.hashCode(), notification);
        }
    }

    public static void showWakeupCheckNotification(Context context, long id, boolean isVoice) {
        // The PendingIntent to launch our activity if the user selects this notification
        Intent launcher = new Intent(context, WakeUpCheck.class);
        launcher.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launcher.putExtra(AlarmAssistant.ID, id);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launcher, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String channelId = "WobbleWakeUpCheckNotif";

        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.wake_up_question))
                .setOngoing(true)
                .setContentText(context.getString(R.string.wakeup_info_title))
                .setSmallIcon(R.drawable.tab_alarm)
                .setColor(ThemeDetails.getThemeAccent(BitmapController.getThemeNumber()))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .build();

        if (isVoice && soundUri!=null) {
            notification.sound = soundUri;
        }

        if (CommonUtils.isOOrLater()) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel mNotificationChannel = CommonUtils.createNotificationChannel(channelId, "WobbleWakeUp");
                mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mNotificationChannel.setSound(null, null);
                manager.createNotificationChannel(mNotificationChannel);
                manager.notify(AlarmAssistant.getHashCode(id), notification);
            }
        } else {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(AlarmAssistant.getHashCode(id), notification);
        }
    }

    public static void showTimerAlertNotification(Context context) {
        // The PendingIntent to launch our activity if the user selects this notification
        Intent launcher = new Intent(context, TimerAlert.class);
        launcher.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launcher, PendingIntent.FLAG_UPDATE_CURRENT);

        String defTitle = context.getString(R.string.default_timer_text);
        int tNumber = context.getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
        String newTitle = context.getSharedPreferences(Clock.TIMER_PREF, Context.MODE_PRIVATE).getString(TimerFragment.TIMER_TEXT, defTitle);
        String channelId = "WobbleTimerAlertNotif";

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.time_up))
                .setOngoing(true)
                .setContentText(newTitle.equals(defTitle) ? defTitle : newTitle)
                .setSmallIcon(R.drawable.tab_timer)
                .setColor(ThemeDetails.getThemeAccent(tNumber))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setWhen(0)
                .setContentIntent(contentIntent)
                .build();

        if (CommonUtils.isOOrLater()) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel mNotificationChannel = CommonUtils.createNotificationChannel(channelId, "WobbleTimerAlert");
                mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(mNotificationChannel);
                manager.notify(R.layout.timer_alert, notification);
            }
        } else {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(R.layout.timer_alert, notification);
        }
    }

    public static void showUpcomingAlarmNotification(Context context, AlarmInstance instance) {

        Intent dismissIntent = new Intent(context, AlarmReceiver.class);
        dismissIntent.setAction(AlarmAssistant.DISMISS_NOW);
        dismissIntent.putExtra(AlarmAssistant.ID, instance.id);
        dismissIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int tNumber = context.getSharedPreferences(Clock.THEME_PREFS, Context.MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);

        Intent launcher = new Intent(context, Clock.class);
        launcher.setAction(Clock.TAB_ALARM);
        launcher.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent actionIntent = PendingIntent.getBroadcast(context, instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent contentIntent = PendingIntent.getActivity(context, instance.hashCode(), launcher, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar alarmTime = Calendar.getInstance(Locale.getDefault());
        alarmTime.set(Calendar.HOUR_OF_DAY, instance.hour);
        alarmTime.set(Calendar.MINUTE, instance.minutes);
        String contentText = CommonUtils.getFormattedTimeWithDay(context, alarmTime);
        String channelId = "WobbleUpcomingNotif";

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.upcoming_alarm))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.tab_alarm)
                .setGroup(String.valueOf(instance.id))
                .setGroupSummary(true)
                .setSound(null)
                .addAction(CommonUtils.isLOrLater() ? R.drawable.ic_dismiss : R.drawable.ic_action_unchecked, context.getString(R.string.dismiss_now), actionIntent)
                .setColor(ThemeDetails.getThemeAccent(tNumber))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(false)
                .setLocalOnly(true)
                .build();

        if (CommonUtils.isOOrLater()) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel mNotificationChannel = CommonUtils.createNotificationChannel(channelId, "WobbleUpcoming");
                mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mNotificationChannel.setSound(null, null);
                manager.createNotificationChannel(mNotificationChannel);
                manager.notify(instance.hashCode(), notification);
            }
        } else {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(instance.hashCode(), notification);
        }
    }

    public static void cancelAlarmNotification(Context context, int hashCode) {

        try {
            (NotificationManagerCompat.from(context)).cancel(hashCode);
        } catch (Exception ignored) {}
    }

    public static void cancelNotification(Context context) {
        try {
            ((NotificationManagerCompat.from(context))).cancel(R.layout.timer);
            (NotificationManagerCompat.from(context)).cancel(R.layout.stopwatch);
        } catch (Exception ignored) {}
    }

}
