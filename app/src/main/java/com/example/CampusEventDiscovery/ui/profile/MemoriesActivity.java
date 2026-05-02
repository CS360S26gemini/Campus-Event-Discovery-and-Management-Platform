package com.example.CampusEventDiscovery.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
import com.example.CampusEventDiscovery.adapter.MemoryEventPickerAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.CloudinaryHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Displays the current user's event-based memory albums.
 */
public class MemoriesActivity extends AppCompatActivity {

    private MaterialToolbar toolbarMemories;
    private MaterialButton btnCreateMemory;
    private RecyclerView rvMemories;
    private ProgressBar progressBarMemories;
    private TextView tvEmptyMemories;

    private EventRepository repository;
    private MemoryAdapter adapter;
    private String currentUserId;
    private final List<Memory> memories = new ArrayList<>();
    private final List<Rsvp> registeredRsvps = new ArrayList<>();
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private Memory selectedMemory;
    private boolean walkthroughMode;

    private final ActivityResultLauncher<Intent> albumLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!walkthroughMode && result.getResultCode() == RESULT_OK) {
                    loadMemories();
                }
            });

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
        walkthroughMode = WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive();

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupActions();
        if (walkthroughMode) {
            Rsvp demoRsvp = new Rsvp();
            demoRsvp.setEventId(WalkthroughManager.getDemoEvent().getEventId());
            demoRsvp.setTitle(WalkthroughManager.getDemoEvent().getTitle());
            demoRsvp.setDate(WalkthroughManager.getDemoEvent().getDate());
            demoRsvp.setStatus("confirmed");
            registeredRsvps.clear();
            registeredRsvps.add(demoRsvp);
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
        btnCreateMemory = findViewById(R.id.btnCreateMemory);
        rvMemories = findViewById(R.id.rvMemories);
        progressBarMemories = findViewById(R.id.progressBarMemories);
        tvEmptyMemories = findViewById(R.id.tvEmptyMemories);
    }

    private void setupToolbar() {
        toolbarMemories.setNavigationOnClickListener(v -> finish());
    }

    private void setupActions() {
        btnCreateMemory.setOnClickListener(v -> showCreateMemoryPicker());
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

        repository.getRegisteredRsvpsForMemories(currentUserId, new EventRepository.RsvpListCallback() {
            @Override
            public void onSuccess(List<Rsvp> rsvps) {
                registeredRsvps.clear();
                if (rsvps != null) {
                    registeredRsvps.addAll(rsvps);
                }
                loadMemoryAlbums();
            }

            @Override
            public void onError(Exception e) {
                registeredRsvps.clear();
                bindMemories(new ArrayList<>());
            }
        });
    }

    private void loadMemoryAlbums() {
        repository.getMemories(currentUserId, new EventRepository.MemoryListCallback() {
            @Override
            public void onSuccess(List<Memory> storedMemories) {
                bindMemories(buildMemoryAlbums(storedMemories));
            }

            @Override
            public void onError(Exception e) {
                bindMemories(new ArrayList<>());
            }
        });
    }

    private List<Memory> buildMemoryAlbums(List<Memory> storedMemories) {
        Map<String, Rsvp> rsvpsByEvent = new LinkedHashMap<>();
        for (Rsvp rsvp : registeredRsvps) {
            if (rsvp != null && !TextUtils.isEmpty(rsvp.getEventId())) {
                rsvpsByEvent.put(rsvp.getEventId(), rsvp);
            }
        }

        Map<String, Memory> albumsByEvent = new LinkedHashMap<>();

        if (storedMemories != null) {
            for (Memory stored : storedMemories) {
                if (stored == null || TextUtils.isEmpty(stored.getEventId())) {
                    continue;
                }

                Memory album = albumsByEvent.get(stored.getEventId());
                if (album == null) {
                    album = new Memory(
                            stored.getEventId(),
                            stored.getEventTitle(),
                            new ArrayList<>(),
                            stored.getAttendedAt(),
                            stored.getRating()
                    );
                    albumsByEvent.put(stored.getEventId(), album);
                }

                Rsvp rsvp = rsvpsByEvent.get(stored.getEventId());
                if (TextUtils.isEmpty(album.getEventTitle())) {
                    album.setEventTitle(rsvp != null && !TextUtils.isEmpty(rsvp.getTitle())
                            ? rsvp.getTitle()
                            : stored.getEventTitle());
                }
                if (album.getAttendedAt() == null) {
                    album.setAttendedAt(rsvp != null && rsvp.getDate() != null
                            ? rsvp.getDate()
                            : stored.getAttendedAt());
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

    private void showCreateMemoryPicker() {
        if (walkthroughMode) {
            Toast.makeText(this, "Walkthrough mode: memory album was not created.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, R.string.login_required_for_checkout, Toast.LENGTH_SHORT).show();
            return;
        }

        if (registeredRsvps.isEmpty()) {
            Toast.makeText(this, R.string.no_registered_events_for_memories, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Rsvp> available = getRsvpsWithoutAlbums();
        if (available.isEmpty()) {
            Toast.makeText(this, R.string.all_registered_events_have_memories, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        repository.getEventsByIds(extractEventIds(available), new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                setLoading(false);
                showCreateMemoryEventDialog(available, events);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showCreateMemoryEventDialog(available, new ArrayList<>());
            }
        });
    }

    private List<Rsvp> getRsvpsWithoutAlbums() {
        Set<String> existingAlbumIds = new HashSet<>();
        for (Memory memory : memories) {
            if (memory != null && !TextUtils.isEmpty(memory.getEventId())) {
                existingAlbumIds.add(memory.getEventId());
            }
        }

        List<Rsvp> available = new ArrayList<>();
        for (Rsvp rsvp : registeredRsvps) {
            if (rsvp != null
                    && !TextUtils.isEmpty(rsvp.getEventId())
                    && !existingAlbumIds.contains(rsvp.getEventId())) {
                available.add(rsvp);
            }
        }
        return available;
    }

    private void createMemoryAlbum(Rsvp rsvp) {
        if (rsvp == null || TextUtils.isEmpty(rsvp.getEventId())) {
            return;
        }

        String eventTitle = TextUtils.isEmpty(rsvp.getTitle())
                ? getString(R.string.memory_album_title)
                : rsvp.getTitle();
        Timestamp attendedAt = rsvp.getDate();

        setLoading(true);
        repository.createMemoryAlbum(currentUserId, rsvp.getEventId(), eventTitle, attendedAt,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        Toast.makeText(MemoriesActivity.this, R.string.memory_album_created, Toast.LENGTH_SHORT).show();
                        Memory createdMemory = new Memory(
                                rsvp.getEventId(),
                                eventTitle,
                                new ArrayList<>(),
                                attendedAt,
                                0
                        );
                        memories.add(0, createdMemory);
                        adapter.updateData(new ArrayList<>(memories));
                        tvEmptyMemories.setVisibility(View.GONE);
                        rvMemories.setVisibility(View.VISIBLE);
                        openMemoryAlbum(createdMemory);
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Toast.makeText(MemoriesActivity.this, R.string.memory_album_create_failed, Toast.LENGTH_SHORT).show();
                    }
                });
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
        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            Toast.makeText(this, "Walkthrough mode: photo picker was not opened.", Toast.LENGTH_SHORT).show();
            return;
        }

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
        intent.putExtra(MemoryAlbumActivity.EXTRA_ATTENDED_AT_MILLIS, timestampMillis(memory.getAttendedAt()));
        intent.putStringArrayListExtra(MemoryAlbumActivity.EXTRA_PHOTO_URLS, photoUrls);
        albumLauncher.launch(intent);
    }

    private void confirmDeleteMemory(Memory memory) {
        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            Toast.makeText(this, "Walkthrough mode: memory folder was not deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

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
        btnCreateMemory.setEnabled(!isLoading);
    }

    private String formatRsvpLabel(Rsvp rsvp) {
        String title = rsvp == null || TextUtils.isEmpty(rsvp.getTitle())
                ? getString(R.string.memory_album_title)
                : rsvp.getTitle();
        if (rsvp == null || rsvp.getDate() == null) {
            return title;
        }
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(rsvp.getDate().toDate());
        return title + "\n" + date;
    }

    private long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? -1L : timestamp.toDate().getTime();
    }

    private List<String> extractEventIds(List<Rsvp> rsvps) {
        List<String> eventIds = new ArrayList<>();
        if (rsvps == null) {
            return eventIds;
        }
        for (Rsvp rsvp : rsvps) {
            if (rsvp != null && !TextUtils.isEmpty(rsvp.getEventId())) {
                eventIds.add(rsvp.getEventId());
            }
        }
        return eventIds;
    }

    private void showCreateMemoryEventDialog(List<Rsvp> availableRsvps, List<Event> fetchedEvents) {
        Map<String, Rsvp> rsvpByEventId = new LinkedHashMap<>();
        if (availableRsvps != null) {
            for (Rsvp rsvp : availableRsvps) {
                if (rsvp != null && !TextUtils.isEmpty(rsvp.getEventId())) {
                    rsvpByEventId.put(rsvp.getEventId(), rsvp);
                }
            }
        }

        Map<String, Event> fetchedById = new LinkedHashMap<>();
        if (fetchedEvents != null) {
            for (Event event : fetchedEvents) {
                if (event != null && !TextUtils.isEmpty(event.getEventId())) {
                    fetchedById.put(event.getEventId(), event);
                }
            }
        }

        List<Event> displayEvents = new ArrayList<>();
        for (Rsvp rsvp : availableRsvps) {
            if (rsvp == null || TextUtils.isEmpty(rsvp.getEventId())) {
                continue;
            }

            Event baseEvent = fetchedById.get(rsvp.getEventId());
            Event displayEvent = baseEvent != null ? baseEvent : new Event();
            displayEvent.setEventId(rsvp.getEventId());
            if (TextUtils.isEmpty(displayEvent.getTitle())) {
                displayEvent.setTitle(rsvp.getTitle());
            }
            if (displayEvent.getDate() == null) {
                displayEvent.setDate(rsvp.getDate());
            }
            displayEvents.add(displayEvent);
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_memory_event_picker, null, false);
        RecyclerView rvPicker = dialogView.findViewById(R.id.rvMemoryEventPicker);
        TextView tvEmpty = dialogView.findViewById(R.id.tvMemoryEventPickerEmpty);

        rvPicker.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty.setVisibility(displayEvents.isEmpty() ? View.VISIBLE : View.GONE);
        rvPicker.setVisibility(displayEvents.isEmpty() ? View.GONE : View.VISIBLE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.choose_memory_event_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .create();

        rvPicker.setAdapter(new MemoryEventPickerAdapter(displayEvents, event -> {
            dialog.dismiss();
            Rsvp selectedRsvp = event == null ? null : rsvpByEventId.get(event.getEventId());
            createMemoryAlbum(selectedRsvp);
        }));

        dialog.show();
    }
}
