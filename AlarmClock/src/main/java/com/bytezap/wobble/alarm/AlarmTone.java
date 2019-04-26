/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.database.MediaProvider;
import com.bytezap.wobble.utils.CommonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class AlarmTone{

    private static boolean isStarted, isLooping;
    private static MediaPlayer tonePlayer;
    private static MediaPlayer nextPlayer;
    private static int alarmUserVolume;
    private static Handler soundHandler;
    private static float soundFactor, appCurrentVolume, timeToDelay;
    private static MediaPlayer.OnCompletionListener completionListener;

    static {
        tonePlayer = null;
        nextPlayer = null;
        isStarted = false;
        isLooping = true;
        appCurrentVolume = 0;
        timeToDelay = 1000;
        completionListener = null;
        soundHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 999:
                        if (isStarted && tonePlayer != null && tonePlayer.isPlaying()) {
                            appCurrentVolume = appCurrentVolume + 0.01f;
                            tonePlayer.setVolume(appCurrentVolume, appCurrentVolume);
                            if (appCurrentVolume < soundFactor) {
                                soundHandler.removeMessages(999); // There should only be one volume adjustment
                                soundHandler.sendEmptyMessageDelayed(999, (long) timeToDelay);
                            }
                        }
                    default:
                        break;
                }
            }
        };
    }

    static void resume(Context context, boolean isVibrate){
         if (isVibrate) {
             startVibration(context);
         }

        if (nextPlayer!=null) {
            nextPlayer.start();
        } else if (tonePlayer!=null) {
            tonePlayer.start();
        }
    }

    public static void pause(Context context){
        if (tonePlayer != null) {
            try {
                tonePlayer.pause();
            } catch (Exception ignored){}
        }

        if(nextPlayer != null) {
            try {
                nextPlayer.pause();
            } catch (Exception ignored) {
            }
        }

        Vibrator vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    public static void terminate(Context context) {

        if (isStarted) {
            isStarted = false;
            isLooping = true;
            // Remove handler messages
            soundHandler.removeMessages(999);
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmUserVolume, 0);
                try {
                    audioManager.setSpeakerphoneOn(false);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            appCurrentVolume = 0;
            soundFactor = 0;
            timeToDelay = 0;
            alarmUserVolume = 0;
            completionListener = null;
            // Stop audio playing
            if (tonePlayer != null) {
                try {
                    tonePlayer.stop();
                } catch (Exception ignored){}
                if (audioManager != null) {
                    audioManager.abandonAudioFocus(null);
                }
                tonePlayer.reset();
                tonePlayer.release();
                tonePlayer = null;
            }

            if(nextPlayer != null){
                try {
                    nextPlayer.stop();
                } catch (Exception ignored){}
                if (audioManager != null) {
                    audioManager.abandonAudioFocus(null);
                }
                nextPlayer.reset();
                nextPlayer.release();
                nextPlayer = null;
            }

            Vibrator vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
            if (vibrator != null) {
                vibrator.cancel();
            }
        }
    }

    public static void start(final Context context, AlarmInstance alarm) {
        // Make sure we are stopped before starting
        terminate(context);

        if (alarm.toneType == AlarmObject.TONE_TYPE_SILENT) {
            if (alarm.isVibrate) {
                startVibration(context);
                isStarted = true;
            }
            return;
        }

        Uri alarmSound = alarm.alarmTone;
        tonePlayer = new MediaPlayer();

        if (alarm.toneType == AlarmObject.TONE_TYPE_SHUFFLE) {
            //User might have revoked permission in M to access storage. There could be many other obscene reasons.
            try{
                final List<Uri> mediaUri = MediaProvider.getAllMediaUri(alarm.uriIds);
                if (mediaUri == null) {
                    alarmSound = MediaProvider.getRandomUri(context);
                    Log.e("Shuffle", "Uri was null");
                } else {
                    final Random random = new Random();
                    alarmSound = mediaUri.get(random.nextInt(mediaUri.size()));
                    isLooping = false;
                    completionListener = new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                if (mp == tonePlayer && nextPlayer != null) {
                                    tonePlayer.release();
                                    tonePlayer = nextPlayer;
                                    tonePlayer = null;
                                }
                                setNextDataSource(context, mediaUri.get(random.nextInt(mediaUri.size())));
                            }
                        };
                    tonePlayer.setOnCompletionListener(completionListener);
                }
            } catch (Exception e){
                e.printStackTrace();
                alarmSound = null;
            }
        }

        // Fall back on the default alarm tone if the database does not have the alarm uri stored
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        tonePlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                terminate(context);
                return true;
            }
        });

        tonePlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                if (!player.isPlaying()) {
                    player.start();
                }
            }
        });

        try {
            if (alarm.toneType == AlarmObject.TONE_TYPE_LOUD_RINGTONE) {
                try {
                    CommonUtils.setSourceFromResource(context, tonePlayer, R.raw.countdown_alarm);
                } catch (IOException e) {
                    e.printStackTrace();
                    tonePlayer.setDataSource(context, alarmSound);
                }
            } else {
                tonePlayer.setDataSource(context, alarmSound);
            }
            startAlarm(context, tonePlayer);
        } catch (Exception ex) {
            if (alarm.toneType == AlarmObject.TONE_TYPE_SHUFFLE) {
                try {
                    tonePlayer.setDataSource(context, alarmSound);
                    startAlarm(context, tonePlayer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // It could be possible that there is no default ringtone on the device. So use the fallback one
                try {
                    // Reset the media player to clear error state
                    tonePlayer.reset();
                    CommonUtils.setSourceFromResource(context, tonePlayer, R.raw.countdown_alarm);
                    startAlarm(context, tonePlayer);
                } catch (Exception ex2) {
                    // At this point just don't play anything
                }
            }
        }

        if (alarm.isVibrate) {
            startVibration(context);
        }
        isStarted = true;
    }

    private static void startAlarm(Context context, MediaPlayer player) throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        SharedPreferences defaultSettings = PreferenceManager.getDefaultSharedPreferences(context);
        // Store phone alarm sound set by user in settings
        alarmUserVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

        // Retrieve max and current volume from app to set on music player
        int alarmMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        float userAppVolume = (float) defaultSettings.getInt(SettingsActivity.ALARM_VOLUME, 100);
        boolean isCrescendo = Integer.parseInt(defaultSettings.getString(SettingsActivity.INCREASING_VOLUME, "30"))!=0;
        // Calculate crescendo timing if not off
        timeToDelay = userAppVolume!=0 ? Integer.parseInt(defaultSettings.getString(SettingsActivity.INCREASING_VOLUME, "30")) * (1000/userAppVolume) : 0;

        if (userAppVolume == 0) {
            terminate(context);
            return;
        }
        // Calculate sound set by user in app
        soundFactor = userAppVolume / 100.0f;

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmMaxVolume, 0);

        if (isHeadsetPlugged(audioManager, context)) {
            audioManager.setSpeakerphoneOn(true);
        }
        if (CommonUtils.isLOrLater()) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
        player.setLooping(isLooping);
        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        if (isCrescendo && timeToDelay!=0) {
            player.setVolume(0.01f, 0.01f);
        } else {
            player.setVolume(soundFactor, soundFactor);
        }
        player.prepare();
        audioManager.requestAudioFocus(null,
                AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        player.start();

        if (isCrescendo) {
            soundHandler.sendEmptyMessageDelayed(999, (long) timeToDelay);
        }
    }

    private static void startVibration(Context context){
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = new long[]{700, 700};
            if (CommonUtils.isLOrLater()) {
                if (vibrator != null) {
                    vibrator.vibrate(pattern, 0, new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }
            } else {
                if (vibrator != null) {
                    vibrator.vibrate(pattern, 0);
                }
            }
    }

    private static boolean isHeadsetPlugged(AudioManager manager, Context context){
        try {
            if (manager.isWiredHeadsetOn() || manager.isBluetoothA2dpOn()) {
                return true;
            }
            Intent registerReceiver = context.registerReceiver(null, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            return registerReceiver != null && registerReceiver.getIntExtra("state", 0) == 1;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void setNextDataSource(Context context, Uri uri) {
        if (nextPlayer != null) {
            nextPlayer.release();
            nextPlayer = null;
        }
        if (uri != null) {
            nextPlayer = new MediaPlayer();
            nextPlayer.setAudioSessionId(tonePlayer.getAudioSessionId());
            try {
                nextPlayer.reset();
                nextPlayer.setOnPreparedListener(null);
                nextPlayer.setDataSource(context, uri);
                nextPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                nextPlayer.setVolume(soundFactor, soundFactor);
                nextPlayer.setLooping(false);
                nextPlayer.prepare();
                nextPlayer.setOnCompletionListener(completionListener);
                nextPlayer.start();
            } catch (Exception e) {
                 e.printStackTrace();
                if (nextPlayer != null) {
                    nextPlayer.release();
                    nextPlayer = null;
                }
                isLooping = true;
                try {
                    if (tonePlayer!=null) {
                        tonePlayer.reset();
                    } else {
                        tonePlayer = new MediaPlayer();
                    }
                    Uri randomUri = MediaProvider.getRandomUri(context);
                    if (randomUri != null) {
                        tonePlayer.setDataSource(context, randomUri);
                        tonePlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                        tonePlayer.setVolume(soundFactor, soundFactor);
                        tonePlayer.setLooping(false);
                        tonePlayer.setOnCompletionListener(completionListener);
                        tonePlayer.prepare();
                        tonePlayer.start();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            try {
                if (tonePlayer!=null) {
                    tonePlayer.reset();
                } else {
                    tonePlayer = new MediaPlayer();
                }
                Uri randomUri = MediaProvider.getRandomUri(context);
                if (randomUri != null) {
                    tonePlayer.setDataSource(context, randomUri);
                    tonePlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    tonePlayer.setVolume(soundFactor, soundFactor);
                    tonePlayer.setLooping(false);
                    tonePlayer.setOnCompletionListener(completionListener);
                    tonePlayer.prepare();
                    tonePlayer.start();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    static boolean isToneNotRunning(){
        return !isStarted;
    }
}
