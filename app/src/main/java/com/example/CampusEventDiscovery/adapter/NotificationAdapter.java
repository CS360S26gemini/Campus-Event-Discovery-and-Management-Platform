package com.example.CampusEventDiscovery.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Notification;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for in-app notifications.
 */
public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        submitList(notifications == null ? null : new java.util.ArrayList<>(notifications));
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = getItem(position);
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
        return super.getItemCount();
    }

    public void updateData(List<Notification> newNotifications) {
        submitList(newNotifications == null ? null : new java.util.ArrayList<>(newNotifications));
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

    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return TextUtils.equals(oldItem.getNotificationId(), newItem.getNotificationId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return TextUtils.equals(oldItem.getTitle(), newItem.getTitle())
                    && TextUtils.equals(oldItem.getBody(), newItem.getBody())
                    && TextUtils.equals(oldItem.getType(), newItem.getType())
                    && TextUtils.equals(oldItem.getEventId(), newItem.getEventId())
                    && oldItem.isRead() == newItem.isRead()
                    && timestampMillis(oldItem.getCreatedAt()) == timestampMillis(newItem.getCreatedAt());
        }
    };

    private static long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? Long.MIN_VALUE : timestamp.toDate().getTime();
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
