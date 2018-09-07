package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditRemTagsFragment extends Fragment {

    private static final String KEY_INIT_REM_TAGS = "init_rem_tags";
    private static final String KEY_INIT_ALL_TAGS = "init_all_tags";
    private String initRemTagsString;
    private String initAllTagsString;

    public EditRemTagsFragment() {
        // Required empty public constructor
    }

    public static EditRemTagsFragment newInstance(String initialRemTags, String initialAllTags) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_INIT_REM_TAGS, initialRemTags);
        bundle.putString(KEY_INIT_ALL_TAGS, initialAllTags);

        EditRemTagsFragment fragment = new EditRemTagsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_rem_tags, container, false);

        readBundle(getArguments());
        loadData(view, initRemTagsString, initAllTagsString);

        return view;
    }

    private void readBundle(Bundle bundle) {
        if (bundle == null) {
            throw new RuntimeException("bundle is null");
        }

        initRemTagsString = bundle.getString(KEY_INIT_REM_TAGS);
        if (initRemTagsString == null) {
            throw new RuntimeException("KEY_INIT_REM_TAGS not found in bundle");
        }

        initAllTagsString = bundle.getString(KEY_INIT_ALL_TAGS);
        if (initAllTagsString == null) {
            throw new RuntimeException("KEY_INIT_ALL_TAGS not found in bundle");
        }
    }

    //
    private void loadData(View view, String remTagsString, String allTagsString) {
        if (view == null) { return; }

        Set<String> remTags = UtilGeneral.splitString(remTagsString, ",");
        Set<String> allTagsPairs = UtilGeneral.splitString(allTagsString, ",");

        //
        TextListAdapter adapter = new TextListAdapter(
                remTags.toArray(new String[] {}), TextListAdapter.SINGLE_SELECTION);

        RecyclerView remTagsRecyclerView = view.findViewById(R.id.reminder_tags);
        remTagsRecyclerView.setAdapter(adapter);
        remTagsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3,
                GridLayoutManager.VERTICAL, false));

        //
        adapter = new TextListAdapter(
                allTagsPairs.toArray(new String[] {}), TextListAdapter.NO_SELECTION);

        RecyclerView allTagsRecyclerView = view.findViewById(R.id.all_tags);
        allTagsRecyclerView.setAdapter(adapter);
        allTagsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3,
                GridLayoutManager.VERTICAL, false));

        // TODO ....




    }



//    private String buildDataString() {
//        return ""; // [temp]
//    }
}
