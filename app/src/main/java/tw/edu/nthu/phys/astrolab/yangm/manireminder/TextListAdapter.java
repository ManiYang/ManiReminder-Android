package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class TextListAdapter
        extends RecyclerView.Adapter<TextListAdapter.ViewHolder> {

    public static final int NO_SELECTION = 0; //Can set NoSelectionOnClickListener.
    public static final int SINGLE_SELECTION = 1; //Item can be selected and deselected, but at most
                                                  //one item is selected at any time.
                                                  //Can set SingleSelectedListener.
    public static final int MULTIPLE_SELECTION = 2; //Item can be selected and deselected.
                                                    //Can use getMultipleSelectedTexts().

    private int selectionMode;
    private String[] texts;
    private SingleSelectionListener singleSelectionListener;
    private NoSelectionOnClickListener noSelectionOnClickListener;

    public TextListAdapter(String[] texts, int selectionMode) {
        this.texts = texts;
        this.selectionMode = selectionMode;
        isPositionSelected = new boolean[texts.length];
    }

    public interface SingleSelectionListener {
        void onSingleSelection(String selectedText);
    }

    public interface NoSelectionOnClickListener {
        void onClick(String clickedText);
    }

    /** set item selection listener for single-selection mode */
    public void setSingleSelectedListener(SingleSelectionListener singleSelectionListener) {
        this.singleSelectionListener = singleSelectionListener;
    }

    /** set item click listener for no-selection mode */
    public void setNoSelectionOnClickListener(NoSelectionOnClickListener noSelectionOnClickListener) {
        this.noSelectionOnClickListener = noSelectionOnClickListener;
    }

    /** get selected texts for multiple-selection mode */
    public ArrayList<String> getMultipleSelectedTexts() {
        ArrayList<String> list = new ArrayList<>();
        for (int p=0; p<texts.length; p++) {
            if (isPositionSelected[p]) {
                list.add(texts[p]);
            }
        }
        return list;
    }

    //
    @Override
    public int getItemCount() {
        return texts.length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.text_list_item, parent, false);
        return new ViewHolder(textView);
    }

    private int singleSelectedPosition = -1;
    private boolean[] isPositionSelected;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.textView.setText(texts[position]);
        holder.textView.setSelected(false);

        if (selectionMode == NO_SELECTION) {
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (noSelectionOnClickListener != null) {
                        noSelectionOnClickListener.onClick(texts[position]);
                    }
                }
            });
        } else if (selectionMode == SINGLE_SELECTION) {
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (singleSelectedPosition == -1) {
                        singleSelectedPosition = position;
                        v.setSelected(true);
                        if (singleSelectionListener != null) {
                            singleSelectionListener.onSingleSelection(texts[position]);
                        }
                    } else if (singleSelectedPosition == position) {
                        singleSelectedPosition = -1;
                        v.setSelected(false);
                    } else {
                        int prevSelectedPosition = singleSelectedPosition;
                        singleSelectedPosition = position;
                        v.setSelected(true);
                        if (singleSelectionListener != null) {
                            singleSelectionListener.onSingleSelection(texts[position]);
                        }
                        notifyItemChanged(prevSelectedPosition);
                    }
                }
            });
        } else if (selectionMode == MULTIPLE_SELECTION) {
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.isSelected() != isPositionSelected[position]) {
                        throw new AssertionError("selected position inconsistent");
                    }
                    v.setSelected(!v.isSelected());
                    isPositionSelected[position] = v.isSelected();
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
