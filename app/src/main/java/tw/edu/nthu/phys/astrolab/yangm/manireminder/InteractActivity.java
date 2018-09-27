package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class InteractActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID = "reminder_id";

    private int reminderId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interact);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onStart() {
        super.onStart();

        reminderId = getIntent().getIntExtra(EXTRA_REMINDER_ID, -1);
        if (reminderId == -1) {
            throw new RuntimeException("reminder ID not found in Intent");
        }

        loadData();
    }

    private void loadData() {
        SQLiteDatabase db = UtilStorage.getReadableDatabase(this);

        // title
        Cursor cursor =
                readTable(db, MainDbHelper.TABLE_REMINDERS_BRIEF, reminderId, new String[] {"title"});
        if (cursor == null) {
            return;
        }
        String remTitle = cursor.getString(0);
        cursor.close();
        ((TextView) findViewById(R.id.reminder_title)).setText(remTitle);




        // todo ...


    }

    /**
     * Remember to close the cursor.
     */
    private Cursor readTable(SQLiteDatabase db, String table, int reminderId, String[] columns) {
        Cursor cursor = db.query(table, columns,
                "_id = ?", new String[] {Integer.toString(reminderId)},
                null, null, null);
        if (!cursor.moveToPosition(0)) {
            Toast.makeText(this, "Could not read reminder data from DB",
                    Toast.LENGTH_SHORT).show();
            cursor.close();
            return null;
        } else {
            return cursor;
        }
    }

    // option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_interact, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_detail:
                startActivity(new Intent(this, DetailActivity.class)
                        .putExtra(DetailActivity.EXTRA_REMINDER_ID, reminderId));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
