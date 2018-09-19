package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditRemBehaviorFragment extends Fragment
        implements EditActivity.EditResultHolder, TimePickerDialogFragment.Listener,
                   DaysPickerDialogFragment.Listener, SimpleTextEditDialogFragment.Listener {

    private static final String FIELD_NAME = "behavior";
    private static final String KEY_INIT_REM_BEHAVIOR_DATA = "init_rem_behavior_data";
    private static final String KEY_INIT_ALL_SITUATIONS = "init_all_sits";
    private static final String KEY_INIT_ALL_EVENTS = "init_all_events";
    private static final String NONE_INDICATOR = "(none)";

    private String initRemBehaviorData; //display string
    private String initAllSitsDict;
    private String initAllEventsDict;

    private SparseArray<String> allSits;
    private SparseArray<String> allEvents;
    private int spinnerSitEventSelectedPos; //keep record when spinner selection is set, except when
                                            //selected position is 0 (adding new sit./event).

    public EditRemBehaviorFragment() {
        // Required empty public constructor
    }

    public static EditRemBehaviorFragment newInstance(String initRemBehaviorData,
                                                      String initAllSitsDict,
                                                      String initAllEventsDict) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_INIT_REM_BEHAVIOR_DATA, initRemBehaviorData);
        bundle.putString(KEY_INIT_ALL_SITUATIONS, initAllSitsDict);
        bundle.putString(KEY_INIT_ALL_EVENTS, initAllEventsDict);

        EditRemBehaviorFragment fragment = new EditRemBehaviorFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(
                R.layout.fragment_edit_rem_behavior, container, false);

        readBundle(getArguments());

        setSpinnersItems(view);

        setSpinnerListeners((Spinner) view.findViewById(R.id.spinner_model));
        setSpinnerListeners((Spinner) view.findViewById(R.id.spinner_start_type));
        setSpinnerListeners((Spinner) view.findViewById(R.id.spinner_sit_or_event));
        setSpinnerListeners((Spinner) view.findViewById(R.id.spinner_end_type));

        view.findViewById(R.id.button_add).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_edit).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_remove).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_start_time).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_days_of_week).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_end_time).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_done).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_cancel).setOnClickListener(OnButtonClickListener);

        //
        loadData(view, initRemBehaviorData, initAllSitsDict, initAllEventsDict);

        allSits = UtilGeneral.parseAsSparseStringArray(initAllSitsDict);
        allEvents = UtilGeneral.parseAsSparseStringArray(initAllEventsDict);

        //
        hideEditBox(view);

        return view;
    }

    private void readBundle(Bundle bundle) {
        if (bundle == null) {
            throw new RuntimeException("bundle is null");
        }

        initRemBehaviorData = bundle.getString(KEY_INIT_REM_BEHAVIOR_DATA);
        if (initRemBehaviorData == null) {
            throw new RuntimeException("KEY_INIT_REM_BEHAVIOR_DATA not found in bundle");
        }

        initAllSitsDict = bundle.getString(KEY_INIT_ALL_SITUATIONS);
        if (initAllSitsDict == null) {
            throw new RuntimeException("KEY_INIT_ALL_SITUATIONS not found in bundle");
        }

        initAllEventsDict = bundle.getString(KEY_INIT_ALL_EVENTS);
        if (initAllEventsDict == null) {
            throw new RuntimeException("KEY_INIT_ALL_EVENTS not found in bundle");
        }
    }

    private void loadData(View view, String remBehaviourDisplayString,
                          String allSitsDict, String allEVentsDict) {
        SparseArray<String> allSits = UtilGeneral.parseAsSparseStringArray(allSitsDict);
        SparseArray<String> allEvents = UtilGeneral.parseAsSparseStringArray(allEVentsDict);
        ReminderDataBehavior dataBehavior = new ReminderDataBehavior()
                .setFromDisplayString(remBehaviourDisplayString, allSits, allEvents);

        // model //
        Spinner spinnerModel = view.findViewById(R.id.spinner_model);
        spinnerModel.setSelection(dataBehavior.getRemType());

        // repeat pattern //
        if (dataBehavior.isTodoRepeatedlyInPeriod()) {
            view.findViewById(R.id.container_repeat_pattern).setVisibility(View.VISIBLE);
            ((EditText) view.findViewById(R.id.edit_repeat_every)).setText(
                    String.format(Locale.US, "%d", dataBehavior.getRepeatEveryMinutes()));
            ((EditText) view.findViewById(R.id.edit_repeat_offset)).setText(
                    String.format(Locale.US, "%d", dataBehavior.getRepeatOffsetMinutes()));
        } else {
            view.findViewById(R.id.container_repeat_pattern).setVisibility(View.GONE);
        }

        // instant/period list //
        if (dataBehavior.hasNoBoardControl()) {
            view.findViewById(R.id.label_instants_or_periods).setVisibility(View.GONE);
            view.findViewById(R.id.container_instants_periods).setVisibility(View.GONE);
        } else {
            TextView label = view.findViewById(R.id.label_instants_or_periods);
            label.setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_instants_periods).setVisibility(View.VISIBLE);
            LinearLayout listContainer = view.findViewById(R.id.instants_periods_list);

            // label
            if (dataBehavior.isTodoAtInstants()) {
                label.setText(R.string.label_instants);
            } else {
                label.setText(R.string.label_periods);
            }

            // populate list
            String[] listItems;
            if (dataBehavior.isTodoAtInstants()) {
                ReminderDataBehavior.Instant[] instants = dataBehavior.getInstants();
                listItems = new String[instants.length];
                for (int i=0; i<listItems.length; i++) {
                    listItems[i] = instants[i].getDisplayString(allSits, allEvents);
                }
            } else {
                ReminderDataBehavior.Period[] periods = dataBehavior.getPeriods();
                listItems = new String[periods.length];
                for (int i=0; i<listItems.length; i++) {
                    listItems[i] = periods[i].getDisplayString(allSits, allEvents);
                }
            }

            listContainer.removeAllViews();
            for (String item: listItems) {
                addItemToInstantPeriodList(view, item);
            }

            // buttons
            view.findViewById(R.id.button_remove).setVisibility(View.GONE);
            view.findViewById(R.id.button_edit).setVisibility(View.GONE);
        }
    }

    // spinners //
    ArrayAdapter<String> adapterSituations; //for spinner_sit_or_event
    ArrayAdapter<String> adapterEvents; //for spinner_sit_or_event
    SpinnerOnItemSelectListener spinnerOnItemSelectListener = new SpinnerOnItemSelectListener();

    private void setSpinnersItems(View view) {
        // spinner_model
        {
            Spinner spinnerModel = view.findViewById(R.id.spinner_model);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.reminder_modes, R.layout.text_list_item_plain);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerModel.setAdapter(adapter);
        }

        // spinner_start_type
        List<String> items = new ArrayList<>();
        items.add("situation start");
        items.add("situation end");
        items.add("event");
        items.add("time");
        ((Spinner) view.findViewById(R.id.spinner_start_type)).setAdapter(
                new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item, items));

        // spinner_end_type
        List<String> endTypes = new ArrayList<>();
        endTypes.add("duration");
        ((Spinner) view.findViewById(R.id.spinner_end_type)).setAdapter(
                new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item, endTypes));

        // for spinner_sit_or_event
        List<String> allSitsList = UtilGeneral.getValuesOfSparseStringArray(
                UtilGeneral.parseAsSparseStringArray(initAllSitsDict));
        Collections.sort(allSitsList);
        if (allSitsList.isEmpty()) {
            allSitsList.add(NONE_INDICATOR);
        }
        allSitsList.add(0, "New Situation...");
        adapterSituations = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, allSitsList);

        List<String> allEventsList = UtilGeneral.getValuesOfSparseStringArray(
                UtilGeneral.parseAsSparseStringArray(initAllEventsDict));
        Collections.sort(allEventsList);
        if (allEventsList.isEmpty()) {
            allEventsList.add(NONE_INDICATOR);
        }
        allEventsList.add(0, "New Event...");
        adapterEvents = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, allEventsList);
    }

    private class SpinnerOnItemSelectListener
            implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

        /* Detect user selection by onTouch(), which won't be called if spinner selection is done
           by program.
           Method onSpinnerXXXItemUserSelect() will be called here if the selection is by user.
           However, it can be called explicitly whenever & wherever necessary.
        */

        private boolean byUser = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            byUser = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (byUser) {
                switch (parent.getId()) {
                    case R.id.spinner_model:
                        onSpinnerModelItemUserSelect(position);
                        break;
                    case R.id.spinner_start_type:
                        onSpinnerStartTypeItemUserSelect(position);
                        break;
                    case R.id.spinner_end_type:
                        onSpinnerEndTypeItemUserSelect(position);
                        break;
                    case R.id.spinner_sit_or_event:
                        onSpinnerSitEventItemUserSelect(position);
                        break;
                }
            }
            byUser = false;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private void setSpinnerListeners(Spinner spinner) {
        spinner.setOnItemSelectedListener(spinnerOnItemSelectListener);
        spinner.setOnTouchListener(spinnerOnItemSelectListener);
    }

    /** According to `position`, setup the related views and set empty EditText's and default
     *  selection of spinners */
    private void onSpinnerModelItemUserSelect(int position) {
        View fragmentView = getView();
        if (fragmentView == null)
            return;
        switch (position) {
            case 0: // no behavior setting
                fragmentView.findViewById(R.id.container_repeat_pattern).setVisibility(View.GONE);
                fragmentView.findViewById(R.id.label_instants_or_periods).setVisibility(View.GONE);
                fragmentView.findViewById(R.id.container_instants_periods).setVisibility(View.GONE);
                break;

            case 1: // to-do at instants
                fragmentView.findViewById(R.id.container_repeat_pattern).setVisibility(View.GONE);
                fragmentView.findViewById(R.id.label_instants_or_periods).setVisibility(View.VISIBLE);
                fragmentView.findViewById(R.id.container_instants_periods).setVisibility(View.VISIBLE);

                ((TextView) fragmentView.findViewById(R.id.label_instants_or_periods))
                        .setText(R.string.label_instants);
                showEmptyInstantPeriodList();
                break;

            case 2: // reminder during periods
                fragmentView.findViewById(R.id.container_repeat_pattern).setVisibility(View.GONE);
                ((TextView) fragmentView.findViewById(R.id.label_instants_or_periods))
                        .setText(R.string.label_periods);
                showEmptyInstantPeriodList();
                break;

            case 3: // to-do repetitively during periods
                fragmentView.findViewById(R.id.container_repeat_pattern).setVisibility(View.VISIBLE);
                ((EditText) fragmentView.findViewById(R.id.edit_repeat_every)).setText("");
                ((EditText) fragmentView.findViewById(R.id.edit_repeat_offset)).setText("");

                ((TextView) fragmentView.findViewById(R.id.label_instants_or_periods))
                        .setText(R.string.label_periods);
                showEmptyInstantPeriodList();
                break;
        }
    }

    /** According to `position`, setup the related views and set empty EditText's and default
     *  selection of spinners */
    private void onSpinnerStartTypeItemUserSelect(int position) {
        View view = getView();
        if (view == null)
            return;

        if (position == 3) { // time
            view.findViewById(R.id.container_start_sit_event).setVisibility(View.GONE);
            view.findViewById(R.id.container_start_time).setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_days_of_week).setVisibility(View.VISIBLE);
        } else { // situation start/end or event
            view.findViewById(R.id.container_start_sit_event).setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_start_time).setVisibility(View.GONE);
            view.findViewById(R.id.container_days_of_week).setVisibility(View.GONE);
        }

        TextView labelSitEvent = view.findViewById(R.id.label_sit_or_event);
        Spinner spinnerEndType = view.findViewById(R.id.spinner_end_type);

        ArrayAdapter<String> adapterEndTypes = (ArrayAdapter<String>) spinnerEndType.getAdapter();
        adapterEndTypes.clear();
        adapterEndTypes.add("duration");

        if (position == 3) { // time
            ((Button) view.findViewById(R.id.button_start_time)).setText(R.string.label_set);
            ((Button) view.findViewById(R.id.button_days_of_week)).setText(R.string.label_set);
            adapterEndTypes.add("time");
        } else if (position == 2) { // event
            labelSitEvent.setText(R.string.label_event);
            setSpinnerSitEventToEvents(1);
        } else {
            labelSitEvent.setText(R.string.label_situation);
            setSpinnerSitEventToSits(1);
            if (position == 0)
                adapterEndTypes.add("situation end");
        }

        adapterEndTypes.notifyDataSetChanged();
        spinnerEndType.setSelection(0);
        onSpinnerEndTypeItemUserSelect(0);
    }

    /** According to `position`, setup the related views and set empty EditText's and default
     *  selection of spinners */
    private void onSpinnerEndTypeItemUserSelect(int position) {
        View view = getView();
        if (view == null)
            return;
        switch (position) {
            case 0: // duration
                view.findViewById(R.id.container_endcondition_after).setVisibility(View.VISIBLE);
                ((EditText) view.findViewById(R.id.edit_after_minutes)).setText("");
                view.findViewById(R.id.container_end_time).setVisibility(View.GONE);
                break;

            case 1: // situation end or time
                view.findViewById(R.id.container_endcondition_after).setVisibility(View.GONE);

                String item = (String) ((Spinner) view.findViewById(R.id.spinner_end_type))
                        .getItemAtPosition(position);
                if (item.startsWith("time")) {
                    view.findViewById(R.id.container_end_time).setVisibility(View.VISIBLE);
                    ((Button) view.findViewById(R.id.button_end_time)).setText(R.string.label_set);
                } else {
                    view.findViewById(R.id.container_end_time).setVisibility(View.GONE);
                }
                break;
        }
    }

    private void onSpinnerSitEventItemUserSelect(int position) {
        if (position != 0) {
            spinnerSitEventSelectedPos = position;
            return;
        }

        // deal with "add new situation/event"
        View view = getView();
        if (view == null)
            return;

        boolean isEvent; //false: situation
        int startInstantType =
                ((Spinner) view.findViewById(R.id.spinner_start_type)).getSelectedItemPosition();
        if (startInstantType == 0 || startInstantType == 1) { // situation start/end
            isEvent = false;

        } else if (startInstantType == 2) { // event
            isEvent = true;
        } else {
            throw new RuntimeException("start instant type is Time");
        }

        SimpleTextEditDialogFragment dialogFragment = SimpleTextEditDialogFragment.newInstance(
                isEvent ? "New event" : "New situation", "", InputType.TYPE_CLASS_TEXT);
        dialogFragment.show(getFragmentManager(), "new_situation_or_event");
        dialogFragment.setTargetFragment(this, 1);
    }

    private void setSpinnerSitEventToSits(int selectPos) {
        Spinner spinnerSitEvent = getView().findViewById(R.id.spinner_sit_or_event);
        spinnerSitEvent.setAdapter(adapterSituations);
        adapterSituations.notifyDataSetChanged();

        if (selectPos < 0 || selectPos >= adapterSituations.getCount())
            throw new RuntimeException(String.format("bad selectPost (%d)", selectPos));
        spinnerSitEvent.setSelection(selectPos);
        spinnerSitEventSelectedPos = selectPos;
    }

    private void setSpinnerSitEventToSits(String sitToSelect) {
        int pos = 1;
        for (int p=0; p<adapterSituations.getCount(); p++) {
            if (adapterSituations.getItem(p).equals(sitToSelect)) {
                pos = p;
                break;
            }
        }
        setSpinnerSitEventToSits(pos);
    }

    private void setSpinnerSitEventToEvents(int selectPos) {
        Spinner spinnerSitEvent = getView().findViewById(R.id.spinner_sit_or_event);
        spinnerSitEvent.setAdapter(adapterEvents);
        adapterEvents.notifyDataSetChanged();

        if (selectPos < 0 || selectPos >= adapterEvents.getCount())
            throw new RuntimeException(String.format("bad selectPos (%d)", selectPos));
        spinnerSitEvent.setSelection(selectPos);
        spinnerSitEventSelectedPos = selectPos;
    }

    private void setSpinnerSitEventToEvents(String eventToSelect) {
        int pos = 1;
        for (int p=0; p<adapterEvents.getCount(); p++) {
            if (adapterEvents.getItem(p).equals(eventToSelect)) {
                pos = p;
                break;
            }
        }
        setSpinnerSitEventToEvents(pos);
    }

    // list of instants/periods //
    private String instantPeriodListSelectedText = "";

    private View.OnClickListener onInstantPeriodListItemClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();
            if (fragmentView == null)
                return;

            boolean editBoxIsVisible = fragmentView.findViewById(R.id.container_edit_box)
                                     .getVisibility() == View.VISIBLE;
            if (editBoxIsVisible) {
                return;
            }

            if (v.isSelected()) {
                // deselect item
                v.setSelected(false);
                instantPeriodListSelectedText = "";
                fragmentView.findViewById(R.id.button_edit).setVisibility(View.GONE);
                fragmentView.findViewById(R.id.button_remove).setVisibility(View.GONE);
                fragmentView.findViewById(R.id.container_edit_box).setVisibility(View.GONE);
            } else {
                // select item
                deselectInstantsPeriodsList();
                v.setSelected(true);
                instantPeriodListSelectedText = ((TextView) v).getText().toString();
                fragmentView.findViewById(R.id.button_edit).setVisibility(View.VISIBLE);
                fragmentView.findViewById(R.id.button_remove).setVisibility(View.VISIBLE);
            }
        }
    };

    private TextView addItemToInstantPeriodList(View fragmentView, String item) {
        TextView textView = (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.text_list_item_plain_monospace, null);
        textView.setText(item);
        textView.setOnClickListener(onInstantPeriodListItemClick);
        ((LinearLayout) fragmentView.findViewById(R.id.instants_periods_list)).addView(textView);
        return textView;
    }

    private void deselectInstantsPeriodsList() {
        View fragmentView = getView();
        if (fragmentView == null)
            return;
        LinearLayout list = fragmentView.findViewById(R.id.instants_periods_list);
        for (int i=0; i<list.getChildCount(); i++) {
            list.getChildAt(i).setSelected(false);
        }
        fragmentView.findViewById(R.id.button_edit).setVisibility(View.GONE);
        fragmentView.findViewById(R.id.button_remove).setVisibility(View.GONE);

        instantPeriodListSelectedText = "";
    }

    private void showEmptyInstantPeriodList() {
        View fragmentView = getView();
        if (fragmentView == null)
            return;
        fragmentView.findViewById(R.id.label_instants_or_periods).setVisibility(View.VISIBLE);
        fragmentView.findViewById(R.id.container_instants_periods).setVisibility(View.VISIBLE);

        LinearLayout list = fragmentView.findViewById(R.id.instants_periods_list);
        list.removeAllViews();

        fragmentView.findViewById(R.id.button_edit).setVisibility(View.GONE);
        fragmentView.findViewById(R.id.button_remove).setVisibility(View.GONE);
    }

    private String[] getInstantPeriodListItems(View view) {
        LinearLayout list = view.findViewById(R.id.instants_periods_list);
        String[] items = new String[list.getChildCount()];
        for (int i=0; i<items.length; i++) {
            items[i] = ((TextView) list.getChildAt(i)).getText().toString();
        }
        return items;
    }

    private TextView getInstantPeriodListSelectedView(View view) {
        // Returns null if not found.
        LinearLayout layout = view.findViewById(R.id.instants_periods_list);
        for (int i=0; i<layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView && child.isSelected()) {
                return (TextView) child;
            }
        }
        return null;
    }

    private String getInstantPeriodListSelectedText(View view) {
        // Returns "" if not found.
        TextView textView = getInstantPeriodListSelectedView(view);
        if (textView != null)
            return textView.getText().toString();
        else
            return "";
    }

    // buttons //
    private View.OnClickListener OnButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();
            if (fragmentView == null)
                return;

            switch (v.getId()) {
                case R.id.button_add:
                    deselectInstantsPeriodsList();
                    showInstantPeriodEditBox("");
                    break;
                case R.id.button_edit:
                    showInstantPeriodEditBox(instantPeriodListSelectedText);
                    break;
                case R.id.button_remove:
                    actionRemoveInstantPeriod(fragmentView, instantPeriodListSelectedText);
                    deselectInstantsPeriodsList();
                    break;

                case R.id.button_done:
                    if (!validateEditBoxData(fragmentView))
                        return;
                    saveEditBoxData(fragmentView);
                    // (no break)
                case R.id.button_cancel:
                    hideEditBox(fragmentView);
                    fragmentView.findViewById(R.id.spinner_model).setEnabled(true);
                    fragmentView.findViewById(R.id.button_add).setVisibility(View.VISIBLE);

                    // update instantPeriodListSelectedText
                    instantPeriodListSelectedText = getInstantPeriodListSelectedText(fragmentView);
                    if (!instantPeriodListSelectedText.isEmpty()) {
                        fragmentView.findViewById(R.id.button_edit).setVisibility(View.VISIBLE);
                        fragmentView.findViewById(R.id.button_remove).setVisibility(View.VISIBLE);
                    }
                    break;

                case R.id.button_start_time: {
                    String hrMinStr = ((Button) fragmentView.findViewById(R.id.button_start_time))
                            .getText().toString();
                    int[] hrMin = parseHrMin(hrMinStr);
                    TimePickerDialogFragment dialogFragment = TimePickerDialogFragment.newInstance(
                            "Set start time", hrMin[0], hrMin[1], false);
                    dialogFragment.show(getFragmentManager(), "pick_start_time");
                    dialogFragment.setTargetFragment(EditRemBehaviorFragment.this, 1);
                    break;
                }
                case R.id.button_end_time: {
                    String hrMinStr = ((Button) fragmentView.findViewById(R.id.button_end_time))
                            .getText().toString();
                    int[] hrMin = parseHrMin(hrMinStr);
                    TimePickerDialogFragment dialogFragment = TimePickerDialogFragment.newInstance(
                            "Set end time", hrMin[0], hrMin[1], true);
                    dialogFragment.show(getFragmentManager(), "pick_end_time");
                    dialogFragment.setTargetFragment(EditRemBehaviorFragment.this, 1);
                    break;
                }
                case R.id.button_days_of_week: {
                    String selectionStr = ((Button) fragmentView.findViewById(R.id.button_days_of_week))
                            .getText().toString();
                    boolean[] daysSelection = parseDaysOfWeekSelection(selectionStr);
                    DaysPickerDialogFragment dialogFragment = DaysPickerDialogFragment.newInstance(
                            "Select days of week", daysSelection);
                    dialogFragment.show(getFragmentManager(), "select_days_of_week");
                    dialogFragment.setTargetFragment(EditRemBehaviorFragment.this, 1);
                }
            }
        }
    };

    private boolean validateEditBoxData(View view) {
        // Returns false if input data is invalid.

        // validate data
        Button buttonStartTime = view.findViewById(R.id.button_start_time);
        if (buttonStartTime.isShown()) {
            if (buttonStartTime.getText().toString().equalsIgnoreCase("set")) {
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage("Please set (start) time.")
                        .setNeutralButton("OK", null).show();
                return false;
            }
        }

        Button buttonDaysOfWeek = view.findViewById(R.id.button_days_of_week);
        if (buttonDaysOfWeek.isShown()) {
            String text = buttonDaysOfWeek.getText().toString();
            String errMsg = null;
            if (text.equalsIgnoreCase("set"))
                errMsg = "Please select days of week.";
            else if (text.equalsIgnoreCase("none"))
                errMsg = "Days of week cannot be empty.";

            if (errMsg != null) {
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage(errMsg).setNeutralButton("OK", null).show();
                return false;
            }
        }

        EditText editAfterMinutes = view.findViewById(R.id.edit_after_minutes);
        if (editAfterMinutes.isShown()) {
            String text = editAfterMinutes.getText().toString().trim();
            String errMsg = null;
            if (text.isEmpty())
                errMsg = "Please set duration (after ? minutes) of end condition.";
            else if (text.matches("0+"))
                errMsg = "Duration of end condition must be > 0.";

            if (errMsg != null) {
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage(errMsg).setNeutralButton("OK", null).show();
                return false;
            }
        }

        Button buttonEndTime = view.findViewById(R.id.button_end_time);
        if (buttonEndTime.isShown()) {
            String text = buttonEndTime.getText().toString();
            String errMsg = null;
            if (text.equalsIgnoreCase("set"))
                errMsg = "Please set time for end condition.";
            else {
                int endHr = Integer.parseInt(text.split(":")[0]);
                int endMin = Integer.parseInt(text.split(":")[1]);
                String startTimeText = buttonStartTime.getText().toString();
                int startHr = Integer.parseInt(startTimeText.split(":")[0]);
                int startMin = Integer.parseInt(startTimeText.split(":")[1]);
                if ((endHr - startHr)*60 + endMin - startMin <= 0)
                    errMsg = "End time must be later than start time.";
            }

            if (errMsg != null) {
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage(errMsg).setNeutralButton("OK", null).show();
                return false;
            }
        }

        return true;
    }

    private String packEditBoxData(View view) {
        // Returns display string.

        int model = ((Spinner) view.findViewById(R.id.spinner_model)).getSelectedItemPosition();
        if (model == 0)
            return "";

        // get (start) instant
        ReminderDataBehavior.Instant instant = new ReminderDataBehavior.Instant();
        int startType = ((Spinner) view.findViewById(R.id.spinner_start_type))
                .getSelectedItemPosition();
        if (startType == 3) {
            String daysStr = ((Button) view.findViewById(R.id.button_days_of_week)).getText()
                    .toString().replace(" ", "");
            String hrMin = ((Button) view.findViewById(R.id.button_start_time)).getText()
                    .toString();
            instant.setAsTime(new ReminderDataBehavior.Time(daysStr+"."+hrMin));
        } else {
            int pos = ((Spinner) view.findViewById(R.id.spinner_sit_or_event))
                    .getSelectedItemPosition();
            if (startType == 2) {
                int eventId = UtilGeneral.searchSparseStringArrayByValue(
                        allEvents, adapterEvents.getItem(pos));
                instant.setAsEvent(eventId);
            } else {
                int sitId = UtilGeneral.searchSparseStringArrayByValue(
                        allSits, adapterSituations.getItem(pos));
                if (startType == 0)
                    instant.setAsSituationStart(sitId);
                else
                    instant.setAsSituationEnd(sitId);
            }
        }

        // get display string
        String displayStr = "";
        boolean endTypeIsDuration = ((Spinner) view.findViewById(R.id.spinner_end_type))
                .getSelectedItemPosition() == 0;
        if (model == 1) {
            displayStr = instant.getDisplayString(allSits, allEvents);
        } else if (model == 2 || model == 3) {
            ReminderDataBehavior.Period period = new ReminderDataBehavior.Period();
            if (endTypeIsDuration) {
                int duration = Integer.parseInt(
                        ((EditText) view.findViewById(R.id.edit_after_minutes)).getText().toString());
                period.setWithDuration(instant, duration);
            } else {
                if (instant.isSituationStart()) {
                    period.setAsSituationStartEnd(instant.getSituationId());
                } else if (instant.isTime()) {
                    String[] hrMin = ((Button) view.findViewById(R.id.button_end_time))
                            .getText().toString().split(":");
                    int endHr = Integer.parseInt(hrMin[0]);
                    int endMin = Integer.parseInt(hrMin[1]);
                    period.setAsTimeRange(instant.getTime(), endHr, endMin);
                }
            }
            displayStr = period.getDisplayString(allSits, allEvents);
        }
        return displayStr;
    }

    private void saveEditBoxData(View view) {
        String displayStr = packEditBoxData(view);

        TextView selectedListItem = getInstantPeriodListSelectedView(view);
        if (selectedListItem == null) { // adding new item
            TextView textView = addItemToInstantPeriodList(view, displayStr);
            textView.setSelected(true);
        } else { // editing selected item
            selectedListItem.setText(displayStr);
        }
    }

    private int[] parseHrMin(String HrMinStr) {
        // Parse HrMinStr as "<hr>:<min>". If failed, default to 12:00.
        int[] hrMin = new int[2];
        try {
            String[] tokens = HrMinStr.split(":");
            hrMin[0] = Integer.parseInt(tokens[0].trim());
            hrMin[1] = Integer.parseInt(tokens[1].trim());
        } catch (Exception e) {
            hrMin[0] = 12;
            hrMin[1] = 0;
        }
        return hrMin;
    }

    private boolean[] parseDaysOfWeekSelection(String selectionStr) {
        boolean[] sel = new boolean[7];

        if (selectionStr.isEmpty() || selectionStr.equals("none"))
            return sel;

        if (selectionStr.equals("M ~ Su")) {
            for (int i=0; i<7; i++)
                sel[i] = true;
            return sel;
        }

        final String[] daySymbols = ReminderDataBehavior.Time.DAY_SYMBOLS;
        for (int i=0; i<7; i++) {
            if (selectionStr.contains(daySymbols[i]))
                sel[i] = true;
        }
        return sel;
    }

    private void actionRemoveInstantPeriod(View view, String itemText) {
        LinearLayout layout = view.findViewById(R.id.instants_periods_list);
        View viewToRemove = null;
        for (int i=0; i<layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof  TextView) {
                if (((TextView) child).getText().toString().equals(itemText)) {
                    viewToRemove = child;
                    break;
                }
            }
        }
        if (viewToRemove != null) {
            layout.removeView(viewToRemove);
        }
    }

    // editing box //
    private void showInstantPeriodEditBox(String data) {
        // `data`: instant or period display string. It can be empty, in which case show an default
        // editing box for adding new instant or period.
        // Otherwise, load `data` to editing box.

        View view = getView();
        if (view == null) {
            return;
        }

        boolean showDefault = data.isEmpty();
        int model = ((Spinner) view.findViewById(R.id.spinner_model)).getSelectedItemPosition();

        // hide buttons and disable model spinner
        view.findViewById(R.id.button_add).setVisibility(View.GONE);
        view.findViewById(R.id.button_remove).setVisibility(View.GONE);
        view.findViewById(R.id.button_edit).setVisibility(View.GONE);
        view.findViewById(R.id.spinner_model).setEnabled(false);

        // show editing box
        view.findViewById(R.id.container_edit_box).setVisibility(View.VISIBLE);

        // editing-box title
        if (showDefault) {
            ((TextView) view.findViewById(R.id.label_edit_box)).setText(
                    model == 1 ? "Add New Instant" : "Add New Period");
        } else {
            ((TextView) view.findViewById(R.id.label_edit_box)).setText(
                    model == 1 ? "Edit Instant" : "Edit Period");
        }

        // show/hide start-instant label, end-condition label, & end-condition container
        if (model == 1) {
            view.findViewById(R.id.label_start_instant).setVisibility(View.GONE);
            view.findViewById(R.id.label_end_condition).setVisibility(View.GONE);
            view.findViewById(R.id.container_end_cond).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.label_start_instant).setVisibility(View.VISIBLE);
            view.findViewById(R.id.label_end_condition).setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_end_cond).setVisibility(View.VISIBLE);
        }

        //
        setEditBoxContents(showDefault ? "" : data, model);
    }

    private void setEditBoxContents(String data, int remModel) {
        // `data` can be empty, in which case set to default.

        View view = getView();
        if (view == null)
            return;

        Spinner spinnerStartType = view.findViewById(R.id.spinner_start_type);
        LinearLayout containerStartSitEvent = view.findViewById(R.id.container_start_sit_event);
        TextView labelStartSitEvent = view.findViewById(R.id.label_sit_or_event);
        LinearLayout containerStartTime = view.findViewById(R.id.container_start_time);
        LinearLayout containerDaysOfWeek = view.findViewById(R.id.container_days_of_week);
        Spinner spinnerEndType = view.findViewById(R.id.spinner_end_type);
        LinearLayout containerEndCondAfter = view.findViewById(R.id.container_endcondition_after);
        LinearLayout containerEndTime = view.findViewById(R.id.container_end_time);

        //
        ReminderDataBehavior.Period period = null;
        if (!data.isEmpty() && (remModel == 2 || remModel == 3)) {
            period = new ReminderDataBehavior.Period()
                    .setFromDisplayString(data, allSits, allEvents);
        }

        if (data.isEmpty()) {
            spinnerStartType.setSelection(0);
            onSpinnerStartTypeItemUserSelect(0);
        }
        else { //(`data` is not empty)
            ReminderDataBehavior.Instant startInstant;
            if (remModel == 1)
                startInstant = new ReminderDataBehavior.Instant()
                        .setFromDisplayString(data, allSits, allEvents);
            else {
                if (period == null)
                    throw new RuntimeException("`period` is null");
                startInstant = period.getStartInstant();
            }

            if (startInstant.isTime()) {
                spinnerStartType.setSelection(3);
                containerStartSitEvent.setVisibility(View.GONE);

                containerStartTime.setVisibility(View.VISIBLE);
                containerDaysOfWeek.setVisibility(View.VISIBLE);
                String hrMinStr = String.format(Locale.US, "%d:%02d",
                        startInstant.getTime().getHour(), startInstant.getTime().getMinute());
                ((Button) view.findViewById(R.id.button_start_time)).setText(hrMinStr);
                String daysOfWeekStr = startInstant.getTime().getDaysOfWeekDisplayString(", ");
                ((Button) view.findViewById(R.id.button_days_of_week)).setText(daysOfWeekStr);
            } else {
                if (startInstant.isEvent()) {
                    spinnerStartType.setSelection(2);
                    labelStartSitEvent.setText(R.string.label_event);
                    String eventName = allEvents.get(startInstant.getEventId());
                    setSpinnerSitEventToEvents(eventName);
                } else { // (startInstant is situation start or situation end)
                    if (startInstant.isSituationStart())
                        spinnerStartType.setSelection(0);
                    else
                        spinnerStartType.setSelection(1);
                    labelStartSitEvent.setText(R.string.label_situation);
                    String sitName = allSits.get(startInstant.getSituationId());
                    setSpinnerSitEventToSits(sitName);
                }
                containerStartSitEvent.setVisibility(View.VISIBLE);
                containerStartTime.setVisibility(View.GONE);
                containerDaysOfWeek.setVisibility(View.GONE);
            }

            if (remModel == 2 || remModel == 3) {
                // set end-condition-type items
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerEndType.getAdapter();
                adapter.clear();
                adapter.add("duration");
                if (startInstant.isTime())
                    adapter.add("time");
                else if (!startInstant.isEvent())
                    adapter.add("situation end");
                adapter.notifyDataSetChanged();

                //
                if (period.isEndingAfterDuration()) {
                    spinnerEndType.setSelection(0);
                    containerEndCondAfter.setVisibility(View.VISIBLE);
                    containerEndTime.setVisibility(View.GONE);

                    int duration = period.getDurationMinutes();
                    ((EditText) view.findViewById(R.id.edit_after_minutes)).setText(
                            String.format(Locale.US, "%d", duration));
                } else {
                    spinnerEndType.setSelection(1);
                    containerEndCondAfter.setVisibility(View.GONE);
                    if (period.isTimeRange()) {
                        containerEndTime.setVisibility(View.VISIBLE);

                        int[] hrMin = period.getEndHrMin();
                        ((Button) view.findViewById(R.id.button_end_time)).setText(
                                String.format(Locale.US, "%d:%02d", hrMin[0], hrMin[1]));
                    } else {
                        containerEndTime.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private void hideEditBox(View view) {
        view.findViewById(R.id.label_start_instant).setVisibility(View.GONE);
        view.findViewById(R.id.container_start_time).setVisibility(View.GONE);
        view.findViewById(R.id.container_days_of_week).setVisibility(View.GONE);
        view.findViewById(R.id.label_end_condition).setVisibility(View.GONE);
        view.findViewById(R.id.container_endcondition_after).setVisibility(View.GONE);
        view.findViewById(R.id.container_end_time).setVisibility(View.GONE);
        view.findViewById(R.id.container_end_cond).setVisibility(View.GONE);
        view.findViewById(R.id.container_edit_box).setVisibility(View.GONE);
    }

    //
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int newHr, int newMin) {
        View view = getView();
        if (view == null)
            return;

        String HrMin = String.format(Locale.US, "%d:%02d", newHr, newMin);
        switch (dialog.getTag()) {
            case "pick_start_time":
                ((Button) view.findViewById(R.id.button_start_time)).setText(HrMin);
                break;
            case "pick_end_time":
                ((Button) view.findViewById(R.id.button_end_time)).setText(HrMin);
                break;
        }
    }

    @Override
    public void onDaysPickerDialogPositiveClick(DialogFragment dialog, boolean[] newSelection) {
        View view = getView();
        if (view == null)
            return;

        String selectionStr;
        if (newSelection[0] & newSelection[1] & newSelection[2] & newSelection[3]
                & newSelection[4] & newSelection[5] & newSelection[6]) {
            selectionStr = "M ~ Su";
        } else {
            final String[] daySymbols = ReminderDataBehavior.Time.DAY_SYMBOLS;
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (int i = 1; i <= 7; i++) {
                int d = (i == 7) ? 0 : i;
                if (newSelection[d]) {
                    if (!first)
                        builder.append(", ");
                    builder.append(daySymbols[d]);
                    first = false;
                }
            }
            selectionStr = builder.toString();

            if (selectionStr.isEmpty())
                selectionStr = "none";
        }

        ((Button) view.findViewById(R.id.button_days_of_week)).setText(selectionStr);
    }

    @Override
    public void onDialogClick(DialogFragment dialog, boolean positive, String newText) {
        View view = getView();
        if (view == null)
            return;

        switch (dialog.getTag()) {
            case "new_situation_or_event":
                if (positive) {
                    int startInstantType = ((Spinner) view.findViewById(R.id.spinner_start_type))
                                    .getSelectedItemPosition();
                    if (startInstantType == 3)
                        throw new RuntimeException("start instant type is Time");

                    newText = newText.trim();
                    if (newText.isEmpty()) { // empty situation/event input
                        Toast.makeText(getContext(), "Empty input ignored", Toast.LENGTH_SHORT)
                                .show();
                        restoreSpinnerSitEventSelection(view);
                    } else {
                        int pos = searchAdapter(
                                startInstantType == 2 ? adapterEvents : adapterSituations, newText);
                        if (pos != -1) { // situation/event already exists
                            Toast.makeText(getContext(),
                                    (startInstantType == 2 ? "Event" : "Situation")
                                            + " already exists", Toast.LENGTH_SHORT).show();
                            ((Spinner) view.findViewById(R.id.spinner_sit_or_event)).setSelection(pos);
                        } else { // new situation/event
                            addNewSituationOrEvent(view, startInstantType != 2, newText);
                        }
                    }
                } else { // user canceled dialog
                    restoreSpinnerSitEventSelection(view);
                }
                break;
        }
    }

    private void addNewSituationOrEvent(@NonNull View view, boolean isSituation, String newSitEvent) {
        if (newSitEvent.contains(",")) {
            newSitEvent = newSitEvent.replace(',', '.');
            Toast.makeText(getContext(), "commas ',' replaced by '.'", Toast.LENGTH_LONG)
                    .show();
        }

        SparseArray<String> allSitOrEvents = isSituation ? allSits : allEvents;
        ArrayAdapter<String> adapter = isSituation ? adapterSituations : adapterEvents;
        Spinner spinnerSitEvent = view.findViewById(R.id.spinner_sit_or_event);

        // 1. add to allSits/allEvents
        int newId = (allSitOrEvents.size() == 0) ? 0 :
                Collections.max(UtilGeneral.getKeysOfSparseStringArray(allSitOrEvents)) + 1;
        allSitOrEvents.append(newId, newSitEvent);

        // 2. add to adapterSituations/adapterEvents
        if (allSitOrEvents.size() == 0) {
            adapter.remove(NONE_INDICATOR);
        }
        int posInsert = -1;
        for (int p=1; p<adapter.getCount(); p++) {
            if (newSitEvent.compareTo(adapter.getItem(p)) < 0) {
                posInsert = p;
                break;
            }
        }
        if (posInsert == -1)
            adapter.add(newSitEvent);
        else
            adapter.insert(newSitEvent, posInsert);
        adapter.notifyDataSetChanged();

        // 2. spinner selects new situation/event
        int selectPos = (posInsert == -1) ? (adapter.getCount() - 1) : posInsert;
        spinnerSitEvent.setSelection(selectPos);
        spinnerSitEventSelectedPos = selectPos;
    }

    private void restoreSpinnerSitEventSelection(View view) {
        ((Spinner) view.findViewById(R.id.spinner_sit_or_event)).setSelection(spinnerSitEventSelectedPos);
    }

    private int searchAdapter(ArrayAdapter<String> adapter, String text) {
        // Returns -1 if not found.
        for (int p = 0; p < adapter.getCount(); p++) {
            if (adapter.getItem(p).equals(text))
                return p;
        }
        return -1;
    }

    // interface EditActivity.EditResultHolder
    @Override
    public boolean validateData() {
        View view = getView();
        if (view == null)
            return true;

        if (view.findViewById(R.id.container_edit_box).isShown()) {
            Toast.makeText(getContext(), "Please finish editing first.", Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        //
        EditText editRepeatEvery = view.findViewById(R.id.edit_repeat_every);
        if (editRepeatEvery.isShown()) {
            String text = editRepeatEvery.getText().toString().trim();
            String msg = null;
            if (text.isEmpty())
                msg = "Please set repeat period (repeat every ? min) for repeat pattern.";
            else {
                if (Integer.parseInt(text) <= 0)
                    msg = "Repeat period (repeat every ? min) should be > 0.";
            }

            if (msg != null) {
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage(msg).setNeutralButton("OK", null).show();
                return false;
            }
        }

        //
        EditText editRepeatOffset = view.findViewById(R.id.edit_repeat_offset);
        if (editRepeatOffset.isShown()) {
            if (editRepeatOffset.getText().toString().trim().isEmpty()) {
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage("Please set offset for repeat pattern.")
                        .setNeutralButton("OK", null).show();
                return false;
            }
        }

        //
        int model = ((Spinner) view.findViewById(R.id.spinner_model)).getSelectedItemPosition();
        LinearLayout list = view.findViewById(R.id.instants_periods_list);
        if (model != 0) {
            if (list.getChildCount() == 0) {
                String msg = (model == 1) ?
                        "Please set at least one instant." : "Please set at least one period.";
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage(msg).setNeutralButton("OK", null).show();
                return false;
            }
        }

        //
        if (model != 0) {
            String[] instantsPeriods = getInstantPeriodListItems(view);
            if (UtilGeneral.hasDuplicatedElement(instantsPeriods)) {
                String msg = "Please remove duplicated " + ((model == 1) ? "instant." : "period.");
                new AlertDialog.Builder(getContext()).setTitle("Invalid settings")
                        .setMessage(msg).setNeutralButton("OK", null).show();
                return false;
            }
        }

        return true;
    }

    @Override
    public Intent getResult() {
        View view = getView();
        if (view == null)
            throw new RuntimeException("`view` is null");

        //
        int model = ((Spinner) view.findViewById(R.id.spinner_model)).getSelectedItemPosition();
        LinearLayout list = view.findViewById(R.id.instants_periods_list);

        ReminderDataBehavior behavior = new ReminderDataBehavior(); //default to no behavior setting
        if (model != 0) {
            String[] displayStrings = getInstantPeriodListItems(view);
            switch (model) {
                case 1:
                    behavior.setAsTodoAtInstants(displayStrings, allSits, allEvents);
                    break;
                case 2:
                    behavior.setAsReminderInPeriod(displayStrings, allSits, allEvents);
                    break;
                case 3:
                    int repeatEvery = Integer.parseInt(
                            ((EditText) view.findViewById(R.id.edit_repeat_every))
                                    .getText().toString().trim());
                    int repeatOffset = Integer.parseInt(
                            ((EditText) view.findViewById(R.id.edit_repeat_offset))
                                    .getText().toString().trim());
                    behavior.setAsTodoRepeatedlyInPeriod(displayStrings, repeatEvery, repeatOffset,
                            allSits, allEvents);
                    break;
            }
        }
        String newBehaviorData = behavior.getStringRepresentation();

        //
        String newAllSitsDict = UtilGeneral.stringifySparseStringArray(allSits);
        String newAllEventsDict = UtilGeneral.stringifySparseStringArray(allEvents);

        //
        return new Intent()
                .putExtra(EditActivity.EXTRA_FIELD_NAME, FIELD_NAME)
                .putExtra(EditActivity.EXTRA_NEW_DATA, newBehaviorData)
                .putExtra(EditActivity.EXTRA_NEW_ALL_SITUATIONS, newAllSitsDict)
                .putExtra(EditActivity.EXTRA_NEW_ALL_EVENTS, newAllEventsDict);
    }
}
