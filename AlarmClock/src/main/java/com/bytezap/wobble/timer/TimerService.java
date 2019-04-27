package com.bytezap.wobble.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.VoiceSettingsActivity;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AppWakeLock;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.TtsSpeaker;

public class TimerService extends Service {

    private NotificationManagerCompat notificationManager;
    private CountDownTimer countDownTimer;
    private SharedPreferences sharedPreferences;
    private NotificationCompat.Builder notification;
    private int totalSec;
    private boolean isVoice;

    @Override
    public void onCreate() {
        super.onCreate();
        AppWakeLock.acquireCpuWakeLock(getApplicationContext());
        sharedPreferences = getSharedPreferences(Clock.TIMER_PREF, Context.MODE_PRIVATE);
        notificationManager = NotificationManagerCompat.from(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        long remainingTime = intent!= null ? intent.getLongExtra(TimerFragment.REMAINING_TIME, 0) : sharedPreferences.getLong(TimerFragment.REMAINING_TIME, 0);
        totalSec = (int) (intent!= null ? intent.getLongExtra(TimerFragment.TOTAL_TIME, 0)/1000 : sharedPreferences.getLong(TimerFragment.TOTAL_TIME, 0)/1000);
        isVoice = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(VoiceSettingsActivity.VOICE, true);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        initializeNotification();
        countDownTimer = new CountDownTimer(remainingTime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                notifyTimerBar(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                notifyTimerBar(0);
                sharedPreferences.edit().putLong(TimerFragment.REMAINING_TIME, 0).apply();

                if (!TimerAlert.isAlertActive) {
                    sendBroadcast(new Intent().setAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    Intent timerFinished = new Intent(getApplicationContext(), TimerAlert.class);
                    timerFinished.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    timerFinished.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(timerFinished);
                }
                if (CommonUtils.isMOrLater()) {
                    stopForeground(true);
                } else {
                    NotificationProvider.cancelNotification(getApplicationContext());
                }
                stopSelf();
            }
        };

        if (CommonUtils.isMOrLater()) {
            //Make service foreground so that it is not killed in doze mode
            startForeground(R.layout.timer, notification.build());
        }
        countDownTimer.start();
        return START_STICKY;
    }

    private void initializeNotification(){
        if (notification == null) {
            Intent launcher = new Intent(this, Clock.class);
            launcher.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launcher.setAction(Clock.TAB_TIMER);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launcher, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

            String channelId = "WobbleServiceTimer";
            String oldTitle = getString(R.string.default_timer_text);
            String newTitle = sharedPreferences.getString(TimerFragment.TIMER_TEXT, oldTitle);

            notification = new NotificationCompat.Builder(this, channelId)
                    .setAutoCancel(false)
                    .setContentText(newTitle.equals(oldTitle) || TextUtils.isEmpty(newTitle) ? getString(R.string.defualt_timer_notif) : newTitle)
                    .setShowWhen(false)
                    .setColor(ThemeDetails.getThemeAccent(BitmapController.getThemeNumber()))
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.tab_timer)
                    .setContentIntent(contentIntent);

            if (CommonUtils.isOOrLater()) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    NotificationChannel mNotificationChannel = CommonUtils.createNotificationChannel(channelId, "WobbleTimer");
                    mNotificationChannel.setVibrationPattern(new long[]{ 0 });
                    mNotificationChannel.enableVibration(true);
                    mNotificationChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
                    mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    manager.createNotificationChannel(mNotificationChannel);
                }
            }
        }
    }

    //Update countdown notification
    private void notifyTimerBar(long millis) {
        notification.setContentTitle(getTimerNotifTime(getApplicationContext(), millis));
        sharedPreferences.edit().putLong(TimerFragment.REMAINING_TIME, millis).apply();
        notificationManager.notify(R.layout.timer, notification.build());
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private String getTimerNotifTime(Context context, long theMillis) {
        long seconds = theMillis / 1000;
        long remainingSec = seconds;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;

        String hourFormatted = context.getString(R.string.hours, hours);
        String hour = getString(R.string.hour);
        String minute = getString(R.string.minute);
        String minFormatted = context.getString(R.string.minutes, minutes);
        String secondsFormat = context.getString(R.string.seconds, seconds);

        if (remainingSec > 10) {
            if (remainingSec == 7*totalSec/8) {
                if (isVoice && TtsSpeaker.isTtsOff()) {
                    TtsSpeaker.init(getApplicationContext());
                }
            }
            if ((remainingSec == totalSec / 4) || (remainingSec == totalSec / 2) || (remainingSec == 3 * totalSec / 4)) {
                if (!TtsSpeaker.isSpeaking() && isVoice) {
                    String timeLeft;
                    String remaining = getString(R.string.remaining);
                    if (hours > 0) {
                        timeLeft = (hours == 1 ? hour : hourFormatted) + " " + minFormatted + " " + remaining;
                    } else if (minutes > 0) {
                        timeLeft = (minutes == 1 ? minute : minFormatted) + " " + (seconds > 0 ? secondsFormat : "") + " " + remaining;
                    } else {
                        timeLeft = secondsFormat + " " + remaining;
                    }
                    TtsSpeaker.speak(getApplicationContext(), timeLeft, TtsSpeaker.isTtsOff(), false);
                }
            }
        }

        if (hours > 0) {
            return (hours == 1 ? hour : hourFormatted) + " " + minFormatted;
        } else if (minutes > 0) {
            return (minutes == 1 ? minute : minFormatted) + " " + secondsFormat;
        } else {
            return secondsFormat;
        }
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (CommonUtils.isMOrLater()) {
            stopForeground(true);
        }
        NotificationProvider.cancelNotification(getApplicationContext());
        AppWakeLock.releaseCpuLock();
        super.onDestroy();
    }
}