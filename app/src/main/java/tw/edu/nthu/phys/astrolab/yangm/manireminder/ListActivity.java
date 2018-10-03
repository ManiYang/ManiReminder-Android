package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ListActivity extends AppCompatActivity {

//    private Cursor cursorRemindersBrief;
    private SparseArray<String> allTags;
    BriefListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //
        SQLiteDatabase db = UtilStorage.getReadableDatabase(this);
        allTags = UtilReminder.getAllTagsFromDb(db);

        adapter = new BriefListAdapter(allTags);
        adapter.setItemClickListener(new BriefListAdapter.ItemClickListener() {
            @Override
            public void onClick(int reminderId) {
                Intent intent = new Intent(ListActivity.this, DetailActivity.class)
                        .putExtra(DetailActivity.EXTRA_REMINDER_ID, reminderId);
                startActivity(intent);
            }
        });

        RecyclerView briefListRecycler = findViewById(R.id.brief_list_recycler);
        briefListRecycler.setAdapter(adapter);
        briefListRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        FilterSpec filterSpec = new FilterSpec().readFromStorage();
        setFilterSpecView(filterSpec);
        populateList(filterSpec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.closeCursor();
    }

    private void populateList(FilterSpec filterSpec) {
        Log.v("ListActivity", "#### populateList() start");

        adapter.closeCursor();

        // query DB according to filter spec
        SQLiteDatabase db = UtilStorage.getReadableDatabase(this);

        Cursor cursor;
        String tableRemBrief = MainDbHelper.TABLE_REMINDERS_BRIEF;
        String tableRemDetail = MainDbHelper.TABLE_REMINDERS_DETAIL;
        if (filterSpec.hasFilterHavingQuickNotes()) {
            cursor = db.rawQuery("SELECT b._id, b.title, b.tags "
                    + "FROM " + tableRemBrief + " b INNER JOIN " + tableRemDetail + " d "
                    + "ON b._id = d._id "
                    + "WHERE d.quick_notes != '';",
                    null);
        } else {
            cursor = db.query(MainDbHelper.TABLE_REMINDERS_BRIEF,
                    new String[]{"_id", "title", "tags"},
                    null, null, null, null, null);
        }

        // assign new cursor to adapter
        adapter.setCursor(cursor);

        RecyclerView briefListRecycler = findViewById(R.id.brief_list_recycler);
        if (briefListRecycler.getAdapter() == null) {
            briefListRecycler.setAdapter(adapter);
            briefListRecycler.setLayoutManager(new LinearLayoutManager(this));
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    // option menu //
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private static final int REQUEST_CODE_FOR_BACKUP_DATA = 0;
    private static final int REQUEST_CODE_FOR_RESTORE_DATA = 1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter_none:
                userSetFilterNone();
                return true;

            case R.id.action_filter_quick_notes:
                userSetFilterHavingQuickNotes();
                return true;

            case R.id.action_create_reminder:
                actionCreateReminder();
                return true;

            case R.id.action_backup_reminder_data:
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        actionBackupRemData();
                    } else {
                        requestWriteExternalStoragePermission(REQUEST_CODE_FOR_BACKUP_DATA);
                    }
                } else {
                    actionBackupRemData();
                }
                return true;

            case R.id.action_restore_rem_data_from_backup:
                actionRestoreRemData();
                return true;

            default:
                return super.onOptionsItemSelected(item);
            }
        }

    private void actionCreateReminder() {
        SQLiteDatabase db = UtilStorage.getWritableDatabase(this);

        // get largest reminder ID
        List<Integer> idList = UtilStorage.getIdsInTable(
                this, MainDbHelper.TABLE_REMINDERS_BRIEF);
        int largestId = idList.isEmpty() ? (-1) : Collections.max(idList);

        // create new empty reminder in database
        int newId = largestId + 1;
        MainDbHelper.addEmptyReminder(db, newId, "New Empty Reminder");

        // launch DetailActivity with the new reminder
        startActivity(new Intent(this, DetailActivity.class)
                .putExtra(DetailActivity.EXTRA_REMINDER_ID, newId));
    }

    private static final String BACKUP_DIRECTORY = "/ManiReminder/backup/";

    private void actionBackupRemData() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "Could not access external storage",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = new File(Environment.getExternalStorageDirectory() + BACKUP_DIRECTORY);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Toast.makeText(this, "Could not create directory", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        }

        boolean check =
                UtilStorage.dumpTableToCsv(this, MainDbHelper.TABLE_REMINDERS_BRIEF,
                        new File(dir, "reminder_brief.csv"))
                && UtilStorage.dumpTableToCsv(this, MainDbHelper.TABLE_REMINDERS_DETAIL,
                        new File(dir, "reminder_detail.csv"))
                && UtilStorage.dumpTableToCsv(this, MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                        new File(dir, "reminder_behavior.csv"))
                && UtilStorage.dumpTableToCsv(this, MainDbHelper.TABLE_TAGS,
                        new File(dir, "tags.csv"))
                && UtilStorage.dumpTableToCsv(this, MainDbHelper.TABLE_SITUATIONS_EVENTS,
                        new File(dir, "situations_events.csv"));
        if (!check) {
            Toast.makeText(this, "Could not write to files", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(this, "files written to external storage",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void actionRestoreRemData() {
        new AlertDialog.Builder(this).setTitle("Warning")
                .setMessage("Are you sure to overwrite current reminder data with last backup? "
                        + "Reminders currently opened may be closed.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= 23) {
                            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED) {
                                restoreRemDataFromBackup();
                            } else {
                                requestReadExternalStoragePermission(REQUEST_CODE_FOR_RESTORE_DATA);
                            }
                        } else {
                            restoreRemDataFromBackup();
                        }
                    }
                }).show();
    }

    private void restoreRemDataFromBackup() {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)
                && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Toast.makeText(this, "Could not access external storage",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = new File(Environment.getExternalStorageDirectory() + BACKUP_DIRECTORY);
        if (! new File(dir, "reminder_brief.csv").exists()) {
            Toast.makeText(this, "Could not find backup data", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        boolean check =
                UtilStorage.overwriteTableFromCsv(this, MainDbHelper.TABLE_REMINDERS_BRIEF,
                        new File(dir, "reminder_brief.csv"))
                && UtilStorage.overwriteTableFromCsv(this, MainDbHelper.TABLE_REMINDERS_DETAIL,
                        new File(dir, "reminder_detail.csv"))
                && UtilStorage.overwriteTableFromCsv(this, MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                        new File(dir, "reminder_behavior.csv"))
                && UtilStorage.overwriteTableFromCsv(this, MainDbHelper.TABLE_TAGS,
                        new File(dir, "tags.csv"))
                && UtilStorage.overwriteTableFromCsv(this, MainDbHelper.TABLE_SITUATIONS_EVENTS,
                        new File(dir, "situations_events.csv"));
        if (!check) {
            Toast.makeText(this, "Could not restore reminder data", Toast.LENGTH_SHORT)
                    .show();
        } else {
            // close all reminders
            UtilStorage.clearOpenedReminders(this);

            // do a fresh restart
            new ReminderBoardLogic(this).freshRestart();
            Toast.makeText(this, "restored reminder data from backup",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // permission request for reading/writing external storage //
    private void requestWriteExternalStoragePermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        }
    }

    private void requestReadExternalStoragePermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_FOR_BACKUP_DATA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    actionBackupRemData();
                } else {
                    Toast.makeText(this, "storage-writing permission denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_FOR_RESTORE_DATA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    restoreRemDataFromBackup();
                } else {
                    Toast.makeText(this, "storage-reading permission denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    // filters
    private void setFilterSpecView(FilterSpec spec) {
        if (spec.havingQuickNotes) {
            ((TextView) findViewById(R.id.filter_name)).setText("Having Quick Notes");
            ((TextView) findViewById(R.id.filter_name)).setTextColor(
                    ContextCompat.getColor(this, R.color.colorPrimary));

        } else {
            ((TextView) findViewById(R.id.filter_name)).setText("None");
            ((TextView) findViewById(R.id.filter_name)).setTextColor(Color.BLACK);
        }
    }

    private void userSetFilterNone() {
        FilterSpec filterSpec = new FilterSpec().readFromStorage();
        if (filterSpec.isEmpty()) {
            return;
        }

        filterSpec.clear();

        setFilterSpecView(filterSpec);
        populateList(filterSpec);
        filterSpec.writeToStorage();
    }

    private void userSetFilterHavingQuickNotes() {
        FilterSpec filterSpec = new FilterSpec().readFromStorage();
        if (filterSpec.hasFilterHavingQuickNotes()) {
            return;
        }

        filterSpec.clear();
        filterSpec.addFilterHavingQuickNotes();

        setFilterSpecView(filterSpec);
        populateList(filterSpec);
        filterSpec.writeToStorage();
    }

    private class FilterSpec {
        private boolean havingQuickNotes;

        FilterSpec() {
            clear();
        }

        private FilterSpec readFromStorage() {
            String specStr = UtilStorage.readSharedPrefString(ListActivity.this,
                    UtilStorage.KEY_ALL_REM_LIST_FILTER_SPEC, "");

            clear();
            if (specStr.trim().isEmpty()) {
                return this;
            }

            if (specStr.equals("quick_notes")) {
                havingQuickNotes = true;
            }
            return this;
        }

        private void clear() {
            havingQuickNotes = false;
        }

        private FilterSpec addFilterHavingQuickNotes() {
            havingQuickNotes = true;
            return this;
        }

        //
        private boolean isEmpty() {
            return !havingQuickNotes;
        }

        private boolean hasFilterHavingQuickNotes() {
            return havingQuickNotes;
        }

        private void writeToStorage() {
            String specStr;
            if (!havingQuickNotes) {
                specStr = "";
            } else {
                specStr = "quick_notes";
            }

            UtilStorage.writeSharedPrefString(ListActivity.this,
                    UtilStorage.KEY_ALL_REM_LIST_FILTER_SPEC, specStr);
        }
    }
}
