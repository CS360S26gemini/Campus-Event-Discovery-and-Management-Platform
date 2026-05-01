package com.example.CampusEventDiscovery.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.MemoryAdapter;
import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.CloudinaryHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the current user's event-based memory albums.
 */
public class MemoriesActivity extends AppCompatActivity {

    private MaterialToolbar toolbarMemories;
    private RecyclerView rvMemories;
    private ProgressBar progressBarMemories;
    private TextView tvEmptyMemories;

    private EventRepository repository;
    private MemoryAdapter adapter;
    private String currentUserId;
    private final List<Memory> memories = new ArrayList<>();
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private Memory selectedMemory;

    private final ActivityResultLauncher<String[]> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                selectedImageUris.clear();
                if (uris != null) {
                    selectedImageUris.addAll(uris);
                }
                if (!selectedImageUris.isEmpty() && selectedMemory != null) {
                    uploadSelectedPhotos();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memories);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            bindMemories(java.util.Collections.singletonList(new Memory(
                    WalkthroughManager.getDemoEvent().getEventId(),
                    WalkthroughManager.getDemoEvent().getTitle(),
                    new ArrayList<>(),
                    WalkthroughManager.getDemoEvent().getDate(),
                    0
            )));
            WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "memories");
        } else {
            loadMemories();
        }
    }

    private void bindViews() {
        toolbarMemories = findViewById(R.id.toolbarMemories);
        rvMemories = findViewById(R.id.rvMemories);
        progressBarMemories = findViewById(R.id.progressBarMemories);
        tvEmptyMemories = findViewById(R.id.tvEmptyMemories);
    }

    private void setupToolbar() {
        toolbarMemories.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new MemoryAdapter(memories, new MemoryAdapter.OnMemoryActionListener() {
            @Override
            public void onOpenAlbum(Memory memory) {
                openMemoryAlbum(memory);
            }

            @Override
            public void onAddPhotos(Memory memory) {
                selectPhotosForMemory(memory);
            }

            @Override
            public void onDeleteMemory(Memory memory) {
                confirmDeleteMemory(memory);
            }
        });
        rvMemories.setLayoutManager(new LinearLayoutManager(this));
        rvMemories.setAdapter(adapter);
    }

    private void loadMemories() {
        setLoading(true);

        if (TextUtils.isEmpty(currentUserId)) {
            bindMemories(new ArrayList<>());
            return;
        }

        repository.getAttendedRsvpsForMemories(currentUserId, new EventRepository.RsvpListCallback() {
            @Override
            public void onSuccess(List<Rsvp> rsvps) {
                loadMemoryAlbums(rsvps);
            }

            @Override
            public void onError(Exception e) {
                bindMemories(new ArrayList<>());
            }
        });
    }

    private void loadMemoryAlbums(List<Rsvp> rsvps) {
        repository.getMemories(currentUserId, new EventRepository.MemoryListCallback() {
            @Override
            public void onSuccess(List<Memory> storedMemories) {
                bindMemories(mergeRsvpsWithMemories(rsvps, storedMemories));
            }

            @Override
            public void onError(Exception e) {
                bindMemories(mergeRsvpsWithMemories(rsvps, new ArrayList<>()));
            }
        });
    }

    private List<Memory> mergeRsvpsWithMemories(List<Rsvp> rsvps, List<Memory> storedMemories) {
        Map<String, Memory> albumsByEvent = new LinkedHashMap<>();

        if (rsvps != null) {
            for (Rsvp rsvp : rsvps) {
                if (rsvp == null || TextUtils.isEmpty(rsvp.getEventId())) {
                    continue;
                }
                albumsByEvent.put(rsvp.getEventId(), new Memory(
                        rsvp.getEventId(),
                        rsvp.getTitle(),
                        new ArrayList<>(),
                        rsvp.getDate(),
                        0
                ));
            }
        }

        if (storedMemories != null) {
            for (Memory stored : storedMemories) {
                if (stored == null || TextUtils.isEmpty(stored.getEventId())) {
                    continue;
                }

                Memory album = albumsByEvent.get(stored.getEventId());
                if (album == null) {
                    continue;
                }

                if (TextUtils.isEmpty(album.getEventTitle())) {
                    album.setEventTitle(stored.getEventTitle());
                }
                if (album.getAttendedAt() == null) {
                    album.setAttendedAt(stored.getAttendedAt());
                }
                if (album.getRating() <= 0) {
                    album.setRating(stored.getRating());
                }

                List<String> mergedPhotos = album.getPhotoUrls();
                if (mergedPhotos == null) {
                    mergedPhotos = new ArrayList<>();
                    album.setPhotoUrls(mergedPhotos);
                }
                if (stored.getPhotoUrls() != null) {
                    for (String url : stored.getPhotoUrls()) {
                        if (!TextUtils.isEmpty(url) && !mergedPhotos.contains(url)) {
                            mergedPhotos.add(url);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(albumsByEvent.values());
    }

    private void bindMemories(List<Memory> items) {
        memories.clear();
        if (items != null) {
            memories.addAll(items);
        }
        adapter.updateData(new ArrayList<>(memories));
        tvEmptyMemories.setVisibility(memories.isEmpty() ? View.VISIBLE : View.GONE);
        rvMemories.setVisibility(memories.isEmpty() ? View.GONE : View.VISIBLE);
        setLoading(false);
    }

    private void selectPhotosForMemory(Memory memory) {
        if (memory == null || TextUtils.isEmpty(memory.getEventId())) {
            return;
        }
        selectedMemory = memory;
        photoPickerLauncher.launch(new String[]{"image/*"});
    }

    private void uploadSelectedPhotos() {
        if (selectedMemory == null || selectedImageUris.isEmpty()) {
            return;
        }
        Toast.makeText(this, R.string.memory_uploading_photos, Toast.LENGTH_SHORT).show();
        setLoading(true);
        uploadNextPhoto(0, new ArrayList<>());
    }

    private void uploadNextPhoto(int index, List<String> uploadedUrls) {
        if (selectedMemory == null) {
            setLoading(false);
            return;
        }

        if (index >= selectedImageUris.size()) {
            saveUploadedPhotos(uploadedUrls);
            return;
        }

        CloudinaryHelper.uploadImage(selectedImageUris.get(index), new CloudinaryHelper.CloudinaryCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                if (!TextUtils.isEmpty(imageUrl)) {
                    uploadedUrls.add(imageUrl);
                }
                uploadNextPhoto(index + 1, uploadedUrls);
            }

            @Override
            public void onError(String error) {
                if (!isFinishing() && !isDestroyed()) {
                    setLoading(false);
                    Toast.makeText(MemoriesActivity.this, R.string.memory_photos_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveUploadedPhotos(List<String> uploadedUrls) {
        if (selectedMemory == null || uploadedUrls.isEmpty()) {
            setLoading(false);
            return;
        }

        repository.addMemoryPhotos(
                currentUserId,
                selectedMemory.getEventId(),
                selectedMemory.getEventTitle(),
                selectedMemory.getAttendedAt(),
                uploadedUrls,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MemoriesActivity.this, R.string.memory_photos_saved, Toast.LENGTH_SHORT).show();
                        selectedMemory = null;
                        selectedImageUris.clear();
                        loadMemories();
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Toast.makeText(MemoriesActivity.this, R.string.memory_photos_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openMemoryAlbum(Memory memory) {
        if (memory == null) {
            return;
        }

        ArrayList<String> photoUrls = new ArrayList<>();
        if (memory.getPhotoUrls() != null) {
            for (String url : memory.getPhotoUrls()) {
                if (!TextUtils.isEmpty(url)) {
                    photoUrls.add(url);
                }
            }
        }

        Intent intent = new Intent(this, MemoryAlbumActivity.class);
        intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_ID, memory.getEventId());
        intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_TITLE, memory.getEventTitle());
        intent.putStringArrayListExtra(MemoryAlbumActivity.EXTRA_PHOTO_URLS, photoUrls);
        startActivity(intent);
    }

    private void confirmDeleteMemory(Memory memory) {
        if (memory == null || TextUtils.isEmpty(memory.getEventId())) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_memory_folder_title)
                .setMessage(R.string.delete_memory_folder_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteMemory(memory))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteMemory(Memory memory) {
        if (memory == null || TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(memory.getEventId())) {
            Toast.makeText(this, R.string.memory_folder_delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        repository.deleteMemory(currentUserId, memory.getEventId(), new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MemoriesActivity.this, R.string.memory_folder_deleted, Toast.LENGTH_SHORT).show();
                loadMemories();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(MemoriesActivity.this, R.string.memory_folder_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBarMemories.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
