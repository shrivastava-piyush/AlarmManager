package com.bytezap.wobble.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import java.util.Calendar;

public class AlarmObject implements Parcelable, ClockContract.Alarm {

    // Place for vocal message
    public static final int ALARM_VOCAL_NOT_CALLED = 0;
    public static final int ALARM_VOCAL_AFTER_TIME = 1;
    public static final int ALARM_VOCAL_BEFORE_TIME = 2;
    public static final int ALARM_VOCAL_AFTER_DISMISS = 3;
    public static final int ALARM_VOCAL_AFTER_SNOOZE = 4;
    public static final int ALARM_VOCAL_REPLACE_TIME = 5;

    // Type of vocal message
    public static final int VOCAL_TYPE_AUDIO = 0;
    public static final int VOCAL_TYPE_TEXT = 1;

    // Type of tone
    public static final int TONE_TYPE_SILENT = 0;
    public static final int TONE_TYPE_RINGTONE = 1;
    public static final int TONE_TYPE_LOUD_RINGTONE = 2;
    public static final int TONE_TYPE_MUSIC = 3;
    public static final int TONE_TYPE_SHUFFLE = 4;

    // Repeating Days
    public static final int SUNDAY = 0;
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;

    //Dismiss methods
    public static final int DISMISS_METHOD_DEFAULT = 0;
    public static final int DISMISS_METHOD_MATH = 1;
    //public static final int DISMISS_METHOD_PICTURE = 2;
    public static final int DISMISS_METHOD_BARCODE = 2;
    public static final int DISMISS_METHOD_SHAKE = 3;

    //Dismiss methods
    public static final int SNOOZE_METHOD_DEFAULT = 0;
    public static final int SNOOZE_METHOD_MATH = 1;
    public static final int SNOOZE_METHOD_SHAKE = 2;

    public static final Creator<AlarmObject> CREATOR = new Creator<AlarmObject>() {
        @Override
        public AlarmObject createFromParcel(Parcel in) {
            return new AlarmObject(in);
        }

        @Override
        public AlarmObject[] newArray(int size) {
            return new AlarmObject[size];
        }
    };

    // Alarm fields
    public long id = -1;
    public int hour;
    public int minutes;
    public Uri alarmTone;
    public int toneType = 1;
    public long[] uriIds = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public String name = "";
    public String vocalMessage = ""; // Can be the location of the message or the message itself
    public int vocalMessageType;
    public int vocalMessagePlace = 0;
    public boolean isEnabled;
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
    public int snoozeTimeIndex = 0;
    public int dismissShake = 30;
    public int snoozeShake = 20;
    public boolean isLaunchApp;
    public String launchAppPkg = "";
    public String barcodeText = "";
    public String imagePath = "";
    public boolean isSkipped;
    private boolean repeatingDays[] = {false, false, false, false, false, false, false};

    public AlarmObject() {
        repeatingDays = new boolean[7];
    }

    public AlarmObject(AlarmObject cachedAlarm) {
        this.id = cachedAlarm.id;
        this.hour = cachedAlarm.hour;
        this.minutes = cachedAlarm.minutes;
        this.repeatingDays = cachedAlarm.repeatingDays;
        this.alarmTone = cachedAlarm.alarmTone;
        this.uriIds = cachedAlarm.uriIds;
        this.toneType = cachedAlarm.toneType;
        this.name = cachedAlarm.name;
        this.vocalMessage = cachedAlarm.vocalMessage;
        this.vocalMessageType = cachedAlarm.vocalMessageType;
        this.vocalMessagePlace = cachedAlarm.vocalMessagePlace;
        this.isEnabled = cachedAlarm.isEnabled;
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
    }

    public AlarmObject(int hourOfDay, int min) {
        hour = hourOfDay;
        minutes = min;
        isEnabled = true;
        dismissMethod = DISMISS_METHOD_MATH;
        dismissLevel = 0;
        dismissSkip = true;
        snoozeMethod = 0;
        name = "";
        toneType = TONE_TYPE_RINGTONE;
        snoozeTimeIndex = 0;
        wakeupCheck = true;
        repeatingDays = new boolean[7];
    }

    protected AlarmObject(Parcel in) {
        id = in.readLong();
        hour = in.readInt();
        minutes = in.readInt();
        repeatingDays = in.createBooleanArray();
        alarmTone = in.readParcelable(Uri.class.getClassLoader());
        uriIds = in.createLongArray();
        toneType = in.readInt();
        name = in.readString();
        vocalMessage = in.readString();
        vocalMessageType = in.readInt();
        vocalMessagePlace = in.readInt();
        isEnabled = in.readInt() != 0;
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
        snoozeTimeIndex = in.readInt();
        dismissShake = in.readInt();
        snoozeShake = in.readInt();
        isLaunchApp = in.readInt() != 0;
        launchAppPkg = in.readString();
        barcodeText = in.readString();
        imagePath = in.readString();
        isSkipped = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(hour);
        dest.writeInt(minutes);
        dest.writeBooleanArray(repeatingDays);
        dest.writeParcelable(alarmTone, flags);
        dest.writeLongArray(uriIds);
        dest.writeInt(toneType);
        dest.writeString(name);
        dest.writeString(vocalMessage);
        dest.writeInt(vocalMessageType);
        dest.writeInt(vocalMessagePlace);
        dest.writeInt(isEnabled ? 1 : 0);
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
        dest.writeInt(snoozeTimeIndex);
        dest.writeInt(dismissShake);
        dest.writeInt(snoozeShake);
        dest.writeInt(isLaunchApp ? 1 : 0);
        dest.writeString(launchAppPkg);
        dest.writeString(barcodeText);
        dest.writeString(imagePath);
        dest.writeInt(isSkipped ? 1 : 0);
    }

    public AlarmObject(Cursor c) {
        id = c.getLong(c.getColumnIndex(_ID));
        name = c.getString(c.getColumnIndex(ALARM_NAME));
        vocalMessage = c.getString(c.getColumnIndex(ALARM_VOCAL_MESSAGE));
        vocalMessageType = c.getInt(c.getColumnIndex(VOCAL_MESSAGE_TYPE));
        vocalMessagePlace = c.getInt(c.getColumnIndex(VOCAL_MESSAGE_PLACE));
        hour = c.getInt(c.getColumnIndex(ALARM_TIME_HOUR));
        minutes = c.getInt(c.getColumnIndex(ALARM_TIME_MINUTE));
        alarmTone = !c.getString(c.getColumnIndex(ALARM_TONE)).equals("") ? Uri.parse(c.getString(c.getColumnIndex(ClockContract.Alarm.ALARM_TONE))) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        toneType = c.getInt(c.getColumnIndex(ALARM_TONE_TYPE));
        isVibrate = c.getInt(c.getColumnIndex(ALARM_VIBRATION)) != 0;
        isEnabled = c.getInt(c.getColumnIndex(ALARM_ENABLED)) != 0;
        dismissMethod = c.getInt(c.getColumnIndex(ALARM_DISMISS_METHOD));
        snoozeMethod = c.getInt(c.getColumnIndex(ALARM_SNOOZE_PROBLEM));
        snoozeTimeIndex = c.getInt(c.getColumnIndex(ALARM_SNOOZE_TIME_INDEX));
        dismissLevel = c.getInt(c.getColumnIndex(ALARM_DISMISS_LEVEL));
        snoozeLevel = c.getInt(c.getColumnIndex(ALARM_SNOOZE_LEVEL));
        mathDismissProb = c.getInt(c.getColumnIndex(ALARM_MATH_DISMISS_NUMBER));
        mathSnoozeProb = c.getInt(c.getColumnIndex(ALARM_MATH_SNOOZE_NUMBER));
        dismissSkip = c.getInt(c.getColumnIndex(ALARM_DISMISS_SKIP)) != 0;
        snoozeSkip = c.getInt(c.getColumnIndex(ALARM_SNOOZE_SKIP)) != 0;
        dismissShake = c.getInt(c.getColumnIndex(ALARM_DISMISS_SHAKE));
        snoozeShake = c.getInt(c.getColumnIndex(ALARM_SNOOZE_SHAKE));
        wakeupCheck = c.getInt(c.getColumnIndex(ALARM_CHECK)) != 0;
        isLaunchApp = c.getInt(c.getColumnIndex(ALARM_IS_LAUNCH_APP)) != 0;
        launchAppPkg = c.getString(c.getColumnIndex(ALARM_LAUNCH_APP_PACKAGE));
        barcodeText = c.getString(c.getColumnIndex(ALARM_BARCODE_TEXT));
        imagePath = c.getString(c.getColumnIndex(ALARM_IMAGE_PATH));
        isSkipped = c.getInt(c.getColumnIndex(ALARM_IS_SKIPPED)) != 0;

        String[] repeatingDays = c.getString(c.getColumnIndex(ClockContract.Alarm.ALARM_REPEAT_DAYS)).split(",");
        for (int i = 0; i < repeatingDays.length; ++i) {
            setRepeatingDay(i, !repeatingDays[i].equals("false"));
        }

        if (toneType == TONE_TYPE_SHUFFLE) {
            String[] uriIds = c.getString(c.getColumnIndex(ClockContract.Alarm.ALARM_URI_IDS)).split(",");
            this.uriIds = new long[uriIds.length];
            for (int i = 0; i < uriIds.length; ++i) {
                this.uriIds[i] = Long.parseLong(uriIds[i]);
            }
        }
    }

    static ContentValues populateContent(AlarmObject alarm) {
        ContentValues values = new ContentValues();
        values.put(ALARM_NAME, alarm.name);
        values.put(ALARM_VOCAL_MESSAGE, alarm.vocalMessage);
        values.put(VOCAL_MESSAGE_TYPE, alarm.vocalMessageType);
        values.put(VOCAL_MESSAGE_PLACE, alarm.vocalMessagePlace);
        values.put(ALARM_TIME_HOUR, alarm.hour);
        values.put(ALARM_TIME_MINUTE, alarm.minutes);
        values.put(ALARM_TONE, alarm.alarmTone != null ? alarm.alarmTone.toString() : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        values.put(ALARM_TONE_TYPE, alarm.toneType);
        values.put(ALARM_VIBRATION, alarm.isVibrate);
        values.put(ALARM_ENABLED, alarm.isEnabled);
        values.put(ALARM_DISMISS_METHOD, alarm.dismissMethod);
        values.put(ALARM_SNOOZE_PROBLEM, alarm.snoozeMethod);
        values.put(ALARM_SNOOZE_TIME_INDEX, alarm.snoozeTimeIndex);
        values.put(ALARM_DISMISS_LEVEL, alarm.dismissLevel);
        values.put(ALARM_SNOOZE_LEVEL, alarm.snoozeLevel);
        values.put(ALARM_DISMISS_SKIP, alarm.dismissSkip);
        values.put(ALARM_SNOOZE_SKIP, alarm.snoozeSkip);
        values.put(ALARM_MATH_DISMISS_NUMBER, alarm.mathDismissProb);
        values.put(ALARM_MATH_SNOOZE_NUMBER, alarm.mathSnoozeProb);
        values.put(ALARM_DISMISS_SHAKE, alarm.dismissShake);
        values.put(ALARM_SNOOZE_SHAKE, alarm.snoozeShake);
        values.put(ALARM_CHECK, alarm.wakeupCheck);
        values.put(ALARM_IS_LAUNCH_APP, alarm.isLaunchApp);
        values.put(ALARM_LAUNCH_APP_PACKAGE, alarm.launchAppPkg);
        values.put(ALARM_BARCODE_TEXT, alarm.barcodeText);
        values.put(ALARM_IMAGE_PATH, alarm.imagePath);
        values.put(ALARM_IS_SKIPPED, alarm.isSkipped);

        StringBuilder repeatingDays = new StringBuilder();
        for (int i = 0; i < 7; ++i) {
            repeatingDays.append(alarm.getRepeatingDay(i)).append(",");
        }

        StringBuilder ids = new StringBuilder();
        if (alarm.toneType == AlarmObject.TONE_TYPE_SHUFFLE) {
            for (int i = 0; i < 20; ++i) {
                ids.append(alarm.uriIds[i]).append(",");
            }
        }

        values.put(ClockContract.Alarm.ALARM_REPEAT_DAYS, repeatingDays.toString());
        values.put(ClockContract.Alarm.ALARM_URI_IDS, ids.toString());

        return values;
    }

    public void setRepeatingDay(int dayOfWeek, boolean value) {
        repeatingDays[dayOfWeek] = value;
    }

    public void setRepeatingDays(boolean value) {
        repeatingDays = new boolean[]{value, value, value, value, value, value, value};
    }

    public boolean getRepeatingDay(int dayOfWeek) {
        return repeatingDays[dayOfWeek];
    }

    public boolean isOneTimeAlarm() {

        for (int i = SUNDAY; i <= SATURDAY; i++) {
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
        for (int i = MONDAY; i <= FRIDAY; i++) {
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

    boolean[] getRepeatingDays(){
        return repeatingDays;
    }

    @Override
    public boolean equals(Object alarm) {
        if (!(alarm instanceof AlarmObject)) {
            return false;
        }

        AlarmObject cachedAlarm = (AlarmObject) alarm;

        // Custom equality check here.
        return ((this.id == cachedAlarm.id) &&
                (this.hour == cachedAlarm.hour) &&
                (this.minutes == cachedAlarm.minutes) &&
                (this.repeatingDays == cachedAlarm.repeatingDays) &&
                (this.alarmTone == cachedAlarm.alarmTone) &&
                (this.uriIds == cachedAlarm.uriIds) &&
                (this.toneType == cachedAlarm.toneType) &&
                (this.name.equals(cachedAlarm.name)) &&
                (this.vocalMessage.equals(cachedAlarm.vocalMessage)) &&
                (this.vocalMessageType == cachedAlarm.vocalMessageType) &&
                (this.vocalMessagePlace == cachedAlarm.vocalMessagePlace) &&
                (this.isEnabled == cachedAlarm.isEnabled) &&
                (this.isVibrate == cachedAlarm.isVibrate) &&
                (this.dismissMethod == cachedAlarm.dismissMethod) &&
                (this.dismissLevel == cachedAlarm.dismissLevel) &&
                (this.snoozeLevel == cachedAlarm.snoozeLevel) &&
                (this.dismissSkip == cachedAlarm.dismissSkip) &&
                (this.snoozeSkip = cachedAlarm.snoozeSkip) &&
                (this.mathDismissProb == cachedAlarm.mathDismissProb) &&
                (this.mathSnoozeProb == cachedAlarm.mathSnoozeProb) &&
                (this.wakeupCheck == cachedAlarm.wakeupCheck) &&
                (this.snoozeMethod == cachedAlarm.snoozeMethod) &&
                (this.snoozeTimeIndex == cachedAlarm.snoozeTimeIndex) &&
                (this.dismissShake == cachedAlarm.dismissShake) &&
                (this.snoozeShake == cachedAlarm.snoozeShake) &&
                (this.isLaunchApp == cachedAlarm.isLaunchApp) &&
                (this.launchAppPkg.equals(cachedAlarm.launchAppPkg)) &&
                (this.barcodeText.equals(cachedAlarm.barcodeText)) &&
                (this.imagePath.equals(cachedAlarm.imagePath)));
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
}
