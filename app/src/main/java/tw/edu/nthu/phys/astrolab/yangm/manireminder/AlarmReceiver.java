package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALARM_ID = "alarm_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);

        // get actions associated to `alarmId`
        List<ScheduleAction> actions = UtilStorage.getScheduledActions(context, alarmId);

        // delete the actions in DB
        UtilStorage.removeScheduledActions(context, alarmId);

        // perform actions
        new ReminderBoardLogic(context).performScheduledActions(actions);
    }
}
