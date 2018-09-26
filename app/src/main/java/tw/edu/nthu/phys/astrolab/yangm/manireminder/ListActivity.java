package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;

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

    private static final int REQUEST_CODE_FOR_BACKUP_DATA = 0;
    private static final int REQUEST_CODE_FOR_RESTORE_DATA = 1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

    private void actionBackupRemData() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "Could not access external storage",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = new File(Environment.getExternalStorageDirectory() + "/Android/data/"
                + "tw.edu.nthu.phys.astrolab.yangm.manireminder/files/Reminders/");
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
                && UtilStorage.dumpTableToCsv(this, MainDbHelper.TABLE_SITUATIONS,
                        new File(dir, "situations.csv"))
                && UtilStorage.dumpTableToCsv(this, MainDbHelper.TABLE_EVENTS,
                        new File(dir, "events.csv"));
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
                .setMessage("Are you sure to overwrite current reminder data with last backup, "
                        + "and do a fresh restart?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= 23) {
                            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED) {
                                restoreRemData();
                            } else {
                                requestReadExternalStoragePermission(REQUEST_CODE_FOR_RESTORE_DATA);
                            }
                        } else {
                            restoreRemData();
                        }
                    }
                }).show();
    }

    private void restoreRemData() {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)
                && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Toast.makeText(this, "Could not access external storage",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = new File(Environment.getExternalStorageDirectory() + "/Android/data/"
                + "tw.edu.nthu.phys.astrolab.yangm.manireminder/files/Reminders/");
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
                && UtilStorage.overwriteTableFromCsv(this, MainDbHelper.TABLE_SITUATIONS,
                        new File(dir, "situations.csv"))
                && UtilStorage.overwriteTableFromCsv(this, MainDbHelper.TABLE_EVENTS,
                        new File(dir, "events.csv"));
        if (!check) {
            Toast.makeText(this, "Could not restore reminder data", Toast.LENGTH_SHORT)
                    .show();
        } else {
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
                    restoreRemData();
                } else {
                    Toast.makeText(this, "storage-reading permission denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
