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
import com.example.CampusEventDiscovery.model.Memory;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for attendee memories.
 */
public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {

    private List<Memory> memories;

    public MemoryAdapter(List<Memory> memories) {
        this.memories = memories;
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory, parent, false);
        return new MemoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        Memory memory = memories.get(position);
        holder.tvTitle.setText(safeText(memory.getEventTitle(), holder.itemView.getContext().getString(R.string.app_name)));
        holder.tvDate.setText(formatTimestamp(memory.getAttendedAt()));
        holder.tvRating.setText(holder.itemView.getContext().getString(R.string.memory_rating, memory.getRating()));
        int photoCount = memory.getPhotoUrls() == null ? 0 : memory.getPhotoUrls().size();
        holder.tvPhotoCount.setText(holder.itemView.getContext().getString(R.string.memory_photo_count, photoCount));

        String coverUrl = photoCount > 0 ? memory.getPhotoUrls().get(0) : null;
        if (!TextUtils.isEmpty(coverUrl)) {
            Glide.with(holder.itemView.getContext())
                    .load(coverUrl)
                    .placeholder(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(holder.ivMemoryCover);
        } else {
            holder.ivMemoryCover.setImageResource(0);
            holder.ivMemoryCover.setBackgroundResource(R.drawable.bg_placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return memories != null ? memories.size() : 0;
    }

    public void updateData(List<Memory> newMemories) {
        memories = newMemories;
        notifyDataSetChanged();
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(timestamp.toDate());
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardMemory;
        final ImageView ivMemoryCover;
        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvRating;
        final TextView tvPhotoCount;

        MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMemory = itemView.findViewById(R.id.cardMemory);
            ivMemoryCover = itemView.findViewById(R.id.ivMemoryCover);
            tvTitle = itemView.findViewById(R.id.tvMemoryTitle);
            tvDate = itemView.findViewById(R.id.tvMemoryDate);
            tvRating = itemView.findViewById(R.id.tvMemoryRating);
            tvPhotoCount = itemView.findViewById(R.id.tvMemoryPhotoCount);
        }
    }
}
