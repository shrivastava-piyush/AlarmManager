package com.bytezap.wobble.utils;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class AppWakeLock {

    private static PowerManager.WakeLock cpuWakeLock;
    private static final String TAG = "AlarmClockWakelock";
    
    private static PowerManager.WakeLock createPartialWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    public static void acquireCpuWakeLock(Context context) {
        if (cpuWakeLock != null && cpuWakeLock.isHeld()) {
            return;
        }
        cpuWakeLock = createPartialWakeLock(context);
        cpuWakeLock.setReferenceCounted(true);
        cpuWakeLock.acquire();
    }

    public static void releaseCpuLock() {
        if (cpuWakeLock != null) {
            try{
                cpuWakeLock.release();
            } catch (Exception e) {
                Log.e(TAG, "Wakelock not released: " + e.getMessage());
            }
            cpuWakeLock = null;
        }
    }
}
