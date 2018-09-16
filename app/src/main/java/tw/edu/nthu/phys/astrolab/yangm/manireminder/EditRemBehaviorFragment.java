package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditRemBehaviorFragment extends Fragment
        implements EditActivity.EditResultHolder {

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
        ((Spinner) view.findViewById(R.id.spinner_model))
                .setOnItemSelectedListener(new SpinnerOnItemSelectListener());
        ((Spinner) view.findViewById(R.id.spinner_start_type))
                .setOnItemSelectedListener(new SpinnerOnItemSelectListener());
        ((Spinner) view.findViewById(R.id.spinner_sit_or_event))
                .setOnItemSelectedListener(new SpinnerOnItemSelectListener());
        ((Spinner) view.findViewById(R.id.spinner_end_type))
                .setOnItemSelectedListener(new SpinnerOnItemSelectListener());

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
        SpinnerOnItemSelectListener listener =
                (SpinnerOnItemSelectListener) spinnerModel.getOnItemSelectedListener();
        if (listener != null)
            listener.setOtherViews = false;
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
                TextView textView = (TextView) LayoutInflater.from(getContext())
                        .inflate(R.layout.text_list_item_plain_monospace, null);
                textView.setText(item);
                textView.setOnClickListener(onInstantPeriodListItemClick);
                listContainer.addView(textView);
            }

            // buttons
            view.findViewById(R.id.button_remove).setVisibility(View.GONE);
            view.findViewById(R.id.button_edit).setVisibility(View.GONE);
        }
    }

    // spinners //
    ArrayAdapter<String> adapterSituations; //for spinner_sit_or_event
    ArrayAdapter<String> adapterEvents; //for spinner_sit_or_event

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
        List<String> allSits = UtilGeneral.getValuesOfSparseStringArray(
                UtilGeneral.parseAsSparseStringArray(initAllSitsDict));
        if (allSits.isEmpty()) {
            allSits.add(NONE_INDICATOR);
        }
        allSits.add(0, "New Situation...");
        adapterSituations = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, allSits);

        List<String> allEvents = UtilGeneral.getValuesOfSparseStringArray(
                UtilGeneral.parseAsSparseStringArray(initAllEventsDict));
        if (allEvents.isEmpty()) {
            allEvents.add(NONE_INDICATOR);
        }
        allEvents.add(0, "New Event...");
        adapterEvents = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, allEvents);
    }

    private class SpinnerOnItemSelectListener implements AdapterView.OnItemSelectedListener {
        private boolean setOtherViews = true;

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getId()) {
                case R.id.spinner_model:
                    if (setOtherViews)
                        onSpinnerModelItemUserSelect(position);
                    break;
                case R.id.spinner_start_type:
                    if (setOtherViews)
                        onSpinnerStartTypeItemUserSelect(position);
                    break;
                case R.id.spinner_end_type:
                    if (setOtherViews)
                        onSpinnerEndTypeItemUserSelect(position);
                    break;
            }
            setOtherViews = true;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

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
        fragmentView.findViewById(R.id.container_edit_box).setVisibility(View.GONE);
    }

    private void onSpinnerStartTypeItemUserSelect(int position) {
        View view = getView();
        if (view == null)
            return;

        Log.v("EditRemBehaviorFragment", "### spinner start-type selected: "+position);

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
        Spinner spinnerSitEvent = view.findViewById(R.id.spinner_sit_or_event);
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
            spinnerSitEvent.setAdapter(adapterEvents);
            adapterEvents.notifyDataSetChanged();
            spinnerSitEvent.setSelection(1);
        } else {
            labelSitEvent.setText(R.string.label_situation);
            spinnerSitEvent.setAdapter(adapterSituations);
            adapterSituations.notifyDataSetChanged();
            spinnerSitEvent.setSelection(1);
            if (position == 0)
                adapterEndTypes.add("situation end");
        }

        adapterEndTypes.notifyDataSetChanged();
        spinnerEndType.setSelection(0);
        onSpinnerEndTypeItemUserSelect(0);
    }

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

    private void setSpinnerSitEventToSits(String sitToSelect) {
        Spinner spinnerSitEvent = getView().findViewById(R.id.spinner_sit_or_event);
        spinnerSitEvent.setAdapter(adapterSituations);
        adapterSituations.notifyDataSetChanged();

        int pos = 1;
        for (int p=0; p<adapterSituations.getCount(); p++) {
            if (adapterSituations.getItem(p).equals(sitToSelect)) {
                pos = p;
                break;
            }
        }
        spinnerSitEvent.setSelection(pos);
    }

    private void setSpinnerSitEventToEvents(String eventToSelect) {
        Spinner spinnerSitEvent = getView().findViewById(R.id.spinner_sit_or_event);
        spinnerSitEvent.setAdapter(adapterEvents);
        adapterEvents.notifyDataSetChanged();

        int pos = 1;
        for (int p=0; p<adapterEvents.getCount(); p++) {
            if (adapterEvents.getItem(p).equals(eventToSelect)) {
                pos = p;
                break;
            }
        }
        spinnerSitEvent.setSelection(pos);
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

    private String getInstantPeriodListSelectedText(View view) {
        LinearLayout layout = view.findViewById(R.id.instants_periods_list);
        for (int i=0; i<layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView && child.isSelected()) {
                return ((TextView) child).getText().toString();
            }
        }
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
                case R.id.button_cancel:
                    if (v.getId() == R.id.button_done) {
                        // todo: save editing box data

                    }

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
            }
        }
    };

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

        // start-instant label, end-condition label, & end-condition container
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
        // Set start instant type according to `data`.
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

        SpinnerOnItemSelectListener listener =
                (SpinnerOnItemSelectListener) spinnerStartType.getOnItemSelectedListener();
        if (listener != null)
            listener.setOtherViews = false;

        if (data.isEmpty()) {
            spinnerStartType.setSelection(0);
            onSpinnerStartTypeItemUserSelect(0);
        }
        else { //(data is not empty)
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
                listener = (SpinnerOnItemSelectListener) spinnerEndType.getOnItemSelectedListener();
                if (listener != null) {
                    listener.setOtherViews = false;
                }

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
    public Intent getResult() {
        // TODO....
        String newBehaviorData = "";
        String newAllSitsDict = "";
        String newAllEventsDict = "";

        return new Intent()
                .putExtra(EditActivity.EXTRA_FIELD_NAME, FIELD_NAME)
                .putExtra(EditActivity.EXTRA_NEW_DATA, newBehaviorData)
                .putExtra(EditActivity.EXTRA_NEW_ALL_SITUATIONS, newAllSitsDict)
                .putExtra(EditActivity.EXTRA_NEW_ALL_EVENTS, newAllEventsDict);
    }
}
