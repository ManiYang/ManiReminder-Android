package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class SitsEventsListAdapter extends RecyclerView.Adapter<SitsEventsListAdapter.ViewHolder>
        implements ItemTouchHelperCallback.AdapterForItemTouchHelper {

    private List<SitEventInfo> sitEventsData;
    private OnStartDragListener onStartDragListener;

    public static class SitEventInfo {
        private boolean isSituation;
        private int sitOrEventId;
        private String name;
        private int remsCount;

        public SitEventInfo(boolean isSituation, int sitOrEventId, String name, int remsCount) {
            this.isSituation = isSituation;
            this.name = name;
            this.sitOrEventId = sitOrEventId;
            this.remsCount = remsCount;
        }

        public boolean isSituation() {
            return isSituation;
        }

        public int getSitOrEventId() {
            return sitOrEventId;
        }

        public String getName() {
            return name;
        }
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public SitsEventsListAdapter(List<SitEventInfo> sitEventsData,
                                 OnStartDragListener onStartDragListener) {
        this.sitEventsData = sitEventsData;
        this.onStartDragListener = onStartDragListener;
    }

    public List<SitEventInfo> getSitEventsData() {
        return sitEventsData;
    }

    //
    @Override
    public int getItemCount() {
        return sitEventsData.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sits_events_list_item, parent, false);
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        SitEventInfo data = sitEventsData.get(position);
        String nameStr;
        if (data.isSituation) {
            nameStr = "(S) " + data.name;
        } else {
            nameStr = "(E) " + data.name;
        }

        LinearLayout layout = holder.layout;
        ((TextView) layout.findViewById(R.id.name)).setText(nameStr);
        ((TextView) layout.findViewById(R.id.count)).setText(
                String.format(Locale.US, "%d", data.remsCount));

        ImageView dragHandle = layout.findViewById(R.id.icon_reorder);
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    onStartDragListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    //
    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        SitEventInfo movedData = sitEventsData.remove(fromPosition);
        sitEventsData.add(toPosition, movedData);

        notifyItemMoved(fromPosition, toPosition);
    }

    //
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layout;

        public ViewHolder(LinearLayout v) {
            super(v);
            this.layout = v;
        }
    }
}
