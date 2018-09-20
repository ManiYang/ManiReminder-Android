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

    int type = -1;
    private Calendar time;
    int reminderId;
    int periodIndex;

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
        this.periodIndex = periodIndex;
        return this;
    }

    public ScheduleAction setAsPeriodStop(Calendar time, int reminderId, int periodIndex) {
        this.type = TYPE_PERIOD_STOP;
        this.time = time;
        this.reminderId = reminderId;
        this.periodIndex = periodIndex;
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

    public ScheduleAction setAsRescheduleModel3ReminderRepeats(Calendar time, int reminderId) {
        this.type = TYPE_RESCHEDULE_M3_REMINDER_REPEATS;
        this.time = time;
        this.reminderId = reminderId;
        return this;
    }

    /**
     * @param cursor (type, time, reminder_id, period_index)
     * */
    public ScheduleAction setFromCursor(Cursor cursor) {
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
            periodIndex = cursor.getInt(3);

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

    public int getPeriodIndex() {
        if (type == -1)
            throw new RuntimeException("data not set");
        if (type != TYPE_PERIOD_START && type != TYPE_PERIOD_STOP)
            throw new RuntimeException("no period index for this type");
        return periodIndex;
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
            values.put("period_index", periodIndex);
        else
            values.putNull("period_index");

        return values;
    }
}
