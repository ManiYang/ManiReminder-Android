package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.brief_list_item, parent, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardView cardView = holder.cardView;

        ((TextView) cardView.findViewById(R.id.item_title)).setText(titles[position]);
        ((TextView) cardView.findViewById(R.id.item_tags)).setText(tagStrings[position]);

        final int reminderId = reminderIds[position];
        cardView.setOnClickListener(new View.OnClickListener() {
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
        private CardView cardView;

        public ViewHolder(CardView v) {
            super(v);
            this.cardView = v;
        }
    }

    //
    interface ItemClickListener {
        void onClick(int reminderId);
    }
}
