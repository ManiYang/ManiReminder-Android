package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class EditRemChecklistFragment extends Fragment
        implements EditActivity.EditResultHolder, SimpleTextEditDialogFragment.Listener {

    private static final String FIELD_NAME = "checklist";
    private static final String KEY_INIT_DATA = "init_data";

    private String initData;
    private RemChecklistAdapter adapter;

    public EditRemChecklistFragment() {
        // Required empty public constructor
    }

    public static EditRemChecklistFragment newInstance(String initData) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_INIT_DATA, initData);

        EditRemChecklistFragment fragment = new EditRemChecklistFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    //
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_rem_checklist, container, false);

        readBundle(getArguments());
        loadData(view, initData);

        view.findViewById(R.id.button_add).setOnClickListener(onButtonClickListener);
        view.findViewById(R.id.button_edit).setOnClickListener(onButtonClickListener);
        view.findViewById(R.id.button_edit).setVisibility(View.GONE);
        view.findViewById(R.id.button_remove).setOnClickListener(onButtonClickListener);
        view.findViewById(R.id.button_remove).setVisibility(View.GONE);

        return view;
    }

    private void readBundle(Bundle bundle) {
        if (bundle == null) {
            throw new RuntimeException("bundle is null");
        }

        initData = bundle.getString(KEY_INIT_DATA);
        if (initData == null) {
            throw new RuntimeException("KEY_INIT_DATA not found in bundle");
        }
    }

    private void loadData(View fragmentView, String checklistStr) {
        if (fragmentView == null) { return; }

        ChecklistInfo info = parseChecklistStr(checklistStr);
        adapter = new RemChecklistAdapter(info.texts, info.checked,
                new RemChecklistAdapter.OnItemSelectDeselectListener() {
                    @Override
                    public void onItemSelectDeselect(boolean anyItemSelected) {
                        itemSelectedOrDeselected(anyItemSelected);
                    }
                });
        RecyclerView recycler = fragmentView.findViewById(R.id.recycler_checklist);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public void itemSelectedOrDeselected(boolean anyItemSelected) {
        View view = getView();
        if (anyItemSelected) {
            view.findViewById(R.id.button_edit).setVisibility(View.VISIBLE);
            view.findViewById(R.id.button_remove).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.button_edit).setVisibility(View.GONE);
            view.findViewById(R.id.button_remove).setVisibility(View.GONE);
        }
    }

    // buttons
    private View.OnClickListener onButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_add:
                    actionAdd();
                    break;

                case R.id.button_edit:
                    break;

                case R.id.button_remove:
                    break;
            }
        }
    };

    private void actionAdd() {
        SimpleTextEditDialogFragment dialog = SimpleTextEditDialogFragment.newInstance(
                "New item", "", InputType.TYPE_CLASS_TEXT);
        dialog.setTargetFragment(this, 1);
        dialog.show(getFragmentManager(), "add_item");
    }

    @Override
    public void onDialogClick(DialogFragment dialog, boolean positive, String newText) {
        if (!positive) {
            return;
        }

        switch (dialog.getTag()) {
            case "add_item":
                adapter.addItem(newText);
                break;
        }
    }

    //
    @Override
    public boolean validateData() {
        return true;
    }

    @Override
    public Intent getResult() {
        String newData = stringifyChecklistData(adapter.getTexts(), adapter.getCheckedList());
        return new Intent()
                .putExtra(EditActivity.EXTRA_FIELD_NAME, FIELD_NAME)
                .putExtra(EditActivity.EXTRA_NEW_DATA, newData);
    }

    //
    public static class ChecklistInfo {
        public List<String> texts = new ArrayList<>();
        public List<Boolean> checked = new ArrayList<>();

        public void add(String text, boolean checked) {
            this.texts.add(text);
            this.checked.add(checked);
        }
    }

    public static ChecklistInfo parseChecklistStr(String checklistStr) {
        ChecklistInfo info = new ChecklistInfo();
        if (checklistStr.trim().isEmpty()) {
            return info;
        }

        String[] items = checklistStr.split("\n");
        try {
            for (String s : items) {
                s = s.trim();
                boolean checked = (s.charAt(0) == 'T');
                String text = s.substring(1);
                info.add(text, checked);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Bad format of `checklistStr`");
        }
        return info;
    }

    public static String stringifyChecklistData(List<String> itemTexts, List<Boolean> itemChecked) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<itemTexts.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(itemChecked.get(i) ? 'T' : 'F')
                    .append(itemTexts.get(i));
        }
        return builder.toString();
    }
}
