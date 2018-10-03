package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;

public class EditSitsEventsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sits_events);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadData();
    }


    private void loadData() {
        // get all sits/events
        SparseArray<String> allSits = UtilStorage.getAllSituations(this);
        SparseArray<String> allEvents = UtilStorage.getAllEvents(this);

        // get reminder counts of sits/events from DB
        SparseIntArray sitsCounts = new SparseIntArray();
        SparseIntArray eventsCounts = new SparseIntArray();

        SQLiteDatabase db = UtilStorage.getReadableDatabase(this);
        Cursor cursor = db.query(MainDbHelper.TABLE_REMINDERS_BEHAVIOR,
                new String[] {"involved_sits", "involved_events"},
                null, null,
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String sitsStr = cursor.getString(0);
            addToCounts(sitsCounts, sitsStr);

            String eventsStr = cursor.getString(1);
            addToCounts(eventsCounts, eventsStr);
        }
        cursor.close();

        // prepare data for adapter
        List<SitsEventsListAdapter.SitEventInfo> sitsEventsData = new ArrayList<>();

        for (int i=0; i<sitsCounts.size(); i++) {
            int sitId = sitsCounts.keyAt(i);
            String sitName = allSits.get(sitId);
            sitsEventsData.add(new SitsEventsListAdapter.SitEventInfo(
                    true, sitName, sitsCounts.valueAt(i)));
        }
        for (int i=0; i<eventsCounts.size(); i++) {
            int eventId = eventsCounts.keyAt(i);
            String eventName = allEvents.get(eventId);
            sitsEventsData.add(new SitsEventsListAdapter.SitEventInfo(
                    false, eventName, eventsCounts.valueAt(i)));
        }

        //
        RecyclerView recycler = findViewById(R.id.recycler_sits_events);
        SitsEventsListAdapter adapter = new SitsEventsListAdapter(sitsEventsData);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void addToCounts(SparseIntArray idsCounts, String ids) {
        String[] idStrArray = ids.split(",");
        for (String idStr: idStrArray) {
            if (idStr.isEmpty()) {
                continue;
            }
            int id = Integer.parseInt(idStr);
            int index = idsCounts.indexOfKey(id);
            if (index < 0) {
                idsCounts.put(id, 1);
            } else {
                idsCounts.put(id, idsCounts.valueAt(index) + 1);
            }
        }
    }
}
