package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BoardListAdapter
        extends RecyclerView.Adapter<BoardListAdapter.ViewHolder> {

    private SparseArray<ReminderData> remindersIdData;
    private ItemListener itemListener;

    public static class ReminderData {
        private String title;
        private String description;
        private boolean highlight;

        public ReminderData(String title, String description, boolean highlight) {
            this.title = title;
            this.description = description;
            this.highlight = highlight;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public boolean isHighlighted() {
            return highlight;
        }

        public void toggleHighlight() {
            highlight = !highlight;
        }
    }

    public BoardListAdapter(SparseArray<ReminderData> remindersIdData) {
        this.remindersIdData = remindersIdData;
    }

    public void setItemListener(ItemListener itemListener) {
        this.itemListener = itemListener;
    }

    @Override
    public int getItemCount() {
        return remindersIdData.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.board_list_card, parent, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final int remId = remindersIdData.keyAt(position);
        ReminderData data = remindersIdData.valueAt(position);

        CardView cardView = holder.cardView;
        ((TextView) cardView.findViewById(R.id.rem_title)).setText(data.getTitle());
        ((TextView) cardView.findViewById(R.id.rem_description)).setText(data.getDescription());
        cardView.setSelected(data.isHighlighted());

        if (itemListener != null) {
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemListener.onClick(v, remId);
                }
            });
            cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    itemListener.onLongClick(v, remId);
                    return true;
                }
            });
        }
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
    interface ItemListener {
        void onClick(View view, int remId);
        void onLongClick(View view, int remId);
    }
}
