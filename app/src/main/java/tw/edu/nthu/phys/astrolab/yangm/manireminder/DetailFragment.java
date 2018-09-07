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


/**
 * A simple {@link Fragment} subclass.
 */
public class DetailFragment extends Fragment {

    private int reminderId = -9;
    private SQLiteDatabase db;
    private final int REQUEST_CODE_EDIT = 0;

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

    public void removeReminder() {
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
            tagsStr = "(none)";
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
            description = "(none)";
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
                    startEditActivity("tags", R.id.tags);
                    break;

                case R.id.label_description:
                case R.id.description:
                    showSimpleTextEditDialog("description", R.id.description,
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    break;
            }
        }
    };

    //
    private void showSimpleTextEditDialog(
            String fieldName, int associateTextViewId, int textInputType) {
        View view = getView();
        if (view == null) { return; }

        String oldText;
        try {
            TextView textView = (TextView) view.findViewById(associateTextViewId);
            oldText = textView.getText().toString();
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

    private void startEditActivity(String fieldName, int textViewIdWithData) {
        // get old data from TextView textViewIdWithData
        String oldData;
        View view = getView();
        if (view == null) { return; }
        try {
            oldData = ((TextView) view.findViewById(textViewIdWithData)).getText().toString();
        } catch (ClassCastException e) {
            throw new ClassCastException("textViewIdWithData does not refer to a TextView");
        }

        //
        Intent intent = new Intent(getContext(), EditActivity.class)
                .putExtra(EditActivity.EXTRA_FIELD_NAME, fieldName)
                .putExtra(EditActivity.EXTRA_INIT_DATA, oldData);
        startActivityForResult(intent, REQUEST_CODE_EDIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_EDIT:
                if (resultCode == EditActivity.RESULT_CODE_OK) {
                    String fieldName = data.getStringExtra(EditActivity.EXTRA_FIELD_NAME);
                    String newData = data.getStringExtra(EditActivity.EXTRA_NEW_DATA);

                    switch (fieldName) {
                        case "tags":
                            Toast.makeText(getActivity(), "to update tags...", Toast.LENGTH_SHORT)
                                    .show();
                            break;
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
