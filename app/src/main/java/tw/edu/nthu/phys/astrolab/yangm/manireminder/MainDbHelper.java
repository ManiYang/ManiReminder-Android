package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MainDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "main.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_REMINDERS_BRIEF = "reminders_brief";
    public static final String TABLE_REMINDERS_DETAIL = "reminders_detail";
    public static final String TABLE_REMINDERS_BEHAVIOR = "reminders_behavior_settings";
    public static final String TABLE_TAGS = "tags";
    public static final String TABLE_SITUATIONS = "situations";
    public static final String TABLE_EVENTS = "events";
    public static final String TABLE_HISTORY = "history";


    public MainDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Log.v("MainDbHelper", "### creating DB");
        updateDb(db, 0, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Log.v("MainDbHelper", "### updating DB");
        updateDb(db, oldVersion, newVersion);
    }

    //
    private void updateDb(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            Log.v("MainDbHelper", "### creating tables (1)");

            // reminder brief
            db.execSQL("CREATE TABLE " + TABLE_REMINDERS_BRIEF + " ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "title TEXT, "
                    + "tags TEXT );");
            ContentValues values = new ContentValues();
            values.put("_id", 0);
            values.put("title", "Testing Reminder");
            values.put("tags", "0");
            db.insert(TABLE_REMINDERS_BRIEF, null, values);

            values = new ContentValues();
            values.put("_id", 1);
            values.put("title", "Testing Reminder 2");
            values.put("tags", "1");
            db.insert(TABLE_REMINDERS_BRIEF, null, values);

            // reminder detail
            db.execSQL("CREATE TABLE " + TABLE_REMINDERS_DETAIL + " ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "description TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("description", "This is a reminder for testing only.");
            db.insert(TABLE_REMINDERS_DETAIL, null, values);

            values = new ContentValues();
            values.put("_id", 1);
            values.put("description", "Also for testing.");
            db.insert(TABLE_REMINDERS_DETAIL, null, values);


            // reminder behavior
            db.execSQL("CREATE TABLE " + TABLE_REMINDERS_BEHAVIOR + " ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "type INTEGER, "
                    + "behavior_settings TEXT, "
                    + "involved_sits TEXT, "
                    + "involved_events TEXT, "
                    + "involve_time_in_start_instant INTEGER );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("type", 3);
            values.put("behavior_settings", "every1m.offset0m in sit0start-sitEnd, event0-after10m");
            values.put("involved_sits", ",0,");
            values.put("involved_events", ",0,");
            values.put("involve_time_in_start_instant", 0);
            long check = db.insert(TABLE_REMINDERS_BEHAVIOR, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row to table " + TABLE_REMINDERS_BEHAVIOR);
            }

            values = new ContentValues();
            values.put("_id", 1);
            values.put("type", 1);
            values.put("behavior_settings", "sit0end, M~Su.9:00, W.13:00");
            values.put("involved_sits", ",0,");
            values.put("involved_events", ",,");
            values.put("involve_time_in_start_instant", 1);
            check = db.insert(TABLE_REMINDERS_BEHAVIOR, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row to table " + TABLE_REMINDERS_BEHAVIOR);
            }

            // tags
            db.execSQL("CREATE TABLE " + TABLE_TAGS + " ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "testing");
            db.insert(TABLE_TAGS, null, values);
            values = new ContentValues();
            values.put("_id", 1);
            values.put("name", "testing1");
            db.insert(TABLE_TAGS, null, values);

            // situations
            db.execSQL("CREATE TABLE " + TABLE_SITUATIONS + " ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "Situation0");
            check = db.insert(TABLE_SITUATIONS, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row to table " + TABLE_SITUATIONS);
            }

            // events
            db.execSQL("CREATE TABLE " + TABLE_EVENTS + " ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "Event0");
            check = db.insert(TABLE_EVENTS, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row to table " + TABLE_EVENTS);
            }
        }

        if (oldVersion < 2) {
            // history
            db.execSQL("CREATE TABLE " + TABLE_HISTORY + " ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "date TEXT, "
                    + "time TEXT, "
                    + "type INTEGER, "
                    + "sit_event_id INTEGER);");
        }
    }

    private List<Integer> getIdsInTable(SQLiteDatabase db, String table) {
        Cursor cursor = db.query(table, new String[] {"_id"}, null, null,
                null, null, null);
        cursor.moveToPosition(-1);
        List<Integer> ids = new ArrayList<>();
        while (cursor.moveToNext()) {
            ids.add(cursor.getInt(0));
        }
        cursor.close();
        return ids;
    }

    //
    public static void addEmptyReminder(SQLiteDatabase db, int remId, String title) {
        if (db == null) {
            throw new RuntimeException("db is null");
        }

        boolean check;

        // brief data
        ContentValues values = new ContentValues();
        values.put("_id", remId);
        values.put("title", title);
        values.put("tags", "");
        check = db.insert(TABLE_REMINDERS_BRIEF, null, values) != -1;

        // detail data
        values.clear();
        values.put("_id", remId);
        values.put("description", "");
        check &= db.insert(TABLE_REMINDERS_DETAIL, null, values) != -1;

        // behavior data
        values.clear();
        values.put("_id", remId);
        values.put("type", 0);
        values.put("behavior_settings", "");
        check &= db.insert(TABLE_REMINDERS_BEHAVIOR, null, values) != -1;

        //
        if (!check) { // there's problem
            db.delete(TABLE_REMINDERS_BRIEF,
                    "_id = ?", new String [] {Integer.toString(remId)});
            db.delete(TABLE_REMINDERS_DETAIL,
                    "_id = ?", new String [] {Integer.toString(remId)});
            db.delete(TABLE_REMINDERS_BEHAVIOR,
                    "_id = ?", new String [] {Integer.toString(remId)});

            throw new RuntimeException("failed to create new empty reminder in database");
        }
    }
}
