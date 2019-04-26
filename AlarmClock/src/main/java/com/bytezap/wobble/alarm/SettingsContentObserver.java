

package com.bytezap.wobble.alarm;

import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

class SettingsContentObserver extends ContentObserver {

    private int maxVolume;
    private AudioManager audioManager;

    SettingsContentObserver(AudioManager manager, Handler handler) {
        super(handler);
        audioManager = manager;

        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        if (maxVolume != currentVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
        }
    }
}