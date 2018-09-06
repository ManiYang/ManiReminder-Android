package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BriefListAdapter
        extends RecyclerView.Adapter<BriefListAdapter.ViewHolder> {

    private int[] reminderIds;
    private String[] titles;
    private String[] tagStrings;
    private ItemClickListener itemClickListener;

    public BriefListAdapter(int[] reminderIds, String[] titles, String[] tagStrings) {
        this.reminderIds = reminderIds;
        this.titles = titles;
        this.tagStrings = tagStrings;
    }

    @Override
    public int getItemCount() {
        return reminderIds.length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.brief_list_item, parent, false);
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LinearLayout layout = holder.layout;

        ((TextView) layout.findViewById(R.id.item_title)).setText(titles[position]);
        ((TextView) layout.findViewById(R.id.item_tags)).setText(tagStrings[position]);

        final int reminderId = reminderIds[position];
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onClick(reminderId);
                }
            }
        });
    }

    //
    public void setItemClickListener(ItemClickListener listener) {
        this.itemClickListener = listener;
    }

    //
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layout;

        public ViewHolder(LinearLayout layout) {
            super(layout);
            this.layout = layout;
        }
    }

    //
    interface ItemClickListener {
        void onClick(int reminderId);
    }
}
