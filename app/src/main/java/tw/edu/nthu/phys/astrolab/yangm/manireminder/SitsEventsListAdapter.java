package tw.edu.nthu.phys.astrolab.yangm.manireminder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class SitsEventsListAdapter extends RecyclerView.Adapter<SitsEventsListAdapter.ViewHolder> {

    List<SitEventInfo> sitEventsData;

    public static class SitEventInfo {
        private boolean isSituation;
        private String name;
        private int remsCount;

        public SitEventInfo(boolean isSituation, String name, int remsCount) {
            this.isSituation = isSituation;
            this.name = name;
            this.remsCount = remsCount;
        }
    }

    public SitsEventsListAdapter(List<SitEventInfo> sitEventsData) {
        this.sitEventsData = sitEventsData;
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
