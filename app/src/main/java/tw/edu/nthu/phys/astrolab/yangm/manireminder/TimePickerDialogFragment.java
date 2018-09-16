package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.Toast;

public class TimePickerDialogFragment extends DialogFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_INIT_HR = "init_hr";
    private static final String KEY_INIT_MIN = "init_min";
    private static final String KEY_CAN_BE_NEXT_DAY = "can_be_next_day";

    private String title;
    private int[] initHrMin = new int[2];
    private boolean canBeNextDay;

    private TimePicker timePicker;
    private CheckBox checkBoxNextDay;
    private Listener listener;

    public interface Listener {
        void onDialogPositiveClick(DialogFragment dialog, int newHr, int newMin);
    }

    public static TimePickerDialogFragment newInstance(
            String title, int initHr, int initMin, boolean canBeNextDay) {
        if (initMin < 0 || initMin > 59)
            throw new RuntimeException("bad `initMin`");
        if (initHr < 0 || (!canBeNextDay && initHr > 23) || initHr > 47)
            throw new RuntimeException("bad `initHr`");

        TimePickerDialogFragment dialog = new TimePickerDialogFragment();
        dialog.setCancelable(false);

        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putInt(KEY_INIT_HR, initHr);
        bundle.putInt(KEY_INIT_MIN, initMin);
        bundle.putBoolean(KEY_CAN_BE_NEXT_DAY, canBeNextDay);
        dialog.setArguments(bundle);

        return dialog;
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
                .inflate(R.layout.dialog_time_picker, null);
        timePicker = customView.findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);
        checkBoxNextDay = customView.findViewById(R.id.checkbox_next_day);
        checkBoxNextDay.setVisibility(canBeNextDay ? View.VISIBLE : View.GONE);

        if (initHrMin[0] > 23) {
            timePicker.setCurrentHour(initHrMin[0] - 24);
            checkBoxNextDay.setChecked(true);
        } else {
            timePicker.setCurrentHour(initHrMin[0]);
            checkBoxNextDay.setChecked(false);
        }
        timePicker.setCurrentMinute(initHrMin[1]);

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
        initHrMin[0] = bundle.getInt(KEY_INIT_HR);
        initHrMin[1] = bundle.getInt(KEY_INIT_MIN);
        canBeNextDay = bundle.getBoolean(KEY_CAN_BE_NEXT_DAY);
    }

    private DialogInterface.OnClickListener onButtonClickListener =
            new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    if (listener != null) {
                        int hr = timePicker.getCurrentHour();
                        if (checkBoxNextDay.isChecked())
                            hr += 24;
                        listener.onDialogPositiveClick(TimePickerDialogFragment.this,
                                hr, timePicker.getCurrentMinute());
                    } else {
                        Toast.makeText(getContext(), "listener is null!", Toast.LENGTH_LONG)
                                .show();
                    }
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
            }
        }
    };
}
