package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            new ReminderBoardLogic(context).onDeviceBootCompleted();
        }
    }
}
