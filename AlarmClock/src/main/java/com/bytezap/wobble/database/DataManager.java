package com.bytezap.wobble.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.RingtoneManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class DataManager extends SQLiteOpenHelper implements ClockContract.Alarm, ClockContract.Instance {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "alarms.db";

    private static final String ALARM_COLUMNS = _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            ALARM_NAME + " TEXT NOT NULL DEFAULT ''," +
            ALARM_VOCAL_MESSAGE + " TEXT NOT NULL DEFAULT ''," +
            VOCAL_MESSAGE_TYPE + " INTEGER NOT NULL DEFAULT 1," +
            VOCAL_MESSAGE_PLACE + " INTEGER NOT NULL DEFAULT 0," +
            ALARM_TIME_HOUR + " INTEGER NOT NULL," +
            ALARM_TIME_MINUTE + " INTEGER NOT NULL," +
            ALARM_REPEAT_DAYS + " TEXT NOT NULL DEFAULT ''," +
            ALARM_TONE + " TEXT NOT NULL DEFAULT ''," +
            ALARM_TONE_TYPE + " INTEGER NOT NULL," +
            ALARM_URI_IDS + " TEXT NOT NULL DEFAULT ''," +
            ALARM_VIBRATION + " INTEGER NOT NULL," +
            ALARM_ENABLED + " INTEGER NOT NULL," +
            ALARM_DISMISS_METHOD + " INTEGER NOT NULL," +
            ALARM_SNOOZE_PROBLEM + " INTEGER NOT NULL," +
            ALARM_SNOOZE_TIME_INDEX + " INTEGER NOT NULL," +
            ALARM_DISMISS_LEVEL + " INTEGER NOT NULL," +
            ALARM_SNOOZE_LEVEL + " INTEGER NOT NULL," +
            ALARM_DISMISS_SKIP + " INTEGER NOT NULL," +
            ALARM_SNOOZE_SKIP + " INTEGER NOT NULL," +
            ALARM_MATH_DISMISS_NUMBER + " INTEGER NOT NULL," +
            ALARM_MATH_SNOOZE_NUMBER + " INTEGER NOT NULL," +
            ALARM_DISMISS_SHAKE + " INTEGER NOT NULL," +
            ALARM_SNOOZE_SHAKE + " INTEGER NOT NULL," +
            ALARM_CHECK + " INTEGER NOT NULL," +
            ALARM_IS_LAUNCH_APP + " INTEGER NOT NULL," +
            ALARM_LAUNCH_APP_PACKAGE + " TEXT NOT NULL DEFAULT ''," +
            ALARM_BARCODE_TEXT + " TEXT NOT NULL DEFAULT ''," +
            ALARM_IMAGE_PATH + " TEXT NOT NULL DEFAULT ''," +
            ALARM_IS_SKIPPED + " INTEGER NOT NULL DEFAULT 0";

    private static final String INSTANCE_COLUMNS = INSTANCE_ID + " INTEGER NOT NULL," +
            INSTANCE_NAME + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_VOCAL_MESSAGE + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_VOCAL_MESSAGE_TYPE + " INTEGER NOT NULL DEFAULT 1," +
            INSTANCE_VOCAL_MESSAGE_PLACE + " INTEGER NOT NULL DEFAULT 0," +
            INSTANCE_TIME_HOUR + " INTEGER NOT NULL," +
            INSTANCE_TIME_MINUTE + " INTEGER NOT NULL," +
            INSTANCE_TIME_DATE + " INTEGER NOT NULL," +
            INSTANCE_TIME_MONTH + " INTEGER NOT NULL," +
            INSTANCE_TIME_YEAR + " INTEGER NOT NULL," +
            INSTANCE_REPEAT_DAYS + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_TONE + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_TONE_TYPE + " INTEGER NOT NULL," +
            INSTANCE_URI_IDS + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_VIBRATION + " INTEGER NOT NULL," +
            INSTANCE_DISMISS_METHOD + " INTEGER NOT NULL," +
            INSTANCE_SNOOZE_PROBLEM + " INTEGER NOT NULL," +
            INSTANCE_SNOOZE_TIMES + " INTEGER NOT NULL," +
            INSTANCE_SNOOZE_TIME_INDEX + " INTEGER NOT NULL," +
            INSTANCE_DISMISS_LEVEL + " INTEGER NOT NULL," +
            INSTANCE_SNOOZE_LEVEL + " INTEGER NOT NULL," +
            INSTANCE_DISMISS_SKIP + " INTEGER NOT NULL," +
            INSTANCE_SNOOZE_SKIP + " INTEGER NOT NULL," +
            INSTANCE_MATH_DISMISS_NUMBER + " INTEGER NOT NULL," +
            INSTANCE_MATH_SNOOZE_NUMBER + " INTEGER NOT NULL," +
            INSTANCE_DISMISS_SHAKE + " INTEGER NOT NULL," +
            INSTANCE_SNOOZE_SHAKE + " INTEGER NOT NULL," +
            INSTANCE_CHECK + " INTEGER NOT NULL," +
            INSTANCE_IS_LAUNCH_APP + " INTEGER NOT NULL," +
            INSTANCE_LAUNCH_APP_PACKAGE + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_BARCODE_TEXT + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_IMAGE_PATH + " TEXT NOT NULL DEFAULT ''," +
            INSTANCE_STATE + " INTEGER NOT NULL DEFAULT 0";

    private static final String[] ALARM_QUERY_COLUMNS = new String[]{
            _ID,
            ALARM_NAME,
            ALARM_VOCAL_MESSAGE,
            VOCAL_MESSAGE_TYPE,
            VOCAL_MESSAGE_PLACE,
            ALARM_TIME_HOUR,
            ALARM_TIME_MINUTE,
            ALARM_REPEAT_DAYS,
            ALARM_TONE,
            ALARM_URI_IDS,
            ALARM_TONE_TYPE,
            ALARM_VIBRATION,
            ALARM_ENABLED,
            ALARM_DISMISS_METHOD,
            ALARM_DISMISS_LEVEL,
            ALARM_SNOOZE_PROBLEM,
            ALARM_SNOOZE_LEVEL,
            ALARM_DISMISS_SKIP,
            ALARM_SNOOZE_SKIP,
            ALARM_MATH_DISMISS_NUMBER,
            ALARM_MATH_SNOOZE_NUMBER,
            ALARM_SNOOZE_TIME_INDEX,
            ALARM_DISMISS_SHAKE,
            ALARM_SNOOZE_SHAKE,
            ALARM_CHECK,
            ALARM_IS_LAUNCH_APP,
            ALARM_LAUNCH_APP_PACKAGE,
            ALARM_BARCODE_TEXT,
            ALARM_IMAGE_PATH,
            ALARM_IS_SKIPPED
    };

    private static final String[] INSTANCE_QUERY_COLUMNS = new String[]{
            INSTANCE_ID,
            INSTANCE_NAME,
            INSTANCE_VOCAL_MESSAGE,
            INSTANCE_VOCAL_MESSAGE_TYPE,
            INSTANCE_VOCAL_MESSAGE_PLACE,
            INSTANCE_TIME_HOUR,
            INSTANCE_TIME_MINUTE,
            INSTANCE_TIME_DATE,
            INSTANCE_TIME_MONTH,
            INSTANCE_TIME_YEAR,
            INSTANCE_REPEAT_DAYS,
            INSTANCE_TONE,
            INSTANCE_URI_IDS,
            INSTANCE_TONE_TYPE,
            INSTANCE_VIBRATION,
            INSTANCE_DISMISS_METHOD,
            INSTANCE_DISMISS_LEVEL,
            INSTANCE_SNOOZE_PROBLEM,
            INSTANCE_SNOOZE_LEVEL,
            INSTANCE_DISMISS_SKIP,
            INSTANCE_SNOOZE_SKIP,
            INSTANCE_MATH_DISMISS_NUMBER,
            INSTANCE_MATH_SNOOZE_NUMBER,
            INSTANCE_SNOOZE_TIMES,
            INSTANCE_SNOOZE_TIME_INDEX,
            INSTANCE_DISMISS_SHAKE,
            INSTANCE_SNOOZE_SHAKE,
            INSTANCE_CHECK,
            INSTANCE_IS_LAUNCH_APP,
            INSTANCE_LAUNCH_APP_PACKAGE,
            INSTANCE_BARCODE_TEXT,
            INSTANCE_IMAGE_PATH,
            INSTANCE_STATE
    };

    private static final String RINGTONE_DEFAULT_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
    private static final String GENERATED_DAYS_1 = "false,true,true,true,true,true,false"; // Working days
    private static final String GENERATED_DAYS_2 = "true,false,false,false,false,false,true"; //Sat, Sun

    // Generate 2 default alarms for users
    private static final String GENERATED_ALARM_1 = "(0, 08, 00, '" + GENERATED_DAYS_1 + "', '" + RINGTONE_DEFAULT_URI + "', 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 30, 30, 1, 0);";
    private static final String GENERATED_ALARM_2 = "(0, 09, 30, '" + GENERATED_DAYS_2 + "', '" + RINGTONE_DEFAULT_URI + "', 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 30, 30, 0, 0);";
    private static DataManager mInstance = null;

    private static final String SQL_DELETE_ALARMS =
            "DROP TABLE IF EXISTS " + ALARM_TABLE_NAME;

    private DataManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static void createTables(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + ALARM_TABLE_NAME + " ("
                + ALARM_COLUMNS + ");");
        database.execSQL("CREATE TABLE " + INSTANCE_TABLE_NAME + " ("
                + INSTANCE_COLUMNS + ");");
    }

    public static synchronized DataManager getInstance(Context context) {
        // Use a single synchronised instance to enforce serialization
        if (mInstance == null) {
            mInstance = new DataManager(context.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);

        //Insert default generated alarms for better user experience
        String generatedAlarm = "INSERT INTO " + ALARM_TABLE_NAME + " (" +
                VOCAL_MESSAGE_PLACE + ", " +
                ALARM_TIME_HOUR + ", " +
                ALARM_TIME_MINUTE + ", " +
                ALARM_REPEAT_DAYS + ", " +
                ALARM_TONE + ", " +
                ALARM_TONE_TYPE + ", " +
                ALARM_VIBRATION + ", " +
                ALARM_ENABLED + ", " +
                ALARM_DISMISS_METHOD + ", " +
                ALARM_SNOOZE_PROBLEM + ", " +
                ALARM_SNOOZE_TIME_INDEX + ", " +
                ALARM_DISMISS_LEVEL + ", " +
                ALARM_SNOOZE_LEVEL + ", " +
                ALARM_DISMISS_SKIP + ", " +
                ALARM_SNOOZE_SKIP + ", " +
                ALARM_MATH_DISMISS_NUMBER + ", " +
                ALARM_MATH_SNOOZE_NUMBER + ", " +
                ALARM_DISMISS_SHAKE + ", " +
                ALARM_SNOOZE_SHAKE + ", " +
                ALARM_CHECK + ", " +
                ALARM_IS_LAUNCH_APP + ") VALUES ";
        db.execSQL(generatedAlarm + GENERATED_ALARM_1);
        db.execSQL(generatedAlarm + GENERATED_ALARM_2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ALARMS);
        Log.e("AlarmProvider", "onUpgrade called on first version"); // We should never reach here, since this is the first version
    }

    public long createAlarm(AlarmObject alarm) {
        ContentValues values = AlarmObject.populateContent(alarm);
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long rowId = -1;
        try {
            rowId = db.insert(ALARM_TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DataManager", "Alarm could not be created: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return rowId;
    }

    public long updateAlarm(AlarmObject object) {
        ContentValues values = AlarmObject.populateContent(object);
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long rows = 0;
        try {
            rows = db.update(ALARM_TABLE_NAME, values, _ID + " = ?", new String[]{String.valueOf(object.id)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DataManager", "Alarm could not be updated: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    public AlarmInstance createInstance(AlarmObject alarm) {
        ContentValues values = AlarmInstance.populateInstanceFromAlarm(alarm);
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.insert(INSTANCE_TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        }  catch (Exception e) {
            Log.e("DataManager", "Instance could not be created: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return new AlarmInstance(alarm);
    }

    public long updateInstance(AlarmInstance instance) {
        ContentValues values = AlarmInstance.populateInstance(instance);
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long rows = 0;
        try {
            rows = db.update(INSTANCE_TABLE_NAME, values, INSTANCE_ID + " = ?", new String[]{String.valueOf(instance.id)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DataManager", "Instance could not be updated: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    public AlarmInstance updateInstanceFromAlarm(AlarmObject object) {
        ContentValues values = AlarmInstance.populateInstanceFromAlarm(object);
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.update(INSTANCE_TABLE_NAME, values, INSTANCE_ID + " = ?", new String[]{String.valueOf(object.id)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DataManager", "Instance could not be updated: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return new AlarmInstance(object);
    }

    public boolean doesInstanceExist(long id){
        final SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(INSTANCE_TABLE_NAME, INSTANCE_QUERY_COLUMNS, INSTANCE_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
            return (cursor.getCount() > 0);
        } catch (Throwable b){
            b.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    public AlarmObject getAlarmById(long id) {
        final SQLiteDatabase db = this.getReadableDatabase();
        AlarmObject object = null;
        db.beginTransaction();
        try {
            Cursor c = db.query(ALARM_TABLE_NAME, ALARM_QUERY_COLUMNS, _ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);

            try {
                if (c.moveToNext()) {
                    object = new AlarmObject(c);
                    db.setTransactionSuccessful();
                }
            } catch (Exception e) {
                Log.e("DataManager", "Alarm could not be retrieved: " + e.toString());
            } finally {
                c.close();
            }
        } finally {
            db.endTransaction();
        }

        return object;
    }

    public AlarmInstance getInstanceById(long id) {
        final SQLiteDatabase db = this.getReadableDatabase();
        AlarmInstance instance = null;
        db.beginTransaction();
        try {
            Cursor c = db.query(INSTANCE_TABLE_NAME, INSTANCE_QUERY_COLUMNS, INSTANCE_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);

            try {
                if (c.moveToNext()) {
                    instance = new AlarmInstance(c);
                    db.setTransactionSuccessful();
                }
            } catch (Exception e) {
                Log.e("DataManager", "Instance could not be retrieved: " + e.toString());
            } finally {
                c.close();
            }
        } finally {
            db.endTransaction();
        }

        return instance;
    }

    public List<AlarmObject> getAlarms() {
        final SQLiteDatabase db = this.getReadableDatabase();
        List<AlarmObject> alarmList = new LinkedList<>();
        final String SORTING_ORDER = ALARM_TIME_HOUR + ", "
                + ALARM_TIME_MINUTE + " ASC " + ", " + _ID + " DESC";
        db.beginTransaction();
        try {
            Cursor c = db.query(ALARM_TABLE_NAME, ALARM_QUERY_COLUMNS, null, null, null, null, SORTING_ORDER);
            try {
                while (c.moveToNext()) {
                    alarmList.add(new AlarmObject(c));
                }
            } catch (Exception e) {
                Log.e("DataManager", "Alarms could not be retrieved: " + e.toString());
            } finally {
                c.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return alarmList;
    }

    public List<AlarmInstance> getInstances() {
        final SQLiteDatabase db = this.getReadableDatabase();
        List<AlarmInstance> alarmList = new LinkedList<>();
        final String SORTING_ORDER = INSTANCE_TIME_HOUR + ", "
                + INSTANCE_TIME_MINUTE + " ASC " + ", " + INSTANCE_ID + " DESC";
        db.beginTransaction();
        try {
            Cursor c = db.query(INSTANCE_TABLE_NAME, INSTANCE_QUERY_COLUMNS, null, null, null, null, SORTING_ORDER);
            try {
                while (c.moveToNext()) {
                    alarmList.add(new AlarmInstance(c));
                }
            } catch (Exception e) {
                Log.e("DataManager", "Alarms could not be retrieved: " + e.toString());
            } finally {
                c.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return alarmList;
    }

    public List<AlarmInstance> getFiredAlarms(){
        final SQLiteDatabase db = this.getReadableDatabase();
        List<AlarmInstance> alarmList = new LinkedList<>();
        db.beginTransaction();
        try {
            Cursor c = db.query(INSTANCE_TABLE_NAME, INSTANCE_QUERY_COLUMNS, INSTANCE_STATE + " = ?", new String[]{String.valueOf(AlarmInstance.ALARM_STATE_TRIGGERED)}, null, null, null);

            try {
                while (c.moveToNext()) {
                    alarmList.add(new AlarmInstance(c));
                }
            } catch (Exception e) {
                Log.e("DataManager", "Alarms could not be retrieved: " + e.toString());
            } finally {
                c.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return alarmList;
    }

    public int deleteAlarmById(long id) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int row = 0;
        try {
            row = db.delete(ALARM_TABLE_NAME, _ID + " = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } catch (Exception e){
            Log.e("DataManager", "Alarm could not be deleted: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return row;
    }

    public int deleteInstanceById(long id) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int row = 0;
        try {
            row = db.delete(INSTANCE_TABLE_NAME, INSTANCE_ID + " = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } catch (Exception e){
            Log.e("DataManager", "Instance could not be deleted: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return row;
    }

    public int deleteAlarms() {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int rows = 0;
        try {
            rows = db.delete(ALARM_TABLE_NAME, "1", null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DataManager", "Alarms could not be deleted: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    public int deleteInstances() {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int rows = 0;
        try {
            rows = db.delete(INSTANCE_TABLE_NAME, "1", null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DataManager", "Instances could not be deleted: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

}
