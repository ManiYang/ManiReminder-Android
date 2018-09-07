package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

public class SimpleTextEditDialogFragment extends DialogFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_INIT_TEXT = "initial_text";
    private static final String KEY_INPUT_TYPE = "input_type";

    private String title = "";
    private String initText = "";
    private int inputType = 1;
    private EditText editText;
    private Listener listener;

    public static SimpleTextEditDialogFragment newInstance(
            String title, String initialText, int inputType) {
        SimpleTextEditDialogFragment dialogFragment = new SimpleTextEditDialogFragment();
        dialogFragment.setCancelable(false);

        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_INIT_TEXT, initialText);
        bundle.putInt(KEY_INPUT_TYPE, inputType);
        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    public interface Listener {
        void onDialogPositiveClick(DialogFragment dialog, String newText);
    }

    //
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        readBundle(getArguments());

        View customView = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_text_edit, null);
        editText = customView.findViewById(R.id.text_edit);
        editText.setText(initText);
        editText.setInputType(inputType);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(title)
                .setView(customView)
                .setPositiveButton(R.string.ok, onButtonClickListener)
                .setNegativeButton(R.string.cancel, onButtonClickListener);
        return dialogBuilder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString()+" should implement SimpleTextEditDialogFragment.Listener");
        }
    }

    //
    private void readBundle(Bundle bundle) {
        if (bundle != null) {
            title = bundle.getString(KEY_TITLE, "(key not found in bundle)");
            initText = bundle.getString(KEY_INIT_TEXT, "(key not found in bundle)");
            inputType = bundle.getInt(KEY_INPUT_TYPE, InputType.TYPE_CLASS_TEXT);
        }
    }

    private DialogInterface.OnClickListener onButtonClickListener
            = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    String newText = editText.getText().toString();
                    if (!newText.equals(initText)) {
                        listener.onDialogPositiveClick(
                                SimpleTextEditDialogFragment.this, newText);
                    }
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
            }
        }
    };
}
