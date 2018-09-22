package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String text = intent.getStringExtra("data");

        Intent intentTap = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intentTap);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(1, PendingIntent.FLAG_ONE_SHOT);

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "channel mani")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Notification Test")
                .setContentText("data: "+text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSound(notificationSound, AudioManager.STREAM_NOTIFICATION)
                .setLights(Color.WHITE,500, 500);
        int notificationId = 32;
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}
