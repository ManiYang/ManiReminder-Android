package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryViewActivity extends AppCompatActivity {

    private SparseArray<String> allSits;
    private SparseArray<String> allEvents;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_view);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadHistoryData();
        allSits = UtilStorage.getAllSituations(this);
        allEvents = UtilStorage.getAllEvents(this);
    }

    private void loadHistoryData() {
        SQLiteDatabase db;
        try {
            SQLiteOpenHelper mainDbHelper = new MainDbHelper(this);
            db = mainDbHelper.getReadableDatabase();
        } catch (SQLiteException e) {
            Toast.makeText(this, "Database unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        if (cursor != null)
            cursor.close();
        cursor = db.query(MainDbHelper.TABLE_HISTORY, null,
                null, null, null, null, "_id DESC");

        HistoryListAdapter adapter = new HistoryListAdapter(this, cursor);
        ((ListView) findViewById(R.id.listview)).setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
    }

    //
    private class HistoryListAdapter extends CursorAdapter {
        public HistoryListAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            TextView view =  (TextView) getLayoutInflater().inflate(
                    R.layout.text_list_item_plain_monospace, parent, false);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String dateStr = cursor.getString(1).substring(4);
            String timeStr = cursor.getString(2);
            int type = cursor.getInt(3);
            int sitOrEventId = cursor.getInt(4);

            String sitOrEventName = (type == UtilStorage.HIST_TYPE_EVENT) ?
                    allEvents.get(sitOrEventId) : allSits.get(sitOrEventId);

            String startEnd = "";
            if (type == UtilStorage.HIST_TYPE_SIT_START)
                startEnd = "start";
            else if (type == UtilStorage.HIST_TYPE_SIT_END)
                startEnd = "end";

            String itemText = String.format("%s.%s %s[%s]%s",
                    dateStr, timeStr,
                    (type == UtilStorage.HIST_TYPE_EVENT ? "event" : "sit"), sitOrEventName, startEnd);
            ((TextView) view).setText(itemText);
        }
    }
}
