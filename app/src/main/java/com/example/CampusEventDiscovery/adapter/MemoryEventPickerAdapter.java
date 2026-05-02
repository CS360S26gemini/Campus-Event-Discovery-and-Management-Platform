package com.example.CampusEventDiscovery.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MemoryEventPickerAdapter extends RecyclerView.Adapter<MemoryEventPickerAdapter.EventViewHolder> {

    public interface OnEventSelectedListener {
        void onEventSelected(Event event);
    }

    private final List<Event> events;
    private final OnEventSelectedListener listener;

    public MemoryEventPickerAdapter(List<Event> events, OnEventSelectedListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_search_row, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvTitle.setText(safeText(event.getTitle(), holder.itemView.getContext().getString(R.string.memory_album_title)));
        holder.tvDateTime.setText(formatDate(event.getDate(), holder.itemView.getContext().getString(R.string.placeholder_date)));
        holder.tvOrganizer.setVisibility(View.GONE);
        holder.tvVenue.setText(safeText(event.getLocation(), holder.itemView.getContext().getString(R.string.placeholder_venue)));
        holder.tvSpots.setText(safeText(event.getCategory(), holder.itemView.getContext().getString(R.string.memory_registered_event)));
        holder.ivHeart.setVisibility(View.GONE);
        holder.ivVerified.setVisibility(View.GONE);

        String imageUrl = event.getThumbnailUrl();
        if (TextUtils.isEmpty(imageUrl)) {
            holder.ivThumbnail.setImageDrawable(null);
            holder.ivThumbnail.setBackgroundResource(R.drawable.bg_placeholder_image);
            holder.ivPlaceholderIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivThumbnail.setBackgroundResource(0);
            holder.ivPlaceholderIcon.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_placeholder_image)
                    .error(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(holder.ivThumbnail);
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventSelected(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String formatDate(Timestamp timestamp, String fallback) {
        if (timestamp == null) {
            return fallback;
        }
        return new SimpleDateFormat("EEE, dd MMM - hh:mm a", Locale.getDefault())
                .format(timestamp.toDate());
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final ImageView ivThumbnail;
        final ImageView ivPlaceholderIcon;
        final ImageView ivVerified;
        final ImageView ivHeart;
        final TextView tvTitle;
        final TextView tvDateTime;
        final TextView tvOrganizer;
        final TextView tvVenue;
        final TextView tvSpots;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = (MaterialCardView) itemView;
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivPlaceholderIcon = itemView.findViewById(R.id.ivPlaceholderIcon);
            ivVerified = itemView.findViewById(R.id.ivVerified);
            ivHeart = itemView.findViewById(R.id.ivHeart);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvSpots = itemView.findViewById(R.id.tvSpots);
        }
    }
}
