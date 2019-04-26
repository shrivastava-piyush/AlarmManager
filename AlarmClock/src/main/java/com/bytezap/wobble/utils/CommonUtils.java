/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.alarm.AlarmAssistant;
import com.bytezap.wobble.alarm.AlarmScreen;
import com.bytezap.wobble.database.AlarmInstance;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class CommonUtils {

    private static String langCode;
    protected static Locale defaultLocale;

    static {
        langCode = "def";
        defaultLocale = Locale.getDefault();
    }

    /**
     * @param context
     * @param selectedAlarm
     * @return
     */
    public static String getAlarmDeletionMessage(Context context, int selectedAlarm) {
        if (selectedAlarm == 1) {
            return context.getString(R.string.alarm_delete_singular);
        } else {
            return context.getString(R.string.alarm_delete_plural);
        }
    }

    public static String getFormattedTimeWithDay(Context context, Calendar alarm) {
        String format = DateFormat.is24HourFormat(context) ? is17OrLater() ? "E H:mm" : "E k:mm" : "E h:mm a";
        return (String) DateFormat.format(format, alarm);
    }

    public static String getFormattedTime(Context context, Calendar alarm) {
        String format = DateFormat.is24HourFormat(context) ? is17OrLater() ? "H:mm" : "k:mm" : "h:mm a";
        return (String) DateFormat.format(format, alarm);
    }

    public static String getFormattedDate(Calendar alarm){
        return (String) DateFormat.format("EEEE, MMMM dd", alarm);
    }

    public static String getFormattedToast(Context context, long timeInMillis) {
        long timeDiff = timeInMillis - System.currentTimeMillis();
        long hours = timeDiff / (1000 * 60 * 60);
        long minutes = timeDiff / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = getDaysSeq(context, days);
        String minSeq = getMinSeq(context, minutes);
        String hourSeq = getHourSeq(context, hours);

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                (dispHour ? 2 : 0) |
                (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_toast_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    public static void showAlarmToast(Context context, long timeInMillis) {
        String toastText = getFormattedToast(context, timeInMillis);
        ToastGaffer.showToast(context, toastText);
    }

    private static String getDaysSeq(Context context, long days) {
        return (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                        context.getString(R.string.days, Long.toString(days));
    }

    private static String getMinSeq(Context context, long minutes) {
        return (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                        context.getString(R.string.minutes, Long.toString(minutes));
    }

    private static String getHourSeq(Context context, long hours) {
        return (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                        context.getString(R.string.hours, Long.toString(hours));
    }

    public static void makeWhiteSnackBar(View view, String instruction){
            Snackbar snackbar = Snackbar.make(view , instruction, Snackbar.LENGTH_LONG);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(Color.WHITE);
            TextView snackText = sbView.findViewById(android.support.design.R.id.snackbar_text);
            if (snackText!=null) {
                snackText.setTextColor(Color.BLACK);
            }
            snackbar.show();
    }

    public static String getUriTitleColumnName(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority()) ? "_display_name" : "title";
    }

    /**
     * @param context
     * @param uri
     * @return
     */
    public static boolean isUriRingtoneValid(Context context, Uri uri) {
        if (uri == null) {
            return false;
        }

        if (uri.getScheme().contentEquals("file")) {
            return new File(uri.getPath()).exists();
        } else if (uri.getScheme().contentEquals("content")) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[]{getUriTitleColumnName(uri)}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.close();
                    return true;
                } else if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                try {
                    Log.e("CommonUtils", "Ringtone uri Exception: " + e.toString());
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable ignored) {}
            }
        }
        return false;
    }

    public static boolean shouldDisableSnooze(int maxSnoozeTime, Calendar currentTime, AlarmInstance instance){
        if (maxSnoozeTime <= 0) {
            return false;
        }
        if (maxSnoozeTime == 1) {
            return true;
        }
        int hour = maxSnoozeTime/60;
        int minutes = maxSnoozeTime - hour*60;
        int finalMinutes = instance.minutes + minutes;
        Calendar maxTime = Calendar.getInstance();
        if (finalMinutes > 60) {
            hour++;
            finalMinutes = (instance.minutes + minutes) % 60;
        }
        int finalHour = instance.hour + hour;
        if (finalHour >= 24) {
            // Date change has occurred
            int currentHour = maxTime.get(Calendar.HOUR_OF_DAY);
            int currentMinute = maxTime.get(Calendar.MINUTE);
            if (currentHour == 0) {
                currentHour = 24;
            }
            return (currentHour*60 + currentMinute) >= (finalHour*60 + finalMinutes);
        }
        maxTime.set(Calendar.HOUR_OF_DAY, finalHour);
        maxTime.set(Calendar.MINUTE, finalMinutes);
        maxTime.set(Calendar.SECOND, 0);
        maxTime.set(Calendar.MILLISECOND, 0);
        return currentTime.getTimeInMillis() >= maxTime.getTimeInMillis();
    }

    public static void setSourceFromResource(Context context, MediaPlayer player, int res)
            throws IOException {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    public static CharSequence getTimeFormat(boolean isFormat24, int amPmFontSize) {
        String pattern;
        if (isFormat24) {
            pattern = CommonUtils.is18OrLater() ? DateFormat.getBestDateTimePattern(Locale.getDefault(), "Hm") : "H:mm";
        } else {
            pattern = CommonUtils.is18OrLater() ? DateFormat.getBestDateTimePattern(Locale.getDefault(), "hma") : "h:mm a";
        }

        // Removing the am/pm if size is zero
        if (amPmFontSize <= 0) {
            pattern = pattern.replaceAll("a", "").trim();
        }
        // Replacing spaces with "Hair Space"
        pattern = pattern.replaceAll(" ", "\u200A");
        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }
        Spannable sp = new SpannableString(pattern);
        sp.setSpan(new StyleSpan(Typeface.NORMAL), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        sp.setSpan(new AbsoluteSizeSpan(amPmFontSize), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        sp.setSpan(new TypefaceSpan("serif"), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        return sp;
    }

    public static String[] getWeekDays(boolean isPortrait) {
        String[] shortWeekdays = new String[7];
            final SimpleDateFormat format = new SimpleDateFormat("EEE", Locale.getDefault());
            long aSunday = new GregorianCalendar(2016, Calendar.FEBRUARY, 14).getTimeInMillis();
            for (int i = 0; i < 7; i++) {
                shortWeekdays[i] = format.format(new Date(aSunday + i * DateUtils.DAY_IN_MILLIS));
                if (isPortrait) {
                    shortWeekdays[i] = shortWeekdays[i].substring(0, 2);
                }
            }
            return shortWeekdays;
        }

    /**
     * @param hour
     * @return
     */
    public static int getFormatted12Hour(int hour) {
        int hr = hour % 12;
        if (hr == 0) {
            return 12;
        }
        return hr;
    }

    public static void launchApp(Context context, String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            try {
                if (doesPackageExist(context, packageName)) {
                    final PackageManager pm = context.getPackageManager();
                    Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                    launchIntent.setAction(Intent.ACTION_MAIN);
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                }
            } catch (Exception e) {
                Log.e("LaunchApp", packageName + " could not be started" + e.toString());
            }
        }
    }

    public static boolean doesPackageExist(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(targetPackage, 0);
        } catch (final Exception e) {
            ai = null;
        }
        return ai != null;
    }

    public static void sendAlarmBroadcast(Context context, long id, String intentAction) {
        if (AlarmScreen.isAlarmActive) {
            Intent dismissIntent = new Intent(intentAction);
            dismissIntent.putExtra(AlarmAssistant.ID, id);
            context.sendBroadcast(dismissIntent);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static NotificationChannel createNotificationChannel(String id, String channelName){
        NotificationChannel channel = new NotificationChannel(id, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setLightColor(Color.WHITE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        return channel;
    }

    //Update date only at midnight
    public static long getDateUpdationTime(){
        long currentTime = System.currentTimeMillis();
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getDefault());
        calendar.setTimeInMillis(currentTime);
        return  ((24 - calendar.get(Calendar.HOUR_OF_DAY)) * 3600 - calendar.get(Calendar.MINUTE) * 60 - calendar.get(Calendar.SECOND) + 1) * 1000;
    }

    public static String getLangCode() {
        return langCode;
    }

    public static void setLangCode(String langCode) {
        CommonUtils.langCode = langCode;
    }

    public static void refreshDefaultLocale(){
        defaultLocale = null;
        defaultLocale = Locale.getDefault();
    }

    //Change locale settings in the app. Application context should be used here
    public static void setLanguage(Resources res, String langCode) {
        //Only change the langCode if needed
        if (langCode.equals("def")) {
            return;
        }

        Locale newLocale = getCurrentLocale();

        try {
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            if (CommonUtils.is17OrLater()) {
                conf.setLocale(newLocale);
            } else {
                conf.locale = newLocale;
            }
            res.updateConfiguration(conf, dm);
            Locale.setDefault(newLocale);
        } catch (Exception e) {
            Log.e("CommonUtils", e.toString());
        }

    }

    public static Intent prepareIntentForAutoStart(Context context) {
        Intent intent;
        intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.parse("package:" + context.getPackageName());
        intent.setData(uri);
        return intent;
    }

    public static Locale getCurrentLocale() {
        if (TextUtils.isEmpty(langCode) || langCode.equals("default") || langCode.equals("def")) {
            return defaultLocale;
        } else {
            // if the language code does also contain a region
            if (langCode.contains("-r") || langCode.contains("-")) {
                // split the language code into language and region
                final String[] language_region = langCode.split("\\-(r)?");
                // construct a new Locale object with the specified language and region
                return new Locale(language_region[0], language_region[1]);
            } else {
                return new Locale(langCode);
            }
        }
    }

    public static boolean isXiaomi() {
        boolean isCustom = false;
        String modelString = Build.MODEL.substring(0, 2);
        if (!TextUtils.isEmpty(Build.MANUFACTURER)
                && Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
            isCustom = true;
        } else if (!TextUtils.isEmpty(modelString)
                && modelString.equalsIgnoreCase("Mi")) {
            isCustom = true;
        } else if (!TextUtils.isEmpty(Build.BRAND)
                && Build.BRAND.equalsIgnoreCase("xiaomi")) {
            isCustom = true;
        }
        return isCustom;
    }

    public static boolean isCustomRom() {
        boolean isCustom = false;
        if (!TextUtils.isEmpty(Build.MANUFACTURER)
                && Build.MANUFACTURER.equalsIgnoreCase("Asus")) {
            isCustom = true;
        } if (!TextUtils.isEmpty(Build.MANUFACTURER)
                && Build.MANUFACTURER.equalsIgnoreCase("Samsung")) {
            isCustom = true;
        } if (!TextUtils.isEmpty(Build.MANUFACTURER)
                && Build.MANUFACTURER.equalsIgnoreCase("Huawei")) {
            isCustom = true;
        }
        return isCustom;
    }

    // get the latest orientation
        public static boolean isPortrait(Resources res) {
        return res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static boolean isOOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static boolean isMOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isLOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isKOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean is18OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean is17OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean is16OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean is15OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
    }
}
