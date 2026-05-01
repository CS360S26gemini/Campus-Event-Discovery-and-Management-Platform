package com.example.CampusEventDiscovery.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.MemoryPhotoGridAdapter;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MemoryAlbumActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";
    public static final String EXTRA_PHOTO_URLS = "photoUrls";

    private MaterialToolbar toolbarMemoryAlbum;
    private RecyclerView rvMemoryAlbumPhotos;
    private TextView tvEmptyMemoryAlbum;

    private EventRepository repository;
    private ArrayList<String> photoUrls;
    private String currentUserId;
    private String eventId;
    private MemoryPhotoGridAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_album);

        repository = new EventRepository();

        toolbarMemoryAlbum = findViewById(R.id.toolbarMemoryAlbum);
        rvMemoryAlbumPhotos = findViewById(R.id.rvMemoryAlbumPhotos);
        tvEmptyMemoryAlbum = findViewById(R.id.tvEmptyMemoryAlbum);

        toolbarMemoryAlbum.setNavigationOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        toolbarMemoryAlbum.setTitle(TextUtils.isEmpty(eventTitle) ? getString(R.string.memory_album_title) : eventTitle);

        photoUrls = getIntent().getStringArrayListExtra(EXTRA_PHOTO_URLS);
        if (photoUrls == null) {
            photoUrls = new ArrayList<>();
        }

        rvMemoryAlbumPhotos.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new MemoryPhotoGridAdapter(photoUrls, this::confirmDeletePhoto);
        rvMemoryAlbumPhotos.setAdapter(adapter);
        updateEmptyState();
    }

    private void confirmDeletePhoto(String photoUrl) {
        if (TextUtils.isEmpty(photoUrl)) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_memory_photo_title)
                .setMessage(R.string.delete_memory_photo_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deletePhoto(photoUrl))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deletePhoto(String photoUrl) {
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(eventId) || TextUtils.isEmpty(photoUrl)) {
            Toast.makeText(this, R.string.memory_photo_delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        repository.removeMemoryPhoto(currentUserId, eventId, photoUrl, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                photoUrls.remove(photoUrl);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Toast.makeText(MemoryAlbumActivity.this, R.string.memory_photo_deleted, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MemoryAlbumActivity.this, R.string.memory_photo_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = photoUrls == null || photoUrls.isEmpty();
        tvEmptyMemoryAlbum.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvMemoryAlbumPhotos.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
