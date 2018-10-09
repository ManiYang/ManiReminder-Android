package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DetailFragment extends Fragment implements SimpleTextEditDialogFragment.Listener {

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

        view.findViewById(R.id.label_quick_notes).setOnClickListener(viewsOnClickListener);
        view.findViewById(R.id.quick_notes).setOnClickListener(viewsOnClickListener);

        view.findViewById(R.id.label_checklist).setOnClickListener(viewsOnClickListener);
        view.findViewById(R.id.container_checklist).setOnClickListener(viewsOnClickListener);
        view.findViewById(R.id.checklist_textview).setOnClickListener(viewsOnClickListener);

        view.findViewById(R.id.button_remove_quick_notes).setOnClickListener(buttonsOnClickListener);

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

    //
    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
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
        allSituations = UtilStorage.getAllSituations(getContext());
        allEvents = UtilStorage.getAllEvents(getContext());

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
        String quickNotes = cursor.getString(2);
        if (quickNotes == null) {
            quickNotes = "";
        }
        String checkListStr = cursor.getString(3);
        if (checkListStr == null) {
            checkListStr = "";
        }
        cursor.close();

        ((TextView) view.findViewById(R.id.description)).setText(
                description.isEmpty() ? NONE_INDICATOR : description);
        ((TextView) view.findViewById(R.id.quick_notes)).setText(
                quickNotes.isEmpty() ? NONE_INDICATOR : quickNotes);
        view.findViewById(R.id.button_remove_quick_notes).setVisibility(
                quickNotes.isEmpty() ? View.GONE : View.VISIBLE);
        loadChecklist(view, checkListStr);

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

    private void loadChecklist(View view, String checkListStr) {
        EditRemChecklistFragment.ChecklistInfo info =
                EditRemChecklistFragment.parseChecklistStr(checkListStr);

        if (info.texts.isEmpty()) {
            view.findViewById(R.id.checklist_textview).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.checklist_textview)).setText(NONE_INDICATOR);
        } else {
            view.findViewById(R.id.checklist_textview).setVisibility(View.GONE);
        }

        LinearLayout layout = view.findViewById(R.id.container_checklist);
        layout.removeAllViews();
        for (int i=0; i<info.texts.size(); i++) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(info.texts.get(i));
            checkBox.setChecked(info.checked.get(i));
            layout.addView(checkBox,
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private String stringifyChecklist() {
        View view = getView();
        LinearLayout layout = view.findViewById(R.id.container_checklist);

        List<String> texts = new ArrayList<>();
        List<Boolean> checked = new ArrayList<>();
        for (int i=0; i<layout.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) layout.getChildAt(i);
            texts.add(checkBox.getText().toString());
            checked.add(checkBox.isChecked());
        }
        return EditRemChecklistFragment.stringifyChecklistData(texts, checked);
    }

    // OnClick listener for views
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

                case R.id.label_checklist:
                case R.id.container_checklist:
                case R.id.checklist_textview:
                    startEditActivity("checklist", stringifyChecklist(),
                            false, false);
                    break;

                case R.id.container_behavior_settings:
                    if (UtilStorage.isReminderOpened(getContext(), reminderId)) {
                        new AlertDialog.Builder(getContext()).setTitle("Warning")
                                .setMessage("The reminder is currently opened. "
                                        + "It may be closed after you update its behavior settings.")
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startEditActivity("behavior",
                                                behaviorData.getDisplayString(allSituations, allEvents),
                                                false, true);
                                    }
                                }).show();
                    } else {
                        startEditActivity("behavior",
                                behaviorData.getDisplayString(allSituations, allEvents),
                                false, true);
                    }
                    break;

                case R.id.label_quick_notes:
                case R.id.quick_notes:
                    showSimpleTextEditDialog("quick_notes", R.id.quick_notes,
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    break;
            }
        }
    };

    // OnClick listener for buttons
    private View.OnClickListener buttonsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_remove_quick_notes:
                    new AlertDialog.Builder(getContext()).setTitle("Remove quick notes")
                            .setMessage("Are you sure?").setCancelable(true)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    actionDeleteQuickNotes();
                                }
                            }).show();
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
        dialogFragment.setTargetFragment(this, 1);
    }

    @Override
    public void onDialogClick(DialogFragment dialog, boolean positive, String newText) {
        // update both DB and view (activity will not resume)

        if (!positive)
            return;

        View view = getView();
        if (view == null)
            return;

        boolean doneDbUpdate = false;
        switch (dialog.getTag()) {
            case "dialog_edit_title": {
                // update title
                newText = newText.trim();
                if (newText.isEmpty()) {
                    newText = "Untitled";
                }
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
                newText = newText.trim();
                ((TextView) view.findViewById(R.id.description)).setText(
                        newText.isEmpty() ? NONE_INDICATOR : newText);
                ContentValues values = new ContentValues();
                values.put("description", newText);
                int check = db.update(MainDbHelper.TABLE_REMINDERS_DETAIL, values,
                        "_id = ?", new String[]{Integer.toString(reminderId)});
                doneDbUpdate = (check == 1);
                break;
            }
            case "dialog_edit_quick_notes": {
                // update quick notes
                newText = newText.trim();
                if (newText.isEmpty()) {
                    ((TextView) view.findViewById(R.id.quick_notes)).setText(NONE_INDICATOR);
                    view.findViewById(R.id.button_remove_quick_notes).setVisibility(View.GONE);
                } else {
                    ((TextView) view.findViewById(R.id.quick_notes)).setText(newText);
                    view.findViewById(R.id.button_remove_quick_notes).setVisibility(View.VISIBLE);
                }

                ContentValues values = new ContentValues();
                values.put("quick_notes", newText);
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
        View view = getView();
        if (view == null)
            return;
        String remTitle = ((TextView) view.findViewById(R.id.title)).getText().toString();

        Intent intent = new Intent(getContext(), EditActivity.class)
                .putExtra(EditActivity.EXTRA_REMINDER_TITLE, remTitle)
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
                intentNewData.getStringExtra(EditActivity.EXTRA_NEW_ALL_TAGS); //may be null
        String newAllSitsDict =
                intentNewData.getStringExtra(EditActivity.EXTRA_NEW_ALL_SITUATIONS); //may be null
        String newAllEventsDict =
                intentNewData.getStringExtra(EditActivity.EXTRA_NEW_ALL_EVENTS); //may be null

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
                if (newAllSitsDict == null || newAllEventsDict == null) {
                    throw new RuntimeException("new all-situations and all-events should be given");
                }

                Log.v("mainlog", String.format("rem %d behavior settings updated", reminderId));

                Calendar now = Calendar.getInstance();
                new ReminderBoardLogic(getContext()).beforeReminderBehaviorUpdate(reminderId);

                //
                allSituations = UtilGeneral.parseAsSparseStringArray(newAllSitsDict);
                allEvents = UtilGeneral.parseAsSparseStringArray(newAllEventsDict);
                UtilStorage.addNewSituationsEvents(db, allSituations, allEvents);

                ReminderDataBehavior behavior = new ReminderDataBehavior()
                        .setFromStringRepresentation(newData);
                String involvedSitsStr = ","
                        + UtilGeneral.joinIntegerList(",", behavior.getInvolvedSituationIds())
                        + ",";
                String involvedEventsStr = ","
                        + UtilGeneral.joinIntegerList(",", behavior.getInvolvedEventIds())
                        + ",";
                boolean involvesTimeInStartInst = behavior.involvesTimeInStartInstant();
                ContentValues values = new ContentValues();
                values.put("type", behavior.getRemType());
                values.put("behavior_settings", newData);
                values.put("involved_sits", involvedSitsStr);
                values.put("involved_events", involvedEventsStr);
                values.put("involve_time_in_start_instant", involvesTimeInStartInst ? 1 : 0);
                int check = db.update(MainDbHelper.TABLE_REMINDERS_BEHAVIOR, values,
                        "_id = ?", new String[] {Integer.toString(reminderId)});
                if (check != 1) {
                    Toast.makeText(getContext(), "Could not update database", Toast.LENGTH_LONG)
                            .show();
                }

                //
                new ReminderBoardLogic(getContext())
                        .afterReminderBehaviorUpdate(reminderId, behavior, now);
                break;
            }
            case "checklist": {
                ContentValues values = new ContentValues();
                values.put("checklist", newData);
                int check = db.update(MainDbHelper.TABLE_REMINDERS_DETAIL, values,
                        "_id = ?", new String[] {Integer.toString(reminderId)});
                if (check != 1) {
                    Toast.makeText(getContext(), "Could not update database", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
        }
    }

    private boolean saveAllTagsToDb(SparseArray<String> allTags, SQLiteDatabase db) {
        db.delete(MainDbHelper.TABLE_TAGS, null, null); //delete all records
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

    private void actionDeleteQuickNotes() {
        View view = getView();
        TextView quickNotes = view.findViewById(R.id.quick_notes);
        if (quickNotes.getText().length() == 0
                || quickNotes.getText().toString().equals(NONE_INDICATOR)) {
            return;
        }

        quickNotes.setText(NONE_INDICATOR);
        view.findViewById(R.id.button_remove_quick_notes).setVisibility(View.GONE);

        ContentValues values = new ContentValues();
        values.put("quick_notes", "");
        db.update(MainDbHelper.TABLE_REMINDERS_DETAIL, values,
                "_id = ?", new String[]{Integer.toString(reminderId)});
    }
}
