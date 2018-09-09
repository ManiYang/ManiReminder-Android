package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

public class UtilGeneral {

    public static final String[] DAYS_OF_WEEK = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat","Sun"};

    /**
     * Mon -> 1, Tue -> 2, etc.. Returns -1 if not done.
     * @param ddd -- Mon, Tue, etc. (case irrelevant)
     * @param sundayIs0 true: Sun -> 0,  false: Sun -> 7
     */
    public static int getDayOfWeekInt(String ddd, boolean sundayIs0) {
        for (int i=0; i<7; i++) {
            if (ddd.equalsIgnoreCase(DAYS_OF_WEEK[i])) {
                if (i == 0) {
                    return sundayIs0 ? 0 : 7;
                }
                return i;
            }
        }
        return -1;
    }



    /**
     * Split input string to form a list of strings. Each token will be trimmed.
     * Empty tokens are ignored.
     * @param s: input string
     * @param regex: the separator
     */
    public static ArrayList<String> splitString(String s, String regex) {
        ArrayList<String> list = new ArrayList<>();
        String[] split = s.split(regex);
        for (String token: split) {
            String tokenTrim = token.trim();
            if (!tokenTrim.isEmpty()) {
                list.add(tokenTrim);
            }
        }
        return list;
    }

    public static String joinStringList(String delimiter, List<String> stringList) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<stringList.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(stringList.get(i));
        }
        return builder.toString();
    }

    public static String joinIntegerList(String delimiter, List<Integer> intList) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<intList.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(intList.get(i));
        }
        return builder.toString();
    }


    /**
     * @return Return the (first) key whose value equals `value`. If not found, return -1.
     */
    public static int searchSparseStringArrayByValue(SparseArray<String> array, String value) {
        for (int i=0; i<array.size(); i++) {
            if (array.valueAt(i).equals(value))
                return array.keyAt(i);
        }
        return -1;
    }
}
