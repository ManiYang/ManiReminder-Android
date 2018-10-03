package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UtilStorage {

    public static final String PREFERENCE_FILE =
            "tw.edu.nthu.phys.astrolab.yangm.manireminder.simple_data";
    public static final String KEY_STARTED_SITUATIONS = "started_situations";
    public static final String KEY_NEW_ALARM_ID = "new_alarm_id";
    public static final String KEY_ALL_REM_LIST_FILTER_SPEC = "all_rem_list_filter_spec";
    public static final String KEY_APP_VERSION_CODE = "app_version_code";

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

    public static String readSharedPrefString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(key, defaultValue);
    }

    public static void writeSharedPrefString(Context context, String key, String value) {
        SharedPreferences sharedPref =
                context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        sharedPref.edit().putString(key, value).commit();
    }

    //

    /**
     * @param versionCode version code to update to
     * @return whether the stored version code does get updated
     */
    public static boolean updateAppVersionCode(Context context, int versionCode) {
        int versionCodeStored = readSharedPrefInt(context, KEY_APP_VERSION_CODE, -1);
        if (versionCodeStored != versionCode) {
            writeSharedPrefInt(context, KEY_APP_VERSION_CODE, versionCode);
            return true;
        } else {
            return false;
        }
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

    public static String qMarks(int n) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<n; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('?');
        }
        return builder.toString();
    }

    /**
     * replace '\\' by "\\\\" and '\n' by "\\n"; use \t as column separator
     * @return true if done
     */
    public static boolean dumpTableToCsv(Context context, String table, File outputFile) {
        FileWriter writer = null;
        Cursor cursor = null;
        boolean done = true;
        try {
            writer = new FileWriter(outputFile);

            SQLiteDatabase db = getReadableDatabase(context);
            cursor = db.query(table, null, null, null,
                    null, null, null);

            // write header (column names)
            String[] columnNames = cursor.getColumnNames();

            writer.write(UtilGeneral.joinStringList("\t", Arrays.asList(columnNames)));
            writer.write('\n');

            // write data rows
            int Ncols = cursor.getColumnCount();
            List<String> strList = new ArrayList<>();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                strList.clear();
                for (int c=0; c<Ncols; c++) {
                    strList.add(cursor.getString(c));
                }
                String line = UtilGeneral.joinStringList("\t", strList)
                        .replaceAll("\\\\", "\\\\\\\\")
                        .replaceAll("\n", "\\\\n");
                writer.write(line + "\n");
            }

            //
            writer.flush();
        } catch (IOException e) {
            done = false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    done = false;
                }
            }
        }
        return done;
    }

    public static boolean overwriteTableFromCsv(Context context, String table, File inputFile) {
        // get column names
        SQLiteDatabase db = getWritableDatabase(context);
        Cursor cursor = db.query(table, null,null, null,
                null, null, null, "1");
        String[] columnNames = cursor.getColumnNames();
        cursor.close();

        //
        db.delete(table, null, null);

        BufferedReader reader = null;
        boolean done = true;
        try {
            reader = new BufferedReader(new FileReader(inputFile));

            // first line
            String line = reader.readLine();
            if (line.split("\t").length != columnNames.length) {
                throw new RuntimeException("Inconsistent column numbers");
            }

            //
            ContentValues values = new ContentValues();
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\\\\n", "\n")
                        .replaceAll("\\\\\\\\", "\\\\");
                String[] strings = line.split("\t", -1);

                values.clear();
                for (int c=0; c<columnNames.length; c++) {
                    values.put(columnNames[c], strings[c]);
                }
                db.insert(table, null, values);
            }
        } catch (IOException e) {
            done = false;
        } catch (RuntimeException e) {
            e.printStackTrace();
            done = false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    done = false;
                }
            }
        }
        return done;
    }


    //// reminders ////
    public static int getReminderModel(Context context, int remId) {
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR, new String[] {"type"},
                "_id = ?", new String[] {Integer.toString(remId)},
                null, null, null);
        int model = -1;
        if (cursor.moveToPosition(0)) {
            model = cursor.getInt(0);
        }
        cursor.close();
        return model;
    }


    //// situations & events ////
    public static SparseArray<String> getAllSituations(Context context) {
        SparseArray<String> allSits = new SparseArray<>();
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SITUATIONS_EVENTS,
                new String[] {"sit_event_id", "name"},
                "is_situation = 1", null,
                null, null, null);
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
        SparseArray<String> allEvents = new SparseArray<>();
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SITUATIONS_EVENTS,
                new String[] {"sit_event_id", "name"},
                "is_situation = 0", null,
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            allEvents.append(id, name);
        }
        cursor.close();
        return allEvents;
    }

    public static class SitOrEvent {
        public boolean isSituation;
        public int id;
        public String name;

        public SitOrEvent(boolean isSituation, int id, String name) {
            this.isSituation = isSituation;
            this.id = id;
            this.name = name;
        }
    }

    public static List<SitOrEvent> getAllSitsEvents(Context context) {
        List<SitOrEvent> list = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SITUATIONS_EVENTS,
                new String[] {"is_situation", "sit_event_id", "name"},
                null, null,
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            list.add(new SitOrEvent(cursor.getInt(0) == 1,
                    cursor.getInt(1), cursor.getString(2)));
        }
        cursor.close();
        return list;
    }

    public static void overwriteAllSituationsEvents(Context context, List<SitOrEvent> sitEvents) {
        SQLiteDatabase db = getWritableDatabase(context);
        db.delete(MainDbHelper.TABLE_SITUATIONS_EVENTS, null, null);
        ContentValues values = new ContentValues();
        for (SitOrEvent sitEvent: sitEvents) {
            values.clear();
            values.put("is_situation", sitEvent.isSituation ? 1 : 0);
            values.put("sit_event_id", sitEvent.id);
            values.put("name", sitEvent.name);
            db.insert(MainDbHelper.TABLE_SITUATIONS_EVENTS, null, values);
        }
    }

    public static boolean overwriteAllSituationsEvents(
            SQLiteDatabase db, SparseArray<String> allSits, SparseArray<String> allEvents) {
        db.delete(MainDbHelper.TABLE_SITUATIONS_EVENTS, null, null);
        ContentValues values = new ContentValues();
        for (int i=0; i<allSits.size(); i++) {
            values.clear();
            values.put("is_situation", 1);
            values.put("sit_event_id", allSits.keyAt(i));
            values.put("name", allSits.valueAt(i));
            long check = db.insert(MainDbHelper.TABLE_SITUATIONS_EVENTS, null, values);
            if (check == -1) {
                return false;
            }
        }
        for (int i=0; i<allEvents.size(); i++) {
            values.clear();
            values.put("is_situation", 0);
            values.put("sit_event_id", allEvents.keyAt(i));
            values.put("name", allEvents.valueAt(i));
            long check = db.insert(MainDbHelper.TABLE_SITUATIONS_EVENTS, null, values);
            if (check == -1) {
                return false;
            }
        }
        return true;
    }


    //// history ////
    public static final int HIST_TYPE_SIT_START = 0;
    public static final int HIST_TYPE_SIT_END = 1;
    public static final int HIST_TYPE_EVENT = 2;

    public static void addToHistory(Context context,
                                    Calendar at, int historyRecordType, int sitOrEventId) {
        if (historyRecordType < 0 || historyRecordType > 2)
            throw new RuntimeException("bad historyRecordType");

        String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.US).format(at.getTime());
        String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.US).format(at.getTime());

        ContentValues values = new ContentValues();
        values.put("date", Integer.parseInt(dateStr));
        values.put("time", timeStr);
        values.put("type", historyRecordType);
        values.put("sit_event_id", sitOrEventId);

        SQLiteDatabase db = getWritableDatabase(context);
        db.insert(MainDbHelper.TABLE_HISTORY, null, values);

        //
        final int historyRecordDaysMax =
                context.getResources().getInteger(R.integer.history_record_days_max);
        Calendar dateMin = (Calendar) at.clone();
        dateMin.add(Calendar.DAY_OF_MONTH, 1 - historyRecordDaysMax);
        String dateMinStr =
                new SimpleDateFormat("yyyyMMdd", Locale.US).format(dateMin.getTime());
        db.delete(MainDbHelper.TABLE_HISTORY,
                "date < ?", new String[] {dateMinStr});
    }

    public static void removeHistoryRecordsOfSituation(Context context, int situationId) {
        SQLiteDatabase db = getWritableDatabase(context);
        db.delete(MainDbHelper.TABLE_HISTORY,
                "type IN (?,?) AND sit_event_id = ?",
                new String[] {Integer.toString(HIST_TYPE_SIT_START),
                        Integer.toString(HIST_TYPE_SIT_END), Integer.toString(situationId)});
    }

    public static void removeHistoryRecordsOfEvent(Context context, int eventId) {
        SQLiteDatabase db = getWritableDatabase(context);
        db.delete(MainDbHelper.TABLE_HISTORY,
                "type = ? AND sit_event_id = ?",
                new String[] {Integer.toString(HIST_TYPE_EVENT), Integer.toString(eventId)});
    }

    public static class HistoryRecord {
        private Calendar time;
        private int type = -1;
        private int sitOrEventId;

        public HistoryRecord() {}

        public HistoryRecord(Calendar time, int type, int sitOrEventId) {
            this.time = time;
            this.type = type;
            this.sitOrEventId = sitOrEventId;
        }

        public HistoryRecord setAsSituationStart(Calendar at, int sitId) {
            this.time = at;
            this.type = HIST_TYPE_SIT_START;
            this.sitOrEventId = sitId;
            return this;
        }

        public HistoryRecord setAsSituationEnd(Calendar at, int sitId) {
            this.time = at;
            this.type = HIST_TYPE_SIT_END;
            this.sitOrEventId = sitId;
            return this;
        }

        public HistoryRecord setAsEvent(Calendar at, int eventId) {
            this.time = at;
            this.type = HIST_TYPE_EVENT;
            this.sitOrEventId = eventId;
            return this;
        }

        public Calendar getTime() {
            return time;
        }

        public int getType() {
            return type;
        }

        public int getSitOrEventId() {
            return sitOrEventId;
        }

        public boolean isEventWithId(int eventId) {
            return type == HIST_TYPE_EVENT && sitOrEventId == eventId;
        }

        public boolean isStartOfSituationWithId(int sitId) {
            return type == HIST_TYPE_SIT_START && sitOrEventId == sitId;
        }

        public boolean isEndOfSituationWithId(int sitId) {
            return type == HIST_TYPE_SIT_END && sitOrEventId == sitId;
        }
    }

    /**
     * @return in descending order (most recent record is first)
     */
    public static List<HistoryRecord> getHistoryRecords(Context context, @Nullable Calendar since) {
        List<HistoryRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_HISTORY,
                new String[] {"_id", "date", "time", "type", "sit_event_id"},
                null, null,
                null, null, "_id DESC");
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            int dateInt = cursor.getInt(1);
            String timeStr = cursor.getString(2);
            Calendar time = Calendar.getInstance();
            try {
                Date d = new SimpleDateFormat("yyyyMMddHH:mm:ss", Locale.US)
                        .parse(Integer.toString(dateInt) + timeStr);
                time.setTime(d);
            } catch (ParseException e) {
                throw new RuntimeException("failed to parse datetime");
            }

            if (since != null && time.compareTo(since) < 0) {
                continue;
            }

            int type = cursor.getInt(3);
            int sitEventId = cursor.getInt(4);
            records.add(new HistoryRecord(time, type, sitEventId));
        }
        cursor.close();
        return records;
    }


    //// started periods ////
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
                "_id IN ("+UtilStorage.qMarks(remIds.size())+")",
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

    /** update the list of started periods for specified reminders */
    public static void updateRemindersStartedPeriodIds(
            Context context, SparseArray<Set<Integer>> remsStartedPeriodIds) {
        if (remsStartedPeriodIds.size() == 0)
            return;

        SQLiteDatabase db = getWritableDatabase(context);
        List<Integer> remIdsExist =
                getIdsInTable(context, MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS);
        ContentValues values = new ContentValues();
        for (int i=0; i<remsStartedPeriodIds.size(); i++) {
            int remId = remsStartedPeriodIds.keyAt(i);

            Set<Integer> periodIds = remsStartedPeriodIds.valueAt(i);
            String startedPeriodsStr =
                    UtilGeneral.joinIntegerList(",", new ArrayList<>(periodIds));

            values.clear();
            if (remIdsExist.contains(remId)) {
                if (periodIds.isEmpty()) {
                    // delete record
                    db.delete(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS,
                            "_id = ?", new String[] {Integer.toString(remId)});

                    Log.v("mainlog", String.format(Locale.US,
                            "rem started periods removed (rem %d)", remId));
                } else {
                    // update record
                    values.put("started_periods", startedPeriodsStr);
                    db.update(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS, values,
                            "_id = ?", new String[]{String.valueOf(remId)});

                    Log.v("mainlog", String.format(Locale.US,
                            "rem started periods updated (rem %d, period-ids %s)",
                            remId, startedPeriodsStr));
                }
            } else {
                if (!periodIds.isEmpty()) {
                    // insert record
                    values.put("_id", remId);
                    values.put("started_periods", startedPeriodsStr);
                    db.insert(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS,
                            null, values);

                    Log.v("mainlog", String.format(Locale.US,
                            "rem started periods updated (rem %d, period-ids %s)",
                            remId, startedPeriodsStr));
                }
            }
        }
    }

    /** update the list of started periods for the specified reminder */
    public static void updateReminderStartedPeriodIds(Context context, int remId,
                                                      List<Integer> startedPeriodIDs) {
        SparseArray<Set<Integer>> sa = new SparseArray<>();
        sa.append(remId, new HashSet<>(startedPeriodIDs));
        updateRemindersStartedPeriodIds(context, sa);
    }

    /** update whole table (removing original data first) */
    public static void writeStartedPeriods(Context context,
                                           SparseArray<List<Integer>> remsStartedPeriodIds) {
        SQLiteDatabase db = getWritableDatabase(context);
        db.delete(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS, null, null);

        ContentValues values = new ContentValues();
        for (int i=0; i<remsStartedPeriodIds.size(); i++) {
            int remId = remsStartedPeriodIds.keyAt(i);
            List<Integer> periodIds = remsStartedPeriodIds.valueAt(i);
            if (periodIds.isEmpty()) {
                continue;
            }

            values.clear();
            values.put("_id", remId);
            values.put("started_periods", UtilGeneral.joinIntegerList(",", periodIds));
            db.insert(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS, null, values);
        }
    }

    public static void removeStartedPeriodsOfReminder(Context context, int remId) {
        SQLiteDatabase db = getWritableDatabase(context);
        db.delete(MainDbHelper.TABLE_REMINDERS_STARTED_PERIODS,
                "_id = ?", new String[] {Integer.toString(remId)});
    }


    //// scheduled actions ////
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

    /**
     * @return action-id to ScheduleAction
     */
    public static SparseArray<ScheduleAction> getScheduledActionsAndIds(Context context,
                                                                        int alarmId) {
        SparseArray<ScheduleAction> actions = new SparseArray<>();
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SCHEDULED_ACTIONS,
                new String[] {"type", "time", "reminder_id",
                              "period_index_or_id", "repeat_start_time", "_id"},
                "alarm_id = ?", new String[] {Integer.toString(alarmId)},
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            actions.append(cursor.getInt(5), new ScheduleAction().setFromCursor(cursor));
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

    /**
     * @return null if main rescheduling not found
     */
    public static Calendar getMainReschedulingTime(Context context) {
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SCHEDULED_ACTIONS, new String[] {"_id", "time"},
                "type = ?",
                new String[] {Integer.toString(ScheduleAction.TYPE_MAIN_RESCHEDULE)},
                null, null, null);
        if (!cursor.moveToPosition(0)) {
            cursor.close();
            return null;
        }
        String timeStr = cursor.getString(1);
        cursor.close();

        Date date;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss", Locale.US).parse(timeStr);
        } catch (ParseException e) {
            throw new RuntimeException("failed to parse scheduled time of action");
        }
        Calendar t = Calendar.getInstance();
        t.setTime(date);
        return t;
    }

    /**
     * @return -1 if not found
     */
    public static int getMainReschedulingAlarmId(Context context) {
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_SCHEDULED_ACTIONS,
                new String[] {"_id", "alarm_id"},
                "type = ?",
                new String[] {Integer.toString(ScheduleAction.TYPE_MAIN_RESCHEDULE)},
                null, null, null);
        if (!cursor.moveToPosition(0)) {
            cursor.close();
            return -1;
        } else {
            int alarmId = cursor.getInt(1);
            cursor.close();
            return alarmId;
        }
    }


    //// opened reminders ////
    public static List<Integer> getOpenedRemindersList(Context context) {
        List<Integer> list = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_OPENED_REMINDERS,
                new String[] {"_id"},
                null, null,
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            list.add(cursor.getInt(0));
        }
        cursor.close();
        return list;
    }

    public static class OpenedRemindersInfo {
        public boolean highlight;
        public Calendar openTime; // can be null

        public OpenedRemindersInfo(boolean highlight, Calendar openTime) {
            this.highlight = highlight;
            this.openTime = openTime;
        }
    }

    public static SparseArray<OpenedRemindersInfo> getOpenedReminders(Context context) {
        SparseArray<OpenedRemindersInfo> array = new SparseArray<>();

        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_OPENED_REMINDERS,
                new String[] {"_id", "highlight", "open_time"},
                null, null,
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String timeStr = cursor.getString(2);
            Calendar t = (timeStr == null) ? null :
                    UtilDatetime.timeFromString(timeStr, UtilDatetime.OPTION_COMPACT);
            array.append(cursor.getInt(0),
                    new OpenedRemindersInfo(cursor.getInt(1) == 1, t));
        }
        cursor.close();
        return array;
    }

    public static boolean isReminderOpened(Context context, int remId) {
        SQLiteDatabase db = getReadableDatabase(context);
        Cursor cursor = db.query(MainDbHelper.TABLE_OPENED_REMINDERS, new String[] {"_id"},
                "_id = ?", new String[] {Integer.toString(remId)},
                null, null, null);
        boolean opened = cursor.getCount() > 0;
        cursor.close();
        return opened;
    }

    /**
     * Reminders will be added with highlight = 1 and open_time = (current time).
     * If a reminder already exists, it will be updated with highlight = 1 and
     * open_time = (current time).
     */
    public static void addOpenedReminders(Context context, Set<Integer> remIdsToAdd) {
        Calendar now = Calendar.getInstance();
        List<Integer> remsOld = getOpenedRemindersList(context);
        SQLiteDatabase db = getWritableDatabase(context);
        for (int remId: remIdsToAdd) {
            if (remsOld.indexOf(remId) != -1) {
                // update
                ContentValues values = new ContentValues();
                values.put("highlight", 1);
                values.put("open_time", UtilDatetime.timeToString(now, UtilDatetime.OPTION_COMPACT));
                db.update(MainDbHelper.TABLE_OPENED_REMINDERS, values,
                        "_id = ?", new String[] {Integer.toString(remId)});
            } else {
                // insert
                ContentValues values = new ContentValues();
                values.put("_id", remId);
                values.put("highlight", 1);
                values.put("open_time", UtilDatetime.timeToString(now, UtilDatetime.OPTION_COMPACT));
                db.insert(MainDbHelper.TABLE_OPENED_REMINDERS, null, values);
            }
        }

        /*
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
        }*/

        //
        for (int remId: remIdsToAdd) {
            Log.v("mainlog",
                    String.format(Locale.US, "rem %d open (DB update)", remId));
        }
    }

    public static void toggleHighlightOfOpenedReminder(Context context, int remId) {
        SparseArray<OpenedRemindersInfo>  remsInfo = getOpenedReminders(context);
        int index = remsInfo.indexOfKey(remId);
        if (index < 0) {
            throw new RuntimeException("`remId` not found in opened reminders");
        }
        int newHighlight = remsInfo.valueAt(index).highlight ? 0 : 1;

        ContentValues values = new ContentValues();
        values.put("highlight", newHighlight);
        SQLiteDatabase db = getWritableDatabase(context);
        db.update(MainDbHelper.TABLE_OPENED_REMINDERS, values,
                "_id = ?", new String[] {Integer.toString(remId)});
    }

    public static void removeFromOpenedReminders(Context context, Set<Integer> remIdsToRemove) {
        SQLiteDatabase db = getWritableDatabase(context);
        for (int remId: remIdsToRemove) {
            int count = db.delete(MainDbHelper.TABLE_OPENED_REMINDERS,
                    "_id = ?", new String[] {Integer.toString(remId)});

            if (count != 0) {
                Log.v("mainlog",
                        String.format(Locale.US, "rem %d close (DB update)", remId));
            }
        }
    }

    public static void removeFromOpenedReminders(Context context, int remId) {
        SQLiteDatabase db = getWritableDatabase(context);
        int count = db.delete(MainDbHelper.TABLE_OPENED_REMINDERS,
                "_id = ?", new String[] {Integer.toString(remId)});

        if (count != 0) {
            Log.v("mainlog",
                    String.format(Locale.US, "rem %d close (DB update)", remId));
        }
    }

    public static void clearOpenedReminders(Context context) {
        SQLiteDatabase db = getWritableDatabase(context);
        db.delete(MainDbHelper.TABLE_OPENED_REMINDERS, null, null);
    }
}
