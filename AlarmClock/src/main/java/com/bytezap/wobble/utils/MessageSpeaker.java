package com.bytezap.wobble.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bytezap.wobble.preference.SettingsActivity;

public class MessageSpeaker{

    private static MediaPlayer messagePlayer;
    private static boolean isSpeaking;
    private static int streamVolume;

    static {
        messagePlayer = null;
        isSpeaking = false;
    }

    public static boolean isSpeaking() {
        return isSpeaking;
    }

    public static void stopMessage(Context context) {

        if (isSpeaking) {
            isSpeaking = false;
            // Stop audio playing
            if (messagePlayer != null) {
                try {
                    messagePlayer.stop();
                } catch (Exception ignored) {
                }
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamVolume, 0);
                audioManager.abandonAudioFocus(null);
                streamVolume = 0;
                messagePlayer.reset();
                messagePlayer.release();
                messagePlayer = null;
            }
        }
    }

    public static MediaPlayer sayMessage(final Context context, String path, boolean isLastTime) {
        stopMessage(context);

        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        try {
            messagePlayer = new MediaPlayer();
            if (path != null) {
                messagePlayer.setDataSource(path);
            }

            if (isLastTime) {
                messagePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopMessage(context);
                    }
                });
                messagePlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        stopMessage(context);
                        return true;
                    }
                });
            }

            streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            float userAppVolume = Math.round(PreferenceManager.getDefaultSharedPreferences(context).getInt(SettingsActivity.VOICE_ALERT_VOLUME, 80)/100.0f);
            messagePlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            messagePlayer.setLooping(false);
            messagePlayer.setVolume(userAppVolume, userAppVolume);
            messagePlayer.prepare();
            audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            messagePlayer.start();
            isSpeaking = true;
        } catch (Exception ex) {
            Log.v("MessageSpeaker", "Could not say message: " + ex.toString());
        }

        return messagePlayer;
    }

}
