package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditRemTagsFragment extends Fragment
        implements EditActivity.EditResultHolder {

    private static final String FIELD_NAME = "tags";
    private static final String KEY_INIT_REM_TAGS = "init_rem_tags";
    private static final String KEY_INIT_ALL_TAGS = "init_all_tags";
    private String initRemTagsString;
    private String initAllTagsDict;
    private ArrayList<String> remTags;
//    private SparseArray<String> allTags;
    private ArrayList<String> allTagNames; //all-tags list adapter references this. Do not re-assign.
    private ArrayList<Integer> allTagIds;

    public EditRemTagsFragment() {
        // Required empty public constructor
    }

    public static EditRemTagsFragment newInstance(String initialRemTags, String initialAllTagsDict) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_INIT_REM_TAGS, initialRemTags);
        bundle.putString(KEY_INIT_ALL_TAGS, initialAllTagsDict);

        EditRemTagsFragment fragment = new EditRemTagsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_rem_tags, container, false);

        readBundle(getArguments());
        loadData(view, initRemTagsString, initAllTagsDict);

        view.findViewById(R.id.button_remove).setVisibility(ImageButton.INVISIBLE);
        view.findViewById(R.id.button_remove).setOnClickListener(buttonClickListener);
        view.findViewById(R.id.button_add).setOnClickListener(buttonClickListener);

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

        initAllTagsDict = bundle.getString(KEY_INIT_ALL_TAGS);
        if (initAllTagsDict == null) {
            throw new RuntimeException("KEY_INIT_ALL_TAGS not found in bundle");
        }
    }

    //
    private void loadData(final View view, String remTagsString, String allTagsDict) {
        if (view == null) { return; }

        // set remTags, allTagNames, allTagIds
        remTags = UtilGeneral.splitString(remTagsString, ",");

        SparseArray<String> allTags = UtilGeneral.parseAsSparseStringArray(allTagsDict);
        allTagNames = UtilGeneral.getValuesOfSparseStringArray(allTags);
        allTagIds = UtilGeneral.getKeysOfSparseStringArray(allTags);
//        List<String> allTagsPairs = UtilGeneral.splitString(allTagsDict, ",");
//        allTagNames = new ArrayList<>();
//        allTagIds = new ArrayList<>();
//        try {
//            for (int i = 0; i < allTagsPairs.size(); i++) {
//                String[] tokens = allTagsPairs.get(i).split(":");
//                allTagIds.add(Integer.parseInt(tokens[0].trim()));
//                allTagNames.add(tokens[1]);
//            }
//        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
//            throw new RuntimeException("wrong format of allTagsString");
//        }

        // populate reminder tags
        TextListAdapter adapter = new TextListAdapter(remTags, TextListAdapter.SINGLE_SELECTION);
        adapter.setSingleSelectedListener(new TextListAdapter.SingleSelectionListener() {
            @Override
            public void onSingleSelection(String selectedText) {
                view.findViewById(R.id.button_remove).setVisibility(ImageButton.VISIBLE);
            }
        });
        adapter.setSingleDeselectedListener(new TextListAdapter.SingleDeselectionListener() {
            @Override
            public void onSingleDeselection(String DeselectedText) {
                view.findViewById(R.id.button_remove).setVisibility(ImageButton.INVISIBLE);
            }
        });

        RecyclerView remTagsRecyclerView = view.findViewById(R.id.reminder_tags);
        remTagsRecyclerView.setAdapter(adapter);
        remTagsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3,
                GridLayoutManager.VERTICAL, false));

        // populate all tags
        adapter = new TextListAdapter(allTagNames, TextListAdapter.NO_SELECTION);
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
                    String tagToAdd = ((EditText) getView().findViewById(R.id.tag_to_add))
                            .getText().toString().trim();
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
                    RecyclerView remTagsRecycler = getView().findViewById(R.id.reminder_tags);
                    int[] selectedPositions =
                            ((TextListAdapter) remTagsRecycler.getAdapter()).getSelectedPositions();
                    if (selectedPositions.length > 0) {
                        actionRemoveTag(selectedPositions[0]);
                    }
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

        if (!allTagNames.contains(tagToAdd)) {
            int newTagId;
            if (allTagIds.size() == 0) {
                newTagId = 0;
            } else {
                newTagId = Collections.max(allTagIds) + 1;
            }

            allTagIds.add(newTagId);
            allTagNames.add(tagToAdd);

            RecyclerView allTagsRecycler = view.findViewById(R.id.all_tags);
            ((TextListAdapter) allTagsRecycler.getAdapter()).itemsAppended();
        }

        ((EditText) view.findViewById(R.id.tag_to_add)).setText("");
    }

    private void actionRemoveTag(int index) {
        View view = getView();

        String removedTag = remTags.remove(index);
        RecyclerView remTagsRecycler = view.findViewById(R.id.reminder_tags);
        ((TextListAdapter) remTagsRecycler.getAdapter()).itemRemoved(index);

        view.findViewById(R.id.button_remove).setVisibility(View.INVISIBLE);

        // if removedTag is not among initial all-tags, also remove it from current all-tags
        if (! isTagInInitAllTags(removedTag)) {
            int at = allTagNames.indexOf(removedTag);
            if (at != -1) {
                allTagNames.remove(at);
                allTagIds.remove(at);

                RecyclerView allTagsRecycler = view.findViewById(R.id.all_tags);
                ((TextListAdapter) allTagsRecycler.getAdapter()).itemRemoved(at);
            }
        }
    }

    private boolean isTagInInitAllTags(String tag) {
        SparseArray<String> initAllTags = UtilGeneral.parseAsSparseStringArray(initAllTagsDict);
        return UtilGeneral.searchSparseStringArrayByValue(initAllTags, tag) != -1;
    }

    //
    @Override
    public Intent getResult() {
        String remTagsString = UtilGeneral.joinStringList(", ", remTags);
        SparseArray<String> allTags =
                UtilGeneral.buildSparseStringArray(allTagIds, allTagNames);
        return new Intent()
                .putExtra(EditActivity.EXTRA_FIELD_NAME, FIELD_NAME)
                .putExtra(EditActivity.EXTRA_NEW_DATA, remTagsString)
                .putExtra(EditActivity.EXTRA_NEW_ALL_TAGS, allTags.toString());
    }
}
