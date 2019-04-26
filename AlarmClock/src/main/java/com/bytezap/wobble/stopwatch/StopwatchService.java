package com.bytezap.wobble.stopwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.AppWakeLock;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.TtsSpeaker;

public class StopwatchService extends Service {

    private NotificationManagerCompat notificationManager;
    private SharedPreferences sharedPreferences;
    private long lastTime = 0;
    private long timeElapsed = 0;
    private final Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            notifyStopwatchBar(timeElapsed);
            timeElapsed += currentTime - lastTime;
            if (timeElapsed < 0) timeElapsed = 0;
            lastTime = currentTime;
            handler.postDelayed(runnable, 1000);
        }
    };
    private NotificationCompat.Builder notification;
    private boolean isVoice;

    @Override
    public void onCreate() {
        super.onCreate();
        AppWakeLock.acquireCpuWakeLock(getApplicationContext());
        notificationManager = NotificationManagerCompat.from(this);
        sharedPreferences = getSharedPreferences(Clock.STOPWATCH_PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        timeElapsed = intent!=null ? intent.getLongExtra(StopwatchFragment.TIME_MILLI_SEC, 0) : sharedPreferences.getLong(StopwatchFragment.TIME_MILLI_SEC, 0);
        isVoice = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.VOICE, true);
        lastTime = System.currentTimeMillis();
        initializeNotif();
        if (CommonUtils.isMOrLater()) {
            //Make service foreground so that it is not killed in doze mode
            startForeground(R.layout.stopwatch, notification.build());
        }
        handler.removeCallbacks(runnable);
        handler.post(runnable);
        return START_STICKY;
    }

    private void initializeNotif(){
        if (notification==null) {
            Intent launcher = new Intent(getApplicationContext(), Clock.class);
            launcher.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launcher.setAction(Clock.TAB_STOPWATCH);
            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, launcher, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

            String channelId = "WobbleServiceStopwatch";

            notification = new NotificationCompat.Builder(this, channelId)
                    .setAutoCancel(false)
                    .setContentText(getString(R.string.default_stopwatch_notif))
                    .setOngoing(true)
                    .setShowWhen(false)
                    .setColor(ThemeDetails.getThemeAccent(BitmapController.getThemeNumber()))
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_flag)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                    .setContentIntent(contentIntent);

            if (CommonUtils.isOOrLater()) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    NotificationChannel mNotificationChannel = CommonUtils.createNotificationChannel(channelId, "WobbleStopwatch");
                    mNotificationChannel.setVibrationPattern(new long[]{ 0 });
                    mNotificationChannel.enableVibration(true);
                    mNotificationChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
                    mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    manager.createNotificationChannel(mNotificationChannel);
                }
            }
        }
    }

    private void notifyStopwatchBar(long currentTime) {
        sharedPreferences.edit().putLong(StopwatchFragment.TIME_MILLI_SEC, currentTime).apply();
        notification.setContentTitle(getChronoNotifTime(getApplicationContext(), currentTime));
        notificationManager.notify(R.layout.stopwatch, notification.build());
    }

    private String getChronoNotifTime(Context context, long theMillis) {
        long seconds = theMillis / 1000;
        long remainingSec = seconds;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;

        String hourFormatted = context.getString(R.string.hours, hours);
        String minFormatted = context.getString(R.string.minutes, minutes);
        String secondsFormat = context.getString(R.string.seconds, seconds);

        if (remainingSec == 240) {
            if (isVoice && TtsSpeaker.isTtsOff()) {
                TtsSpeaker.init(getApplicationContext());
            }
        }
        if (remainingSec > 10 && (remainingSec%300 == 0)) {
            if (!TtsSpeaker.isSpeaking() && isVoice) {
                String timeLeft;
                String elapsed = getString(R.string.elapsed_time);
                if (hours > 0) {
                    timeLeft = elapsed + " " + hourFormatted + (minutes >=0 ? " " + minFormatted : "");
                } else {
                    timeLeft = elapsed + " " + minFormatted;
                }
                TtsSpeaker.speak(getApplicationContext(), timeLeft, TtsSpeaker.isTtsOff(), false);
            }
        }

        if (hours > 0) {
            return hourFormatted + " " + minFormatted;
        } else if (minutes > 0) {
            return minFormatted + " " + secondsFormat;
        } else {
            return secondsFormat;
        }
    }

    @Override
    public void onDestroy() {
        if (CommonUtils.isMOrLater()) {
            stopForeground(true);
        }
        NotificationProvider.cancelNotification(getApplicationContext());
        handler.removeCallbacks(runnable);
        AppWakeLock.releaseCpuLock();
        super.onDestroy();
    }
}
