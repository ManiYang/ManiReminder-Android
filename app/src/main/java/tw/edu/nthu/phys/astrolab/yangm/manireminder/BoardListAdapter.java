package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BoardListAdapter
        extends RecyclerView.Adapter<BoardListAdapter.ViewHolder> {

    private SparseArray<ReminderData> remindersIdData;
    private ItemListener itemListener;

    public static class ReminderData {
        private String title;
        private String description;
        private boolean highlight;
        private boolean isTodo;
        private Calendar openTime;  //can be null

        public ReminderData(String title, String description,
                            boolean highlight, boolean isTodo, Calendar openTime) {
            this.title = title;
            this.description = description;
            this.highlight = highlight;
            this.isTodo = isTodo;
            this.openTime = openTime;
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

        public boolean isTodo() {
            return isTodo;
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

        TextView textViewDescription = cardView.findViewById(R.id.rem_description);
        if (data.getDescription().isEmpty()) {
            textViewDescription.setVisibility(View.GONE);
        } else {
            textViewDescription.setVisibility(View.VISIBLE);
            textViewDescription.setText(data.getDescription());
        }

        if (data.isTodo) {
            cardView.findViewById(R.id.todo).setVisibility(View.VISIBLE);
            cardView.findViewById(R.id.open_time).setVisibility(View.VISIBLE);
            if (data.openTime != null) {
                ((TextView) cardView.findViewById(R.id.open_time)).setText(
                        new SimpleDateFormat("HH:mm", Locale.US)
                                .format(data.openTime.getTime()));
            }
        } else {
            cardView.findViewById(R.id.todo).setVisibility(View.GONE);
            cardView.findViewById(R.id.open_time).setVisibility(View.GONE);
        }

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
