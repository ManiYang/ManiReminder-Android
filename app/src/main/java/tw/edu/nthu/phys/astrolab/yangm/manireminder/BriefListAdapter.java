package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

public class BriefListAdapter
        extends RecyclerView.Adapter<BriefListAdapter.ViewHolder> {

    private Cursor cursor; // {"_id", "title", "tags"}

    private SparseArray<String> allTags;
    private ItemClickListener itemClickListener;

    public BriefListAdapter(SparseArray<String> allTags) {
        this.allTags = allTags;
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public void closeCursor() {
        if (cursor != null) {
            cursor.close();
        }
        cursor = null;
    }

    @Override
    public int getItemCount() {
        if (cursor != null) {
            return cursor.getCount();
        } else
            return 0;
    }

    public void setItemClickListener(ItemClickListener listener) {
        this.itemClickListener = listener;
    }

    //
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.brief_list_item, parent, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        CardView cardView = holder.cardView;
        ((TextView) cardView.findViewById(R.id.item_title)).setText(cursor.getString(1));

        String tagsStr = UtilReminder.buildTagsString(cursor.getString(2), allTags);
        ((TextView) cardView.findViewById(R.id.item_tags)).setText(tagsStr);

        final int reminderId = cursor.getInt(0);
        ((TextView) cardView.findViewById(R.id.item_id)).setText(
                String.format(Locale.US, "#%d", reminderId));

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
