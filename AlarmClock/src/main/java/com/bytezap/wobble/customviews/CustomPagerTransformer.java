package com.bytezap.wobble.customviews;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ListView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.utils.CommonUtils;

public class CustomPagerTransformer implements ViewPager.PageTransformer{

    private Context context;

    public CustomPagerTransformer(Context context) {
        this.context = context;
    }

    private static final float MIN_SCALE = 0.90f;

    public void transformPage(View view, float position) {

        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();

        ListView alarmList = view.findViewById(R.id.alarmList);
        View alarmAdd = view.findViewById(R.id.alarm_add_new);
        View emptyView = view.findViewById(R.id.emptyView);

        View clockLayout = view.findViewById(R.id.clock_frame);
        View clockSwitchButton = view.findViewById(R.id.switchButton);
        View clockDigital = view.findViewById(R.id.digital_clock_layout);
        View clockAnalog = view.findViewById(R.id.analog_clock_layout);
        View date = view.findViewById(R.id.date_frame);

        View timer = view.findViewById(R.id.custom_timer);
        ListView timerList = view.findViewById(R.id.presetList);
        View timerText = view.findViewById(R.id.timer_text);
        View timerButtons = view.findViewById(R.id.button_layout);

        View stopwatch = view.findViewById(R.id.stopwatch);
        View digital = view.findViewById(R.id.chrono_digital);
        ListView stopwatchList = view.findViewById(R.id.lapList);
        View stopwatchButtons = view.findViewById(R.id.stopButtonLayout);

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(0);

        } else if (position <= 1) { // [-1,1]

            if (CommonUtils.isPortrait(context.getResources())) {

                if (alarmList != null) {

                    for (int i = alarmList.getChildCount() - 1; i >= 0; i--) {
                        View child = alarmList.getChildAt(i);
                        child.setTranslationX((position) * (pageWidth / (alarmList.getChildCount() - i) + 1) * 2);
                    }
                    alarmAdd.setTranslationX((position) * (pageWidth));
                    emptyView.setTranslationX((position) * (pageWidth / 2));
                } else if (clockLayout != null) {

                    clockLayout.setTranslationX((position) * (pageWidth / 2));
                    clockDigital.setTranslationX((position) * (pageWidth));
                    clockAnalog.setTranslationX((position) * (pageWidth));
                    clockSwitchButton.setTranslationX((position) * (pageWidth / 8));
                    date.setTranslationX((position) * (pageWidth)/2);
                } else if (timer != null) {

                    timerText.setTranslationX((position) * (pageWidth / 12));
                    timer.setTranslationX((position) * (pageWidth / 6));
                    for (int i = 0; i < timerList.getChildCount(); i++) {
                        View child = timerList.getChildAt(i);
                        child.setTranslationX((position) * (pageWidth / (i + 1) * 2));
                    }
                    timerButtons.setTranslationX((position) * (pageWidth));
                } else if (stopwatch != null) {

                    digital.setTranslationX((position) * (pageWidth / 12));
                    stopwatch.setTranslationX((position) * (pageWidth / 6));

                    for (int i = 0; i < stopwatchList.getChildCount(); i++) {
                        View child = stopwatchList.getChildAt(i);
                        child.setTranslationX((position) * (pageWidth / (i + 1) * 2));
                    }
                    stopwatchButtons.setTranslationX((position) * (pageWidth));
                }

            } else {
                if (alarmList != null) {

                    for (int i = alarmList.getChildCount() - 1; i >= 0; i--) {
                        View child = alarmList.getChildAt(i);
                        child.setTranslationX((position) * (pageWidth / ((alarmList.getChildCount() - i) + 1)));
                    }
                    alarmAdd.setTranslationX((position) * (pageWidth));
                    emptyView.setTranslationX((position) * (pageWidth / 4));
                } else if (clockLayout != null) {

                    clockLayout.setTranslationX((position) * (pageWidth / 4));
                    clockDigital.setTranslationX((position) * (pageWidth)/3);
                    clockAnalog.setTranslationX((position) * (pageWidth)/3);
                    clockSwitchButton.setTranslationX((position) * (pageWidth / 10));
                    date.setTranslationX((position) * (pageWidth)/4);
                } else if (timer != null) {

                    timerText.setTranslationX((position) * (pageWidth / 12));
                    timer.setTranslationX((position) * (pageWidth / 8));
                    for (int i = 0; i < timerList.getChildCount(); i++) {
                        View child = timerList.getChildAt(i);
                        child.setTranslationX((position) * (pageWidth / ((i + 1) * 4)));
                    }
                    //timerButtons.setTranslationX((position) * (pageWidth / 2));
                } else if (stopwatch != null) {

                    digital.setTranslationX((position) * (pageWidth / 12));
                    stopwatch.setTranslationX((position) * (pageWidth / 8));
                    for (int i = 0; i < stopwatchList.getChildCount(); i++) {
                        View child = stopwatchList.getChildAt(i);
                        child.setTranslationX((position) * (pageWidth / ((i + 1) * 4)));
                    }
                    //stopwatchButtons.setTranslationX((position) * (pageWidth / 2));
                }
            }

            view.setAlpha(1);

        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(0);
        }
    }
}
