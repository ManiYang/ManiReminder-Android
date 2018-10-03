package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * For items of situation/event list in EditSitsEventsActivity
 */
public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    AdapterForItemTouchHelper adapter;

    public interface AdapterForItemTouchHelper {
        void onItemMove(int fromPosition, int toPosition);
    }

    public ItemTouchHelperCallback(AdapterForItemTouchHelper adapter) {
        this.adapter = adapter;
    }

    //
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}
}
