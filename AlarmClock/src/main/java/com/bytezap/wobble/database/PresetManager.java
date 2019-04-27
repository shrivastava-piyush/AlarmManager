/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class PresetManager extends SQLiteOpenHelper implements ClockContract.Preset{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "presets.db";
    private static PresetManager mInstance = null;

    private static final String PRESET_COLUMNS = _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            PRESET_NAME + " TEXT NOT NULL DEFAULT ''," +
            PRESET_HOURS + " INTEGER NOT NULL DEFAULT 5," +
            PRESET_MINUTES + " INTEGER NOT NULL DEFAULT 0," +
            PRESET_SECONDS + " INTEGER NOT NULL DEFAULT 0";
    private static final String[] PRESET_QUERY_COLUMNS = new String[]{
            _ID,
            PRESET_NAME,
            PRESET_HOURS,
            PRESET_MINUTES,
            PRESET_SECONDS
    };

    private static final String GENERATED_PRESET_1 = "('', 0, 5, 0);";
    private static final String GENERATED_PRESET_2 = "('', 0, 10, 0);";

    private PresetManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized PresetManager getInstance(Context context) {
        // Use a single synchronised instance to enforce serialization
        if (mInstance == null) {
            mInstance = new PresetManager(context.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + PRESET_TABLE_NAME + " ("
                + PRESET_COLUMNS + ");");

        String generatedPreset = "INSERT INTO " + PRESET_TABLE_NAME + " (" +
                PRESET_NAME + ", " +
                PRESET_HOURS + ", " +
                PRESET_MINUTES + ", " +
                PRESET_SECONDS + ") VALUES ";
        database.execSQL(generatedPreset + GENERATED_PRESET_1);
        database.execSQL(generatedPreset + GENERATED_PRESET_2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public long createPreset(PresetObject preset) {
        ContentValues values = PresetObject.populateContent(preset);
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long rowId = -1;
        try {
            rowId = db.insert(PRESET_TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("PresetManager", "Preset could not be created: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return rowId;
    }

    public long updatePreset(PresetObject preset) {
        ContentValues values = PresetObject.populateContent(preset);
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long rows = 0;
        try {
            rows = db.update(PRESET_TABLE_NAME, values, _ID + " = ?", new String[]{String.valueOf(preset.id)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("PresetManager", "Preset could not be updated: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    public PresetObject getPresetById(long id) {
        final SQLiteDatabase db = this.getReadableDatabase();
        PresetObject object = null;
        db.beginTransaction();
        try {
            Cursor c = db.query(PRESET_TABLE_NAME, PRESET_QUERY_COLUMNS, _ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);

            try {
                if (c.moveToNext()) {
                    object = new PresetObject(c);
                    db.setTransactionSuccessful();
                }
            } catch (Exception e) {
                Log.e("PresetManager", "Preset could not be retrieved: " + e.toString());
            } finally {
                c.close();
            }
        } finally {
            db.endTransaction();
        }

        return object;
    }

    public List<PresetObject> getPresets() {
        final SQLiteDatabase db = this.getReadableDatabase();
        List<PresetObject> presetList = new LinkedList<>();
        db.beginTransaction();
        try {
            Cursor c = db.query(PRESET_TABLE_NAME, PRESET_QUERY_COLUMNS, null, null, null, null, null);
            try {
                while (c.moveToNext()) {
                    presetList.add(new PresetObject(c));
                }
            } catch (Exception e) {
                Log.e("PresetManager", "Presets could not be retrieved: " + e.toString());
            } finally {
                c.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return presetList;
    }

    public int deletePresetById(long id) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int row = 0;
        try {
            row = db.delete(PRESET_TABLE_NAME, _ID + " = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } catch (Exception e){
            Log.e("PresetManager", "Preset could not be deleted: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return row;
    }
}
