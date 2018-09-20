package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.util.Log;
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
        if (s.trim().isEmpty())
            return list;
        String[] split = s.split(regex);
        for (String token: split) {
            token = token.trim();
            if (!token.isEmpty()) {
                list.add(token);
            }
        }
        return list;
    }

    public static ArrayList<Integer> splitStringAsIntegerList(String s, String regex) {
        ArrayList<Integer> list = new ArrayList<>();
        if (s.trim().isEmpty())
            return list;
        String[] tokens = s.split(regex);
        for (String token: tokens) {
            token = token.trim();
            if (!token.isEmpty()) {
                list.add(Integer.parseInt(token));
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

    public static boolean hasDuplicatedElement(String[] array) {
        for (int i=0; i<array.length; i++) {
            String s = array[i];
            for (int j=i+1; j<array.length; j++) {
                if(s.equals(array[j]))
                    return true;
            }
        }
        return false;
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

    public static ArrayList<String> getValuesOfSparseStringArray(SparseArray<String> array) {
        ArrayList<String> list = new ArrayList<>();
        for (int i=0; i<array.size(); i++) {
            list.add(array.valueAt(i));
        }
        return list;
    }

    public static ArrayList<Integer> getKeysOfSparseStringArray(SparseArray<String> array) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i=0; i<array.size(); i++) {
            list.add(array.keyAt(i));
        }
        return list;
    }

    /**
     * @param string Must be of a form like "{0=value, 2=value, ...}". Each value cannot contain
     *               character ','.
     */
    public static SparseArray<String> parseAsSparseStringArray(String string) {
        SparseArray<String> array = new SparseArray<>();
        try {
            string = string.substring(1, string.length() - 1);
            if (string.isEmpty())
                return array;
            String[] tokens = string.split(", *");
            for (String token: tokens) {
                int pos = token.indexOf('=');
                if (pos < 0) {
                    throw new RuntimeException();
                }
                int key = Integer.parseInt(token.substring(0, pos));
                String value = token.substring(pos+1);
                array.append(key, value);
            }
        } catch (RuntimeException e) {
            Log.v("UtilGeneral", "### string=\""+string+"\"");
            throw new RuntimeException("Bad format of `string`");
        }
        return array;
    }

    public static String stringifySparseStringArray(SparseArray<String> array) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        for (int i=0; i<array.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(array.keyAt(i)).append('=').append(array.valueAt(i));
        }
        builder.append('}');
        return builder.toString();
    }

    public static SparseArray<String> buildSparseStringArray(List<Integer> keys,
                                                             List<String> values) {
        if (keys.size() != values.size()) {
            throw new RuntimeException("`keys` and `values` must have the same size");
        }
        SparseArray<String> array = new SparseArray<>();
        for (int i=0; i<keys.size(); i++) {
            array.append(keys.get(i), values.get(i));
        }
        return array;
    }
}
