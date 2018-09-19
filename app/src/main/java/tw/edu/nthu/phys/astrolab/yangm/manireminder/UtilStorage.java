package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.content.SharedPreferences;

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
}
