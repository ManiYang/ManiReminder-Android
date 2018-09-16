package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class DaysPickerDialogFragment extends DialogFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_INIT_SELECTION = "init_selection";

    private String title;
    private boolean[] initSelection;

    private Listener listener;
    private CheckBox[] checkBoxes = new CheckBox[7];

    public interface Listener {
        void onDaysPickerDialogPositiveClick(DialogFragment dialog, boolean[] newSelection);
    }

    public static DaysPickerDialogFragment newInstance(String title, boolean[] initSelection) {
        // initSelection[0]: for Sun,  [1] for Mon, etc.

        if (initSelection.length != 7)
            throw new RuntimeException("initSelection must be of length 7");

        DaysPickerDialogFragment dialogFragment = new DaysPickerDialogFragment();
        dialogFragment.setCancelable(false);

        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putBooleanArray(KEY_INIT_SELECTION, initSelection);
        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (Listener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("target fragment should implement Listener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        readBundle(getArguments());

        View customView = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_days_picker, null);
        checkBoxes[0] = customView.findViewById(R.id.checkBox7);
        checkBoxes[1] = customView.findViewById(R.id.checkBox1);
        checkBoxes[2] = customView.findViewById(R.id.checkBox2);
        checkBoxes[3] = customView.findViewById(R.id.checkBox3);
        checkBoxes[4] = customView.findViewById(R.id.checkBox4);
        checkBoxes[5] = customView.findViewById(R.id.checkBox5);
        checkBoxes[6] = customView.findViewById(R.id.checkBox6);
        ((Button) customView.findViewById(R.id.button_none)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i=0; i<7; i++)
                            checkBoxes[i].setChecked(false);
                    }
                }
        );
        ((Button) customView.findViewById(R.id.button_weekdays)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i=1; i<=5; i++)
                            checkBoxes[i].setChecked(true);
                        checkBoxes[6].setChecked(false);
                        checkBoxes[0].setChecked(false);
                    }
                }
        );
        ((Button) customView.findViewById(R.id.button_weekend)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i=1; i<=5; i++)
                            checkBoxes[i].setChecked(false);
                        checkBoxes[6].setChecked(true);
                        checkBoxes[0].setChecked(true);
                    }
                }
        );
        ((Button) customView.findViewById(R.id.button_alldays)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i=0; i<7; i++)
                            checkBoxes[i].setChecked(true);
                    }
                }
        );

        for (int i=0; i<7; i++)
            checkBoxes[i].setChecked(initSelection[i]);

        return new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(customView)
                .setPositiveButton(R.string.ok, onButtonClickListener)
                .setNegativeButton(R.string.cancel, onButtonClickListener)
                .create();
    }

    private void readBundle(Bundle bundle) {
        if (bundle == null) {
            throw new RuntimeException("`bundle` is null");
        }
        title = bundle.getString(KEY_TITLE);
        initSelection = bundle.getBooleanArray(KEY_INIT_SELECTION);
    }

    private DialogInterface.OnClickListener onButtonClickListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case AlertDialog.BUTTON_POSITIVE:
                            if (listener != null) {
                                listener.onDaysPickerDialogPositiveClick(
                                        DaysPickerDialogFragment.this, getSelection());
                            } else {
                                Toast.makeText(getContext(), "listener is null!",
                                        Toast.LENGTH_LONG).show();
                            }
                            break;
                        case AlertDialog.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

    private boolean[] getSelection() {
        boolean[] selection = new boolean[7];
        for (int i=0; i<7; i++)
            selection[i] = checkBoxes[i].isChecked();
        return selection;
    }
}
