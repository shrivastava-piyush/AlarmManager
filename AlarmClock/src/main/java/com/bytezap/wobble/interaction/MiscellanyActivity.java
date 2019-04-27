/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.interaction;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.alarm.AlarmAssistant;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MiscellanyActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String SELECTED_ALARMS = "selected_alarms";

    private List<AlarmInstance> selectedAlarms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.miscellany_layout);

        overridePendingTransition(R.anim.fade_in, 0);

        getWindow().setBackgroundDrawableResource(R.drawable.faded_bg);

        final Intent intent = getIntent();
        final Parcelable[] alarmsFromIntent = intent.getParcelableArrayExtra(SELECTED_ALARMS);

        final Button dismiss = findViewById(R.id.miscellany_dismiss_all);
        final Button cancel = findViewById(R.id.miscellany_cancel);

        //Use this hack currently to obtain text size
        int size = Math.round(2 * dismiss.getTextSize() / 3);
        CharSequence format = CommonUtils.getTimeFormat(DateFormat.is24HourFormat(getApplicationContext()), size);

        final CustomListView listView = findViewById(R.id.miscellany_listview);
        MiscellanyAdapter adapter = new MiscellanyAdapter(getApplicationContext(), R.layout.miscellany_item);
        listView.setAdapter(adapter);

        for (Parcelable parcelable :
                alarmsFromIntent) {
            final AlarmInstance instance = (AlarmInstance) parcelable;
            selectedAlarms.add(instance);
            adapter.add(new MiscellanyAlarm(getTimeString(format, instance), getRepeatString(getApplicationContext(), instance)));
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AlarmInstance instance = selectedAlarms.get((int) id);
                new DismissAlarmTask(getApplicationContext(), instance).execute();
                finish();
            }
        });
        dismiss.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.miscellany_dismiss_all:
                new DismissAllTask(getApplicationContext(), selectedAlarms).execute();
                finish();
                break;

            case R.id.miscellany_cancel:
                finish();
                break;
        }
    }

    private String getTimeString(CharSequence format, AlarmInstance instance){
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, instance.hour);
        calendar.set(Calendar.MINUTE, instance.minutes);
        return DateFormat.format(format, calendar).toString();
    }

    private String getRepeatString(Context context, AlarmInstance instance){
        switch (instance.getAlarmDays()){
            case 0:
                return getString(R.string.one_time_alarm);

            case 2:
                if (instance.getRepeatingDay(AlarmObject.SATURDAY) && instance.getRepeatingDay(AlarmObject.SUNDAY)) {
                    return getString(R.string.alarm_item_weekends);
                } else {
                    return getRepeatDays(CommonUtils.getWeekDays(CommonUtils.isPortrait(getResources())));
                }

            case 1:
            case 3:
            case 4:
                return getRepeatDays(CommonUtils.getWeekDays(CommonUtils.isPortrait(getResources())));

            case 5:
                if (instance.isWeekDays()) {
                    return getString(R.string.alarm_item_weekdays);
                } else {
                    return getRepeatDays(CommonUtils.getWeekDays(CommonUtils.isPortrait(getResources())));
                }

            case 6:
                return getRepeatDays(CommonUtils.getWeekDays(CommonUtils.isPortrait(getResources())));

            case 7:
                return context.getString(R.string.alarm_every_day);

            default:
                return "";

        }
    }

    private String getRepeatDays(String[] days) {
        String repeatDays = "";
        for (String day : days) {
            repeatDays =  repeatDays + " " + day;
        }
        return repeatDays;
    }

    private static class DismissAllTask extends AsyncTask<Void, Void, Void> {

        private Context context;
        private List<AlarmInstance> alarmInstances;

        public DismissAllTask(Context c, List<AlarmInstance> objects) {
            this.context = c;
            this.alarmInstances = objects;
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (AlarmInstance instance : alarmInstances) {
                AlarmAssistant.dismissAlarm(context, instance);
            }
            if(!BitmapController.isAppNotRunning()){
                context.sendBroadcast(new Intent(Clock.REFRESH_LIST));
            }
            AlarmAssistant.updateClosestAlarm(context, null);
            return null;
        }
    }

    private static class DismissAlarmTask extends AsyncTask<Void, Void, Void> {

        private Context context;
        private AlarmInstance instance;

        public DismissAlarmTask(Context c, AlarmInstance alarmInstance) {
            this.context = c;
            this.instance = alarmInstance;
        }

        @Override
        protected Void doInBackground(Void... params) {
            AlarmAssistant.dismissAlarm(context, instance);
            if(!BitmapController.isAppNotRunning()){
                context.sendBroadcast(new Intent(Clock.REFRESH_LIST));
            }
            AlarmAssistant.updateClosestAlarm(context, null);
            return null;
        }
    }

}
