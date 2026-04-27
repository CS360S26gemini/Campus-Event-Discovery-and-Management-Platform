package com.example.CampusEventDiscovery.ui.event;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.CloudinaryHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets attendees submit ratings, reviews, and optional photos for events.
 */
public class EventFeedbackActivity extends AppCompatActivity {

    private MaterialToolbar toolbarFeedback;
    private TextView tvFeedbackEventTitle;
    private RatingBar ratingBarFeedback;
    private EditText etFeedbackReview;
    private TextView tvSelectedPhotos;
    private MaterialButton btnSelectPhotos;
    private MaterialButton btnSubmitFeedback;
    private ProgressBar progressBarFeedback;

    private EventRepository repository;
    private String currentUserId;
    private String eventId;
    private String eventTitle;
    private final List<Uri> selectedImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String[]> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                selectedImageUris.clear();
                if (uris != null) {
                    selectedImageUris.addAll(uris);
                }
                updateSelectedPhotosText();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_feedback);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);

        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");

        bindViews();
        setupToolbar();
        bindStaticUi();
        setupActions();
    }

    private void bindViews() {
        toolbarFeedback = findViewById(R.id.toolbarFeedback);
        tvFeedbackEventTitle = findViewById(R.id.tvFeedbackEventTitle);
        ratingBarFeedback = findViewById(R.id.ratingBarFeedback);
        etFeedbackReview = findViewById(R.id.etFeedbackReview);
        tvSelectedPhotos = findViewById(R.id.tvSelectedPhotos);
        btnSelectPhotos = findViewById(R.id.btnSelectPhotos);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);
        progressBarFeedback = findViewById(R.id.progressBarFeedback);
    }

    private void setupToolbar() {
        toolbarFeedback.setNavigationOnClickListener(v -> finish());
    }

    private void bindStaticUi() {
        tvFeedbackEventTitle.setText(TextUtils.isEmpty(eventTitle) ? getString(R.string.app_name) : eventTitle);
        updateSelectedPhotosText();
    }

    private void setupActions() {
        btnSelectPhotos.setOnClickListener(v -> photoPickerLauncher.launch(new String[]{"image/*"}));
        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, getString(R.string.feedback_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        int stars = Math.round(ratingBarFeedback.getRating());
        if (stars <= 0) {
            Toast.makeText(this, getString(R.string.feedback_requires_rating), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if (selectedImageUris.isEmpty()) {
            saveFeedback(new ArrayList<>());
            return;
        }

        uploadNextPhoto(0, new ArrayList<>());
    }

    private void uploadNextPhoto(int index, List<String> uploadedUrls) {
        if (index >= selectedImageUris.size()) {
            saveFeedback(uploadedUrls);
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
                    showLoading(false);
                    Toast.makeText(EventFeedbackActivity.this, getString(R.string.feedback_failed), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveFeedback(List<String> photoUrls) {
        String review = etFeedbackReview.getText().toString().trim();
        int stars = Math.round(ratingBarFeedback.getRating());

        repository.addRating(eventId, currentUserId, stars, review, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                repository.addMemory(
                        currentUserId,
                        eventId,
                        TextUtils.isEmpty(eventTitle) ? getString(R.string.app_name) : eventTitle,
                        photoUrls,
                        stars,
                        new EventRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                showLoading(false);
                                Toast.makeText(EventFeedbackActivity.this, getString(R.string.feedback_saved), Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(Exception e) {
                                showLoading(false);
                                Toast.makeText(EventFeedbackActivity.this, getString(R.string.feedback_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(EventFeedbackActivity.this, getString(R.string.feedback_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedPhotosText() {
        if (selectedImageUris.isEmpty()) {
            tvSelectedPhotos.setText(R.string.no_photos_selected);
        } else {
            tvSelectedPhotos.setText(getString(R.string.selected_photos_count, selectedImageUris.size()));
        }
    }

    private void showLoading(boolean isLoading) {
        progressBarFeedback.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        btnSelectPhotos.setEnabled(!isLoading);
        btnSubmitFeedback.setEnabled(!isLoading);
    }
}
