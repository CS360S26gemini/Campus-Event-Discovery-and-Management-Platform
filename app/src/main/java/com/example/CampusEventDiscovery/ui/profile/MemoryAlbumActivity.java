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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.MemoryPhotoGridAdapter;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.CloudinaryHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MemoryAlbumActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";
    public static final String EXTRA_PHOTO_URLS = "photoUrls";
    public static final String EXTRA_ATTENDED_AT_MILLIS = "attendedAtMillis";

    private MaterialToolbar toolbarMemoryAlbum;
    private MaterialButton btnAddAlbumPhotos;
    private MaterialButton btnSelectAlbumPhotos;
    private MaterialButton btnRemoveSelectedPhotos;
    private RecyclerView rvMemoryAlbumPhotos;
    private ProgressBar progressBarMemoryAlbum;
    private TextView tvEmptyMemoryAlbum;

    private EventRepository repository;
    private ArrayList<String> photoUrls;
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private String currentUserId;
    private String eventId;
    private String eventTitle;
    private Timestamp attendedAt;
    private MemoryPhotoGridAdapter adapter;

    private final ActivityResultLauncher<String[]> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                selectedImageUris.clear();
                if (uris != null) {
                    selectedImageUris.addAll(uris);
                }
                if (!selectedImageUris.isEmpty()) {
                    uploadSelectedPhotos();
                }
            });

    private final ActivityResultLauncher<Intent> photoViewerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> updatedPhotos = result.getData()
                            .getStringArrayListExtra(MemoryPhotoViewerActivity.EXTRA_PHOTO_URLS);
                    if (updatedPhotos != null) {
                        photoUrls.clear();
                        photoUrls.addAll(updatedPhotos);
                        adapter.updateData(photoUrls);
                        updateEmptyState();
                        setResult(RESULT_OK);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_album);

        repository = new EventRepository();

        toolbarMemoryAlbum = findViewById(R.id.toolbarMemoryAlbum);
        btnAddAlbumPhotos = findViewById(R.id.btnAddAlbumPhotos);
        btnSelectAlbumPhotos = findViewById(R.id.btnSelectAlbumPhotos);
        btnRemoveSelectedPhotos = findViewById(R.id.btnRemoveSelectedPhotos);
        rvMemoryAlbumPhotos = findViewById(R.id.rvMemoryAlbumPhotos);
        progressBarMemoryAlbum = findViewById(R.id.progressBarMemoryAlbum);
        tvEmptyMemoryAlbum = findViewById(R.id.tvEmptyMemoryAlbum);

        toolbarMemoryAlbum.setNavigationOnClickListener(v -> finish());
        btnAddAlbumPhotos.setOnClickListener(v -> selectPhotosForAlbum());
        btnSelectAlbumPhotos.setOnClickListener(v -> toggleSelectionMode());
        btnRemoveSelectedPhotos.setOnClickListener(v -> confirmRemoveSelectedPhotos());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        long attendedAtMillis = getIntent().getLongExtra(EXTRA_ATTENDED_AT_MILLIS, -1L);
        if (attendedAtMillis > 0L) {
            attendedAt = new Timestamp(new Date(attendedAtMillis));
        }

        toolbarMemoryAlbum.setTitle(TextUtils.isEmpty(eventTitle)
                ? getString(R.string.memory_album_title)
                : eventTitle);

        photoUrls = getIntent().getStringArrayListExtra(EXTRA_PHOTO_URLS);
        if (photoUrls == null) {
            photoUrls = new ArrayList<>();
        }

        rvMemoryAlbumPhotos.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new MemoryPhotoGridAdapter(photoUrls, this::openPhotoViewer, this::updateSelectionUi);
        rvMemoryAlbumPhotos.setAdapter(adapter);
        updateEmptyState();
        updateSelectionUi(0);
        WalkthroughManager.maybeShow(this, getWindow().getDecorView(), "memory_album");
    }

    private void selectPhotosForAlbum() {
        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            Toast.makeText(this, "Walkthrough mode: photo picker was not opened.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.memory_photos_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        photoPickerLauncher.launch(new String[]{"image/*"});
    }

    private void uploadSelectedPhotos() {
        Toast.makeText(this, R.string.memory_uploading_photos, Toast.LENGTH_SHORT).show();
        setLoading(true);
        uploadNextPhoto(0, new ArrayList<>());
    }

    private void uploadNextPhoto(int index, List<String> uploadedUrls) {
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
                    Toast.makeText(MemoryAlbumActivity.this, R.string.memory_photos_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveUploadedPhotos(List<String> uploadedUrls) {
        if (uploadedUrls == null || uploadedUrls.isEmpty()) {
            setLoading(false);
            return;
        }

        repository.addMemoryPhotos(
                currentUserId,
                eventId,
                eventTitle,
                attendedAt,
                uploadedUrls,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        photoUrls.addAll(uploadedUrls);
                        adapter.updateData(photoUrls);
                        selectedImageUris.clear();
                        setLoading(false);
                        updateEmptyState();
                        setResult(RESULT_OK);
                        Toast.makeText(MemoryAlbumActivity.this, R.string.memory_photos_saved, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Toast.makeText(MemoryAlbumActivity.this, R.string.memory_photos_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openPhotoViewer(int position) {
        if (photoUrls == null || photoUrls.isEmpty() || position < 0 || position >= photoUrls.size()) {
            return;
        }

        Intent intent = new Intent(this, MemoryPhotoViewerActivity.class);
        intent.putExtra(MemoryPhotoViewerActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(MemoryPhotoViewerActivity.EXTRA_EVENT_TITLE, eventTitle);
        intent.putExtra(MemoryPhotoViewerActivity.EXTRA_START_INDEX, position);
        intent.putStringArrayListExtra(MemoryPhotoViewerActivity.EXTRA_PHOTO_URLS, new ArrayList<>(photoUrls));
        photoViewerLauncher.launch(intent);
    }

    private void toggleSelectionMode() {
        boolean enableSelection = !adapter.isSelectionMode();
        adapter.setSelectionMode(enableSelection);
        if (enableSelection) {
            btnSelectAlbumPhotos.setText(R.string.cancel);
        }
    }

    private void confirmRemoveSelectedPhotos() {
        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            Toast.makeText(this, "Walkthrough mode: memory photo was not deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedPhotos = adapter.getSelectedPhotoUrls();
        if (selectedPhotos.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_memory_photo_title)
                .setMessage(getString(R.string.delete_selected_memory_photos_message, selectedPhotos.size()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteSelectedPhotos(selectedPhotos, 0))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteSelectedPhotos(List<String> selectedPhotos, int index) {
        if (selectedPhotos == null || selectedPhotos.isEmpty()) {
            adapter.setSelectionMode(false);
            return;
        }

        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.memory_photo_delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (index == 0) {
            setLoading(true);
        }

        if (index >= selectedPhotos.size()) {
            photoUrls.removeAll(selectedPhotos);
            adapter.updateData(photoUrls);
            adapter.setSelectionMode(false);
            updateEmptyState();
            setLoading(false);
            setResult(RESULT_OK);
            Toast.makeText(this, R.string.memory_photo_deleted, Toast.LENGTH_SHORT).show();
            return;
        }

        String photoUrl = selectedPhotos.get(index);
        repository.removeMemoryPhoto(currentUserId, eventId, photoUrl, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                deleteSelectedPhotos(selectedPhotos, index + 1);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(MemoryAlbumActivity.this, R.string.memory_photo_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = photoUrls == null || photoUrls.isEmpty();
        tvEmptyMemoryAlbum.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvMemoryAlbumPhotos.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        progressBarMemoryAlbum.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnAddAlbumPhotos.setEnabled(!isLoading);
        btnSelectAlbumPhotos.setEnabled(!isLoading);
        btnRemoveSelectedPhotos.setEnabled(!isLoading);
    }

    private void updateSelectionUi(int selectedCount) {
        boolean selectionMode = adapter != null && adapter.isSelectionMode();
        btnSelectAlbumPhotos.setText(selectionMode ? R.string.cancel : R.string.select_memory_photos);
        btnRemoveSelectedPhotos.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        btnRemoveSelectedPhotos.setText(getString(R.string.remove_selected_photos_count, selectedCount));
        btnRemoveSelectedPhotos.setEnabled(selectionMode && selectedCount > 0);
        btnAddAlbumPhotos.setEnabled(!selectionMode && progressBarMemoryAlbum.getVisibility() != View.VISIBLE);
    }
}
