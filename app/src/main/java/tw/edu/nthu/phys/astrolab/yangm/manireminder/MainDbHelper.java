package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MainDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "main.db";
    public static final int DATABASE_VERSION = 2;

    public static final String TABLE_REMINDERS_BRIEF = "reminders_brief";
    public static final String TABLE_REMINDERS_DETAIL = "reminders_detail";
    public static final String TABLE_REMINDERS_BEHAVIOR = "reminders_behavior_settings";
    public static final String TABLE_TAGS = "tags";
    public static final String TABLE_SITUATIONS = "situations";
    public static final String TABLE_EVENTS = "events";


    public MainDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v("MainDbHelper", "### creating DB");
        updateDb(db, 0, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.v("MainDbHelper", "### updating DB");
        updateDb(db, oldVersion, newVersion);
    }

    //
    private void updateDb(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            Log.v("MainDbHelper", "### creating tables (1)");

            db.execSQL("CREATE TABLE "+TABLE_REMINDERS_BRIEF+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "title TEXT, "
                    + "tags TEXT );");
            ContentValues values = new ContentValues();
            values.put("_id", 0);
            values.put("title", "Testing Reminder");
            values.put("tags", "0");
            db.insert(TABLE_REMINDERS_BRIEF, null, values);

            db.execSQL("CREATE TABLE "+TABLE_REMINDERS_DETAIL+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "description TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("description", "This is a reminder for testing only.");
            db.insert(TABLE_REMINDERS_DETAIL, null, values);

            db.execSQL("CREATE TABLE "+TABLE_TAGS+" ("
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
        }

        if (oldVersion < 2) {
            Log.v("MainDbHelper", "### creating tables (2)");

            db.execSQL("CREATE TABLE "+TABLE_REMINDERS_BEHAVIOR+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "type INTEGER, "
                    + "behavior_settings TEXT );");
            ContentValues values = new ContentValues();
            values.put("_id", 0);
            values.put("type", 3);
            values.put("behavior_settings", "every1m.offset0m in sit0start-sitEnd, event0-after10m");
            long check = db.insert(TABLE_REMINDERS_BEHAVIOR, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row to table "+TABLE_REMINDERS_BEHAVIOR);
            }

            db.execSQL("CREATE TABLE "+TABLE_SITUATIONS+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "Situation0");
            check = db.insert(TABLE_SITUATIONS, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row to table "+TABLE_SITUATIONS);
            }

            db.execSQL("CREATE TABLE "+TABLE_EVENTS+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "Event0");
            check = db.insert(TABLE_EVENTS, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row to table "+TABLE_EVENTS);
            }
        }
    }
}
