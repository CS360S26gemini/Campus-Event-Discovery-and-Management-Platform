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

import java.util.List;

public class MemoryPhotoGridAdapter extends RecyclerView.Adapter<MemoryPhotoGridAdapter.PhotoViewHolder> {

    public interface OnPhotoActionListener {
        void onDeletePhoto(String photoUrl);
    }

    private final List<String> photoUrls;
    private final OnPhotoActionListener listener;

    public MemoryPhotoGridAdapter(List<String> photoUrls) {
        this(photoUrls, null);
    }

    public MemoryPhotoGridAdapter(List<String> photoUrls, OnPhotoActionListener listener) {
        this.photoUrls = photoUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory_album_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.itemView.post(() -> {
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = holder.itemView.getWidth();
            holder.itemView.setLayoutParams(params);
        });

        String url = photoUrls.get(position);
        if (TextUtils.isEmpty(url)) {
            holder.ivPhoto.setImageResource(R.drawable.bg_placeholder_image);
            return;
        }

        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(R.drawable.bg_placeholder_image)
                .centerCrop()
                .into(holder.ivPhoto);

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeletePhoto(url);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return photoUrls == null ? 0 : photoUrls.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivMemoryAlbumPhoto);
        }
    }
}
