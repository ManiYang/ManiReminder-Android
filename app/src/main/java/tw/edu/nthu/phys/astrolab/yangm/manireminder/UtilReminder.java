package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

public class UtilReminder {

    /**
     * Build a string for displaying a reminder's tags.
     * @param tagIdsString: e.g., "0,3,4"
     * @param allTags: names of all tag ID's
     * @return a string joining the tag names corresponding to tag ID's in tagIdsString
     */
    public static String buildTagsString(String tagIdsString, SparseArray<String> allTags) {
        if (tagIdsString.trim().isEmpty()) {
            return "";
        }

        String[] idStringSplit = tagIdsString.split(",");
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<idStringSplit.length; i++) {
            String s = idStringSplit[i].trim();
            if (s.isEmpty()) {
                continue;
            }

            int id = Integer.parseInt(s);
            String tagName = allTags.get(id, "(invalid_tag_id)");
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(tagName);
        }
        return builder.toString();
    }

    public static SparseArray<String> getAllTagsFromDb(SQLiteDatabase db) {
        SparseArray<String> allTags = new SparseArray<>();
        if (db != null) {
            Cursor cursor = db.query(MainDbHelper.TABLE_TAGS, null,
                    null, null, null, null, null);
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                int tagId = cursor.getInt(0);
                String tagName = cursor.getString(1);
                allTags.put(tagId, tagName);
            }
            cursor.close();
        }
        return allTags;
    }

    public static String getAllTagsEncodedFromDb(SQLiteDatabase db) {
        SparseArray<String> allTags = getAllTagsFromDb(db);
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<allTags.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(allTags.keyAt(i))
                    .append(':')
                    .append(allTags.valueAt(i));
        }
        return builder.toString();
    }

    public static

}
