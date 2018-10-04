package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.SparseIntArray;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditSitsEventsActivity extends AppCompatActivity
        implements SitsEventsListAdapter.OnStartDragListener, SimpleTextEditDialogFragment.Listener {

    private SitsEventsListAdapter adapter;
    private ItemTouchHelper itemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sits_events);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set up RecyclerView
        List<SitsEventsListAdapter.SitEventInfo> sitsEventsData = readSitsEventsData();

        adapter = new SitsEventsListAdapter(sitsEventsData, this);

        RecyclerView recycler = findViewById(R.id.recycler_sits_events);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recycler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSitsEvents();
    }

    // context menu item
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int position = item.getItemId();
        final SitsEventsListAdapter.SitEventInfo sitEvent = adapter.getSitEventsData().get(position);

        String actionTitle = item.getTitle().toString();
        switch (actionTitle) {
            case "Edit": {
                SimpleTextEditDialogFragment dialog = SimpleTextEditDialogFragment.newInstance(
                        "Edit " + (sitEvent.isSituation() ? "situation" : "event") + " name:",
                        sitEvent.getName(), InputType.TYPE_CLASS_TEXT);
                String dialogTag = String.format(Locale.US, "edit_%s_%d",
                        sitEvent.isSituation() ? "sit" : "event", sitEvent.getSitOrEventId());
                dialog.show(getSupportFragmentManager(), dialogTag);
                return true;
            }
            case "Remove": {
                String extraMsg = " If the situation is started, it will first be stopped.";
                new AlertDialog.Builder(this)
                        .setTitle("Please confirm")
                        .setMessage("Are you sure to remove "
                                + (sitEvent.isSituation() ? "situation" : "event") + " \""
                                + sitEvent.getName() + "\"?"
                                + (sitEvent.isSituation() ? extraMsg : ""))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                actionRemoveSitEvent(position);
                            }
                        }).show();
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onDialogClick(DialogFragment dialog, boolean positive, String newText) {
        if (!positive) {
            return;
        }

        String tag = dialog.getTag();
        newText = newText.trim().replace(',', '.');
        if (tag.startsWith("edit_")) {
            if (tag.substring(5).startsWith("sit_")) {
                newText = newText.replace('：', ':')
                        .replaceAll(" *: *", ":");
                int sitId = Integer.parseInt(tag.substring(9));
                adapter.renameSituation(sitId, newText);
            } else if (tag.substring(5).startsWith("event_")) {
                int eventId = Integer.parseInt(tag.substring(11));
                adapter.renameEvent(eventId, newText);
            }
        }
    }

    private void actionRemoveSitEvent(int position) {
        SitsEventsListAdapter.SitEventInfo sitEvent = adapter.getSitEventsData().get(position);
        adapter.removeSitOrEvent(position);

        // remove from started situation
        if (sitEvent.isSituation()) {
            UtilStorage.removeFromStartedSituations(this, sitEvent.getSitOrEventId());
        }

        // remove from history
        if (sitEvent.isSituation()) {
            UtilStorage.removeHistoryRecordsOfSituation(this, sitEvent.getSitOrEventId());
        } else {
            UtilStorage.removeHistoryRecordsOfEvent(this, sitEvent.getSitOrEventId());

        }
    }

    //
    private List<SitsEventsListAdapter.SitEventInfo> readSitsEventsData() {
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

        // prepare data (for adapter)
        List<SitsEventsListAdapter.SitEventInfo> sitsEventsData = new ArrayList<>();

        for (UtilStorage.SitOrEvent sitEvent: allSitsEvents) {
            boolean isSit = sitEvent.isSituation;
            int id = sitEvent.id;
            sitsEventsData.add(new SitsEventsListAdapter.SitEventInfo(
                    isSit, id, sitEvent.name,
                    isSit ? sitsCounts.get(id) : eventsCounts.get(id)));
        }
        return sitsEventsData;
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
