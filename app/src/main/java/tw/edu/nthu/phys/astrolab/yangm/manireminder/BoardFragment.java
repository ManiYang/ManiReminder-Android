package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class BoardFragment extends Fragment {

    private BroadcastReceiver updateBroadcastReceiver;

    public BoardFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_board, container, false);


        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        populateList();

        // register broadcast receiver
        if (updateBroadcastReceiver == null) {
            updateBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // todo: update view

                    Toast.makeText(context, "update board...", Toast.LENGTH_SHORT).show();



                }
            };
        }
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(updateBroadcastReceiver, new IntentFilter(
                        "tw.edu.nthu.phys.astrolab.yangm.manireminder.UPDATE_BOARD"));
    }

    @Override
    public void onStop() {
        super.onStop();

        //unregister broadcast receiver
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(updateBroadcastReceiver);
    }

    private void populateList() {
        SparseArray<BoardListAdapter.ReminderData> remindersIdData = new SparseArray<>();


        BoardListAdapter adapter = new BoardListAdapter(remindersIdData);
        View view = getView();
        if (view == null)
            throw new RuntimeException("`view` is null");
        RecyclerView boardRecycler = view.findViewById(R.id.board_recycler);
        boardRecycler.setAdapter(adapter);
        boardRecycler.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
    }
}
