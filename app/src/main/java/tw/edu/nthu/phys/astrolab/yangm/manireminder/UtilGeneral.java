package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import java.util.ArrayList;

public class UtilGeneral {

    /**
     * Split input string to form a list of strings. Empty tokens are ignored.
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
}
