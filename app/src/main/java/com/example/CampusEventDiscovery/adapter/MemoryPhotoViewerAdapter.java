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

public class MemoryPhotoViewerAdapter extends RecyclerView.Adapter<MemoryPhotoViewerAdapter.PhotoViewHolder> {

    private List<String> photoUrls;

    public MemoryPhotoViewerAdapter(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory_photo_viewer, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String url = photoUrls.get(position);
        if (TextUtils.isEmpty(url)) {
            holder.ivPhoto.setImageResource(R.drawable.bg_placeholder_image);
            return;
        }

        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(R.drawable.bg_placeholder_image)
                .fitCenter()
                .into(holder.ivPhoto);
    }

    @Override
    public int getItemCount() {
        return photoUrls == null ? 0 : photoUrls.size();
    }

    public void updateData(List<String> newPhotoUrls) {
        photoUrls = newPhotoUrls;
        notifyDataSetChanged();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivMemoryViewerPhoto);
        }
    }
}
