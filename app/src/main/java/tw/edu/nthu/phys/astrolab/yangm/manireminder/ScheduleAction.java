package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.database.Cursor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ScheduleAction {

    public static final int TYPE_MAIN_RESCHEDULE = 0;
    public static final int TYPE_PERIOD_START = 1;
    public static final int TYPE_PERIOD_STOP = 2;
    public static final int TYPE_REMINDER_M1_OPEN = 3;
    public static final int TYPE_REMINDER_M3_OPEN = 4;
    public static final int TYPE_RESCHEDULE_M3_REMINDER_REPEATS = 5;

    private int type = -1;
    private Calendar time;
    private int reminderId;
    private int periodIndexOrId;
    private Calendar repeatStartAt;


    public ScheduleAction() {}

    public ScheduleAction setAsMainReschedule(Calendar time) {
        this.type = TYPE_MAIN_RESCHEDULE;
        this.time = time;
        return this;
    }

    public ScheduleAction setAsPeriodStart(Calendar time, int reminderId, int periodIndex) {
        this.type = TYPE_PERIOD_START;
        this.time = time;
        this.reminderId = reminderId;
        this.periodIndexOrId = periodIndex;
        return this;
    }

    public ScheduleAction setAsPeriodStop(Calendar time, int reminderId, int periodId) {
        this.type = TYPE_PERIOD_STOP;
        this.time = time;
        this.reminderId = reminderId;
        this.periodIndexOrId = periodId;
        return this;
    }

    public ScheduleAction setAsModel1ReminderOpen(Calendar time, int reminderId) {
        this.type = TYPE_REMINDER_M1_OPEN;
        this.time = time;
        this.reminderId = reminderId;
        return this;
    }

    public ScheduleAction setAsModel3ReminderOpen(Calendar time, int reminderId) {
        this.type = TYPE_REMINDER_M3_OPEN;
        this.time = time;
        this.reminderId = reminderId;
        return this;
    }

    public ScheduleAction setAsRescheduleModel3ReminderRepeats(Calendar time, int reminderId,
                                                               Calendar repeatStartAt) {
        this.type = TYPE_RESCHEDULE_M3_REMINDER_REPEATS;
        this.time = time;
        this.reminderId = reminderId;
        this.repeatStartAt = repeatStartAt;
        return this;
    }

    /**
     * @param cursor must contain columns
     *               {type, time, reminder_id, period_index_or_id, repeat_start_time} */
    public ScheduleAction setFromCursor(Cursor cursor) {
        if (cursor.getColumnCount() < 5) {
            throw new RuntimeException("column count < 5");
        }
        if (!cursor.getColumnNames()[0].equals("type") ||
                !cursor.getColumnNames()[1].equals("time") ||
                !cursor.getColumnNames()[2].equals("reminder_id") ||
                !cursor.getColumnNames()[3].equals("period_index_or_id") ||
                !cursor.getColumnNames()[4].equals("repeat_start_time") ) {
            throw new RuntimeException("`cursor` does not contain the expected columns");
        }

        type = cursor.getInt(0);

        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss", Locale.US)
                    .parse(cursor.getString(1));
            time = new GregorianCalendar();
            time.setTime(date);
        } catch (ParseException e) {
            throw new RuntimeException("Could not parse time string");
        }

        if (!cursor.isNull(2))
            reminderId = cursor.getInt(2);

        if (!cursor.isNull(3))
            periodIndexOrId = cursor.getInt(3);

        if (!cursor.isNull(4)) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss", Locale.US)
                        .parse(cursor.getString(4));
                repeatStartAt = new GregorianCalendar();
                repeatStartAt.setTime(date);
            } catch (ParseException e) {
                throw new RuntimeException("Could not parse repeatStartAt string");
            }
        }

        return this;
    }

    //
    public int getType() {
        if (type == -1)
            throw new RuntimeException("data not set");
        return type;
    }

    public Calendar getTime() {
        if (type == -1)
            throw new RuntimeException("data not set");
        return time;
    }

    public int getReminderId() {
        if (type == -1)
            throw new RuntimeException("data not set");
        if (type == TYPE_MAIN_RESCHEDULE)
            throw new RuntimeException("no reminder ID for this type");
        return reminderId;
    }

    public int getPeriodId() {
        if (type == -1)
            throw new RuntimeException("data not set");
        if (type != TYPE_PERIOD_STOP)
            throw new RuntimeException("no period id for this type");
        return periodIndexOrId;
    }

    public int getPeriodIndex() {
        if (type == -1)
            throw new RuntimeException("data not set");
        if (type != TYPE_PERIOD_START)
            throw new RuntimeException("no period index for this type");
        return periodIndexOrId;
    }

    public Calendar getRepeatStartAt() {
        if (type == -1)
            throw new RuntimeException("data not set");
        if (type != TYPE_RESCHEDULE_M3_REMINDER_REPEATS)
            throw new RuntimeException("no repeat start time for this type");
        return repeatStartAt;
    }

    public ContentValues getContentValues() {
        if (type == -1)
            throw new RuntimeException("data not set");

        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("time", new SimpleDateFormat(
                "yyyy-MM-dd.HH:mm:ss", Locale.US).format(time.getTime()));

        if (type == TYPE_MAIN_RESCHEDULE)
            values.putNull("reminder_id");
        else
            values.put("reminder_id", reminderId);

        if (type == TYPE_PERIOD_START || type == TYPE_PERIOD_STOP)
            values.put("period_index_or_id", periodIndexOrId);
        else
            values.putNull("period_index_or_id");

        if (type == TYPE_RESCHEDULE_M3_REMINDER_REPEATS)
            values.put("repeat_start_time", new SimpleDateFormat(
                    "yyyy-MM-dd.HH:mm:ss", Locale.US).format(repeatStartAt.getTime()));
        else
            values.putNull("repeat_start_time");

        return values;
    }

    public String getDisplayString() {
        String timeStr = new SimpleDateFormat("MM/dd.HH:mm:ss", Locale.US)
                .format(time.getTime());
        switch (type) {
            case -1:
                return "not set";
            case TYPE_MAIN_RESCHEDULE:
                return timeStr + ", main reschedule";
            case TYPE_PERIOD_START:
                return String.format(Locale.US, "%s, period start (rem %d, period-index %d)",
                        timeStr, reminderId, periodIndexOrId);
            case TYPE_PERIOD_STOP:
                return String.format(Locale.US, "%s, period stop (rem %d, period-id %d)",
                        timeStr, reminderId, periodIndexOrId);
            case TYPE_REMINDER_M1_OPEN:
                return timeStr + ", open m1 rem " + Integer.toString(reminderId);
            case TYPE_REMINDER_M3_OPEN:
                return timeStr + ", open m3 rem " + Integer.toString(reminderId);
            case TYPE_RESCHEDULE_M3_REMINDER_REPEATS:
                return String.format(Locale.US, "%s, reschedule for m3 rem %d started %s",
                        timeStr, reminderId,
                        new SimpleDateFormat("MM/dd.HH:mm:ss", Locale.US)
                                .format(repeatStartAt.getTime()));
            default:
                return "";
        }
    }
}
