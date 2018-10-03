package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;

public class EditSitsEventsActivity extends AppCompatActivity
        implements SitsEventsListAdapter.OnStartDragListener {

    private SitsEventsListAdapter adapter;
    private ItemTouchHelper itemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sits_events);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSitsEvents();
    }

    //
    private void loadData() {
        // get all sits/events
        List<UtilStorage.SitOrEvent> allSitsEvents = UtilStorage.getAllSitsEvents(this);

        // get reminder counts of sits/events from DB
        SparseIntArray sitsCounts = new SparseIntArray();
        SparseIntArray eventsCounts = new SparseIntArray();
        for (UtilStorage.SitOrEvent sitEvent: allSitsEvents) {
            if (sitEvent.isSituation) {
                sitsCounts.append(sitEvent.id, 0);
            } else {
                eventsCounts.append(sitEvent.id, 0);
            }
        }

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

        for (UtilStorage.SitOrEvent sitEvent: allSitsEvents) {
            boolean isSit = sitEvent.isSituation;
            int id = sitEvent.id;
            sitsEventsData.add(new SitsEventsListAdapter.SitEventInfo(
                    isSit, id, sitEvent.name,
                    isSit ? sitsCounts.get(id) : eventsCounts.get(id)));
        }

        // set up RecyclerView
        adapter = new SitsEventsListAdapter(sitsEventsData, this);

        RecyclerView recycler = findViewById(R.id.recycler_sits_events);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recycler);
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

    private void saveSitsEvents() {
        List<SitsEventsListAdapter.SitEventInfo> sitEventInfoList = adapter.getSitEventsData();

        List<UtilStorage.SitOrEvent> sitEvents = new ArrayList<>();
        for (SitsEventsListAdapter.SitEventInfo info: sitEventInfoList) {
            sitEvents.add(new UtilStorage.SitOrEvent(
                    info.isSituation(), info.getSitOrEventId(), info.getName()));
        }
        UtilStorage.overwriteAllSituationsEvents(this, sitEvents);
    }

    //
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }
}
