package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import java.util.ArrayList;
import java.util.List;

public class UtilGeneral {

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
}
