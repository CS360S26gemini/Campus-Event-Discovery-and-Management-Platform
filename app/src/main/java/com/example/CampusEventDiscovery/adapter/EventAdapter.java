package com.example.CampusEventDiscovery.adapter;

import android.content.Intent;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * EventAdapter.java
 *
 * RecyclerView adapter used to render standard event cards from Firestore data.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onItemClick(Event event);
        void onHeartClick(Event event, boolean isCurrentlySaved);
        void onItemLongClick(Event event);
    }

    private List<Event> events;
    private Set<String> savedEventIds;
    private final String currentUserId;
    private final OnEventClickListener listener;
    private final boolean useCompactRows;

    public EventAdapter(List<Event> events,
                        Set<String> savedEventIds,
                        String currentUserId,
                        OnEventClickListener listener) {
        this(events, savedEventIds, currentUserId, listener, false);
    }

    public EventAdapter(List<Event> events,
                        Set<String> savedEventIds,
                        String currentUserId,
                        OnEventClickListener listener,
                        boolean useCompactRows) {
        this.events = events;
        this.savedEventIds = savedEventIds != null ? savedEventIds : new HashSet<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.useCompactRows = useCompactRows;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = useCompactRows ? R.layout.item_event_search_row : R.layout.item_event_card;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds an Event object into a reusable event card layout.
     *
     * @param holder ViewHolder being bound.
     * @param position Adapter position.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvTitle.setText(safeText(event.getTitle(), holder.itemView.getContext().getString(R.string.app_name)));
        holder.tvDateTime.setText(formatDateTime(event.getDate(), holder.itemView.getContext()));
        holder.tvVenue.setText(safeText(event.getLocation(),
                holder.itemView.getContext().getString(R.string.placeholder_venue)));
        if (holder.chipCategory != null) {
            String category = event.getCategory();
            holder.chipCategory.setText(category);
            holder.chipCategory.setVisibility(TextUtils.isEmpty(category) ? View.GONE : View.VISIBLE);
        }

        long rsvpCount = event.getRsvpCount();
        long capacity = event.getCapacity();
        holder.tvSpots.setText(holder.itemView.getContext().getString(
                R.string.spots_ratio,
                rsvpCount,
                capacity
        ));
        bindStatusChip(holder, event, rsvpCount, capacity);

        if (holder.tvNewBadge != null) {
            holder.tvNewBadge.setVisibility(View.GONE);
        }

        if (holder.ivVerified != null) {
            holder.ivVerified.setVisibility(event.isVerified() ? View.VISIBLE : View.GONE);
        }

        boolean isSaved = event.getEventId() != null && savedEventIds.contains(event.getEventId());
        if (holder.ivHeart != null) {
            holder.ivHeart.setImageResource(isSaved ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            holder.ivHeart.setVisibility(View.GONE);
        }

        String imageUrl = event.getThumbnailUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            if (holder.ivPlaceholderIcon != null) holder.ivPlaceholderIcon.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(0);
            holder.ivThumbnail.setBackgroundResource(R.drawable.bg_placeholder_image);
            if (holder.ivPlaceholderIcon != null) holder.ivPlaceholderIcon.setVisibility(View.VISIBLE);
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(event);
            }
        });

        holder.cardRoot.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(event);
                return true;
            }
            return false;
        });

        if (holder.ivHeart != null) {
            holder.ivHeart.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(currentUserId) && listener != null) {
                    listener.onHeartClick(event, isSaved);
                }
            });
        }

        if (holder.ivShare != null) {
            holder.ivShare.setOnClickListener(v -> {
                String shareText = buildShareText(event, v.getContext());
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                v.getContext().startActivity(Intent.createChooser(
                        shareIntent,
                        v.getContext().getString(R.string.share)
                ));
            });
        }
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public void updateSavedIds(Set<String> ids) {
        this.savedEventIds = ids != null ? ids : new HashSet<>();
        notifyDataSetChanged();
    }

    public void updateData(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    private boolean isRecentEvent(Timestamp createdAt) {
        if (createdAt == null) {
            return false;
        }

        long createdMillis = createdAt.toDate().getTime();
        long nowMillis = System.currentTimeMillis();
        long threeDaysInMillis = 3L * 24L * 60L * 60L * 1000L;

        return (nowMillis - createdMillis) <= threeDaysInMillis;
    }

    private void bindStatusChip(EventViewHolder holder, Event event, long rsvpCount, long capacity) {
        if (holder.chipStatus == null) {
            return;
        }

        String status = event.getStatus() == null ? "" : event.getStatus().trim().toLowerCase(Locale.getDefault());
        if (!TextUtils.isEmpty(status) && !"active".equals(status)) {
            holder.chipStatus.setText(holder.itemView.getContext().getString(R.string.status_closed));
            holder.chipStatus.setChipBackgroundColorResource(R.color.colorSurfaceVariant);
            holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnSurfaceVariant));
            return;
        }

        if (capacity > 0 && rsvpCount >= capacity) {
            holder.chipStatus.setText(holder.itemView.getContext().getString(R.string.status_full));
            holder.chipStatus.setChipBackgroundColorResource(R.color.colorErrorContainer);
            holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnErrorContainer));
            return;
        }

        holder.chipStatus.setText(holder.itemView.getContext().getString(R.string.status_open));
        holder.chipStatus.setChipBackgroundColorResource(R.color.colorPrimaryContainer);
        holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnPrimaryContainer));
    }

    private String formatDateTime(Timestamp timestamp, Context context) {
        if (timestamp == null) {
            return context.getString(R.string.placeholder_date);
        }

        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    private String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }

    private String buildShareText(Event event, Context context) {
        String title = safeText(event.getTitle(), "Campus Event");
        String location = safeText(event.getLocation(), "Venue TBD");
        String date = formatDateTime(event.getDate(), context);

        return title + "\n" + date + "\n" + location;
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardRoot;
        ImageView ivThumbnail;
        ImageView ivPlaceholderIcon;
        ImageView ivVerified;
        ImageView ivHeart;
        ImageView ivShare;
        TextView tvTitle;
        TextView tvDateTime;
        TextView tvVenue;
        TextView tvSpots;
        TextView tvNewBadge;
        Chip chipCategory;
        Chip chipStatus;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            cardRoot = (MaterialCardView) itemView;
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivPlaceholderIcon = itemView.findViewById(R.id.ivPlaceholderIcon);
            ivVerified = itemView.findViewById(R.id.ivVerified);
            ivHeart = itemView.findViewById(R.id.ivHeart);
            ivShare = itemView.findViewById(R.id.ivShare);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvSpots = itemView.findViewById(R.id.tvSpots);
            tvNewBadge = itemView.findViewById(R.id.tvNewBadge);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }
}
