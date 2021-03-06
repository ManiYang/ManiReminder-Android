package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextListAdapter
        extends RecyclerView.Adapter<TextListAdapter.ViewHolder> {

    public static final int NO_SELECTION = 0; //Can set NoSelectionOn(Long)ClickListener.
    public static final int SINGLE_SELECTION = 1; //Item can be selected and deselected, but at most
                                                  //one item is selected at any time.
                                                  //Can set SingleSelectedListener and
                                                  //SingleDeselectedListener.
    public static final int MULTIPLE_SELECTION = 2; //Item can be selected and deselected.

    private int selectionMode;
    private List<String> texts; //reference to data
    private int singleSelectedPosition = -1; //used only in single-selection mode
    private List<Boolean> isPositionSelected; //used in all selection modes

    private SingleSelectionListener singleSelectionListener;
    private SingleDeselectionListener singleDeselectionListener;
    private NoSelectionOnClickListener noSelectionOnClickListener;
    private NoSelectionOnLongClickListener noSelectionOnLongClickListener;

    public TextListAdapter(List<String> textsRef, int selectionMode) {
        this.texts = textsRef;
        this.selectionMode = selectionMode;
        isPositionSelected = new ArrayList<>(Collections.nCopies(texts.size(), false));
    }

    // Do not call notifyItemInserted(...), notifyItemRemoved(...), notifyItemRangeChanged(...), or
    // notifyDataSetChanged(). Rather, call the following methods on data update.

    /** call this when items are appended */
    public void itemsAppended() {
        if (texts.size() <= isPositionSelected.size()) {
            throw new RuntimeException(String.format(
                    "texts.size() (%d) <= isPositionSelected.size() (%d)",
                    texts.size(), isPositionSelected.size()));
        }

        for (int p=isPositionSelected.size(); p<texts.size(); p++) {
            isPositionSelected.add(false);
            notifyItemInserted(p);
        }
    }

    /** call this when an item is removed */
    public void itemRemoved(int position) {
        if (texts.size() != isPositionSelected.size() - 1) {
            throw new AssertionError("texts.size() != isPositionSelected.size() - 1");
        }

        if (singleSelectedPosition == position)
            singleSelectedPosition = -1;
        isPositionSelected.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, texts.size() - position);
    }

    /** call this when all items are removed */
    public void dataCleared() {
        if (! texts.isEmpty())
            throw new RuntimeException("`texts` is not empty");
        singleSelectedPosition = -1;
        isPositionSelected.clear();
        notifyDataSetChanged();
    }

    public interface SingleSelectionListener {
        void onSingleSelection(String selectedText);
    }

    public interface SingleDeselectionListener {
        void onSingleDeselection(String DeselectedText);
    }

    public interface NoSelectionOnClickListener {
        void onClick(String clickedText);
    }

    public interface NoSelectionOnLongClickListener {
        void onLongClick(String clickedText);
    }

    /** set item selection/deselection listeners for single-selection mode */
    public void setSingleSelectedListener(SingleSelectionListener singleSelectionListener) {
        this.singleSelectionListener = singleSelectionListener;
    }
    public void setSingleDeselectedListener(SingleDeselectionListener singleDeselectionListener) {
        this.singleDeselectionListener = singleDeselectionListener;
    }

    /** set item click listener for no-selection mode */
    public void setNoSelectionOnClickListener(NoSelectionOnClickListener noSelectionOnClickListener) {
        this.noSelectionOnClickListener = noSelectionOnClickListener;
    }

    /** set item long-click listener for no-selection mode */
    public void setNoSelectionOnLongClickListener(
            NoSelectionOnLongClickListener noSelectionOnLongClickListener) {
        this.noSelectionOnLongClickListener = noSelectionOnLongClickListener;
    }

    /** get selected texts */
    public String[] getSelectedTexts() {
        int[] selectedPositions = getSelectedPositions();
        String[] selectedTexts = new String[selectedPositions.length];
        for (int i=0; i<selectedPositions.length; i++) {
            selectedTexts[i] = texts.get(selectedPositions[i]);
        }
        return selectedTexts;
    }

    /** get selected positions */
    public int[] getSelectedPositions() {
        int count = 0;
        for (boolean b: isPositionSelected) {
            if (b) {
                count++;
            }
        }

        int[] positions = new int[count];
        int i = 0;
        for (int p=0; p<isPositionSelected.size(); p++) {
            if (isPositionSelected.get(p)) {
                positions[i] = p;
                i++;
            }
        }
        return positions;
    }

    //
    @Override
    public int getItemCount() {
        return texts.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.text_list_item_oval, parent, false);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.textView.setText(texts.get(position));
        holder.textView.setSelected(isPositionSelected.get(position));
//        Log.v("onBindViewHolder",
//                String.format("### position=%d, text=%s", position, texts.get(position)));

        if (selectionMode == NO_SELECTION) {
            holder.textView.setSelected(false);
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (noSelectionOnClickListener != null) {
                        noSelectionOnClickListener.onClick(texts.get(position));
                    }
                }
            });
            holder.textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (noSelectionOnLongClickListener != null) {
                        noSelectionOnLongClickListener.onLongClick(texts.get(position));
                        return true;
                    }
                    return false;
                }
            });

        } else if (selectionMode == SINGLE_SELECTION) {
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (singleSelectedPosition == -1) {
                        singleSelectedPosition = position;
                        isPositionSelected.set(position, true);
                        v.setSelected(true);
                        if (singleSelectionListener != null) {
                            singleSelectionListener.onSingleSelection(texts.get(position));
                        }
                    } else if (singleSelectedPosition == position) {
                        singleSelectedPosition = -1;
                        isPositionSelected.set(position, false);
                        v.setSelected(false);
                        if (singleDeselectionListener != null) {
                            singleDeselectionListener.onSingleDeselection(texts.get(position));
                        }
                    } else {
                        int prevSelectedPosition = singleSelectedPosition;
                        singleSelectedPosition = position;
                        isPositionSelected.set(prevSelectedPosition, false);
                        isPositionSelected.set(singleSelectedPosition, true);
                        v.setSelected(true);
                        if (singleSelectionListener != null) {
                            singleSelectionListener.onSingleSelection(texts.get(position));
                        }
                        notifyItemChanged(prevSelectedPosition);
                    }
                }
            });
        } else if (selectionMode == MULTIPLE_SELECTION) {
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.isSelected() != isPositionSelected.get(position)) {
                        throw new AssertionError("selected position inconsistent");
                    }
                    v.setSelected(!v.isSelected());
                    isPositionSelected.set(position, v.isSelected());
                }
            });
        }
    }

    //
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public ViewHolder(TextView v) {
            super(v);
            this.textView = v;
        }
    }
}
