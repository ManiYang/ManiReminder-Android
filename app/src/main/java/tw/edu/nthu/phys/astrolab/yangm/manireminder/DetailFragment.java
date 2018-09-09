package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DetailFragment extends Fragment {

    private int reminderId = -9;
    private SQLiteDatabase db;
    private final int REQUEST_CODE_EDIT = 0;
    private static final String NONE_INDICATOR = "(none)";

    public DetailFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // open database
        SQLiteOpenHelper mainDbHelper = new MainDbHelper(getContext());
        try {
            db = mainDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            db = null;
            Toast.makeText(getContext(), "Database unavailable", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        // set OnClick listeners
        view.findViewById(R.id.title).setOnClickListener(viewsOnClickListener);
        view.findViewById(R.id.label_tags).setOnClickListener(viewsOnClickListener);
        view.findViewById(R.id.tags).setOnClickListener(viewsOnClickListener);
        view.findViewById(R.id.label_description).setOnClickListener(viewsOnClickListener);
        view.findViewById(R.id.description).setOnClickListener(viewsOnClickListener);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.v("DetailFragment", "### Reminder ID: " + Integer.toString(reminderId));
        if (reminderId < 0) {
            throw new RuntimeException("### reminderId < 0");
        }
        loadReminderData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    //
    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
    }

    public void removeReminder() { // called by DetailActivity
        if (db == null) {
            return;
        }

        db.delete(MainDbHelper.TABLE_REMINDERS_BRIEF,
                "_id = ?", new String[] {Integer.toString(reminderId)});
        db.delete(MainDbHelper.TABLE_REMINDERS_DETAIL,
                "_id = ?", new String[] {Integer.toString(reminderId)});
    }

    //
    private void loadReminderData() {
        if (db == null) {
            return;
        }

        View view = getView();
        if (view == null) {
            return;
        }

        // get all tags
        SparseArray<String> allTags = UtilReminder.getAllTagsFromDb(db);

        // get brief data
        Cursor cursor = db.query(MainDbHelper.TABLE_REMINDERS_BRIEF, null,
                "_id = ?", new String[] {Integer.toString(reminderId)},
                null, null, null);
        if (!cursor.moveToFirst()) {
            throw new RuntimeException(
                    String.format("Reminder (id: %d) not found in database", reminderId));
        }
        String title = cursor.getString(1);
        String tagsStr = UtilReminder.buildTagsString(cursor.getString(2), allTags);
        if (tagsStr.isEmpty()) {
            tagsStr = NONE_INDICATOR;
        }
        cursor.close();

        // get detailed data
        cursor = db.query(MainDbHelper.TABLE_REMINDERS_DETAIL, null,
                "_id = ?", new String[] {Integer.toString(reminderId)},
                null, null, null);
        if (!cursor.moveToFirst()) {
            throw new RuntimeException(String.format(
                    "Reminder (id: %d) not found in table 'reminders_detail'", reminderId));
        }
        String description = cursor.getString(1);
        if (description.isEmpty()) {
            description = NONE_INDICATOR;
        }
        cursor.close();

        // set contents of views
        ((TextView) view.findViewById(R.id.title)).setText(title);
        ((TextView) view.findViewById(R.id.tags)).setText(tagsStr);
        ((TextView) view.findViewById(R.id.description)).setText(description);
    }

    // OnClick listener for all views
    private View.OnClickListener viewsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.title:
                    showSimpleTextEditDialog("title", R.id.title,
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    break;

                case R.id.label_tags:
                case R.id.tags:
                    startEditActivity("tags", R.id.tags, true);
                    break;

                case R.id.label_description:
                case R.id.description:
                    showSimpleTextEditDialog("description", R.id.description,
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    break;
            }
        }
    };

    // SimpleTextEditDialog //
    private void showSimpleTextEditDialog(
            String fieldName, int associateTextViewId, int textInputType) {
        View view = getView();
        if (view == null) { return; }

        String oldText;
        try {
            TextView textView = (TextView) view.findViewById(associateTextViewId);
            oldText = textView.getText().toString();
            if (oldText.equals(NONE_INDICATOR)) {
                oldText = "";
            }
        } catch (ClassCastException e) {
            throw new ClassCastException("'associateTextViewId' does not refer to a TextView");
        }

        SimpleTextEditDialogFragment dialogFragment = SimpleTextEditDialogFragment.newInstance(
                "Edit "+fieldName+":", oldText, textInputType);
        dialogFragment.show(getFragmentManager(), "dialog_edit_"+fieldName);
    }

    public void onDialogPositiveClicked(String dialogFragmentTag, String newText) {
        // (called by DetailActivity)
        View view = getView();
        if (view == null) { return; }

        boolean doneDbUpdate = false;
        switch (dialogFragmentTag) {
            case "dialog_edit_title": {
                // update title
                ((TextView) view.findViewById(R.id.title)).setText(newText);
                ContentValues values = new ContentValues();
                values.put("title", newText);
                int check = db.update(MainDbHelper.TABLE_REMINDERS_BRIEF, values,
                        "_id = ?", new String[]{Integer.toString(reminderId)});
                doneDbUpdate = (check == 1);
                break;
            }
            case "dialog_edit_description": {
                // update description
                ((TextView) view.findViewById(R.id.description)).setText(newText);
                ContentValues values = new ContentValues();
                values.put("description", newText);
                int check = db.update(MainDbHelper.TABLE_REMINDERS_DETAIL, values,
                        "_id = ?", new String[]{Integer.toString(reminderId)});
                doneDbUpdate = (check == 1);
                break;
            }
        }

        if (! doneDbUpdate) {
            Toast.makeText(getActivity(), "Could not update database!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    // EditActivity //
    private void startEditActivity(String fieldName, int textViewIdWithData,
                                   boolean needAllTags) {
        // get old data from TextView textViewIdWithData
        String oldData;
        View view = getView();
        if (view == null) { return; }
        try {
            oldData = ((TextView) view.findViewById(textViewIdWithData)).getText().toString();
            if (oldData.equals(NONE_INDICATOR)) {
                oldData = "";
            }
        } catch (ClassCastException e) {
            throw new ClassCastException("textViewIdWithData does not refer to a TextView");
        }

        //
        Intent intent = new Intent(getContext(), EditActivity.class)
                .putExtra(EditActivity.EXTRA_FIELD_NAME, fieldName)
                .putExtra(EditActivity.EXTRA_INIT_DATA, oldData);

        if (needAllTags) {
            String allTagsString = UtilReminder.getAllTagsEncodedFromDb(db);
            intent.putExtra(EditActivity.EXTRA_INIT_ALL_TAGS, allTagsString);
        }

        startActivityForResult(intent, REQUEST_CODE_EDIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_EDIT:
                if (resultCode == EditActivity.RESULT_CODE_OK) {
                    updateDataAfterEditActivity(data);
                } else {
                    Toast.makeText(getActivity(), "Discarded editing", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateDataAfterEditActivity(Intent intentNewData) {
        String fieldName = intentNewData.getStringExtra(EditActivity.EXTRA_FIELD_NAME);
        String newData = intentNewData.getStringExtra(EditActivity.EXTRA_NEW_DATA);
        String newAllTagsPairString =
                intentNewData.getStringExtra(EditActivity.EXTRA_NEW_ALL_TAGS); //can be null
//        Log.v("DetailFragment", "### newData = "+newData);

        View view = getView();
        switch (fieldName) {
            case "tags": {
                if (newAllTagsPairString == null) {
                    throw new RuntimeException("new all-tags should be given");
                }

                // update data in database (no need to update view)
                SparseArray<String> allTags = parseAllTagsPairString(newAllTagsPairString);
                saveAllTagsToDb(allTags, db);

                List<String> remTags = UtilGeneral.splitString(newData, ",");
                List<Integer> remTagIds = getTagIds(remTags, allTags);
                String remTagIdsString = UtilGeneral.joinIntegerList(",", remTagIds);
                ContentValues values = new ContentValues();
                values.put("tags", remTagIdsString);
                int check = db.update(MainDbHelper.TABLE_REMINDERS_BRIEF, values,
                        "_id = ?", new String[] {Integer.toString(reminderId)});
                if (check != 1) {
                    Toast.makeText(getContext(), "Could not update database", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
        }
    }

    private SparseArray<String> parseAllTagsPairString(String allTagsPairString) {
        if (allTagsPairString == null) {
            return null;
        }

        SparseArray<String> allTags = new SparseArray<>();
        List<String> allTagsPair = UtilGeneral.splitString(allTagsPairString, ",");
        try {
            for (String tagPair : allTagsPair) {
                String[] tokens = tagPair.split(":");
                int id = Integer.parseInt(tokens[0].trim());
                allTags.append(id, tokens[1].trim());
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            //Log.e("DetailFragment", "allTagsPairString = "+allTagsPairString);
            throw new RuntimeException("wrong format of allTagsPairString");
        }
        return allTags;
    }

    private boolean saveAllTagsToDb(SparseArray<String> allTags, SQLiteDatabase db) {
        db.delete(MainDbHelper.TABLE_TAGS, null, null);
        for (int i=0; i<allTags.size(); i++) {
            ContentValues values = new ContentValues();
            values.put("_id", allTags.keyAt(i));
            values.put("name", allTags.valueAt(i));
            long check = db.insert(MainDbHelper.TABLE_TAGS, null, values);
            if (check == -1) {
                Toast.makeText(getContext(), "Could not write to database", Toast.LENGTH_LONG)
                        .show();
                return false;
            }
        }
        return true;
    }

    private ArrayList<Integer> getTagIds(List<String> tags, SparseArray<String> allTags) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (String tag: tags) {
            //int i = allTags.indexOfValue(tag); // doesn't work
            int index  = -1;
            for (int i=0; i<allTags.size(); i++) {
                if (allTags.valueAt(i).equals(tag)) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                ids.add(allTags.keyAt(index));
            } else {
                throw new RuntimeException("tag not found in 'allTags'");
            }
        }
        return ids;
    }
}
