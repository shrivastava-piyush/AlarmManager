package com.bytezap.wobble.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.List;

public class CalendarContentResolver {
    public static final String[] FIELDS = {
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.VISIBLE
    };

    public static final Uri CALENDAR_URI = Uri.parse("content://com.android.calendar/calendars");

    ContentResolver contentResolver;
    List<String> calendars = new ArrayList<String>();

    public  CalendarContentResolver(Context ctx) {
        contentResolver = ctx.getContentResolver();
    }

    public List<String> getCalendars() {
        // Fetch a list of all calendars sync'd with the device and their display names
        Cursor cursor = contentResolver.query(CALENDAR_URI, FIELDS, null, null, null);

        try {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.NAME));
                    String displayName = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME));
                    String color = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR));
                    Boolean selected = !cursor.getString(3).equals("0");
                    calendars.add(displayName);
                }
                cursor.close();
            }
        } catch (AssertionError ex) {
            if (cursor != null) {
                cursor.close();
            }
        }

        return calendars;
    }
}