package tw.edu.nthu.phys.astrolab.yangm.manireminder;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class BoardFragment extends Fragment {


    public BoardFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_board, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        populateList();
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
