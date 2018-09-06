package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class DetailFragment extends Fragment {

    private int reminderId = -9;
    private SQLiteDatabase db;

    public DetailFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // open database
        SQLiteOpenHelper mainDbHelper = new MainDbHelper(getContext());
        try {
            db = mainDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            db = null;
            Toast.makeText(getContext(), "Database unavailable", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.v("DetailFragment", "### Reminder ID: " + Integer.toString(reminderId));
        if (reminderId < 0) {
            throw new RuntimeException("### reminderId < 0");
        }
        loadReminderData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    //
    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
    }

    //
    private void loadReminderData() {
        if (db == null) {
            return;
        }

        View view = getView();
        if (view == null) {
            return;
        }

        // get all tags
        SparseArray<String> allTags = UtilReminder.getAllTagsFromDb(db);

        // get brief data
        Cursor cursor = db.query(MainDbHelper.TABLE_REMINDERS_BRIEF, null,
                "_id = ?", new String[] {Integer.toString(reminderId)},
                null, null, null);
        if (!cursor.moveToFirst()) {
            throw new RuntimeException(
                    String.format("Reminder (id: %d) not found in database", reminderId));
        }
        String title = cursor.getString(1);
        String tagsStr = UtilReminder.buildTagsString(cursor.getString(2), allTags);
        if (tagsStr.isEmpty()) {
            tagsStr = "(none)";
        }
        cursor.close();

        // get detailed data
        cursor = db.query(MainDbHelper.TABLE_REMINDERS_DETAIL, null,
                "_id = ?", new String[] {Integer.toString(reminderId)},
                null, null, null);
        if (!cursor.moveToFirst()) {
            throw new RuntimeException(String.format(
                    "Reminder (id: %d) not found in table 'reminders_detail'", reminderId));
        }
        String description = cursor.getString(1);
        if (description.isEmpty()) {
            description = "(none)";
        }
        cursor.close();

        // set contents of views
        ((TextView) view.findViewById(R.id.title)).setText(title);
        ((TextView) view.findViewById(R.id.tags)).setText(tagsStr);
        ((TextView) view.findViewById(R.id.description)).setText(description);
    }
}
