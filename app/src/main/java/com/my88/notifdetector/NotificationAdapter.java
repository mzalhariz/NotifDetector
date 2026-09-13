package com.my88.notifdetector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> items = new ArrayList<>();

    public void setItems(List<NotificationItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public void addItem(NotificationItem item) {
        items.add(0, item);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.tvAppName.setText(item.getAppName());
        holder.tvTitle.setText(item.getTitle());
        holder.tvText.setText(item.getText());
        holder.tvTimestamp.setText(item.getTimestamp());

        if (item.isMatched()) {
            holder.matchIndicator.setVisibility(View.VISIBLE);
            holder.tvMatchBadge.setVisibility(View.VISIBLE);
        } else {
            holder.matchIndicator.setVisibility(View.GONE);
            holder.tvMatchBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName, tvTitle, tvText, tvTimestamp, tvMatchBadge;
        View matchIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvText = itemView.findViewById(R.id.tvText);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvMatchBadge = itemView.findViewById(R.id.tvMatchBadge);
            matchIndicator = itemView.findViewById(R.id.matchIndicator);
        }
    }
}
