package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MainDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "main.db";
    public static final int DATABASE_VERSION = 2;

    public static final String TABLE_REMINDERS_BRIEF = "reminders_brief";
    public static final String TABLE_REMINDERS_DETAIL = "reminders_detail";
    public static final String TABLE_REMINDERS_BOARD_CONTROL = "reminders_board_control";
    public static final String TABLE_TAGS = "tags";
    public static final String TABLE_SITUATIONS = "situations";
    public static final String TABLE_EVENTS = "events";


    public MainDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        updateDb(db, 0, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateDb(db, oldVersion, newVersion);
    }

    //
    private void updateDb(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
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
            db.execSQL("CREATE TABLE "+TABLE_REMINDERS_BOARD_CONTROL+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "type INTEGER, "
                    + "board_control_spec TEXT );");
            ContentValues values = new ContentValues();
            values.put("_id", 0);
            values.put("type", 3);
            values.put("board_control_spec", "every1m.offset0m in sit0start-sitEnd, event0-after10m");
            db.insert(TABLE_REMINDERS_BOARD_CONTROL, null, values);

            db.execSQL("CREATE TABLE "+TABLE_SITUATIONS+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "Situation0");

            db.execSQL("CREATE TABLE "+TABLE_EVENTS+" ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "Event0");
        }
    }
}
