package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


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
        if (bundle != null) {
            initRemTagsString = bundle.getString(KEY_INIT_REM_TAGS, "(key not found in bundle)");
            initAllTagsString = bundle.getString(KEY_INIT_ALL_TAGS, "(key not found in bundle)");
        }
    }

    //
    private void loadData(View view, String remTagsString, String allTagsString) {
        if (view == null) { return; }

        // TODO ....

    }

//    private String buildDataString() {
//        return ""; // [temp]
//    }
}
