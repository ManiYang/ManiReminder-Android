package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditRemBehaviorFragment extends Fragment
        implements EditActivity.EditResultHolder {

    private static final String FIELD_NAME = "behavior";
    private static final String KEY_INIT_REM_BEHAVIOR_DATA = "init_rem_behavior_data";
    private static final String KEY_INIT_ALL_SITUATIONS = "init_all_sits";
    private static final String KEY_INIT_ALL_EVENTS = "init_all_events";

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

    private SpinnerListener spinnerListener = new SpinnerListener();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(
                R.layout.fragment_edit_rem_behavior, container, false);

        readBundle(getArguments());
        loadData(view, initRemBehaviorData, initAllSitsDict, initAllEventsDict);

        Spinner spinnerModel = view.findViewById(R.id.spinner_model);
        spinnerModel.setOnItemSelectedListener(spinnerListener);
        spinnerModel.setOnTouchListener(spinnerListener);

        view.findViewById(R.id.button_add).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_edit).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_remove).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_start_time).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_days_of_week).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_end_time).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_done).setOnClickListener(OnButtonClickListener);
        view.findViewById(R.id.button_cancel).setOnClickListener(OnButtonClickListener);

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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.reminder_modes, R.layout.text_list_item_plain);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(adapter);

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
    private class SpinnerListener
            implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        private boolean isUserSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            isUserSelect = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (isUserSelect) {
                isUserSelect = false;
                switch (parent.getId()) {
                    case R.id.spinner_model:
                        onSpinnerModeItemUserSelect(position);
                        break;
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private void onSpinnerModeItemUserSelect(int position) {
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
    private View.OnClickListener onInstantPeriodListItemClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();

            if (v.isSelected()) {
                // deselect item
                v.setSelected(false);
                fragmentView.findViewById(R.id.button_edit).setVisibility(View.GONE);
                fragmentView.findViewById(R.id.button_remove).setVisibility(View.GONE);
                fragmentView.findViewById(R.id.container_edit_box).setVisibility(View.GONE);
            } else {
                // select item
                deselectInstantsPeriodsList();
                v.setSelected(true);
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
                    deselectInstantsPeriodsList();
                    fragmentView.findViewById(R.id.container_edit_box).setVisibility(View.VISIBLE);

                    boolean addInstant =
                            ((TextView) fragmentView.findViewById(R.id.label_instants_or_periods))
                                    .getText().toString().startsWith("Instants");
                    ((TextView) fragmentView.findViewById(R.id.label_edit_box)).setText(
                            addInstant ? "Add New Instant" : "Add New Period");




                    break;
            }
        }
    };


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
