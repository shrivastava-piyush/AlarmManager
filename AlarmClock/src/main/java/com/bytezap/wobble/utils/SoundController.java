package com.bytezap.wobble.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseIntArray;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;

public class SoundController {

    public static final int SOUND_LAP_TIME = 1;
    public static final int SOUND_RESET = 2;
    public static final int SOUND_START = 3;
    public static final int SOUND_STOP = 4;
    public static final int SOUND_TICK = 5;
    private static SoundController mSoundControllerInstance;

    // Start, stop, reset indicators
    private Context mContext;
    private SoundPool soundPool;
    private final SparseIntArray soundPoolArray;

    // Timer alert
    private boolean mPlaying = false;
    private MediaPlayer alertPlayer;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private int alarmTimerVolume;

    private SoundController(Context cxt) {
        this.mContext = cxt;

        if (CommonUtils.isLOrLater()) {
            createSoundPoolWithBuilder();
        } else {
            createSoundPoolWithConstructor();
        }

        soundPoolArray = new SparseIntArray();
        soundPoolArray.put(SOUND_LAP_TIME, soundPool.load(mContext, R.raw.lap, 1));
        soundPoolArray.put(SOUND_RESET, soundPool.load(mContext, R.raw.reset, 1));
        soundPoolArray.put(SOUND_START, soundPool.load(mContext, R.raw.start, 1));
        soundPoolArray.put(SOUND_STOP, soundPool.load(mContext, R.raw.stop, 1));
        soundPoolArray.put(SOUND_TICK, soundPool.load(mContext, R.raw.tick, 1));

        mTelephonyManager =
                (TelephonyManager) cxt.getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String ignored) {
                if (state != TelephonyManager.CALL_STATE_IDLE) {
                    terminate();
                }
            }
        };
        mTelephonyManager.listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public synchronized static SoundController getInstance(Context context) {
        if (mSoundControllerInstance == null) {
            mSoundControllerInstance = new SoundController(context);
        }
        return mSoundControllerInstance;
    }

    @TargetApi(21)
    protected void createSoundPoolWithBuilder() {
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            soundPool = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(3).build();
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private void createSoundPoolWithConstructor() {
        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 100);
    }

    public void playSound(int soundId) {
        soundPool.play(soundPoolArray.get(soundId), 1.0f,
                1.0f, 1, 0, 1f);
    }

    // Play timer sound
    public void activateAlert() {

        if (mPlaying) {
            return;
        }

        alertPlayer = new MediaPlayer();
        alertPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mp.stop();
                mp.release();
                alertPlayer = null;
                return true;
            }
        });

        Uri uri = null;
        SharedPreferences preferences = mContext.getSharedPreferences(Clock.TIMER_PREF, Context.MODE_PRIVATE);
        String uriString = preferences.getString(Clock.TIMER_TONE, "");
        if (!TextUtils.isEmpty(uriString)) {
            uri = Uri.parse(uriString);
        }

        try {
            // Check the phone state to see if the user is currently in a call
            if (mTelephonyManager.getCallState()
                    != TelephonyManager.CALL_STATE_IDLE) {
                alertPlayer.setVolume(0, 0); // Set volume to zero when call is ongoing
                setDataSourceFromResource(mContext.getResources(), alertPlayer,
                        R.raw.countdown_alarm);
            } else if (uri != null) {
                alertPlayer.setDataSource(mContext, uri);
            } else {
                AssetFileDescriptor afd = mContext.getAssets().openFd("sounds/countdown_alarm.ogg");
                alertPlayer.setDataSource(
                        afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            }
            startTimerAlarm(alertPlayer);
        } catch (Exception ex) {
            try {
                // Reset the media player to clear the error state.
                alertPlayer.reset();
                setDataSourceFromResource(mContext.getResources(), alertPlayer,
                        R.raw.countdown_alarm);
                startTimerAlarm(alertPlayer);
            } catch (Exception ex2) {
                // At this point just don't play anything.
            }
        }

        mPlaying = true;
    }

    private void startTimerAlarm(MediaPlayer player)
            throws java.io.IOException, IllegalArgumentException,
            IllegalStateException {
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        alarmTimerVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        float appTimerVolume = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(SettingsActivity.TIMER_VOLUME, 100) / 100.0f;

        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        player.setLooping(true);
        player.setVolume(appTimerVolume, appTimerVolume);
        player.prepare();
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        audioManager.requestAudioFocus(
                null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        player.start();
    }

    //Stop the timer alarm
    public void terminate() {
        if (mPlaying) {
            mPlaying = false;
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            alarmTimerVolume = 0;
            // Stop audio playing
            if (alertPlayer != null) {
                alertPlayer.stop();
                final AudioManager audioManager =
                        (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmTimerVolume, 0);
                audioManager.abandonAudioFocus(null);
                alertPlayer.reset();
                alertPlayer.release();
                alertPlayer = null;
            }
        }
    }

    private void setDataSourceFromResource(Resources resources,
                                           MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    public void tick() {
        soundPool.play(soundPoolArray.get(SOUND_TICK), 1.0f,
                1.0f, 1, 0, 1f);
    }

    public boolean isPlaying() {
        return mPlaying;
    }
}

