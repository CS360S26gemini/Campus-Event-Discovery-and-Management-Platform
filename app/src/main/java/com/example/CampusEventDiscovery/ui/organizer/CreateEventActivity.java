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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.CampusEventDiscovery.util.Constants;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CreateEventActivity.java
 *
 * Organizer event proposal form that writes a pending proposal to Firestore.
 * Integrated with Cloudinary for image uploads and Campus Map location selection.
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
    private AutoCompleteTextView actvBuilding;
    private EditText etLocationDetail;
    private EditText etVenue;
    private EditText etDescription;
    private EditText etCapacity;
    private CheckBox cbUseTiers;
    private View tilTicketPrice;
    private EditText etTicketPrice;
    private LinearLayout llTierBuilderContainer;
    private LinearLayout llTiersList;
    private MaterialButton btnAddTier;
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
        setupBuildingDropdown();
        setupDatePicker();
        setupTimePicker();
        preloadOrganizerName();
        setupTierBuilder();
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
        actvBuilding = findViewById(R.id.actvBuilding);
        etLocationDetail = findViewById(R.id.etLocationDetail);
        etVenue = findViewById(R.id.etVenue);
        etDescription = findViewById(R.id.etDescription);
        etCapacity = findViewById(R.id.etCapacity);
        cbUseTiers = findViewById(R.id.cbUseTiers);
        tilTicketPrice = findViewById(R.id.tilTicketPrice);
        etTicketPrice = findViewById(R.id.etTicketPrice);
        llTierBuilderContainer = findViewById(R.id.llTierBuilderContainer);
        llTiersList = findViewById(R.id.llTiersList);
        btnAddTier = findViewById(R.id.btnAddTier);
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

    private void setupBuildingDropdown() {
        String[] buildings = {
                Constants.MAP_LOC_HSS,
                Constants.MAP_LOC_SSE,
                Constants.MAP_LOC_SAHSOL,
                Constants.MAP_LOC_SPORTS_COMPLEX,
                Constants.MAP_LOC_PARKING_LOT,
                Constants.MAP_LOC_REDC,
                Constants.MAP_LOC_CRICKET_GROUND,
                Constants.MAP_LOC_SDSB,
                Constants.MAP_LOC_IST,
                Constants.MAP_LOC_MASJID
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                buildings
        );
        actvBuilding.setAdapter(adapter);
        actvBuilding.setOnClickListener(v -> actvBuilding.showDropDown());
        actvBuilding.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvBuilding.showDropDown();
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

    private void setupTierBuilder() {
        cbUseTiers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilTicketPrice.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            llTierBuilderContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && llTiersList.getChildCount() == 0) {
                addNewTierView();
            }
        });

        btnAddTier.setOnClickListener(v -> addNewTierView());
    }

    private void addNewTierView() {
        View tierView = LayoutInflater.from(this).inflate(R.layout.item_ticket_tier_builder, llTiersList, false);
        TextView tvTierNumber = tierView.findViewById(R.id.tvTierNumber);
        View btnRemoveTier = tierView.findViewById(R.id.btnRemoveTier);

        int tierCount = llTiersList.getChildCount() + 1;
        tvTierNumber.setText("Tier " + tierCount);

        btnRemoveTier.setOnClickListener(v -> {
            llTiersList.removeView(tierView);
            updateTierNumbers();
        });

        llTiersList.addView(tierView);
    }

    private void updateTierNumbers() {
        for (int i = 0; i < llTiersList.getChildCount(); i++) {
            View v = llTiersList.getChildAt(i);
            TextView tv = v.findViewById(R.id.tvTierNumber);
            tv.setText("Tier " + (i + 1));
        }
    }

    private void setupSubmitButton() {
        btnSubmitEvent.setOnClickListener(v -> submitProposal());
    }

    private void submitProposal() {
        String title = etEventTitle.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String buildingKey = actvBuilding.getText().toString().trim();
        String locationDesc = etLocationDetail.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String capacityText = etCapacity.getText().toString().trim();
        String ticketPriceText = etTicketPrice.getText().toString().trim();
        String tagsText = etTags.getText().toString().trim();
        String sponsorsText = etSponsors.getText().toString().trim();
        String foodStallsText = etFoodStalls.getText().toString().trim();
        String trailerUrl = etTrailerUrl.getText().toString().trim();

        if (TextUtils.isEmpty(buildingKey)) {
            Toast.makeText(this, "Please select a building", Toast.LENGTH_SHORT).show();
            return;
        }

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

        List<Map<String, Object>> tiers = new ArrayList<>();
        double baseTicketPrice = 0.0;

        if (cbUseTiers.isChecked()) {
            if (llTiersList.getChildCount() == 0) {
                Toast.makeText(this, getString(R.string.no_tiers_error), Toast.LENGTH_SHORT).show();
                return;
            }
            for (int i = 0; i < llTiersList.getChildCount(); i++) {
                View v = llTiersList.getChildAt(i);
                EditText etName = v.findViewById(R.id.etTierName);
                EditText etPrice = v.findViewById(R.id.etTierPrice);
                EditText etCap = v.findViewById(R.id.etTierCapacity);
                EditText etDesc = v.findViewById(R.id.etTierDescription);

                String n = etName.getText().toString().trim();
                String pStr = etPrice.getText().toString().trim();
                String cStr = etCap.getText().toString().trim();
                String d = etDesc.getText().toString().trim();

                if (TextUtils.isEmpty(n) || TextUtils.isEmpty(pStr) || TextUtils.isEmpty(cStr)) {
                    Toast.makeText(this, getString(R.string.invalid_tier_data), Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double p = Double.parseDouble(pStr);
                    long c = Long.parseLong(cStr);
                    Map<String, Object> tierMap = new HashMap<>();
                    tierMap.put("name", n);
                    tierMap.put("price", p);
                    tierMap.put("capacity", c);
                    tierMap.put("description", d);
                    tierMap.put("rsvpCount", 0L);
                    tiers.add(tierMap);
                    
                    if (i == 0) baseTicketPrice = p; // Fallback for list views
                } catch (NumberFormatException e) {
                    Toast.makeText(this, getString(R.string.invalid_tier_data), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } else {
            if (!TextUtils.isEmpty(ticketPriceText)) {
                try {
                    baseTicketPrice = Double.parseDouble(ticketPriceText);
                    if (baseTicketPrice < 0.0) {
                        Toast.makeText(this, getString(R.string.invalid_ticket_price), Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, getString(R.string.invalid_ticket_price), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        String validationError = EventValidator.validate(
                title,
                description,
                buildingKey, // Passing buildingKey as venue for simple validation
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
        proposal.setLocation(locationDesc + ", " + buildingKey);
        proposal.setLocationKey(buildingKey);
        proposal.setLocationDescription(locationDesc);
        proposal.setCapacity(capacity);
        proposal.setTicketPrice(baseTicketPrice);
        proposal.setTiers(tiers);
        proposal.setSponsors(parseCommaSeparated(sponsorsText, false));
        proposal.setFoodStalls(parseCommaSeparated(foodStallsText, false));
        proposal.setTrailerUrl(trailerUrl);
        proposal.setOrganizerId(currentUserId);
        proposal.setOrganizerName(TextUtils.isEmpty(currentOrganizerName) ? "Organizer" : currentOrganizerName);
        proposal.setStatus("pending");
        proposal.setAdminNote("");
        proposal.setSubmittedAt(Timestamp.now());
        proposal.setReviewedAt(null);

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
