package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditRemTagsFragment extends Fragment {

    private static final String KEY_INIT_REM_TAGS = "init_rem_tags";
    private static final String KEY_INIT_ALL_TAGS = "init_all_tags";
    private String initRemTagsString;
    private String initAllTagsString;
    private ArrayList<String> remTags;
    private ArrayList<String> allTags;

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

        ((ImageButton) view.findViewById(R.id.button_remove)).setVisibility(ImageButton.INVISIBLE);
        ((ImageButton) view.findViewById(R.id.button_remove)).setOnClickListener(buttonClickListener);
        ((ImageButton) view.findViewById(R.id.button_add)).setOnClickListener(buttonClickListener);

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
    private void loadData(final View view, String remTagsString, String allTagsString) {
        if (view == null) { return; }

        // set remTags and allTags
        remTags = UtilGeneral.splitString(remTagsString, ",");

        List<String> allTagsPairs = UtilGeneral.splitString(allTagsString, ",");
        allTags = new ArrayList<>();
        for (int i=0; i<allTagsPairs.size(); i++) {
            String[] tokens = allTagsPairs.get(i).split(":");
            allTags.add(tokens[tokens.length - 1]);
        }

        // populate reminder tags
        TextListAdapter adapter = new TextListAdapter(remTags, TextListAdapter.SINGLE_SELECTION);
        adapter.setSingleSelectedListener(new TextListAdapter.SingleSelectionListener() {
            @Override
            public void onSingleSelection(String selectedText) {
                ((ImageButton) view.findViewById(R.id.button_remove))
                        .setVisibility(ImageButton.VISIBLE);
            }
        });
        adapter.setSingleDeselectedListener(new TextListAdapter.SingleDeselectionListener() {
            @Override
            public void onSingleDeselection(String DeselectedText) {
                ((ImageButton) view.findViewById(R.id.button_remove))
                        .setVisibility(ImageButton.INVISIBLE);
            }
        });

        RecyclerView remTagsRecyclerView = view.findViewById(R.id.reminder_tags);
        remTagsRecyclerView.setAdapter(adapter);
        remTagsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3,
                GridLayoutManager.VERTICAL, false));

        // populate all tags
        adapter = new TextListAdapter(allTags, TextListAdapter.NO_SELECTION);
        adapter.setNoSelectionOnClickListener(new TextListAdapter.NoSelectionOnClickListener() {
            @Override
            public void onClick(String clickedText) {
                ((EditText) view.findViewById(R.id.tag_to_add)).setText(clickedText);
            }
        });

        RecyclerView allTagsRecyclerView = view.findViewById(R.id.all_tags);
        allTagsRecyclerView.setAdapter(adapter);
        allTagsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3,
                GridLayoutManager.VERTICAL, false));
    }

    //
    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_add:
                    EditText editText = getView().findViewById(R.id.tag_to_add);
                    String tagToAdd = editText.getText().toString().trim();
                    if (!tagToAdd.isEmpty()) {
                        if (tagToAdd.contains(",")) {
                            new AlertDialog.Builder(getContext())
                                    .setMessage(R.string.msg_tag_should_have_no_comma)
                                    .setCancelable(true)
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        } else {
                            actionAddTag(tagToAdd);
                        }
                    }
                    break;

                case R.id.button_remove:
                    // TODO...
                    Toast.makeText(getContext(), "remove tag...", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void actionAddTag(String tagToAdd) {
        if (remTags.contains(tagToAdd)) {
            Toast.makeText(getContext(), "Tag already included", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = getView();

        remTags.add(tagToAdd);
        RecyclerView remTagsRecycler = view.findViewById(R.id.reminder_tags);
        ((TextListAdapter) remTagsRecycler.getAdapter()).itemsAppended();

        if (!allTags.contains(tagToAdd)) {
            allTags.add(tagToAdd);
            RecyclerView allTagsRecycler = view.findViewById(R.id.all_tags);
            ((TextListAdapter) allTagsRecycler.getAdapter()).itemsAppended();
        }

        ((EditText) view.findViewById(R.id.tag_to_add)).setText("");
    }



//    private String buildDataString() {
//        return ""; // [temp]
//    }
}
