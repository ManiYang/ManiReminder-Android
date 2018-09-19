package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class UtilStorage {

    public static final String PREFERENCE_FILE =
            "tw.edu.nthu.phys.astrolab.yangm.manireminder.simple_data";

    public static final String KEY_STARTED_SITUATIONS = "started_situations";

    //
    public static List<Integer> getStartedSituations(Context context) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        String startedSitsStr = sharedPref.getString(KEY_STARTED_SITUATIONS, null);

        List<Integer> ids = new ArrayList<>();
        if (startedSitsStr == null) {
            return ids;
        } else if (startedSitsStr.length() == 0) {
            return ids;
        } else {
            String[] tokens = startedSitsStr.split(",");
            for (String str: tokens) {
                ids.add(Integer.parseInt(str));
            }
            return ids;
        }
    }

    /** write asynchronously */
    public static void writeStartedSituations(Context context, List<Integer> startedSituationIds) {
        String data = UtilGeneral.joinIntegerList(",", startedSituationIds);

        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        sharedPref.edit().putString(KEY_STARTED_SITUATIONS, data).apply();
    }

    //
    public static SparseArray<String> getAllSituations(Context context) {
        SQLiteDatabase db;
        try {
            SQLiteOpenHelper mainDbHelper = new MainDbHelper(context);
            db = mainDbHelper.getReadableDatabase();
        } catch (SQLiteException e) {
            Toast.makeText(context, "Database unavailable", Toast.LENGTH_LONG).show();
            return null;
        }

        SparseArray<String> allSits = new SparseArray<>();
        Cursor cursor = db.query(MainDbHelper.TABLE_SITUATIONS, null,
                null, null, null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            allSits.append(id, name);
        }
        cursor.close();
        return allSits;
    }

    public static SparseArray<String> getAllEvents(Context context) {
        SQLiteDatabase db;
        try {
            SQLiteOpenHelper mainDbHelper = new MainDbHelper(context);
            db = mainDbHelper.getReadableDatabase();
        } catch (SQLiteException e) {
            Toast.makeText(context, "Database unavailable", Toast.LENGTH_LONG).show();
            return null;
        }

        SparseArray<String> allEvents = new SparseArray<>();
        Cursor cursor = db.query(MainDbHelper.TABLE_EVENTS, null,
                null, null, null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            allEvents.append(id, name);
        }
        cursor.close();
        return allEvents;
    }
}
