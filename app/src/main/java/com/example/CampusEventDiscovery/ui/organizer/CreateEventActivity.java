package com.example.CampusEventDiscovery.ui.organizer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.CloudinaryHelper;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.EventValidator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * CreateEventActivity.java
 *
 * Organizer event proposal form that writes a pending proposal to Firestore.
 * Integrated with Cloudinary for image uploads.
 */
public class CreateEventActivity extends AppCompatActivity {

    private MaterialToolbar toolbarCreateEvent;
    private EditText etEventTitle;
    private ImageView ivEventThumbnail;
    private MaterialButton btnSelectImage;
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;
    private MaterialButton btnPickDate;
    private MaterialButton btnPickTime;
    private AutoCompleteTextView actvCategory;
    private EditText etVenue;
    private EditText etDescription;
    private EditText etCapacity;
    private EditText etTicketPrice;
    private EditText etTags;
    private EditText etSponsors;
    private EditText etFoodStalls;
    private EditText etTrailerUrl;
    private ProgressBar progressBarCreateEvent;
    private MaterialButton btnSubmitEvent;

    private EventRepository repository;

    private Calendar selectedDateCalendar;
    private Timestamp selectedTimestamp;
    private Uri selectedImageUri;
    private boolean hasSelectedDate;
    private boolean hasSelectedTime;
    private boolean submitInProgress;

    private String currentUserId;
    private String currentOrganizerName = "";

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivEventThumbnail.setImageURI(selectedImageUri);
                }
            });

    private final ActivityResultLauncher<String> mediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchImagePicker();
                } else {
                    Toast.makeText(this,
                            "Storage permission is required to select an image.",
                            Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        repository = new EventRepository();
        selectedDateCalendar = Calendar.getInstance();
        selectedDateCalendar.set(Calendar.SECOND, 0);
        selectedDateCalendar.set(Calendar.MILLISECOND, 0);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupToolbar();
        setupImagePicker();
        setupCategoryDropdown();
        setupDatePicker();
        setupTimePicker();
        preloadOrganizerName();
        setupSubmitButton();
    }

    private void bindViews() {
        toolbarCreateEvent = findViewById(R.id.toolbarCreateEvent);
        etEventTitle = findViewById(R.id.etEventTitle);
        ivEventThumbnail = findViewById(R.id.ivEventThumbnail);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        actvCategory = findViewById(R.id.actvCategory);
        etVenue = findViewById(R.id.etVenue);
        etDescription = findViewById(R.id.etDescription);
        etCapacity = findViewById(R.id.etCapacity);
        etTicketPrice = findViewById(R.id.etTicketPrice);
        etTags = findViewById(R.id.etTags);
        etSponsors = findViewById(R.id.etSponsors);
        etFoodStalls = findViewById(R.id.etFoodStalls);
        etTrailerUrl = findViewById(R.id.etTrailerUrl);
        progressBarCreateEvent = findViewById(R.id.progressBarCreateEvent);
        btnSubmitEvent = findViewById(R.id.btnSubmitEvent);
    }

    private void setupToolbar() {
        toolbarCreateEvent.setNavigationOnClickListener(v -> finish());
    }

    private void setupImagePicker() {
        btnSelectImage.setOnClickListener(v -> checkMediaPermissionAndPick());
    }

    private void checkMediaPermissionAndPick() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            launchImagePicker();
        } else {
            mediaPermissionLauncher.launch(permission);
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.event_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        actvCategory.setAdapter(adapter);
        actvCategory.setOnClickListener(v -> actvCategory.showDropDown());
        actvCategory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvCategory.showDropDown();
            }
        });
    }

    private void setupDatePicker() {
        btnPickDate.setOnClickListener(v -> {
            int year = selectedDateCalendar.get(Calendar.YEAR);
            int month = selectedDateCalendar.get(Calendar.MONTH);
            int day = selectedDateCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    CreateEventActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedDateCalendar.set(Calendar.YEAR, selectedYear);
                        selectedDateCalendar.set(Calendar.MONTH, selectedMonth);
                        selectedDateCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                        selectedDateCalendar.set(Calendar.SECOND, 0);
                        selectedDateCalendar.set(Calendar.MILLISECOND, 0);
                        hasSelectedDate = true;

                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
                        tvSelectedDate.setText(getString(R.string.selected_date_label, sdf.format(selectedDateCalendar.getTime())));
                        tvSelectedDate.setTextColor(getColor(R.color.colorOnBackground));
                        refreshSelectedTimestamp();
                    },
                    year,
                    month,
                    day
            );

            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1_000L);
            dialog.show();
        });
    }

    private void setupTimePicker() {
        btnPickTime.setOnClickListener(v -> {
            Calendar source = hasSelectedTime ? selectedDateCalendar : Calendar.getInstance();
            int hour = source.get(Calendar.HOUR_OF_DAY);
            int minute = hasSelectedTime ? source.get(Calendar.MINUTE) : 0;

            TimePickerDialog dialog = new TimePickerDialog(
                    CreateEventActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        selectedDateCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                        selectedDateCalendar.set(Calendar.MINUTE, selectedMinute);
                        selectedDateCalendar.set(Calendar.SECOND, 0);
                        selectedDateCalendar.set(Calendar.MILLISECOND, 0);
                        hasSelectedTime = true;

                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        tvSelectedTime.setText(getString(R.string.selected_time_label, sdf.format(selectedDateCalendar.getTime())));
                        tvSelectedTime.setTextColor(getColor(R.color.colorOnBackground));
                        refreshSelectedTimestamp();
                    },
                    hour,
                    minute,
                    false
            );

            dialog.show();
        });
    }

    private void refreshSelectedTimestamp() {
        if (hasSelectedDate && hasSelectedTime) {
            selectedTimestamp = new Timestamp(selectedDateCalendar.getTime());
        } else {
            selectedTimestamp = null;
        }
    }

    private void preloadOrganizerName() {
        if (DevSessionManager.shouldUseBypass(this)) {
            currentOrganizerName = DevSessionManager.getDisplayName(this);
            return;
        }

        if (currentUserId == null) {
            currentOrganizerName = "Organizer";
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null && !TextUtils.isEmpty(user.getFullName())) {
                    currentOrganizerName = user.getFullName();
                } else if (user != null && !TextUtils.isEmpty(user.getEmail())) {
                    currentOrganizerName = user.getEmail();
                } else {
                    currentOrganizerName = "Organizer";
                }
            }

            @Override
            public void onError(Exception e) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null && !TextUtils.isEmpty(currentUser.getEmail())) {
                    currentOrganizerName = currentUser.getEmail();
                } else {
                    currentOrganizerName = "Organizer";
                }
            }
        });
    }

    private void setupSubmitButton() {
        btnSubmitEvent.setOnClickListener(v -> submitProposal());
    }

    private void submitProposal() {
        if (submitInProgress) {
            return;
        }

        String title = etEventTitle.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String capacityText = etCapacity.getText().toString().trim();
        String ticketPriceText = etTicketPrice.getText().toString().trim();
        String tagsText = etTags.getText().toString().trim();
        String sponsorsText = etSponsors.getText().toString().trim();
        String foodStallsText = etFoodStalls.getText().toString().trim();
        String trailerUrl = etTrailerUrl.getText().toString().trim();

        if (!hasSelectedDate || !hasSelectedTime || selectedTimestamp == null) {
            Toast.makeText(this, getString(R.string.date_and_time_required), Toast.LENGTH_SHORT).show();
            return;
        }

        long capacity;
        try {
            capacity = Long.parseLong(capacityText);
            if (capacity <= 0) {
                Toast.makeText(this, getString(R.string.invalid_capacity), Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.invalid_capacity), Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, getString(R.string.login_required_for_checkout), Toast.LENGTH_SHORT).show();
            return;
        }

        double ticketPrice = 0.0;
        if (!TextUtils.isEmpty(ticketPriceText)) {
            try {
                ticketPrice = Double.parseDouble(ticketPriceText);
                if (ticketPrice < 0.0) {
                    Toast.makeText(this, getString(R.string.invalid_ticket_price), Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.invalid_ticket_price), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String validationError = EventValidator.validate(
                title,
                description,
                venue,
                selectedTimestamp.toDate().getTime(),
                capacity,
                category,
                currentUserId
        );
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> tags = parseCommaSeparated(tagsText, true);
        String normalizedCategory = category.toLowerCase(Locale.getDefault());
        if (!tags.contains(normalizedCategory)) {
            tags.add(0, normalizedCategory);
        }

        EventProposal proposal = new EventProposal();
        proposal.setTitle(title);
        proposal.setDescription(description);
        proposal.setCategory(category);
        proposal.setTags(tags);
        proposal.setDate(selectedTimestamp);
        proposal.setLocation(venue);
        proposal.setCapacity(capacity);
        proposal.setTicketPrice(ticketPrice);
        proposal.setSponsors(parseCommaSeparated(sponsorsText, false));
        proposal.setFoodStalls(parseCommaSeparated(foodStallsText, false));
        proposal.setTrailerUrl(trailerUrl);
        proposal.setOrganizerId(currentUserId);
        proposal.setOrganizerName(TextUtils.isEmpty(currentOrganizerName) ? "Organizer" : currentOrganizerName);
        proposal.setStatus("pending");
        proposal.setAdminNote("");
        proposal.setSubmittedAt(Timestamp.now());
        proposal.setReviewedAt(null);

        submitInProgress = true;
        showLoading(true);

        if (selectedImageUri != null) {
            // [cloudinary] upload image instead of using Firebase Storage
            CloudinaryHelper.uploadImage(selectedImageUri, new CloudinaryHelper.CloudinaryCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    proposal.setImageUrl(imageUrl);
                    proposal.setThumbnailUrl(imageUrl); // Sync both for compatibility
                    saveProposal(proposal);
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    Toast.makeText(CreateEventActivity.this,
                            "Image upload failed: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            proposal.setImageUrl("");
            proposal.setThumbnailUrl("");
            saveProposal(proposal);
        }
    }

    private void saveProposal(EventProposal proposal) {
        repository.proposeEvent(proposal, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);

                new AlertDialog.Builder(CreateEventActivity.this)
                        .setTitle(getString(R.string.submitted_title))
                        .setMessage(getString(R.string.submitted_message))
                        .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(CreateEventActivity.this, getString(R.string.submit_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        submitInProgress = isLoading;
        progressBarCreateEvent.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        btnSubmitEvent.setEnabled(!isLoading);
    }

    private List<String> parseCommaSeparated(String rawValue, boolean normalizeLowerCase) {
        List<String> values = new ArrayList<>();
        if (TextUtils.isEmpty(rawValue)) {
            return values;
        }

        String[] split = rawValue.split(",");
        for (String item : split) {
            String value = item == null ? "" : item.trim();
            if (TextUtils.isEmpty(value)) {
                continue;
            }

            if (normalizeLowerCase) {
                value = value.toLowerCase(Locale.getDefault());
            }

            if (!values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }
}
