package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID = "reminder_id";
    private DetailFragment detailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //
        int reminderId = getIntent().getIntExtra(EXTRA_REMINDER_ID, -99);
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
                AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_confirm_remove)
                        .setMessage(R.string.dialog_msg_confirm_remove)
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes, dlgConfirmRemoveBtnListener)
                        .setNegativeButton(R.string.no, dlgConfirmRemoveBtnListener);
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private DialogInterface.OnClickListener dlgConfirmRemoveBtnListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            if (detailFragment != null) {
                                detailFragment.removeReminder();
                                Toast.makeText(DetailActivity.this,
                                        "Reminder removed!", Toast.LENGTH_LONG).show();
                            }
                            NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            break;
                    }
                }
            };
}
