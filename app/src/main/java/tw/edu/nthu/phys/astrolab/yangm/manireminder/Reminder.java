package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;

import java.util.HashSet;
import java.util.Set;

public class Reminder {

    private BriefData briefData;
    private DetailedData detailedData;

    //
    public Reminder(int id, String title, Set<Integer> tagIds) {
        briefData = new BriefData(id, title, tagIds);
        detailedData = new DetailedData();
    }

    public Reminder(int id) { // create an empty reminder
        this(id, "", new HashSet<Integer>());
    }

    //
    public Reminder setAsNewReminderTemplate() {
        briefData.setTitle("New Reminder");
        briefData.clearTags();
        detailedData.description = "";
        return this;
    }

    public int getId() {
        return briefData.getId();
    }

    public void setTitle(String title) {
        briefData.setTitle(title);
    }

    public String getTitle() {
        return briefData.getTitle();
    }

    public void clearTags() {
        briefData.clearTags();
    }

    public void addTag(int tagId) {
        briefData.addTag(tagId);
    }

    public void removeTag(int tagId) {
        briefData.removeTag(tagId);
    }

    public Set<Integer> getTagIds() {
        return briefData.getTagIds();
    }

    public String getTagsDisplayString() {
        Set<Integer> tagIds = briefData.getTagIds();
        if (tagIds.isEmpty()) {
            return "none";
        } else {
            /*
            SparseArray<String> allTags = StorageManager.getInstance().getAllTags();
            Integer[] tagIdsArray = tagIds.toArray(new Integer[0]);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(allTags.get(tagIdsArray[0]));
            for (int i=1; i<tagIdsArray.length; i++) {
                stringBuilder.append(", ").append(allTags.get(tagIdsArray[i]));
            }
            return stringBuilder.toString();*/
            return "";
        }
    }

    public void setDescription(String description) {
        detailedData.description = description;
    }

    public String getDescription() {
        return detailedData.description;
    }

    public ContentValues getBriefContentValues() {
        return briefData.getContentValues();
    }

    public ContentValues getDetailedContentValues() {
        ContentValues values = new ContentValues();
        values.put("_id", briefData.getId());
        values.put("description", detailedData.description);
        return values;
    }

    //// inner classes ////
    public static class BriefData {
        private int id;
        private String title;
        private Set<Integer> tagIds;

        public BriefData(int id, String title, Set<Integer> tagIds) {
            this.id = id;
            this.title = title;
            this.tagIds = tagIds;
        }

        public BriefData(int id) {
            this(id, "", new HashSet<Integer>());
        }

        //
        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Set<Integer> getTagIds() {
            // return a copy
            return new HashSet<>(tagIds);
        }

        public void setTagIds(Set<Integer> tagIds) {
            this.tagIds = tagIds;
        }

        public void clearTags() {
            tagIds.clear();
        }

        public void addTag(int tagId) {
            tagIds.add(tagId);
        }

        public void removeTag(int tagId) {
            tagIds.remove(tagId);
        }

        public ContentValues getContentValues() {
            ContentValues values = new ContentValues();
            values.put("_id", id);
            values.put("title", title);
            values.put("tags", tagIds.toString().replaceAll("[\\[\\] ]", ""));
            return values;
        }
    }

    private class DetailedData {
        public String description = "";
    }
}
