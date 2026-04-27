package com.example.CampusEventDiscovery.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VendorEventAdapter extends ListAdapter<Event, VendorEventAdapter.EventViewHolder> {

    public interface OnEventSelectedListener {
        void onEventSelected(Event event);
    }

    private final OnEventSelectedListener listener;
    private String selectedEventId;

    public VendorEventAdapter(List<Event> events, OnEventSelectedListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        submitList(events == null ? null : new java.util.ArrayList<>(events));
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_search_row, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = getItem(position);
        holder.tvTitle.setText(TextUtils.isEmpty(event.getTitle()) ? holder.itemView.getContext().getString(R.string.app_name) : event.getTitle());
        holder.tvDateTime.setText(formatDateTime(event.getDate()));
        holder.tvVenue.setText(TextUtils.isEmpty(event.getLocation()) ? holder.itemView.getContext().getString(R.string.placeholder_venue) : event.getLocation());
        holder.tvSpots.setText(holder.itemView.getContext().getString(R.string.spots_ratio, event.getRsvpCount(), event.getCapacity()));
        if (holder.ivHeart != null) {
            holder.ivHeart.setVisibility(View.GONE);
        }
        if (holder.ivVerified != null) {
            holder.ivVerified.setVisibility(event.isVerified() ? View.VISIBLE : View.GONE);
        }
        if (!TextUtils.isEmpty(event.getThumbnailUrl())) {
            holder.ivPlaceholder.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(event.getThumbnailUrl())
                    .placeholder(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(0);
            holder.ivThumbnail.setBackgroundResource(R.drawable.bg_placeholder_image);
            holder.ivPlaceholder.setVisibility(View.VISIBLE);
        }
        holder.card.setChecked(event.getEventId() != null && event.getEventId().equals(selectedEventId));
        holder.itemView.setOnClickListener(v -> {
            String previousSelectedEventId = selectedEventId;
            selectedEventId = event.getEventId();
            refreshSelectedState(previousSelectedEventId, selectedEventId);
            if (listener != null) {
                listener.onEventSelected(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    public void updateData(List<Event> newEvents) {
        submitList(newEvents == null ? null : new java.util.ArrayList<>(newEvents));
    }

    public void setSelectedEventId(String selectedEventId) {
        String previousSelectedEventId = this.selectedEventId;
        this.selectedEventId = selectedEventId;
        refreshSelectedState(previousSelectedEventId, selectedEventId);
    }

    private void refreshSelectedState(String previousSelectedEventId, String newSelectedEventId) {
        int previousPosition = findPosition(previousSelectedEventId);
        int newPosition = findPosition(newSelectedEventId);
        if (previousPosition >= 0) {
            notifyItemChanged(previousPosition);
        }
        if (newPosition >= 0 && newPosition != previousPosition) {
            notifyItemChanged(newPosition);
        }
    }

    private int findPosition(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < getItemCount(); i++) {
            Event event = getItem(i);
            if (event != null && TextUtils.equals(eventId, event.getEventId())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        Date date = timestamp.toDate();
        return new SimpleDateFormat("EEE, dd MMM - hh:mm a", Locale.getDefault()).format(date);
    }

    private static final DiffUtil.ItemCallback<Event> DIFF_CALLBACK = new DiffUtil.ItemCallback<Event>() {
        @Override
        public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return TextUtils.equals(oldItem.getEventId(), newItem.getEventId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return TextUtils.equals(oldItem.getTitle(), newItem.getTitle())
                    && eventDateMillis(oldItem.getDate()) == eventDateMillis(newItem.getDate())
                    && TextUtils.equals(oldItem.getLocation(), newItem.getLocation())
                    && oldItem.getRsvpCount() == newItem.getRsvpCount()
                    && oldItem.getCapacity() == newItem.getCapacity()
                    && TextUtils.equals(oldItem.getThumbnailUrl(), newItem.getThumbnailUrl())
                    && oldItem.isVerified() == newItem.isVerified();
        }
    };

    private static long eventDateMillis(Timestamp timestamp) {
        return timestamp == null ? Long.MIN_VALUE : timestamp.toDate().getTime();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivThumbnail;
        ImageView ivPlaceholder;
        ImageView ivVerified;
        ImageView ivHeart;
        TextView tvTitle;
        TextView tvDateTime;
        TextView tvVenue;
        TextView tvSpots;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivPlaceholder = itemView.findViewById(R.id.ivPlaceholderIcon);
            ivVerified = itemView.findViewById(R.id.ivVerified);
            ivHeart = itemView.findViewById(R.id.ivHeart);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvSpots = itemView.findViewById(R.id.tvSpots);
        }
    }
}
