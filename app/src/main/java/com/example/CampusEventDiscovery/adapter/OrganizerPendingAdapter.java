package com.example.CampusEventDiscovery.adapter;

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
import java.util.List;
import java.util.Locale;

/**
 * OrganizerPendingAdapter.java
 *
 * Adapter for showing organizer pending proposals using the same event card
 * layout but hiding heart/share and showing a PENDING badge.
 */
public class OrganizerPendingAdapter extends RecyclerView.Adapter<OrganizerPendingAdapter.PendingViewHolder> {

    public interface OnPendingClickListener {
        void onItemClick(Event event);
    }

    private List<Event> events;
    private final OnPendingClickListener listener;

    public OrganizerPendingAdapter(List<Event> events, OnPendingClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new PendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvTitle.setText(safeText(event.getTitle(), holder.itemView.getContext().getString(R.string.app_name)));
        holder.tvDateTime.setText(formatDateTime(event.getDate(), holder.itemView));
        holder.tvVenue.setText(safeText(event.getLocation(),
                holder.itemView.getContext().getString(R.string.placeholder_venue)));
        if (holder.chipCategory != null) {
            String category = event.getCategory();
            holder.chipCategory.setText(category);
            holder.chipCategory.setVisibility(TextUtils.isEmpty(category) ? View.GONE : View.VISIBLE);
        }

        long capacity = event.getCapacity();
        holder.tvSpots.setText(holder.itemView.getContext().getString(
                R.string.spots_ratio,
                0,
                capacity
        ));
        bindProposalStatus(holder, event);

        if (holder.ivVerified != null) holder.ivVerified.setVisibility(View.GONE);
        if (holder.ivHeart != null) holder.ivHeart.setVisibility(View.GONE);
        if (holder.ivShare != null) holder.ivShare.setVisibility(View.GONE);
        if (holder.chipStatus != null) holder.chipStatus.setVisibility(View.GONE);

        if (holder.tvNewBadge != null) {
            holder.tvNewBadge.setVisibility(View.GONE);
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
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public void updateData(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    private String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }

    private String formatDateTime(Timestamp timestamp, View view) {
        if (timestamp == null) {
            return view.getContext().getString(R.string.placeholder_date);
        }

        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    private String resolveStatusBadge(View itemView, Event event) {
        String status = event == null || event.getStatus() == null
                ? ""
                : event.getStatus().trim().toLowerCase(Locale.getDefault());

        if ("rejected".equals(status)) {
            return itemView.getContext().getString(R.string.rejected_badge);
        }

        if ("approved".equals(status)) {
            return itemView.getContext().getString(R.string.approved_badge);
        }

        return itemView.getContext().getString(R.string.pending_badge);
    }

    private void bindProposalStatus(PendingViewHolder holder, Event event) {
        Chip statusChip = holder.chipTitleStatus != null ? holder.chipTitleStatus : holder.chipStatus;
        if (statusChip == null) {
            return;
        }

        String statusText = resolveStatusBadge(holder.itemView, event);
        statusChip.setText(statusText);
        statusChip.setVisibility(View.VISIBLE);

        String status = event == null || event.getStatus() == null
                ? ""
                : event.getStatus().trim().toLowerCase(Locale.getDefault());

        if ("rejected".equals(status)) {
            statusChip.setChipBackgroundColorResource(R.color.colorErrorContainer);
            statusChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnErrorContainer));
        } else if ("approved".equals(status)) {
            statusChip.setChipBackgroundColorResource(R.color.colorPrimaryContainer);
            statusChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnPrimaryContainer));
        } else {
            statusChip.setChipBackgroundColorResource(R.color.colorSecondaryContainer);
            statusChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnSecondaryContainer));
        }
    }

    public static class PendingViewHolder extends RecyclerView.ViewHolder {

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
        Chip chipTitleStatus;

        public PendingViewHolder(@NonNull View itemView) {
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
            chipTitleStatus = itemView.findViewById(R.id.chipTitleStatus);
        }
    }
}
