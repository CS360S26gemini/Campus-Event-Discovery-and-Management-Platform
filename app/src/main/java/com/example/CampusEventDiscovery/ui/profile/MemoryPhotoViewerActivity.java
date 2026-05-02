package com.example.CampusEventDiscovery.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.MemoryPhotoViewerAdapter;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MemoryPhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";
    public static final String EXTRA_PHOTO_URLS = "photoUrls";
    public static final String EXTRA_START_INDEX = "startIndex";

    private MaterialToolbar toolbarPhotoViewer;
    private MaterialButton btnDeleteViewerPhoto;
    private RecyclerView rvPhotoViewer;

    private EventRepository repository;
    private ArrayList<String> photoUrls;
    private LinearLayoutManager layoutManager;
    private PagerSnapHelper snapHelper;
    private MemoryPhotoViewerAdapter adapter;
    private String currentUserId;
    private String eventId;
    private int currentPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_photo_viewer);

        repository = new EventRepository();

        toolbarPhotoViewer = findViewById(R.id.toolbarPhotoViewer);
        btnDeleteViewerPhoto = findViewById(R.id.btnDeleteViewerPhoto);
        rvPhotoViewer = findViewById(R.id.rvPhotoViewer);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        photoUrls = getIntent().getStringArrayListExtra(EXTRA_PHOTO_URLS);
        if (photoUrls == null) {
            photoUrls = new ArrayList<>();
        }

        currentPosition = Math.max(0, Math.min(
                getIntent().getIntExtra(EXTRA_START_INDEX, 0),
                Math.max(0, photoUrls.size() - 1)
        ));

        toolbarPhotoViewer.setNavigationOnClickListener(v -> finish());
        btnDeleteViewerPhoto.setOnClickListener(v -> confirmDeleteCurrentPhoto());

        layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        rvPhotoViewer.setLayoutManager(layoutManager);
        adapter = new MemoryPhotoViewerAdapter(photoUrls);
        rvPhotoViewer.setAdapter(adapter);
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvPhotoViewer);
        rvPhotoViewer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@androidx.annotation.NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updatePositionFromSnap();
                }
            }
        });

        rvPhotoViewer.scrollToPosition(currentPosition);
        rvPhotoViewer.post(this::updateCounter);
        if (photoUrls.isEmpty()) {
            Toast.makeText(this, R.string.memory_viewer_empty, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updatePositionFromSnap() {
        if (snapHelper == null || layoutManager == null) {
            return;
        }

        android.view.View snapView = snapHelper.findSnapView(layoutManager);
        if (snapView == null) {
            return;
        }

        int position = layoutManager.getPosition(snapView);
        if (position != RecyclerView.NO_POSITION) {
            currentPosition = position;
            updateCounter();
        }
    }

    private void updateCounter() {
        if (photoUrls == null || photoUrls.isEmpty()) {
            toolbarPhotoViewer.setTitle("");
            return;
        }
        toolbarPhotoViewer.setTitle(getString(
                R.string.memory_photo_position,
                currentPosition + 1,
                photoUrls.size()
        ));
    }

    private void confirmDeleteCurrentPhoto() {
        if (WalkthroughManager.isWalkthroughIntent(getIntent()) || WalkthroughManager.isActive()) {
            Toast.makeText(this, "Walkthrough mode: memory photo was not deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        String photoUrl = getCurrentPhotoUrl();
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

    private String getCurrentPhotoUrl() {
        if (photoUrls == null || photoUrls.isEmpty()
                || currentPosition < 0 || currentPosition >= photoUrls.size()) {
            return "";
        }
        return photoUrls.get(currentPosition);
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
                adapter.updateData(photoUrls);
                publishResult();

                if (photoUrls.isEmpty()) {
                    finish();
                    return;
                }

                currentPosition = Math.min(currentPosition, photoUrls.size() - 1);
                rvPhotoViewer.post(() -> {
                    rvPhotoViewer.scrollToPosition(currentPosition);
                    updateCounter();
                });
                Toast.makeText(MemoryPhotoViewerActivity.this, R.string.memory_photo_deleted, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MemoryPhotoViewerActivity.this, R.string.memory_photo_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void publishResult() {
        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_PHOTO_URLS, new ArrayList<>(photoUrls));
        setResult(RESULT_OK, data);
    }
}
