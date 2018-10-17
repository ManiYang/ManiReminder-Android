package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.util.List;

public class RemChecklistAdapter extends RecyclerView.Adapter<RemChecklistAdapter.ViewHolder> {

    private List<String> texts;
    private List<Boolean> checked;
    private int selectedPos = -1;
    private OnItemSelectDeselectListener listener;

    public interface OnItemSelectDeselectListener {
        void onItemSelectDeselect(boolean anyItemSelected);
    }

    public RemChecklistAdapter(List<String> texts, List<Boolean> checked,
                               OnItemSelectDeselectListener listener) {
        this.texts = texts;
        this.checked = checked;
        this.listener = listener;
    }

    public void addItem(String text) {
        texts.add(text);
        checked.add(false);
        notifyItemInserted(texts.size() - 1);
    }

    public List<String> getTexts() {
        return texts;
    }

    public List<Boolean> getCheckedList() {
        return checked;
    }

    public String getSelectedText() {
        return (selectedPos == -1) ? null : texts.get(selectedPos);
    }

    public void renameSelected(String newText) {
        if (selectedPos != -1) {
            texts.set(selectedPos, newText);
            notifyItemChanged(selectedPos);
        }
    }

    public void removeSelected() {
        if (selectedPos != -1) {
            texts.remove(selectedPos);
            checked.remove(selectedPos);
            selectedPos = -1;
            notifyDataSetChanged();
            listener.onItemSelectDeselect(false);
        }
    }

    //
    @Override
    public int getItemCount() {
        return texts.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rem_checklist_item, parent, false);
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        CheckBox checkBox = holder.layout.findViewById(R.id.checkBox);
        checkBox.setText(texts.get(position));
        checkBox.setChecked(checked.get(position));
        holder.layout.setSelected(position == selectedPos);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checked.set(position, isChecked);
            }
        });

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.isSelected()) {
                    v.setSelected(false);
                    selectedPos = -1;
                } else {
                    int prevSelectedPos = selectedPos;
                    selectedPos = position;
                    v.setSelected(true);

                    if (prevSelectedPos != -1) {
                        notifyItemChanged(prevSelectedPos);
                    }
                }

                if (listener != null) {
                    listener.onItemSelectDeselect(selectedPos != -1);
                }
            }
        });
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
