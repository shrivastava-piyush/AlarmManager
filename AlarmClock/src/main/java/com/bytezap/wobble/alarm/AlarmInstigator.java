/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class AlarmInstigator extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String intentAction = intent.getAction();
        if (intentAction != null) {

            switch (intentAction) {
                case Intent.ACTION_BOOT_COMPLETED:
                    AlarmAssistant.rescheduleInstancesAfterBoot(context);
                    Log.v("AlarmInstigator", intentAction + ": AlarmInsigator updated the alarms");
                    break;

                case Intent.ACTION_TIME_CHANGED:
                case Intent.ACTION_TIMEZONE_CHANGED:
                case Intent.ACTION_LOCALE_CHANGED:
                case Intent.ACTION_MY_PACKAGE_REPLACED:
                    AlarmAssistant.rescheduleInstances(context);
                    Log.v("AlarmInstigator", intentAction + ": AlarmInsigator updated the alarms");
                    break;

                case AlarmAssistant.INDICATOR_ACTION:
                    Log.v("AlarmInstigator", intentAction + ": isAlarmActive: " + String.valueOf(AlarmScreen.isAlarmActive));
                    break;
            }
        }
    }
}
