package com.example.CampusEventDiscovery.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.MemoryPhotoGridAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class MemoryAlbumActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_TITLE = "eventTitle";
    public static final String EXTRA_PHOTO_URLS = "photoUrls";

    private MaterialToolbar toolbarMemoryAlbum;
    private RecyclerView rvMemoryAlbumPhotos;
    private TextView tvEmptyMemoryAlbum;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_album);

        toolbarMemoryAlbum = findViewById(R.id.toolbarMemoryAlbum);
        rvMemoryAlbumPhotos = findViewById(R.id.rvMemoryAlbumPhotos);
        tvEmptyMemoryAlbum = findViewById(R.id.tvEmptyMemoryAlbum);

        toolbarMemoryAlbum.setNavigationOnClickListener(v -> finish());

        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        toolbarMemoryAlbum.setTitle(TextUtils.isEmpty(eventTitle) ? getString(R.string.memory_album_title) : eventTitle);

        ArrayList<String> photoUrls = getIntent().getStringArrayListExtra(EXTRA_PHOTO_URLS);
        if (photoUrls == null) {
            photoUrls = new ArrayList<>();
        }

        rvMemoryAlbumPhotos.setLayoutManager(new GridLayoutManager(this, 3));
        rvMemoryAlbumPhotos.setAdapter(new MemoryPhotoGridAdapter(photoUrls));
        tvEmptyMemoryAlbum.setVisibility(photoUrls.isEmpty() ? View.VISIBLE : View.GONE);
        rvMemoryAlbumPhotos.setVisibility(photoUrls.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
