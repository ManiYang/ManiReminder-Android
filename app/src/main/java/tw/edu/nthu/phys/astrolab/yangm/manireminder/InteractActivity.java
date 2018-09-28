package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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

        // button "close reminder"
        findViewById(R.id.button_close_reminder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UtilStorage.removeFromOpenedReminders(InteractActivity.this, reminderId);
                finish();
            }
        });

        // button "time stamp"
        findViewById(R.id.button_time_stamp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonTimeStampClick();
            }
        });
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

        // "close reminder" button
        cursor = readTable(
                db, MainDbHelper.TABLE_REMINDERS_BEHAVIOR, reminderId, new String[] {"type"});
        if (cursor == null) {
            return;
        }
        int remModel = cursor.getInt(0);
        cursor.close();

        findViewById(R.id.button_close_reminder).
                setVisibility((remModel == 2) ? View.GONE : View.VISIBLE);

        // description and quick notes
        cursor = readTable(db, MainDbHelper.TABLE_REMINDERS_DETAIL, reminderId,
                new String[] {"description", "quick_notes"});
        if (cursor == null) {
            return;
        }
        String description = cursor.getString(0);
        String quickNotes = cursor.getString(1);
        if (quickNotes == null) {
            quickNotes = "";
        }
        cursor.close();

        TextView textViewDescription = findViewById(R.id.reminder_description);
        if (description.isEmpty()) {
            findViewById(R.id.label_rem_description).setVisibility(View.GONE);
            textViewDescription.setVisibility(View.GONE);
        } else {
            findViewById(R.id.label_rem_description).setVisibility(View.VISIBLE);
            textViewDescription.setVisibility(View.VISIBLE);
            textViewDescription.setText(description);
        }
        ((EditText) findViewById(R.id.quick_notes)).setText(quickNotes);
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveQuickNotes();
    }

    private void saveQuickNotes() {
        String quickNotes = ((EditText) findViewById(R.id.quick_notes)).getText().toString();

        ContentValues values = new ContentValues();
        values.put("quick_notes", quickNotes);

        SQLiteDatabase db = UtilStorage.getWritableDatabase(this);
        db.update(MainDbHelper.TABLE_REMINDERS_DETAIL, values,
                "_id = ?", new String[] {Integer.toString(reminderId)});
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

    private void onButtonTimeStampClick() {
        String timeStamp = "<" + new SimpleDateFormat("MM/dd EEE HH:mm", Locale.US)
                .format(Calendar.getInstance().getTime()) + ">\n";

        EditText editQuickNote = findViewById(R.id.quick_notes);
        editQuickNote.getText().insert(editQuickNote.getSelectionStart(), timeStamp);
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
