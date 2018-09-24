package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UtilStorage {

    public static final String PREFERENCE_FILE =
            "tw.edu.nthu.phys.astrolab.yangm.manireminder.simple_data";
    public static final String KEY_STARTED_SITUATIONS = "started_situations";
    public static final String KEY_OPENED_REMINDERS = "opened_reminders";
    public static final String KEY_NEW_ALARM_ID = "new_alarm_id";

    public static final int MAX_RECORDS_IN_HISTORY = 300;

    //
    public static int readSharedPrefInt(Context context, String key, int defaultValue) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, defaultValue);
    }

    public static void writeSharedPrefInt(Context context, String key, int value) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        sharedPref.edit().putInt(key, value).commit();
    }

    //
    public static List<Integer> getStartedSituations(Context context) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        String startedSitsStr = sharedPref.getString(KEY_STARTED_SITUATIONS, null);

        List<Integer> ids = new ArrayList<>();
        if (startedSitsStr == null || startedSitsStr.isEmpty()) {
            return new ArrayList<>();
        } else {
            return UtilGeneral.splitStringAsIntegerList(startedSitsStr, ",");
        }
    }

    public static void writeStartedSituations(Context context, List<Integer> startedSituationIds) {
        String data = UtilGeneral.joinIntegerList(",", startedSituationIds);
        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        sharedPref.edit().putString(KEY_STARTED_SITUATIONS, data).commit(); //synchronous
    }

    //
    public static SQLiteDatabase getReadableDatabase(Context context) {
        SQLiteDatabase db;
        try {
            db = new MainDbHelper(context).getReadableDatabase();
        } catch (SQLiteException e) {
            throw new RuntimeException("database unavailable");
        }
        return db;
    }

    public static SQLiteDatabase getWritableDatabase(Context context) {
        SQLiteDatabase db;
        try {
            db = new MainDbHelper(context).getWritableDatabase();
        } catch (SQLiteException e) {
            throw new RuntimeException("database unavailable");
        }
        return db;
    }

    public static List<Integer> getIdsInTable(Context context, String table) {
        List<Integer> ids = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(table, new String[] {"_id"}, null, null,
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            ids.add(cursor.getInt(0));
        }
        cursor.close();
        return ids;
    }

    public static int getRowCountInTable(Context context, String table) {
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(table, new String[] {"COUNT(*)"}, null, null,
                null, null, null);
        if (!cursor.moveToPosition(0)) {
            throw new RuntimeException("query failed");
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    //
    public static SparseArray<String> getAllSituations(Context context) {
        SQLiteDatabase db = getReadableDatabase(context);
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
        SQLiteDatabase db = getReadableDatabase(context);
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

    //
    public static final int TYPE_SIT_START = 0;
    public static final int TYPE_SIT_END = 1;
    public static final int TYPE_EVENT = 2;

    public static void addToHistory(Context context,
                                    Calendar at, int historyRecordType, int sitOrEventId) {
        if (historyRecordType < 0 || historyRecordType > 2)
            throw new RuntimeException("bad historyRcordType");

        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(at.getTime());
        String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.US).format(at.getTime());

        ContentValues values = new ContentValues();
        values.put("date", dateStr);
        values.put("time", timeStr);
        values.put("type", historyRecordType);
        values.put("sit_event_id", sitOrEventId);

        SQLiteDatabase db = getWritableDatabase(context);
        db.insert(MainDbHelper.TABLE_HISTORY, null, values);

        // when number of records exceeds MAX_RECORDS_IN_HISTORY
        Cursor cursor = db.query(MainDbHelper.TABLE_HISTORY, new String[] {"COUNT(*)", "MIN(_id)"},
                null, null, null, null, null);
        if (cursor.moveToFirst()) {
            int rowCount = cursor.getInt(0);
            int minId = cursor.getInt(1);
            cursor.close();

            if (rowCount > MAX_RECORDS_IN_HISTORY) {
                db.delete(MainDbHelper.TABLE_HISTORY, "_id < ?",
                        new String[] {String.valueOf(minId + rowCount - MAX_RECORDS_IN_HISTORY)});
            }
        } else {
            cursor.close();
        }
    }

    //
    public static List<List<Integer>> getRemindersStartedPeriodIds(Context context,
                                                                   List<Integer> remIds) {
        List<List<Integer>> remsStartedPeriodIds = new ArrayList<>();
        if (remIds.isEmpty()) {
            return remsStartedPeriodIds;
        }
        for (int i=0; i<remIds.size(); i++) {
            remsStartedPeriodIds.add(null);
        }

        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS, null,
                "_id IN ("+UtilStorage.placeHolders(remIds.size())+")",
                UtilGeneral.toStringArray(remIds),
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);

            String startedPeriodsStr = cursor.getString(1);
            List<Integer> startedPeriods =
                    UtilGeneral.splitStringAsIntegerList(startedPeriodsStr, ",");

            int index = remIds.indexOf(id);
            if (index != -1) {
                remsStartedPeriodIds.set(index, startedPeriods);
            }
        }
        cursor.close();

        for (int i=0; i<remIds.size(); i++) {
            if (remsStartedPeriodIds.get(i) == null) {
                remsStartedPeriodIds.set(i, new ArrayList<Integer>());
            }
        }
        return remsStartedPeriodIds;
    }

    public static void updateRemindersStartedPeriodIds(
            Context context, SparseArray<Set<Integer>> remsStartedPeriodIds) {
        if (remsStartedPeriodIds.size() == 0)
            return;

        SQLiteDatabase db = getWritableDatabase(context);
        List<Integer> idsExist = getIdsInTable(context, MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS);
        for (int i=0; i<remsStartedPeriodIds.size(); i++) {
            int id = remsStartedPeriodIds.keyAt(i);

            Set<Integer> periods = remsStartedPeriodIds.valueAt(i);
            String startedPeriodsStr =
                    UtilGeneral.joinIntegerList(",", new ArrayList<>(periods));

            if (idsExist.contains(id)) {
                // update record
                ContentValues values = new ContentValues();
                values.put("started_periods", startedPeriodsStr);
                db.update(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS, values,
                        "_id = ?", new String[] {String.valueOf(id)});
            } else {
                // insert record
                ContentValues values = new ContentValues();
                values.put("_id", id);
                values.put("started_periods", startedPeriodsStr);
                db.insert(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS, null, values);
            }
        }
    }

    //
    public static void addScheduledActions(
            Context context, int alarmId, List<ScheduleAction> actions) {
        if (actions.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase(context);
        for (ScheduleAction action: actions) {
            ContentValues values = action.getContentValues();
            values.put("alarm_id", alarmId);
            long check = db.insert(MainDbHelper.TABLE_SCHEDULED_ACTIONS, null, values);
            if (check == -1) {
                throw new RuntimeException("failed to insert a row into table");
            }
        }
    }

    public static void removeScheduledActions(Context context, int alarmId) {
        SQLiteDatabase db = getWritableDatabase(context);
        db.delete(MainDbHelper.TABLE_SCHEDULED_ACTIONS,
                "alarm_id = ?", new String[] {Integer.toString(alarmId)});
    }

    public static List<ScheduleAction> getScheduledActions(Context context, int alarmId) {
        List<ScheduleAction> actions = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SCHEDULED_ACTIONS,
                new String[] {"type", "time", "reminder_id", "period_index_or_id", "repeat_start_time"},
                "alarm_id = ?", new String[] {Integer.toString(alarmId)},
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            actions.add(new ScheduleAction().setFromCursor(cursor));
        }
        cursor.close();
        return actions;
    }

    public static int countMainSchedulingInScheduledActions(Context context) {
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SCHEDULED_ACTIONS, new String[] {"COUNT(*)"},
                "type = ?",
                new String[] {Integer.toString(ScheduleAction.TYPE_MAIN_RESCHEDULE)},
                null, null, null);
        if (!cursor.moveToPosition(0)) {
            throw new RuntimeException("query failed");
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public static Set<Integer> getScheduledAlarmIds(Context context) {
        Set<Integer> alarmIds = new HashSet<>();
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SCHEDULED_ACTIONS, new String[] {"alarm_id"},
                null, null, "alarm_id", null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            alarmIds.add(cursor.getInt(0));
        }
        cursor.close();
        return alarmIds;
    }

    //
    public static String placeHolders(int n) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<n; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('?');
        }
        return builder.toString();
    }

    //
    public static SparseBooleanArray getOpenedReminders(Context context) {
        SparseBooleanArray rems = new SparseBooleanArray();

        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_OPENED_REMINDERS,
                new String[] {"_id", "highlight"},
                null, null,
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            rems.append(cursor.getInt(0), cursor.getInt(1) == 1);
        }
        cursor.close();
        return rems;
    }

    /** Reminders will be added with highlight = 1.
     *  If a reminder already exists, it will be updated with highlight = 1. */
    public static void addOpenedReminders(Context context, Set<Integer> remIdsToAdd) {
        SparseBooleanArray remsOld = getOpenedReminders(context);

        // update
        for (int i=0; i<remsOld.size(); i++) {
            int remId = remsOld.keyAt(i);
            if (remIdsToAdd.contains(remId)) {
                if (! remsOld.valueAt(i)) {
                    SQLiteDatabase db = getWritableDatabase(context);
                    ContentValues values = new ContentValues();
                    values.put("highlight", 1);
                    db.update(MainDbHelper.TABLE_OPENED_REMINDERS, values,
                            "_id = ?", new String[] {Integer.toString(remId)});
                }
            }
        }

        // insert
        SQLiteDatabase db = getWritableDatabase(context);
        for (int remId: remIdsToAdd) {
            if (remsOld.indexOfKey(remId) < 0) {
                ContentValues values = new ContentValues();
                values.put("_id", remId);
                values.put("highlight", 1);
                db.insert(MainDbHelper.TABLE_OPENED_REMINDERS, null, values);
            }
        }
    }

    public static void toggleHighlightOfOpenedReminder(Context context, int remId) {
        SparseBooleanArray rems = getOpenedReminders(context);
        int index = rems.indexOfKey(remId);
        if (index < 0) {
            throw new RuntimeException("`remId` not found in opened reminders");
        }
        int newHighlight = rems.valueAt(index) ? 0 : 1;

        ContentValues values = new ContentValues();
        values.put("highlight", newHighlight);
        SQLiteDatabase db = getWritableDatabase(context);
        db.update(MainDbHelper.TABLE_OPENED_REMINDERS, values,
                "_id = ?", new String[] {Integer.toString(remId)});
    }

    public static void removeOpenedReminders(Context context, Set<Integer> remIdsToRemove) {
        SQLiteDatabase db = getWritableDatabase(context);
        for (int remId: remIdsToRemove) {
            db.delete(MainDbHelper.TABLE_OPENED_REMINDERS,
                    "_id = ?", new String[] {Integer.toString(remId)});
        }
    }
}
