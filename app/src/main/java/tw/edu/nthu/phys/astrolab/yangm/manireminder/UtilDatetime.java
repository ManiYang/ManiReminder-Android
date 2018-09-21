package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import java.util.Calendar;
import java.util.GregorianCalendar;

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
}
