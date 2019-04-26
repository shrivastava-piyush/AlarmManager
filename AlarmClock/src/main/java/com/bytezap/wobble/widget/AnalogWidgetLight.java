/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;

public class AnalogWidgetLight extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.layout_widget_light);

            views.setOnClickPendingIntent(R.id.appwidget_light,
                    PendingIntent.getActivity(context, 0,
                        new Intent(context, Clock.class).setAction(Clock.TAB_CLOCK), 0));

            int[] appWidgetIds = intent.getIntArrayExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS);

            AppWidgetManager gm = AppWidgetManager.getInstance(context);
            gm.updateAppWidget(appWidgetIds, views);
        }
    }
}

