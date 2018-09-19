package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class SituationsEventsFragment extends Fragment {


    public SituationsEventsFragment() {
        // Required empty public constructor
    }

    private List<Integer> startedSitIds = new ArrayList<>(); //will be re-assigned
    private List<String> startedSitNames = new ArrayList<>(); //do not re-assign


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_situations_events, container, false);

        // setup recycler_started_situations
        TextListAdapter adapter = new TextListAdapter(
                startedSitNames, TextListAdapter.SINGLE_SELECTION);
        //adapter.setSingleSelectedListener(...);

        RecyclerView recyclerStartedSits = view.findViewById(R.id.recycler_started_situations);
        recyclerStartedSits.setAdapter(adapter);
        recyclerStartedSits.setLayoutManager(new GridLayoutManager(
                getContext(), 2, GridLayoutManager.VERTICAL, false));

        //

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view == null)
            throw new RuntimeException("`view` is null");

        // read all situations & events



        // read & load started situations
        startedSitIds = UtilStorage.getStartedSituations(getContext());



        RecyclerView recyclerStartedSits = view.findViewById(R.id.recycler_started_situations);
        TextListAdapter adapter = (TextListAdapter) recyclerStartedSits.getAdapter();



    }
}
