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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VendorEventAdapter extends RecyclerView.Adapter<VendorEventAdapter.EventViewHolder> {

    public interface OnEventSelectedListener {
        void onEventSelected(Event event);
    }

    private List<Event> events;
    private final OnEventSelectedListener listener;
    private String selectedEventId;

    public VendorEventAdapter(List<Event> events, OnEventSelectedListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_search_row, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
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
            selectedEventId = event.getEventId();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onEventSelected(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }

    public void updateData(List<Event> newEvents) {
        events = newEvents;
        notifyDataSetChanged();
    }

    public void setSelectedEventId(String selectedEventId) {
        this.selectedEventId = selectedEventId;
        notifyDataSetChanged();
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        Date date = timestamp.toDate();
        return new SimpleDateFormat("EEE, dd MMM - hh:mm a", Locale.getDefault()).format(date);
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
