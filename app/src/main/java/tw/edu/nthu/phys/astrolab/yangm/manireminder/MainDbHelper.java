package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MainDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "main.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_REMINDERS_BRIEF = "reminders_brief";
    public static final String TABLE_REMINDERS_DETAIL = "reminders_detail";
    public static final String TABLE_TAGS = "tags";


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

            db.execSQL("CREATE TABLE tags ("
                    + "_id INTEGER PRIMARY KEY, "
                    + "name TEXT );");
            values = new ContentValues();
            values.put("_id", 0);
            values.put("name", "testing");
            db.insert(TABLE_TAGS, null, values);
        }
    }
}
