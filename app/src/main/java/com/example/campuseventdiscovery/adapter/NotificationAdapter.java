package com.example.campuseventdiscovery.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.model.Notification;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for in-app notifications.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final OnNotificationClickListener listener;
    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.tvTitle.setText(safeText(notification.getTitle(), holder.itemView.getContext().getString(R.string.notifications_title)));
        holder.tvBody.setText(safeText(notification.getBody(), ""));
        holder.tvMeta.setText(formatTimestamp(notification.getCreatedAt()));
        holder.tvUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        holder.cardNotification.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public void updateData(List<Notification> newNotifications) {
        notifications = newNotifications;
        notifyDataSetChanged();
    }

    private String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(timestamp.toDate());
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardNotification;
        final TextView tvTitle;
        final TextView tvBody;
        final TextView tvMeta;
        final TextView tvUnread;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvMeta = itemView.findViewById(R.id.tvNotificationMeta);
            tvUnread = itemView.findViewById(R.id.tvUnread);
        }
    }
}
