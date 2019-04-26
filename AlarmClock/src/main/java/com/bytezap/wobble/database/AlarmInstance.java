/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.Calendar;

public class AlarmInstance implements Parcelable, ClockContract.Instance {

    // Alarm states
    public static final int ALARM_STATE_FRESHLY_STARTED = 0; // When alarm has been set or updated by the user specifically
    public static final int ALARM_STATE_DISMISSED_WITH_NO_CHECK = 1; // When alarm is dismissed with no wake up check, but can be called in future, if repetition is on
    public static final int ALARM_STATE_TRIGGERED = 2; // When alarm is fired by the service
    public static final int ALARM_STATE_SNOOZED = 3; // When alarm is in snoozed condition
    public static final int ALARM_STATE_DISMISSED_WITH_CHECK = 4; // When wakeup check has been instantiated for the alarm
    public static final int ALARM_STATE_SKIPPED = 5; // When alarm is skipped
    public static final int ALARM_STATE_PRE_DISMISS = 6; // When alarm is dismissed from upcoming notification or InteractionHandler

    public static final Creator<AlarmInstance> CREATOR = new Creator<AlarmInstance>() {
        @Override
        public AlarmInstance createFromParcel(Parcel in) {
            return new AlarmInstance(in);
        }

        @Override
        public AlarmInstance[] newArray(int size) {
            return new AlarmInstance[size];
        }
    };

    // Alarm fields
    public long id = -1;
    public int hour;
    public int minutes;
    public int date;
    public int month;
    public int year;
    public Uri alarmTone;
    public int toneType = 1;
    public long[] uriIds = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public String name = "";
    public String vocalMessage = ""; // Can be the location of the message or the message itself
    public int vocalMessageType;
    public int vocalMessagePlace = 0;
    public boolean isVibrate = false;
    public int dismissLevel = 0;
    public int snoozeLevel = 0;
    public int mathDismissProb = 1;
    public int mathSnoozeProb = 1;
    public boolean dismissSkip = true;
    public boolean snoozeSkip = true;
    public int dismissMethod = 0;
    public boolean wakeupCheck;
    public int snoozeMethod = 0;
    public int snoozeTimes = 0;
    public int snoozeTimeIndex = 0;
    public int dismissShake = 30;
    public int snoozeShake = 20;
    public boolean isLaunchApp;
    public String launchAppPkg = "";
    public String barcodeText = "";
    public String imagePath = "";
    public int alarmState;
    private boolean repeatingDays[] = {false, false, false, false, false, false, false};

    public AlarmInstance() {
        repeatingDays = new boolean[7];
    }

    public AlarmInstance(int hourOfDay, int min) {
        hour = hourOfDay;
        minutes = min;
        alarmState = AlarmInstance.ALARM_STATE_FRESHLY_STARTED;
        dismissMethod = AlarmObject.DISMISS_METHOD_MATH;
        dismissLevel = 0;
        dismissSkip = true;
        snoozeMethod = 0;
        name = "";
        toneType = AlarmObject.TONE_TYPE_RINGTONE;
        snoozeTimes = 0;
        snoozeTimeIndex = 0;
        wakeupCheck = true;
        repeatingDays = new boolean[7];
    }

    public AlarmInstance(AlarmObject cachedAlarm) {
        this.id = cachedAlarm.id;
        this.hour = cachedAlarm.hour;
        this.minutes = cachedAlarm.minutes;
        Calendar calendar = Calendar.getInstance();
        this.date = calendar.get(Calendar.DATE);
        this.month = calendar.get(Calendar.MONTH);
        this.year = calendar.get(Calendar.YEAR);
        this.repeatingDays = cachedAlarm.getRepeatingDays();
        this.alarmTone = cachedAlarm.alarmTone;
        this.uriIds = cachedAlarm.uriIds;
        this.toneType = cachedAlarm.toneType;
        this.name = cachedAlarm.name;
        this.vocalMessage = cachedAlarm.vocalMessage;
        this.vocalMessageType = cachedAlarm.vocalMessageType;
        this.vocalMessagePlace = cachedAlarm.vocalMessagePlace;
        this.isVibrate = cachedAlarm.isVibrate;
        this.dismissMethod = cachedAlarm.dismissMethod;
        this.dismissLevel = cachedAlarm.dismissLevel;
        this.snoozeLevel = cachedAlarm.snoozeLevel;
        this.dismissSkip = cachedAlarm.dismissSkip;
        this.snoozeSkip = cachedAlarm.snoozeSkip;
        this.mathDismissProb = cachedAlarm.mathDismissProb;
        this.mathSnoozeProb = cachedAlarm.mathSnoozeProb;
        this.wakeupCheck = cachedAlarm.wakeupCheck;
        this.snoozeMethod = cachedAlarm.snoozeMethod;
        this.snoozeTimeIndex = cachedAlarm.snoozeTimeIndex;
        this.dismissShake = cachedAlarm.dismissShake;
        this.snoozeShake = cachedAlarm.snoozeShake;
        this.isLaunchApp = cachedAlarm.isLaunchApp;
        this.launchAppPkg = cachedAlarm.launchAppPkg;
        this.barcodeText = cachedAlarm.barcodeText;
        this.imagePath = cachedAlarm.imagePath;
        this.alarmState = ALARM_STATE_FRESHLY_STARTED;
    }

    protected AlarmInstance(Parcel in) {
        id = in.readLong();
        hour = in.readInt();
        minutes = in.readInt();
        date = in.readInt();
        month = in.readInt();
        year = in.readInt();
        repeatingDays = in.createBooleanArray();
        alarmTone = in.readParcelable(Uri.class.getClassLoader());
        uriIds = in.createLongArray();
        toneType = in.readInt();
        name = in.readString();
        vocalMessage = in.readString();
        vocalMessageType = in.readInt();
        vocalMessagePlace = in.readInt();
        isVibrate = in.readInt() != 0;
        dismissMethod = in.readInt();
        dismissLevel = in.readInt();
        snoozeLevel = in.readInt();
        dismissSkip = in.readInt() != 0;
        snoozeSkip = in.readInt() != 0;
        mathDismissProb = in.readInt();
        mathSnoozeProb = in.readInt();
        wakeupCheck = in.readInt() != 0;
        snoozeMethod = in.readInt();
        snoozeTimes = in.readInt();
        snoozeTimeIndex = in.readInt();
        dismissShake = in.readInt();
        snoozeShake = in.readInt();
        isLaunchApp = in.readInt() != 0;
        launchAppPkg = in.readString();
        barcodeText = in.readString();
        imagePath = in.readString();
        alarmState = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(hour);
        dest.writeInt(minutes);
        dest.writeInt(date);
        dest.writeInt(month);
        dest.writeInt(year);
        dest.writeBooleanArray(repeatingDays);
        dest.writeParcelable(alarmTone, flags);
        dest.writeLongArray(uriIds);
        dest.writeInt(toneType);
        dest.writeString(name);
        dest.writeString(vocalMessage);
        dest.writeInt(vocalMessageType);
        dest.writeInt(vocalMessagePlace);
        dest.writeInt(isVibrate ? 1 : 0);
        dest.writeInt(dismissMethod);
        dest.writeInt(dismissLevel);
        dest.writeInt(snoozeLevel);
        dest.writeInt(dismissSkip ? 1 : 0);
        dest.writeInt(snoozeSkip ? 1 : 0);
        dest.writeInt(mathDismissProb);
        dest.writeInt(mathSnoozeProb);
        dest.writeInt(wakeupCheck ? 1 : 0);
        dest.writeInt(snoozeMethod);
        dest.writeInt(snoozeTimes);
        dest.writeInt(snoozeTimeIndex);
        dest.writeInt(dismissShake);
        dest.writeInt(snoozeShake);
        dest.writeInt(isLaunchApp ? 1 : 0);
        dest.writeString(launchAppPkg);
        dest.writeString(barcodeText);
        dest.writeString(imagePath);
        dest.writeInt(alarmState);
    }

    public AlarmInstance(Cursor c) {
        id = c.getLong(c.getColumnIndex(INSTANCE_ID));
        name = c.getString(c.getColumnIndex(INSTANCE_NAME));
        vocalMessage = c.getString(c.getColumnIndex(INSTANCE_VOCAL_MESSAGE));
        vocalMessageType = c.getInt(c.getColumnIndex(INSTANCE_VOCAL_MESSAGE_TYPE));
        vocalMessagePlace = c.getInt(c.getColumnIndex(INSTANCE_VOCAL_MESSAGE_PLACE));
        hour = c.getInt(c.getColumnIndex(INSTANCE_TIME_HOUR));
        minutes = c.getInt(c.getColumnIndex(INSTANCE_TIME_MINUTE));
        date = c.getInt(c.getColumnIndex(INSTANCE_TIME_DATE));
        month = c.getInt(c.getColumnIndex(INSTANCE_TIME_MONTH));
        year = c.getInt(c.getColumnIndex(INSTANCE_TIME_YEAR));
        alarmTone = !c.getString(c.getColumnIndex(INSTANCE_TONE)).equals("") ? Uri.parse(c.getString(c.getColumnIndex(INSTANCE_TONE))) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        toneType = c.getInt(c.getColumnIndex(INSTANCE_TONE_TYPE));
        isVibrate = c.getInt(c.getColumnIndex(INSTANCE_VIBRATION)) != 0;
        dismissMethod = c.getInt(c.getColumnIndex(INSTANCE_DISMISS_METHOD));
        snoozeMethod = c.getInt(c.getColumnIndex(INSTANCE_SNOOZE_PROBLEM));
        snoozeTimes = c.getInt(c.getColumnIndex(INSTANCE_SNOOZE_TIMES));
        snoozeTimeIndex = c.getInt(c.getColumnIndex(INSTANCE_SNOOZE_TIME_INDEX));
        dismissLevel = c.getInt(c.getColumnIndex(INSTANCE_DISMISS_LEVEL));
        snoozeLevel = c.getInt(c.getColumnIndex(INSTANCE_SNOOZE_LEVEL));
        mathDismissProb = c.getInt(c.getColumnIndex(INSTANCE_MATH_DISMISS_NUMBER));
        mathSnoozeProb = c.getInt(c.getColumnIndex(INSTANCE_MATH_SNOOZE_NUMBER));
        dismissSkip = c.getInt(c.getColumnIndex(INSTANCE_DISMISS_SKIP)) != 0;
        snoozeSkip = c.getInt(c.getColumnIndex(INSTANCE_SNOOZE_SKIP)) != 0;
        dismissShake = c.getInt(c.getColumnIndex(INSTANCE_DISMISS_SHAKE));
        snoozeShake = c.getInt(c.getColumnIndex(INSTANCE_SNOOZE_SHAKE));
        wakeupCheck = c.getInt(c.getColumnIndex(INSTANCE_CHECK)) != 0;
        isLaunchApp = c.getInt(c.getColumnIndex(INSTANCE_IS_LAUNCH_APP)) != 0;
        launchAppPkg = c.getString(c.getColumnIndex(INSTANCE_LAUNCH_APP_PACKAGE));
        barcodeText = c.getString(c.getColumnIndex(INSTANCE_BARCODE_TEXT));
        imagePath = c.getString(c.getColumnIndex(INSTANCE_IMAGE_PATH));
        alarmState = c.getInt(c.getColumnIndex(INSTANCE_STATE));

        String[] repeatingDays = c.getString(c.getColumnIndex(INSTANCE_REPEAT_DAYS)).split(",");
        for (int i = 0; i < repeatingDays.length; ++i) {
            setRepeatingDay(i, !repeatingDays[i].equals("false"));
        }

        if (toneType == AlarmObject.TONE_TYPE_SHUFFLE) {
            String[] uriIds = c.getString(c.getColumnIndex(INSTANCE_URI_IDS)).split(",");
            this.uriIds = new long[uriIds.length];
            for (int i = 0; i < uriIds.length; ++i) {
                this.uriIds[i] = Long.parseLong(uriIds[i]);
            }
        }
    }

    static ContentValues populateInstanceFromAlarm(AlarmObject object) {
        ContentValues values = new ContentValues();
        values.put(INSTANCE_ID, object.id);
        values.put(INSTANCE_NAME, object.name);
        values.put(INSTANCE_VOCAL_MESSAGE, object.vocalMessage);
        values.put(INSTANCE_VOCAL_MESSAGE_TYPE, object.vocalMessageType);
        values.put(INSTANCE_VOCAL_MESSAGE_PLACE, object.vocalMessagePlace);
        values.put(INSTANCE_TIME_HOUR, object.hour);
        values.put(INSTANCE_TIME_MINUTE, object.minutes);
        Calendar calendar = Calendar.getInstance();
        values.put(INSTANCE_TIME_DATE, calendar.get(Calendar.DATE));
        values.put(INSTANCE_TIME_MONTH, calendar.get(Calendar.MONTH));
        values.put(INSTANCE_TIME_YEAR, calendar.get(Calendar.YEAR));
        values.put(INSTANCE_TONE, object.alarmTone != null ? object.alarmTone.toString() : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        values.put(INSTANCE_TONE_TYPE, object.toneType);
        values.put(INSTANCE_VIBRATION, object.isVibrate);
        values.put(INSTANCE_DISMISS_METHOD, object.dismissMethod);
        values.put(INSTANCE_SNOOZE_PROBLEM, object.snoozeMethod);
        values.put(INSTANCE_SNOOZE_TIMES, 0);
        values.put(INSTANCE_SNOOZE_TIME_INDEX, object.snoozeTimeIndex);
        values.put(INSTANCE_DISMISS_LEVEL, object.dismissLevel);
        values.put(INSTANCE_SNOOZE_LEVEL, object.snoozeLevel);
        values.put(INSTANCE_DISMISS_SKIP, object.dismissSkip);
        values.put(INSTANCE_SNOOZE_SKIP, object.snoozeSkip);
        values.put(INSTANCE_MATH_DISMISS_NUMBER, object.mathDismissProb);
        values.put(INSTANCE_MATH_SNOOZE_NUMBER, object.mathSnoozeProb);
        values.put(INSTANCE_DISMISS_SHAKE, object.dismissShake);
        values.put(INSTANCE_SNOOZE_SHAKE, object.snoozeShake);
        values.put(INSTANCE_CHECK, object.wakeupCheck);
        values.put(INSTANCE_IS_LAUNCH_APP, object.isLaunchApp);
        values.put(INSTANCE_LAUNCH_APP_PACKAGE, object.launchAppPkg);
        values.put(INSTANCE_BARCODE_TEXT, object.barcodeText);
        values.put(INSTANCE_IMAGE_PATH, object.imagePath);
        values.put(INSTANCE_STATE, ALARM_STATE_FRESHLY_STARTED);

        String repeatingDays = "";
        for (int i = 0; i < 7; ++i) {
            repeatingDays += object.getRepeatingDay(i) + ",";
        }

        String ids = "";
        if (object.toneType == AlarmObject.TONE_TYPE_SHUFFLE) {
            for (int i = 0; i < 20; ++i) {
                ids += object.uriIds[i] + ",";
            }
        }

        values.put(INSTANCE_REPEAT_DAYS, repeatingDays);
        values.put(INSTANCE_URI_IDS, ids);

        return values;
    }

    static ContentValues populateInstance(AlarmInstance instance) {
        ContentValues values = new ContentValues();
        values.put(INSTANCE_NAME, instance.name);
        values.put(INSTANCE_VOCAL_MESSAGE, instance.vocalMessage);
        values.put(INSTANCE_VOCAL_MESSAGE_TYPE, instance.vocalMessageType);
        values.put(INSTANCE_VOCAL_MESSAGE_PLACE, instance.vocalMessagePlace);
        values.put(INSTANCE_TIME_HOUR, instance.hour);
        values.put(INSTANCE_TIME_MINUTE, instance.minutes);
        Calendar calendar = Calendar.getInstance();
        values.put(INSTANCE_TIME_DATE, calendar.get(Calendar.DATE));
        values.put(INSTANCE_TIME_MONTH, calendar.get(Calendar.MONTH));
        values.put(INSTANCE_TIME_YEAR, calendar.get(Calendar.YEAR));
        values.put(INSTANCE_TONE, instance.alarmTone != null ? instance.alarmTone.toString() : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        values.put(INSTANCE_TONE_TYPE, instance.toneType);
        values.put(INSTANCE_VIBRATION, instance.isVibrate);
        values.put(INSTANCE_DISMISS_METHOD, instance.dismissMethod);
        values.put(INSTANCE_SNOOZE_PROBLEM, instance.snoozeMethod);
        values.put(INSTANCE_SNOOZE_TIMES, instance.snoozeTimes);
        values.put(INSTANCE_SNOOZE_TIME_INDEX, instance.snoozeTimeIndex);
        values.put(INSTANCE_DISMISS_LEVEL, instance.dismissLevel);
        values.put(INSTANCE_SNOOZE_LEVEL, instance.snoozeLevel);
        values.put(INSTANCE_DISMISS_SKIP, instance.dismissSkip);
        values.put(INSTANCE_SNOOZE_SKIP, instance.snoozeSkip);
        values.put(INSTANCE_MATH_DISMISS_NUMBER, instance.mathDismissProb);
        values.put(INSTANCE_MATH_SNOOZE_NUMBER, instance.mathSnoozeProb);
        values.put(INSTANCE_DISMISS_SHAKE, instance.dismissShake);
        values.put(INSTANCE_SNOOZE_SHAKE, instance.snoozeShake);
        values.put(INSTANCE_CHECK, instance.wakeupCheck);
        values.put(INSTANCE_IS_LAUNCH_APP, instance.isLaunchApp);
        values.put(INSTANCE_LAUNCH_APP_PACKAGE, instance.launchAppPkg);
        values.put(INSTANCE_BARCODE_TEXT, instance.barcodeText);
        values.put(INSTANCE_IMAGE_PATH, instance.imagePath);
        values.put(INSTANCE_STATE, instance.alarmState);

        String repeatingDays = "";
        for (int i = 0; i < 7; ++i) {
            repeatingDays += instance.getRepeatingDay(i) + ",";
        }

        String ids = "";
        if (instance.toneType == AlarmObject.TONE_TYPE_SHUFFLE) {
            for (int i = 0; i < 20; ++i) {
                ids += instance.uriIds[i] + ",";
            }
        }

        values.put(INSTANCE_REPEAT_DAYS, repeatingDays);
        values.put(INSTANCE_URI_IDS, ids);

        return values;
    }

    private void setRepeatingDay(int dayOfWeek, boolean value) {
        repeatingDays[dayOfWeek] = value;
    }

    public void setRepeatingDays(boolean value) {
        repeatingDays = new boolean[]{value, value, value, value, value, value, value};
    }

    public boolean getRepeatingDay(int dayOfWeek) {
        return repeatingDays[dayOfWeek];
    }

    public boolean isOneTimeAlarm() {

        for (int i = AlarmObject.SUNDAY; i <= AlarmObject.SATURDAY; i++) {
            if (getRepeatingDay(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllDaySet() {

        int days = 0;
        for (int i = AlarmObject.SUNDAY; i <= AlarmObject.SATURDAY; i++) {
            if (repeatingDays[i]) {
                days++;
            }
        }
        return days == 7;
    }

    public int getAlarmDays() {
        int days = 0;
        for (int i = AlarmObject.SUNDAY; i <= AlarmObject.SATURDAY; i++) {
            if (repeatingDays[i]) {
                days++;
            }
        }
        return days;
    }

    public boolean isWeekDays() {
        for (int i = AlarmObject.MONDAY; i <= AlarmObject.FRIDAY; i++) {
            if (!repeatingDays[i]) {
                return false;
            }
        }
        return true;
    }

    // Closest time to next alarm is figured out
    public Calendar getNextAlarmTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (!isOneTimeAlarm()) {
            //Find next time to set
            Calendar currentCal = Calendar.getInstance();
            int currentDay = currentCal.get(Calendar.DAY_OF_WEEK);
            int currentHour = currentCal.get(Calendar.HOUR_OF_DAY);
            int currentMinute = currentCal.get(Calendar.MINUTE);
            boolean alarmSet = false;
            int days = 0;

            //First check if the alarm is after the day today
            for (int dayOfWeek = currentDay; dayOfWeek <= Calendar.SATURDAY; dayOfWeek++) {
                if (getRepeatingDay(dayOfWeek - 1) && !(dayOfWeek == currentDay && (hour < currentHour || hour == currentHour && minutes <= currentMinute))) {
                    alarmSet = true;
                    break;
                } else {
                    days++;
                }
            }

            //Else check if it's earlier in the week
            if (!alarmSet) {
                for (int dayOfWeek = Calendar.SUNDAY; dayOfWeek <= currentDay; dayOfWeek++) {
                    if (getRepeatingDay(dayOfWeek - 1)) {
                        break;
                    } else {
                        days++;
                    }
                }
            }

            if (days > 0) {
                calendar.add(Calendar.DAY_OF_WEEK, days);
            }

        } else {
            Calendar calendarNew = Calendar.getInstance();
            if (calendar.before(calendarNew)) {
                calendar.add(Calendar.DAY_OF_WEEK, 1);
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minutes);

        return calendar;
    }

    // Calculate current alarm's time
    public Calendar getAlarmTime() {
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minutes);
        alarmTime.set(Calendar.DATE, date);
        alarmTime.set(Calendar.MONTH, month);
        alarmTime.set(Calendar.YEAR, year);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);

        return alarmTime;
    }

    // Calculate last alarm's time
    public Calendar getLastAlarmTime() {
        Calendar lastTime = Calendar.getInstance();
        lastTime.set(Calendar.HOUR_OF_DAY, hour);
        lastTime.set(Calendar.MINUTE, minutes);
        lastTime.set(Calendar.SECOND, 0);
        lastTime.set(Calendar.MILLISECOND, 0);

        int days = getPreviousDays(lastTime);
        if (days > 0) {
            Log.d("days", String.valueOf(days));
            lastTime.add(Calendar.DAY_OF_WEEK, -days);
        }

        return lastTime;
    }

    public void setAlarmTime(Calendar time){
        this.hour = time.get(Calendar.HOUR_OF_DAY);
        this.minutes = time.get(Calendar.MINUTE);
    }

    private int getPreviousDays(Calendar currentTime){
        if (isOneTimeAlarm()) {
            return -1;
        }

        int totalIndex = 0; //No day is set initially. Range is 0-127
        for (int i = 0; i < 7; i++) {
            int dayIndex = (i+6)%7;
            if (repeatingDays[dayIndex]) {
                totalIndex |= 1<<dayIndex;
            }
        }
        int days = 0;
        int currentDay = (currentTime.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (; days >= -7; days--) {
            if (isDayEnabled(currentDay, totalIndex)) {
                break;
            }
        }
        return days * -1;
    }

    private boolean isDayEnabled(int dayIndex, int totalIndex){
        return ((totalIndex & (1 << dayIndex)) > 0);
    }

    public boolean isWithin2Hours(){
        return getNextAlarmTime().getTimeInMillis() - System.currentTimeMillis() <= 2 * DateUtils.HOUR_IN_MILLIS;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getStateName(int state){
        switch (state){
            case 0:
                return "FRESHLY_STARTED";
            case 1:
                return "DISMISSED_WITH_NO_CHECK";
            case 2:
                return "TRIGGERED";
            case 3:
                return "SNOOZED";
            case 4:
                return "DISMISSED_WITH_CHECK";
            case 5:
                return "COMPLETELY_OFF";
            case 6:
                return "SKIPPED";
            case 7:
                return "PRE-DISMISSED";
            default:
                return "Invalid state";
        }
    }

    @Override
    public String toString() {
        return "id: " + id +
                "name: " + name +
                getStateName(alarmState);
    }
}
