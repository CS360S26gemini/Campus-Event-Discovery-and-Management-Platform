package com.example.CampusEventDiscovery.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MemoryPhotoGridAdapter extends RecyclerView.Adapter<MemoryPhotoGridAdapter.PhotoViewHolder> {

    public interface OnPhotoSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public interface OnPhotoOpenListener {
        void onOpenPhoto(int position);
    }

    private List<String> photoUrls;
    private final OnPhotoOpenListener openListener;
    private final OnPhotoSelectionChangedListener selectionChangedListener;
    private final Set<String> selectedPhotoUrls = new LinkedHashSet<>();
    private boolean selectionMode;

    public MemoryPhotoGridAdapter(List<String> photoUrls) {
        this(photoUrls, null, null);
    }

    public MemoryPhotoGridAdapter(List<String> photoUrls, OnPhotoSelectionChangedListener selectionChangedListener) {
        this(photoUrls, null, selectionChangedListener);
    }

    public MemoryPhotoGridAdapter(List<String> photoUrls,
                                  OnPhotoOpenListener openListener,
                                  OnPhotoSelectionChangedListener selectionChangedListener) {
        this.photoUrls = photoUrls;
        this.openListener = openListener;
        this.selectionChangedListener = selectionChangedListener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory_album_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String url = photoUrls.get(position);
        if (TextUtils.isEmpty(url)) {
            holder.ivPhoto.setImageResource(R.drawable.bg_placeholder_image);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.bg_placeholder_image)
                    .error(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(holder.ivPhoto);
        }

        boolean isSelected = selectedPhotoUrls.contains(url);
        holder.viewSelectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.ivSelectionCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.itemView.setActivated(isSelected);

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (selectionMode) {
                toggleSelection(photoUrls.get(adapterPosition));
                return;
            }

            if (openListener != null) {
                openListener.onOpenPhoto(adapterPosition);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return false;
            }
            if (!selectionMode) {
                selectionMode = true;
            }
            toggleSelection(photoUrls.get(adapterPosition));
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return photoUrls == null ? 0 : photoUrls.size();
    }

    public void updateData(List<String> newPhotoUrls) {
        photoUrls = newPhotoUrls;
        selectedPhotoUrls.retainAll(newPhotoUrls);
        dispatchSelectionChanged();
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!enabled) {
            selectedPhotoUrls.clear();
            dispatchSelectionChanged();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public List<String> getSelectedPhotoUrls() {
        return new java.util.ArrayList<>(selectedPhotoUrls);
    }

    private void toggleSelection(String photoUrl) {
        if (TextUtils.isEmpty(photoUrl)) {
            return;
        }

        if (selectedPhotoUrls.contains(photoUrl)) {
            selectedPhotoUrls.remove(photoUrl);
        } else {
            selectedPhotoUrls.add(photoUrl);
        }

        if (selectedPhotoUrls.isEmpty()) {
            selectionMode = false;
        }

        dispatchSelectionChanged();
        notifyDataSetChanged();
    }

    private void dispatchSelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedPhotoUrls.size());
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        final View viewSelectionOverlay;
        final ImageView ivSelectionCheck;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivMemoryAlbumPhoto);
            viewSelectionOverlay = itemView.findViewById(R.id.viewMemoryPhotoSelectionOverlay);
            ivSelectionCheck = itemView.findViewById(R.id.ivMemoryPhotoSelectionCheck);
        }
    }
}
