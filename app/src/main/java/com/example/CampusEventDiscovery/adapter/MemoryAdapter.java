package com.example.CampusEventDiscovery.adapter;

import android.content.res.ColorStateList;
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
import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for attendee memories.
 */
public class MemoryAdapter extends ListAdapter<Memory, MemoryAdapter.MemoryViewHolder> {

    private final OnMemoryActionListener listener;

    public interface OnMemoryActionListener {
        void onOpenAlbum(Memory memory);
        void onAddPhotos(Memory memory);
        void onDeleteMemory(Memory memory);
    }

    public MemoryAdapter(List<Memory> memories, OnMemoryActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        submitList(memories == null ? null : new java.util.ArrayList<>(memories));
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory, parent, false);
        return new MemoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        Memory memory = getItem(position);
        holder.tvTitle.setText(safeText(memory.getEventTitle(), holder.itemView.getContext().getString(R.string.app_name)));
        holder.tvDate.setText(formatTimestamp(memory.getAttendedAt()));
        if (memory.getRating() > 0) {
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText(holder.itemView.getContext().getString(R.string.memory_rating, memory.getRating()));
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }
        int photoCount = memory.getPhotoUrls() == null ? 0 : memory.getPhotoUrls().size();
        holder.tvPhotoCount.setText(holder.itemView.getContext().getString(R.string.memory_photo_count, photoCount));
        holder.btnAddPhotos.setText(photoCount > 0
                ? holder.itemView.getContext().getString(R.string.add_more_photos)
                : holder.itemView.getContext().getString(R.string.add_album_photos));

        String coverUrl = photoCount > 0 ? memory.getPhotoUrls().get(0) : null;
        if (!TextUtils.isEmpty(coverUrl)) {
            holder.ivMemoryCover.setPadding(0, 0, 0, 0);
            holder.ivMemoryCover.setImageTintList(null);
            Glide.with(holder.itemView.getContext())
                    .load(coverUrl)
                    .placeholder(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(holder.ivMemoryCover);
        } else {
            holder.ivMemoryCover.setImageResource(R.drawable.ic_folder);
            holder.ivMemoryCover.setBackgroundResource(R.drawable.bg_placeholder_image);
            holder.ivMemoryCover.setImageTintList(ColorStateList.valueOf(
                    ThemeManager.getAccentColor(holder.itemView.getContext())
            ));
            holder.ivMemoryCover.setPadding(18, 18, 18, 18);
        }

        holder.cardMemory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenAlbum(memory);
            }
        });
        holder.btnAddPhotos.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddPhotos(memory);
            }
        });
        holder.btnViewPhotos.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenAlbum(memory);
            }
        });
        holder.btnDeleteMemory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteMemory(memory);
            }
        });
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    public void updateData(List<Memory> newMemories) {
        submitList(newMemories == null ? null : new java.util.ArrayList<>(newMemories));
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

    private static final DiffUtil.ItemCallback<Memory> DIFF_CALLBACK = new DiffUtil.ItemCallback<Memory>() {
        @Override
        public boolean areItemsTheSame(@NonNull Memory oldItem, @NonNull Memory newItem) {
            return TextUtils.equals(oldItem.getEventId(), newItem.getEventId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Memory oldItem, @NonNull Memory newItem) {
            int oldCount = oldItem.getPhotoUrls() == null ? 0 : oldItem.getPhotoUrls().size();
            int newCount = newItem.getPhotoUrls() == null ? 0 : newItem.getPhotoUrls().size();
            return TextUtils.equals(oldItem.getEventId(), newItem.getEventId())
                    && TextUtils.equals(oldItem.getEventTitle(), newItem.getEventTitle())
                    && oldItem.getRating() == newItem.getRating()
                    && oldCount == newCount
                    && timestampMillis(oldItem.getAttendedAt()) == timestampMillis(newItem.getAttendedAt());
        }
    };

    private static long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? Long.MIN_VALUE : timestamp.toDate().getTime();
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardMemory;
        final ImageView ivMemoryCover;
        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvRating;
        final TextView tvPhotoCount;
        final MaterialButton btnViewPhotos;
        final MaterialButton btnAddPhotos;
        final MaterialButton btnDeleteMemory;

        MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMemory = itemView.findViewById(R.id.cardMemory);
            ivMemoryCover = itemView.findViewById(R.id.ivMemoryCover);
            tvTitle = itemView.findViewById(R.id.tvMemoryTitle);
            tvDate = itemView.findViewById(R.id.tvMemoryDate);
            tvRating = itemView.findViewById(R.id.tvMemoryRating);
            tvPhotoCount = itemView.findViewById(R.id.tvMemoryPhotoCount);
            btnViewPhotos = itemView.findViewById(R.id.btnViewMemoryPhotos);
            btnAddPhotos = itemView.findViewById(R.id.btnAddMemoryPhotos);
            btnDeleteMemory = itemView.findViewById(R.id.btnDeleteMemory);
        }
    }
}
