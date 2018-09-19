package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class SituationsEventsFragment extends Fragment {


    public SituationsEventsFragment() {
        // Required empty public constructor
    }

    private List<Integer> startedSitIds = new ArrayList<>(); //will be re-assigned
    private List<String> startedSitNames = new ArrayList<>(); //do not re-assign, used by adapter
                                                              //of recycler_started_situations

    private List<String> allSitsEventsListItem = new ArrayList<>(); //do not re-assign, used by
                                                                    //adapter of list_all_sits_events

    SparseArray<String> allSits;
    SparseArray<String> allEvents;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(
                R.layout.fragment_situations_events, container, false);

        // setup an empty recycler_started_situations
        TextListAdapter adapterStartedSits =
                new TextListAdapter(startedSitNames, TextListAdapter.NO_SELECTION);
        adapterStartedSits.setNoSelectionOnClickListener(
                new TextListAdapter.NoSelectionOnClickListener() {
                    @Override
                    public void onClick(String clickedText) {
                        Toast.makeText(getContext(), "Long click to stop situation",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
        adapterStartedSits.setNoSelectionOnLongClickListener(
                new TextListAdapter.NoSelectionOnLongClickListener() {
                    @Override
                    public void onLongClick(String clickedText) {
                        onStartedSitsItemLongClick(clickedText);
                    }
                }
        );

        RecyclerView recyclerStartedSits = view.findViewById(R.id.recycler_started_situations);
        recyclerStartedSits.setAdapter(adapterStartedSits);
        recyclerStartedSits.setLayoutManager(new GridLayoutManager(
                getContext(), 2, GridLayoutManager.VERTICAL, false));

        // setup an empty list_all_sits_events
        ArrayAdapter<String> adapterAllSitsEvents = new ArrayAdapter<>(
                getContext(), R.layout.text_list_item_plain, allSitsEventsListItem);

        ListView listViewAllSitsEvents = view.findViewById(R.id.list_all_sits_events);
        listViewAllSitsEvents.setAdapter(adapterAllSitsEvents);

        listViewAllSitsEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getContext(), "Long click to start/trigger", Toast.LENGTH_SHORT)
                        .show();
            }
        });
        listViewAllSitsEvents.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onListAllSitsEventsItemLongClick(position);
                return true;
            }
        });

        // radio buttons
        ((RadioButton) view.findViewById(R.id.radioButton_both)).setChecked(true);

        RadioGroup radioGroup =  view.findViewById(R.id.radioGroup_sits_events);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                loadAllSitsEventsList(getView());
            }
        });

        Log.v("SituationsEventsFrag", "### onCreateView() done");

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view == null)
            throw new RuntimeException("`view` is null");

        // read all situations & events
        allSits = UtilStorage.getAllSituations(getContext());
        allEvents = UtilStorage.getAllEvents(getContext());

        // read & load started situations
        RecyclerView recyclerStartedSits = view.findViewById(R.id.recycler_started_situations);
        TextListAdapter adapter = (TextListAdapter) recyclerStartedSits.getAdapter();

        startedSitIds = UtilStorage.getStartedSituations(getContext());
        startedSitNames.clear();
        adapter.dataCleared();

        for (int id: startedSitIds) {
            startedSitNames.add(allSits.get(id));
        }
        if (! startedSitIds.isEmpty()) {
            adapter.itemsAppended();
        }

        //
        loadAllSitsEventsList(view);

        Log.v("SituationsEventsFrag", "### onStart() done");
    }

    private void loadAllSitsEventsList(View view) {
        if (view == null)
            return;

        boolean showSits = false;
        boolean showEvents = false;
        if (((RadioButton) view.findViewById(R.id.radioButton_both)).isChecked()) {
            showSits = true;
            showEvents = true;
        } else if (((RadioButton) view.findViewById(R.id.radioButton_situations)).isChecked()) {
            showSits = true;
        } else {
            showEvents = true;
        }

        allSitsEventsListItem.clear();
        if (showSits) {
            for (int i=0; i<allSits.size(); i++) {
                allSitsEventsListItem.add("(S) " + allSits.valueAt(i));
            }
        }
        if (showEvents) {
            for (int i=0; i<allEvents.size(); i++) {
                allSitsEventsListItem.add("(E) " + allEvents.valueAt(i));
            }
        }

        ArrayAdapter<String> adapter = (ArrayAdapter<String>)
                ((ListView) view.findViewById(R.id.list_all_sits_events)).getAdapter();
        adapter.notifyDataSetChanged();
    }

    //
    private void onListAllSitsEventsItemLongClick(int position) {
        View view = getView();
        if (view == null)
            return;

        Calendar clickTime = Calendar.getInstance();

        ArrayAdapter<String> adapter = (ArrayAdapter<String>)
                ((ListView) view.findViewById(R.id.list_all_sits_events)).getAdapter();
        String clickedText = adapter.getItem(position);
        boolean isEvent = clickedText.startsWith("(E)");
        String sitEventName = clickedText.substring(4);

        if (!isEvent) {
            // to start situation
            int sitId = UtilGeneral.searchSparseStringArrayByValue(allSits, sitEventName);
            if (sitId == -1)
                throw new RuntimeException("situation not found");
            userStartSituation(sitId, clickTime, view);
        } else {
            // to trigger event
            int eventId = UtilGeneral.searchSparseStringArrayByValue(allEvents, sitEventName);
            if (eventId == -1) {
                throw new RuntimeException("event not found");
            }
            userTriggerEvent(eventId, clickTime);
        }
    }

    private void onStartedSitsItemLongClick(String clickedText) {
        View view = getView();
        if (view == null)
            return;

        Calendar clickTime = Calendar.getInstance();

        int index = startedSitNames.indexOf(clickedText);
        userStopSituation(index, clickTime, view);
    }

    //
    private void userStartSituation(int sitId, Calendar at, View view) {
        if (startedSitIds.contains(sitId)) {
            Toast.makeText(getContext(), "Situation has been started", Toast.LENGTH_SHORT).show();
            return;
        }

        // add to started situations
        startedSitIds.add(sitId);
        startedSitNames.add(allSits.get(sitId));

        TextListAdapter adapter = (TextListAdapter)
                ((RecyclerView) view.findViewById(R.id.recycler_started_situations)).getAdapter();
        adapter.itemsAppended();

        UtilStorage.writeStartedSituations(getContext(), startedSitIds);

        // add to history
        UtilStorage.addToHistory(getContext(), at, UtilStorage.TYPE_SIT_START, sitId);

        // more...


    }

    private void userTriggerEvent(int eventId, Calendar at) {
        Toast.makeText(getContext(), "Event triggered", Toast.LENGTH_SHORT).show();

        // add to history
        UtilStorage.addToHistory(getContext(), at, UtilStorage.TYPE_EVENT, eventId);


        // more...


    }

    private void userStopSituation(int startedSitIndex, Calendar at, View view) {
        // remove started situations
        int sitId = startedSitIds.remove(startedSitIndex);
        startedSitNames.remove(startedSitIndex);

        TextListAdapter adapter = (TextListAdapter)
                ((RecyclerView) view.findViewById(R.id.recycler_started_situations)).getAdapter();
        adapter.notifyDataSetChanged();

        UtilStorage.writeStartedSituations(getContext(), startedSitIds);

        // add to history (situation end)
        UtilStorage.addToHistory(getContext(), at, UtilStorage.TYPE_SIT_END, sitId);


        // more...
    }
}
