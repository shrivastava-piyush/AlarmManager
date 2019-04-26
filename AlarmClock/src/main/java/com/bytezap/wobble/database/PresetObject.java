/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.database;

import android.content.ContentValues;
import android.database.Cursor;

public class PresetObject implements ClockContract.Preset{

    public long id = -1;
    public String name;
    public int hours;
    public int minutes;
    public int seconds;

    public PresetObject(String name, int hours, int minutes, int seconds) {
        this.name = name;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    PresetObject(Cursor c) {
        id = c.getLong(c.getColumnIndex(_ID));
        name = c.getString(c.getColumnIndex(PRESET_NAME));
        hours = c.getInt(c.getColumnIndex(PRESET_HOURS));
        minutes = c.getInt(c.getColumnIndex(PRESET_MINUTES));
        seconds = c.getInt(c.getColumnIndex(PRESET_SECONDS));
    }

    static ContentValues populateContent(PresetObject preset) {
        ContentValues values = new ContentValues();
        values.put(PRESET_NAME, preset.name);
        values.put(PRESET_HOURS, preset.hours);
        values.put(PRESET_MINUTES, preset.minutes);
        values.put(PRESET_SECONDS, preset.seconds);
        return values;
    }
}