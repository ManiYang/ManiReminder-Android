package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID = "reminder_id";
    private DetailFragment detailFragment;
    private int reminderId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //
        reminderId = getIntent().getIntExtra(EXTRA_REMINDER_ID, -99);
        detailFragment =
                (DetailFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_detail);
        detailFragment.setReminderId(reminderId);
        //Log.v("DetailActivity", "### reminder ID: "+Integer.toString(reminderId));
    }

    // menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_reminder:
                // show dialog for confirming removal
                String msg = getResources().getString(R.string.dialog_msg_confirm_remove);
                if (UtilStorage.isReminderOpened(this, reminderId)) {
                    msg += " The reminder is currently opened.";
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_confirm_remove)
                        .setMessage(msg)
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                actionRemoveReminder();
                            }
                        })
                        .setNegativeButton(R.string.no, null);
                dialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //
    private void actionRemoveReminder() {
        new ReminderBoardLogic(DetailActivity.this).beforeReminderRemove(reminderId);

        // remove reminder data
        SQLiteDatabase db = UtilStorage.getWritableDatabase(this);
        db.delete(MainDbHelper.TABLE_REMINDERS_BRIEF,
                "_id = ?", new String[] {Integer.toString(reminderId)});
        db.delete(MainDbHelper.TABLE_REMINDERS_DETAIL,
                "_id = ?", new String[] {Integer.toString(reminderId)});
        db.delete(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                "_id = ?", new String[] {Integer.toString(reminderId)});

        Log.v("mainlog", String.format("rem %d removed", reminderId));

        //
        Toast.makeText(DetailActivity.this,
                "Reminder removed!", Toast.LENGTH_LONG).show();
        NavUtils.navigateUpFromSameTask(DetailActivity.this);
    }
}
