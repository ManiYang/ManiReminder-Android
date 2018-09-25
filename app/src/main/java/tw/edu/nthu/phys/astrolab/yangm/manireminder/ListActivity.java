package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

public class ListActivity extends AppCompatActivity {

//    private SQLiteDatabase db;
    private Cursor cursorRemindersBrief;
    private SparseArray<String> allTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SQLiteDatabase db = UtilStorage.getReadableDatabase(this);
        cursorRemindersBrief = db.query(MainDbHelper.TABLE_REMINDERS_BRIEF, null,
                null, null, null, null, null);
        allTags = UtilReminder.getAllTagsFromDb(db);
        populateList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursorRemindersBrief != null) {
            cursorRemindersBrief.close();
        }
    }

    private void populateList() {
        // get reminders brief data from database
        int nRows = cursorRemindersBrief.getCount();
        Log.v("ListActivity", "### Number of reminders: "+Integer.toString(nRows));

        int[] ids = new int[nRows];
        String[] titles = new String[nRows];
        String[] tagsStrings = new String[nRows];

        for(int p=0; p<nRows; p++) {
            cursorRemindersBrief.moveToPosition(p);
            ids[p] = cursorRemindersBrief.getInt(0);
            titles[p] = cursorRemindersBrief.getString(1);
            tagsStrings[p] = UtilReminder.buildTagsString(
                    cursorRemindersBrief.getString(2), allTags);
        }

        // create BriefListAdapter with data
        BriefListAdapter adapter = new BriefListAdapter(ids, titles, tagsStrings);
        adapter.setItemClickListener(new BriefListAdapter.ItemClickListener() {
            @Override
            public void onClick(int reminderId) {
                Intent intent = new Intent(ListActivity.this, DetailActivity.class)
                        .putExtra(DetailActivity.EXTRA_REMINDER_ID, reminderId);
                startActivity(intent);
            }
        });

        // prepare brief_list_recycler
        RecyclerView briefListRecycler = findViewById(R.id.brief_list_recycler);
        briefListRecycler.setAdapter(adapter);
        briefListRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    // option menu //
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_reminder:
                actionCreateReminder();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void actionCreateReminder() {
        SQLiteDatabase db = UtilStorage.getWritableDatabase(this);

        // get largest reminder ID
        int largestId;
        if (cursorRemindersBrief.getCount() == 0) {
            largestId = -1;
        } else {
            Cursor cursor = db.query(MainDbHelper.TABLE_REMINDERS_BRIEF, new String[] {"MAX(_id)"},
                    null, null, null, null, null);
            cursor.moveToFirst();
            largestId = cursor.getInt(0);
            cursor.close();
        }
        Log.v("ListActivity", "### largestId = "+Integer.toString(largestId));

        // create new empty reminder in database
        int newId = largestId + 1;
        MainDbHelper.addEmptyReminder(db, newId, "New Empty Reminder");

        // launch DetailActivity with the new reminder
        startActivity(new Intent(this, DetailActivity.class)
                .putExtra(DetailActivity.EXTRA_REMINDER_ID, newId));
    }
}
