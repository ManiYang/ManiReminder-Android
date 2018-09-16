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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailFragment extends Fragment {

    private int reminderId = -9;
    private SQLiteDatabase db;
    private final int REQUEST_CODE_EDIT = 0;
    private static final String NONE_INDICATOR = "(none)";

    public DetailFragment() {}

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

        view.findViewById(R.id.container_behavior_settings).setOnClickListener(viewsOnClickListener);

        return view;
    }

    @Override
    public void onResume() {
        Log.v("DetailFragment", "=== resume");
        super.onResume();

        //Log.v("DetailFragment", "### Reminder ID: " + Integer.toString(reminderId));
        if (reminderId < 0) {
            throw new RuntimeException("reminderId < 0");
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
    private ReminderDataBehavior behaviorData;
    SparseArray<String> allSituations;
    SparseArray<String> allEvents;
    SparseArray<String> allTags;

    private void loadReminderData() {
        if (db == null) {
            return;
        }

        View view = getView();
        if (view == null) {
            return;
        }

        // get all tags, situations, events
        allTags = UtilReminder.getAllTagsFromDb(db);
        allSituations = UtilReminder.getAllSituationsFromDb(db);
        allEvents = UtilReminder.getAllEventsFromDb(db);

        // get and load brief data
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

        ((TextView) view.findViewById(R.id.title)).setText(title);
        ((TextView) view.findViewById(R.id.tags)).setText(tagsStr);

        // get and load detailed data
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

        ((TextView) view.findViewById(R.id.description)).setText(description);

        // get and load behavior settings data
        cursor = db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR, null,
                "_id = ?", new String[] {Integer.toString(reminderId)},
                null, null, null);
          if (!cursor.moveToFirst()) {
            throw new RuntimeException(String.format("Reminder (id: %d) not found in table '%s'",
                    reminderId, MainDbHelper.TABLE_REMINDERS_BEHAVIOR));
        }
        int remType = cursor.getInt(1);
        String behaviorSettingDbString = cursor.getString(2);
        cursor.close();
        loadBehaviorData(remType, behaviorSettingDbString, allSituations, allEvents);
    }

    private void loadBehaviorData(int remType, String stringRepresentation,
                                  SparseArray<String> allSituations, SparseArray<String> allEvents) {
        behaviorData = new ReminderDataBehavior().setFromStringRepresentation(stringRepresentation);
        if (behaviorData.getRemType() != remType) {
            throw new RuntimeException("behaviorData.getRemType() != remType");
        }

        View view = getView();
        TextView tvModel = view.findViewById(R.id.model);
        LinearLayout layoutRepeatSpec = view.findViewById(R.id.layout_repeat_spec);
        TextView labelInstantsOrPeriods = view.findViewById(R.id.label_instants_or_periods);
        LinearLayout layoutInstantsOrPeriodsList =
                view.findViewById(R.id.layout_instants_or_periods_list);

        String[] instantsOrPeriodsDisplayStrings = null;
        switch (remType) {
            case ReminderDataBehavior.TYPE_NO_BOARD_CONTROL:
                tvModel.setText(getResources().getString(R.string.no_settings));
                layoutRepeatSpec.setVisibility(View.GONE);
                labelInstantsOrPeriods.setVisibility(View.GONE);
                layoutInstantsOrPeriodsList.setVisibility(View.GONE);
                break;

            case ReminderDataBehavior.TYPE_TODO_AT_INSTANTS: {
                tvModel.setText(getResources().getString(R.string.todo_at_instants));
                layoutRepeatSpec.setVisibility(View.GONE);
                labelInstantsOrPeriods.setVisibility(View.VISIBLE);
                labelInstantsOrPeriods.setText(getResources().getString(R.string.label_instants));

                ReminderDataBehavior.Instant[] instants = behaviorData.getInstants();
                instantsOrPeriodsDisplayStrings = new String[instants.length];
                for (int i = 0; i < instants.length; i++) {
                    instantsOrPeriodsDisplayStrings[i] =
                            instants[i].getDisplayString(allSituations, allEvents);
                }
                break;
            }
            case ReminderDataBehavior.TYPE_REMINDER_IN_PERIOD: {
                tvModel.setText(getResources().getString(R.string.remind_during_periods));
                layoutRepeatSpec.setVisibility(View.GONE);
                labelInstantsOrPeriods.setVisibility(View.VISIBLE);
                labelInstantsOrPeriods.setText(getResources().getString(R.string.label_periods));

                ReminderDataBehavior.Period[] periods = behaviorData.getPeriods();
                instantsOrPeriodsDisplayStrings = new String[periods.length];
                for (int i = 0; i < periods.length; i++) {
                    instantsOrPeriodsDisplayStrings[i] = periods[i].getDisplayString(allSituations, allEvents);
                }
                break;
            }
            case ReminderDataBehavior.TYPE_TODO_REPETITIVE_IN_PERIOD: {
                tvModel.setText(getResources().getString(R.string.todo_repeatedly_during_periods));

                layoutRepeatSpec.setVisibility(View.VISIBLE);
                String repeatSpecStr = String.format(Locale.US, "every %d min, offset %d min",
                        behaviorData.getRepeatEveryMinutes(), behaviorData.getRepeatOffsetMinutes());
                ((TextView) view.findViewById(R.id.repeat_spec)).setText(repeatSpecStr);

                labelInstantsOrPeriods.setVisibility(View.VISIBLE);
                labelInstantsOrPeriods.setText(getResources().getString(R.string.label_periods));

                ReminderDataBehavior.Period[] periods = behaviorData.getPeriods();
                instantsOrPeriodsDisplayStrings = new String[periods.length];
                for (int i = 0; i < periods.length; i++) {
                    instantsOrPeriodsDisplayStrings[i] = periods[i].getDisplayString(allSituations, allEvents);
                }
                break;
            }
        }

        // populate list of instants/periods with instantsOrPeriodsDisplayStrings
        if (remType != ReminderDataBehavior.TYPE_NO_BOARD_CONTROL) {
            layoutInstantsOrPeriodsList.setVisibility(View.VISIBLE);
            layoutInstantsOrPeriodsList.removeAllViews();
            if (instantsOrPeriodsDisplayStrings != null) {
                for (String s : instantsOrPeriodsDisplayStrings) {
                    TextView textView = (TextView) LayoutInflater.from(getContext())
                            .inflate(R.layout.text_list_item_plain_monospace, null);
                    textView.setText(s);
                    layoutInstantsOrPeriodsList.addView(textView);
                }
            }
        }
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
                    startEditActivity("tags", R.id.tags, true, false);
                    break;

                case R.id.label_description:
                case R.id.description:
                    showSimpleTextEditDialog("description", R.id.description,
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    break;

                case R.id.container_behavior_settings:
                    startEditActivity("behavior",
                            behaviorData.getDisplayString(allSituations, allEvents),
                            false, true);
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
            TextView textView = view.findViewById(associateTextViewId);
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
        // update both DB and view (activity will not resume)

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
                                   boolean needAllTags, boolean needAllSitsEvents) {
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
        startEditActivity(fieldName, oldData, needAllTags, needAllSitsEvents);
    }

    private void startEditActivity(String fieldName, String initialData,
                                   boolean needAllTags, boolean needAllSitsEvents) {
        Intent intent = new Intent(getContext(), EditActivity.class)
                .putExtra(EditActivity.EXTRA_FIELD_NAME, fieldName)
                .putExtra(EditActivity.EXTRA_INIT_DATA, initialData);

        if (needAllTags) {
            intent.putExtra(EditActivity.EXTRA_INIT_ALL_TAGS,
                    UtilGeneral.stringifySparseStringArray(allTags));
        }

        if (needAllSitsEvents) {
            intent.putExtra(EditActivity.EXTRA_INIT_ALL_SITUATIONS,
                    UtilGeneral.stringifySparseStringArray(allSituations));
            intent.putExtra(EditActivity.EXTRA_INIT_ALL_EVENTS,
                    UtilGeneral.stringifySparseStringArray(allEvents));
        }

        startActivityForResult(intent, REQUEST_CODE_EDIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("DetailFragment", "=== onActivityResult");

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
        // Just update data in database. No need to update view, as the activity will soon be
        // resumed.
        String fieldName = intentNewData.getStringExtra(EditActivity.EXTRA_FIELD_NAME);
        String newData = intentNewData.getStringExtra(EditActivity.EXTRA_NEW_DATA);
        String newAllTagsDict =
                intentNewData.getStringExtra(EditActivity.EXTRA_NEW_ALL_TAGS); //can be null

        switch (fieldName) {
            case "tags": {
                if (newAllTagsDict == null) {
                    throw new RuntimeException("new all-tags should be given");
                }

                allTags = UtilGeneral.parseAsSparseStringArray(newAllTagsDict);
                boolean b = saveAllTagsToDb(allTags, db);
                if (!b) {
                    Toast.makeText(getContext(), "Could not write to database", Toast.LENGTH_LONG)
                            .show();
                    return;
                }

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
            case "behavior": {
                // TODO...
                Toast.makeText(getContext(), "to update behavior data...", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private boolean saveAllTagsToDb(SparseArray<String> allTags, SQLiteDatabase db) {
        db.delete(MainDbHelper.TABLE_TAGS, null, null);
        for (int i=0; i<allTags.size(); i++) {
            ContentValues values = new ContentValues();
            values.put("_id", allTags.keyAt(i));
            values.put("name", allTags.valueAt(i));
            long check = db.insert(MainDbHelper.TABLE_TAGS, null, values);
            if (check == -1) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<Integer> getTagIds(List<String> tags, SparseArray<String> allTags) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (String tag: tags) {
            int id = UtilGeneral.searchSparseStringArrayByValue(allTags, tag);
            if (id != -1) {
                ids.add(id);
            } else {
                throw new RuntimeException("tag not found in 'allTags'");
            }
        }
        return ids;
    }
}
