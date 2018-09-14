package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

        loadData(view, initRemBehaviorData, initAllSitsDict, initAllEventsDict);

        return view;
    }

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
        {
            List<String> items = new ArrayList<>();
            items.add("situation start");
            items.add("situation end");
            items.add("event");
            items.add("time");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item, items);
            ((Spinner) view.findViewById(R.id.spinner_start_type)).setAdapter(adapter);
        }

        // spinner_end_type
        List<String> endTypes = new ArrayList<>();
        endTypes.add("duration");
        ArrayAdapter<String> adapterEndTypes = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, endTypes);
        ((Spinner) view.findViewById(R.id.spinner_end_type)).setAdapter(adapterEndTypes);

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
        ((SpinnerOnItemSelectListener) spinnerModel.getOnItemSelectedListener()).setOtherViews
                = false;
        spinnerModel.setSelection(dataBehavior.getRemType());

        // repeat pattern //
        if (dataBehavior.isTodoRepeatedlyInPeriod()) {
            view.findViewById(R.id.container_repeat_pattern).setVisibility(View.VISIBLE);
            ((EditText) view.findViewById(R.id.edit_repeat_every))
                    .setText(Integer.toString(dataBehavior.getRepeatEveryMinutes()));
            ((EditText) view.findViewById(R.id.edit_repeat_offset))
                    .setText(Integer.toString(dataBehavior.getRepeatOffsetMinutes()));

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

        //
        view.findViewById(R.id.container_edit_box).setVisibility(View.GONE);
    }

    //
    private class SpinnerOnItemSelectListener implements AdapterView.OnItemSelectedListener {
        private boolean setOtherViews = true;

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getId()) {
                case R.id.spinner_model:
                    if (setOtherViews) {
                        onSpinnerModelItemUserSelect(position);
                    }
                    break;
                case R.id.spinner_start_type:
//                    onSpinnerStartTypeItemSelect(position, setOtherViews);
                    break;
                case R.id.spinner_end_type:
//                    onSpinnerEndTypeItemSelect(position, setOtherViews);
                    break;

            }

            //
            setOtherViews = true;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private void onSpinnerModelItemUserSelect(int position) {
        View fragmentView = getView();
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

        if (position == 3) {
            view.findViewById(R.id.container_start_sit_event).setVisibility(View.GONE);
            view.findViewById(R.id.container_start_time).setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_days_of_week).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.container_start_sit_event).setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_start_time).setVisibility(View.GONE);
            view.findViewById(R.id.container_days_of_week).setVisibility(View.GONE);
        }

        TextView labelSitEvent = view.findViewById(R.id.label_sit_or_event);
        Spinner spinnerSitEvent = view.findViewById(R.id.spinner_sit_or_event);
        Spinner spinnerEndType = view.findViewById(R.id.spinner_end_type);

        List<String> endTypes = new ArrayList<>();
        endTypes.add("duration");
        ArrayAdapter<String> adapterEndTypes = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, endTypes);

        switch (position) {
            case 0: //situation start
                labelSitEvent.setText(R.string.label_situation);

                spinnerSitEvent.setAdapter(adapterSituations);
                adapterSituations.notifyDataSetChanged();
                spinnerSitEvent.setSelection(1);

                adapterEndTypes.add("situation end");
                break;

            case 1: //situation end
                labelSitEvent.setText(R.string.label_situation);

                spinnerSitEvent.setAdapter(adapterSituations);
                adapterSituations.notifyDataSetChanged();
                spinnerSitEvent.setSelection(1);
                break;

            case 2: //event
                labelSitEvent.setText(R.string.label_event);
                spinnerSitEvent.setAdapter(adapterEvents);
                adapterEvents.notifyDataSetChanged();
                spinnerSitEvent.setSelection(1);
                break;

            case 3: //time
                ((Button) view.findViewById(R.id.button_start_time)).setText(R.string.label_set);
                ((Button) view.findViewById(R.id.button_days_of_week)).setText(R.string.label_set);

                adapterEndTypes.add("time");
                break;
        }

        // end condition
        spinnerEndType.setAdapter(adapterEndTypes);
        adapterEndTypes.notifyDataSetChanged();
        view.findViewById(R.id.container_end_time).setVisibility(View.GONE);
    }

    private void onSpinnerEndTypeItemUserSelect(int position) {
        switch (position) {
            case 0: //duration
                break;
            case 1: //situation end or time
                break;
        }
    }

    private void showEmptyInstantPeriodList() {
        View fragmentView = getView();
        fragmentView.findViewById(R.id.label_instants_or_periods).setVisibility(View.VISIBLE);
        fragmentView.findViewById(R.id.container_instants_periods).setVisibility(View.VISIBLE);

        LinearLayout list = fragmentView.findViewById(R.id.instants_periods_list);
        list.removeAllViews();

        fragmentView.findViewById(R.id.button_edit).setVisibility(View.GONE);
        fragmentView.findViewById(R.id.button_remove).setVisibility(View.GONE);
    }

    //
    private String instantPeriodListSelectedText = "";

    private View.OnClickListener onInstantPeriodListItemClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();

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
        LinearLayout list = fragmentView.findViewById(R.id.instants_periods_list);
        for (int i=0; i<list.getChildCount(); i++) {
            list.getChildAt(i).setSelected(false);
        }
        fragmentView.findViewById(R.id.button_edit).setVisibility(View.GONE);
        fragmentView.findViewById(R.id.button_remove).setVisibility(View.GONE);
    }

    //
    private View.OnClickListener OnButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();
            switch (v.getId()) {
                case R.id.button_add:
                    showInstantPeriodEditBox(instantPeriodListSelectedText);
                    break;
                case R.id.button_edit:

                    showInstantPeriodEditBox(instantPeriodListSelectedText);
                    break;
            }
        }
    };

    private byte editBoxState; // 0:editing  1:adding

    private void showInstantPeriodEditBox(String data) {
        boolean toAddNew = data.isEmpty();
        editBoxState = toAddNew ? (byte)1 : 0;

        if (toAddNew) {
            deselectInstantsPeriodsList();
        }

        View view = getView();

        view.findViewById(R.id.container_edit_box).setVisibility(View.VISIBLE);
        int model = ((Spinner) view.findViewById(R.id.spinner_model))
                .getSelectedItemPosition();

        if (toAddNew) {
            ((TextView) view.findViewById(R.id.label_edit_box)).setText(
                    model == 1 ? "Add New Instant" : "Add New Period");
        } else {
            ((TextView) view.findViewById(R.id.label_edit_box)).setText(
                    model == 1 ? "Edit Instant" : "Edit Period");
        }

        // start instant
        if (model == 1) {
            view.findViewById(R.id.label_start_instant).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.label_start_instant).setVisibility(View.VISIBLE);
        }


        //
        if (model == 1) {
            view.findViewById(R.id.label_end_condition).setVisibility(View.GONE);
            view.findViewById(R.id.container_end_cond).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.label_end_condition).setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_end_cond).setVisibility(View.VISIBLE);
        }


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
