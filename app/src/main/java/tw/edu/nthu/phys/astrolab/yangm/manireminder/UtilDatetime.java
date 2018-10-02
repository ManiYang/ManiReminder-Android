package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class UtilDatetime {

    /** t1 - t2 in minutes */
    public static float timeDifference(Calendar t1, Calendar t2) {
        return (t1.getTimeInMillis() - t2.getTimeInMillis()) / 60000.0f;
    }

    public static Calendar addMinutes(Calendar t, double minutes) {
        long t1 = t.getTimeInMillis() + (long)(minutes*60000);
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(t1);
        return c;
    }

    //
    public static final int OPTION_COMPACT = 0;
    public static final int OPTION_READABLE = 1;

    public static String timeToString(Calendar t, int option) {
        switch (option) {
            case OPTION_COMPACT:
                return new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.US).format(t.getTime());
            case OPTION_READABLE:
                return new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss", Locale.US).format(t.getTime());
            default:
                throw new RuntimeException("bad value of `option`");
        }
    }

    public static Calendar timeFromString(String s, int option) {
        Calendar t = Calendar.getInstance();
        try {
            switch (option) {
                case OPTION_COMPACT:
                    t.setTime(new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.US).parse(s));
                    return t;
                case OPTION_READABLE:
                    t.setTime(new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss", Locale.US).parse(s));
                    return t;
                default:
                    throw new RuntimeException("bad value of `option`");
            }
        } catch (ParseException e) {
            throw new RuntimeException("failed to parse string as time");
        }
    }
}
