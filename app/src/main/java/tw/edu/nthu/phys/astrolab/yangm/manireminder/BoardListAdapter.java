package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

public class BoardListAdapter
        extends RecyclerView.Adapter<BoardListAdapter.ViewHolder> {

    private SparseArray<ReminderData> remindersIdData;
    //private ItemClickListener itemClickListener;

    public static class ReminderData {
        private String title;
        private String description;

        public ReminderData(int id, String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    //
    public BoardListAdapter(SparseArray<ReminderData> remindersIdData) {
        this.remindersIdData = remindersIdData;
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReminderData data = remindersIdData.valueAt(position);

        CardView cardView = holder.cardView;
        ((TextView) cardView.findViewById(R.id.rem_title)).setText(data.getTitle());
        ((TextView) cardView.findViewById(R.id.rem_description)).setText(data.getDescription());

//        cardView.setOnClickListener(...);
//        cardView.setOnLongClickListener(...);
    }

    //
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;

        public ViewHolder(CardView v) {
            super(v);
            this.cardView = v;
        }
    }
}
