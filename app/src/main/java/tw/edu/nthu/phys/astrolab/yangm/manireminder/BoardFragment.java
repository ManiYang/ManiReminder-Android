package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class BoardFragment extends Fragment {

    private SparseArray<BoardListAdapter.ReminderData> remindersIdData; //referenced by adapter,
                                                                        //do not re-assign
    private BroadcastReceiver updateBroadcastReceiver;

    public BoardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_board, container, false);

        //
        remindersIdData = new SparseArray<>();
        BoardListAdapter adapter = new BoardListAdapter(remindersIdData);
        RecyclerView boardRecycler = view.findViewById(R.id.board_recycler);
        boardRecycler.setAdapter(adapter);
        boardRecycler.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // create broadcast receiver (registered in onStart())
        updateBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadData();
            }
        };

        //
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        //
        loadData();

        // register broadcast receiver
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(updateBroadcastReceiver,
                        new IntentFilter(getString(R.string.action_update_board)));
    }

    @Override
    public void onStop() {
        super.onStop();

        //unregister broadcast receiver
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(updateBroadcastReceiver);
    }

    private void loadData() {
        // read opened reminders (ID's)
        List<Integer> openedRemIds = UtilStorage.getOpenedReminders(getContext());

        // read reminder titles
        SparseArray<String> remTitles = new SparseArray<>();

        String whereArgIds = UtilGeneral.joinIntegerList(", ", openedRemIds);
        Log.v("BoardFragment", "whereArgIds="+whereArgIds);

        SQLiteDatabase db = UtilStorage.getReadableDatabase(getContext());
        Cursor cursor = db.query(MainDbHelper.TABLE_REMINDERS_BRIEF, new String[] {"_id", "title"},
                "_id IN ("+UtilStorage.placeHolders(openedRemIds.size())+")",
                 UtilGeneral.toStringArray(openedRemIds),
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            remTitles.append(cursor.getInt(0), cursor.getString(1));
        }
        cursor.close();
        Log.v("BoardFragment", "remTitles.size()="+remTitles.size());


        // read reminder descriptions
        SparseArray<String> remDescriptions = new SparseArray<>();

        cursor = db.query(MainDbHelper.TABLE_REMINDERS_DETAIL, new String[] {"_id", "description"},
                "_id IN ("+UtilStorage.placeHolders(openedRemIds.size())+")",
                 UtilGeneral.toStringArray(openedRemIds),
                null, null, null);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            remDescriptions.append(cursor.getInt(0), cursor.getString(1));
        }
        cursor.close();

        // update `remindersIdData`
        remindersIdData.clear();
        for (int remId: openedRemIds) {
            String title = remTitles.get(remId);
            String description = remDescriptions.get(remId);
            if (title == null || description == null) {
                throw new RuntimeException("failed to get reminder data");
            }
            remindersIdData.append(remId, new BoardListAdapter.ReminderData(title, description));
        }

        // notify adapter about data change
        View view = getView();
        if (view == null)
            throw new RuntimeException("`view` is null");
        BoardListAdapter adapter = (BoardListAdapter)
                ((RecyclerView) view.findViewById(R.id.board_recycler)).getAdapter();
        adapter.notifyDataSetChanged();
    }
}
