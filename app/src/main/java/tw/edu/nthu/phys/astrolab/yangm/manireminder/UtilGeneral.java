package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import java.util.HashSet;
import java.util.Set;

public class UtilGeneral {

    /**
     * Split input string to form a set of strings. Empty tokens are ignored.
     * @param s: input string
     * @param regex: the separator
     */
    public static Set<String> splitString(String s, String regex) {
        Set<String> set = new HashSet<>();
        String[] split = s.split(regex);
        for (int i=0; i<split.length; i++) {
            String token = split[i].trim();
            if (!token.isEmpty()) {
                set.add(token);
            }
        }
        return set;
    }
}
