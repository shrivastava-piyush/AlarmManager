/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.utils.CommonUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

class AlarmAdapter extends ArrayAdapter<AlarmObject> {

    private static final int darkEnabled = Color.parseColor("#85000000");
    private static final int darkerEnabled = Color.parseColor("#97000000");
    private static final int darkDisabled = Color.parseColor("#55000000");
    private static final int whiteDisabled = Color.parseColor("#70ffffff");
    private final AlarmInterface listener;
    private List<AlarmObject> mAlarms;
    private SparseBooleanArray mSelectedItemsIds;
    private LayoutInflater inflater;
    private AlarmHolder holder;
    private CharSequence format;
    private boolean isCardDark;

    AlarmAdapter(Context context, boolean isCardDark, AlarmInterface listener, List<AlarmObject> alarms) {
        super(context, isCardDark ? R.layout.alarm_card_dark : R.layout.alarm_card, alarms);
        inflater = LayoutInflater.from(context);
        this.mAlarms = alarms;
        this.listener = listener;
        this.mSelectedItemsIds = new SparseBooleanArray();
        this.isCardDark = isCardDark;
        CommonUtils.setLanguage(context.getResources(), CommonUtils.getLangCode());
    }

    public void setAlarms(List<AlarmObject> list) {
        this.mAlarms = list;
        notifyDataSetChanged();
    }

    void refreshFormat() {
        if (holder !=null && holder.time != null) {
            int amPmSize = Math.round(2 * holder.time.getTextSize() / 3);
            format = CommonUtils.getTimeFormat(DateFormat.is24HourFormat(getContext()), amPmSize);
            notifyDataSetChanged();
        }
    }

    @Override
    public void remove(AlarmObject object) {
        if (mAlarms != null) {
            mAlarms.remove(object);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mAlarms != null ? mAlarms.size() : 0;
    }

    @Override
    public AlarmObject getItem(int position) {
        return mAlarms != null ? mAlarms.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        if (mAlarms != null) {
            return mAlarms.get(position).id;
        }
        return -1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @NonNull
    @Override
    public View getView(int position, View alarmView, @NonNull ViewGroup parent) {

        if (alarmView == null) {
            alarmView = inflater.inflate(isCardDark ? R.layout.alarm_card_dark : R.layout.alarm_card, parent, false);
            setHolder(alarmView);
        } else {
            Object tag = alarmView.getTag();
            if (tag != null) {
                holder = (AlarmHolder) tag;
            } else {
                setHolder(alarmView);
            }
        }

        holder.alarm = getItem(position);
        refreshView(position);
        return alarmView;
    }

    private void setHolder(View view) {
        holder = new AlarmHolder();
        holder.mainLayout = view.findViewById(R.id.alarm_item_layout);
        holder.time = view.findViewById(R.id.alarm_item_time);
        holder.name = view.findViewById(R.id.alarm_item_name);
        holder.alarm_days = new TextView[]{view.findViewById(R.id.alarm_item_sunday), view.findViewById(R.id.alarm_item_monday), view.findViewById(R.id.alarm_item_tuesday),
                view.findViewById(R.id.alarm_item_wednesday), view.findViewById(R.id.alarm_item_thursday), view.findViewById(R.id.alarm_item_friday),
                view.findViewById(R.id.alarm_item_saturday)};
        holder.btnToggle = view.findViewById(R.id.alarm_item_toggle);
        holder.customDays = view.findViewById(R.id.alarm_days_layout);
        holder.specialDays = view.findViewById(R.id.special_days);
        holder.skippedView = view.findViewById(R.id.alarm_item_skipped);
        int amPmSize = Math.round(2 * holder.time.getTextSize() / 3);
        format = CommonUtils.getTimeFormat(DateFormat.is24HourFormat(this.getContext()), amPmSize);
        if (!CommonUtils.is16OrLater()) {
            //Multi-choice mode in listview causes this mess
            holder.time.setBackgroundResource(0);
        }

        holder.time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() != null) {
                    listener.selectTime(v, (int) v.getTag());
                }
            }
        });
        view.setTag(holder);
    }

    private void refreshView(int position) {

        holder.btnToggle.setTag(position);
        holder.time.setTag(position);
        holder.name.setTag(position);

        // This is needed to stop toggleAlarm from being called when views refresh
        holder.btnToggle.setOnCheckedChangeListener(null);
        holder.btnToggle.setChecked(holder.alarm.isEnabled);

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, holder.alarm.hour);
        calendar.set(Calendar.MINUTE, holder.alarm.minutes);
        holder.time.setText(DateFormat.format(format, calendar));
        if (isCardDark) {
            holder.time.setTextColor(holder.alarm.isEnabled ? darkEnabled : darkDisabled);
        } else {
            holder.time.setTextColor(holder.alarm.isEnabled ? Color.WHITE : whiteDisabled);
        }
        if (!TextUtils.isEmpty(holder.alarm.name)) {
            holder.name.setVisibility(View.VISIBLE);
            if (isCardDark) {
                holder.name.setTextColor(holder.alarm.isEnabled ? darkerEnabled : darkDisabled);
            } else {
                holder.name.setTextColor(holder.alarm.isEnabled ? Color.WHITE : whiteDisabled);
            }
            holder.name.setText(holder.alarm.name);
        } else {
            holder.name.setVisibility(View.INVISIBLE);
        }

        //Check repetition schedule of the alarm
        switch (holder.alarm.getAlarmDays()) {
            case 0:
                holder.customDays.setVisibility(View.INVISIBLE);
                holder.specialDays.setVisibility(View.VISIBLE);
                holder.specialDays.setText(R.string.one_time_alarm);
                holder.specialDays.setCompoundDrawablesWithIntrinsicBounds(isCardDark ? R.drawable.ic_one_time_alarm_dark : R.drawable.ic_one_time_alarm, 0, 0, 0);
                holder.specialDays.setCompoundDrawablePadding(2);
                if (isCardDark) {
                    holder.specialDays.setTextColor(holder.alarm.isEnabled ? darkerEnabled : darkDisabled);
                } else {
                    holder.specialDays.setTextColor(holder.alarm.isEnabled ? Color.WHITE : whiteDisabled);
                }
                break;

            case 2:
                if (holder.alarm.getRepeatingDay(AlarmObject.SATURDAY) && holder.alarm.getRepeatingDay(AlarmObject.SUNDAY)) {
                    holder.customDays.setVisibility(View.INVISIBLE);
                    holder.specialDays.setVisibility(View.VISIBLE);
                    holder.specialDays.setText(R.string.alarm_item_weekends);
                    holder.specialDays.setCompoundDrawablesWithIntrinsicBounds(isCardDark ? R.drawable.ic_refresh_dark : R.drawable.ic_refresh, 0, 0, 0);
                    holder.specialDays.setCompoundDrawablePadding(2);
                    if (isCardDark) {
                        holder.specialDays.setTextColor(holder.alarm.isEnabled ? darkerEnabled : darkDisabled);
                    } else {
                        holder.specialDays.setTextColor(holder.alarm.isEnabled ? Color.WHITE : whiteDisabled);
                    }
                } else {
                    setDaysLayout();
                }
                break;

            case 1:
            case 3:
            case 4:
                setDaysLayout();
                break;

            case 5:
                if (holder.alarm.isWeekDays()) {
                    holder.customDays.setVisibility(View.INVISIBLE);
                    holder.specialDays.setVisibility(View.VISIBLE);
                    holder.specialDays.setText(R.string.alarm_item_weekdays);
                    if (isCardDark) {
                        holder.specialDays.setTextColor(holder.alarm.isEnabled ? darkerEnabled : darkDisabled);
                    } else {
                        holder.specialDays.setTextColor(holder.alarm.isEnabled ? Color.WHITE : whiteDisabled);
                    }
                    holder.specialDays.setCompoundDrawablesWithIntrinsicBounds(isCardDark ? R.drawable.ic_refresh_dark : R.drawable.ic_refresh, 0, 0, 0);
                    holder.specialDays.setCompoundDrawablePadding(2);
                } else {
                    setDaysLayout();
                }
                break;

            case 6:
                setDaysLayout();
                break;

            case 7:
                holder.customDays.setVisibility(View.INVISIBLE);
                holder.specialDays.setVisibility(View.VISIBLE);
                holder.specialDays.setText(R.string.alarm_every_day);
                if (isCardDark) {
                    holder.specialDays.setTextColor(holder.alarm.isEnabled ? darkerEnabled : darkDisabled);
                } else {
                    holder.specialDays.setTextColor(holder.alarm.isEnabled ? Color.WHITE : whiteDisabled);
                }
                holder.specialDays.setCompoundDrawablesWithIntrinsicBounds(isCardDark ? R.drawable.ic_refresh_dark : R.drawable.ic_refresh, 0, 0, 0);
                holder.specialDays.setCompoundDrawablePadding(2);
                break;
        }

        holder.skippedView.setTag(holder.alarm.id);
        holder.skippedView.setVisibility(holder.alarm.isSkipped ? View.VISIBLE : View.INVISIBLE);

        if (isCardDark) {
            holder.mainLayout.setBackgroundResource(mSelectedItemsIds.get(position) ? R.drawable.card_selected_white : holder.alarm.isSkipped ? R.drawable.card_selector_skipped : R.drawable.card_selector_white);
        } else {
            holder.mainLayout.setBackgroundResource(mSelectedItemsIds.get(position) ? R.drawable.card_selected : holder.alarm.isSkipped ? R.drawable.card_selector_skipped : R.drawable.card_selector);
        }
        holder.mainLayout.setAlpha(holder.alarm.isEnabled ? 1 : 0.7f);

        holder.btnToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.getTag() != null) {
                    listener.toggleAlarm((int) buttonView.getTag(), isChecked);
                }
            }
        });

    }

    private void setDaysLayout() {
        holder.specialDays.setVisibility(View.INVISIBLE);
        holder.customDays.setVisibility(View.VISIBLE);
        String[] days = CommonUtils.getWeekDays(CommonUtils.isPortrait(getContext().getResources()));
        for (int i = AlarmObject.SUNDAY; i <= AlarmObject.SATURDAY; i++) {
            holder.alarm_days[i].setText(days[i]);
            if (isCardDark) {
                holder.alarm_days[i].setTextColor(holder.alarm.getRepeatingDay(i) ? darkerEnabled : darkDisabled);
            } else {
                holder.alarm_days[i].setTextColor(holder.alarm.getRepeatingDay(i) ? Color.WHITE : whiteDisabled);
            }
        }
    }

    void toggleSelection(int position, boolean checked) {
        selectView(position, checked);
    }

    private void selectView(int position, boolean isChecked) {
        if (isChecked) {
            mSelectedItemsIds.put(position, true);
        } else {
            mSelectedItemsIds.delete(position);
        }
        notifyDataSetChanged();
    }

    int toggleAll() {
        if (mSelectedItemsIds.size() != mAlarms.size()) {
            for (int i = 0; i < mAlarms.size(); i++) {
                mSelectedItemsIds.put(i, true);
            }
        }
        notifyDataSetChanged();
        return getSelectedCount();
    }

    void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    boolean areSomeSelected() {
        return getSelectedCount() != getCount();
    }

    void putIds(SharedPreferences.Editor editor) {
        Gson gson = new Gson();
        String json = gson.toJson(mSelectedItemsIds);
        editor.putString(Clock.CHECKED_COUNT_ALARMS, json);
    }

    void getIds(SharedPreferences preferences) {
        mSelectedItemsIds = new SparseBooleanArray();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SparseBooleanArray.class, new SelectedAlarmsInstanceCreator());
        Gson gson = builder.create();
        String json = preferences.getString(Clock.CHECKED_COUNT_ALARMS, null);
        Type type = new TypeToken<SparseBooleanArray>() {
        }.getType();
        if (json != null) {
            SparseBooleanArray booleanArray = gson.fromJson(json, type);
            for (int i = 0; i <= (booleanArray.size() - 1); i++) {
                mSelectedItemsIds.put(booleanArray.keyAt(i), booleanArray.valueAt(i));
            }
        }

        notifyDataSetChanged();
    }

    long getSelectedAlarmId() {
        if (getSelectedCount() == 1) {
            for (int i = 0; i < getCount(); i++) {
                if (mSelectedItemsIds.valueAt(i)) {
                    AlarmObject alarmObject = getItem(mSelectedItemsIds.keyAt(i));
                    if (alarmObject != null) {
                        return alarmObject.id;
                    }
                }
            }
        }
        return -1;
    }

    AlarmObject getSelectedAlarm() {
        if (getSelectedCount() == 1) {
            for (int i = 0; i < getCount(); i++) {
                if (mSelectedItemsIds.valueAt(i)) {
                    return getItem(mSelectedItemsIds.keyAt(i));
                }
            }
        }
        return null;
    }

    int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    private class AlarmHolder {
        private TextView time;
        private TextView name;
        private TextView[] alarm_days;
        private ToggleButton btnToggle;
        private RelativeLayout mainLayout;
        private LinearLayout customDays;
        private TextView specialDays;
        private TextView skippedView;

        private AlarmObject alarm;
    }

}
