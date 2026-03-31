package com.example.campuseventdiscovery.ui.organizer;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.model.EventProposal;
import com.example.campuseventdiscovery.model.User;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.util.DevSessionManager;
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
 */
public class CreateEventActivity extends AppCompatActivity {

    private MaterialToolbar toolbarCreateEvent;
    private EditText etEventTitle;
    private TextView tvSelectedDate;
    private MaterialButton btnPickDate;
    private AutoCompleteTextView actvCategory;
    private EditText etVenue;
    private EditText etDescription;
    private EditText etCapacity;
    private EditText etTrailerUrl;
    private ProgressBar progressBarCreateEvent;
    private MaterialButton btnSubmitEvent;

    private EventRepository repository;

    private Calendar selectedDateCalendar;
    private Timestamp selectedTimestamp;

    private String currentUserId;
    private String currentOrganizerName = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        repository = new EventRepository();
        selectedDateCalendar = Calendar.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(this);

        bindViews();
        setupToolbar();
        setupCategoryDropdown();
        setupDatePicker();
        preloadOrganizerName();
        setupSubmitButton();
    }

    private void bindViews() {
        toolbarCreateEvent = findViewById(R.id.toolbarCreateEvent);
        etEventTitle = findViewById(R.id.etEventTitle);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        actvCategory = findViewById(R.id.actvCategory);
        etVenue = findViewById(R.id.etVenue);
        etDescription = findViewById(R.id.etDescription);
        etCapacity = findViewById(R.id.etCapacity);
        etTrailerUrl = findViewById(R.id.etTrailerUrl);
        progressBarCreateEvent = findViewById(R.id.progressBarCreateEvent);
        btnSubmitEvent = findViewById(R.id.btnSubmitEvent);
    }

    private void setupToolbar() {
        toolbarCreateEvent.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.event_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        actvCategory.setAdapter(adapter);
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
                        selectedDateCalendar.set(Calendar.HOUR_OF_DAY, 18);
                        selectedDateCalendar.set(Calendar.MINUTE, 0);
                        selectedDateCalendar.set(Calendar.SECOND, 0);

                        selectedTimestamp = new Timestamp(selectedDateCalendar.getTime());

                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
                        tvSelectedDate.setText(getString(R.string.selected_date_label, sdf.format(selectedDateCalendar.getTime())));
                        tvSelectedDate.setTextColor(getColor(R.color.colorOnBackground));
                    },
                    year,
                    month,
                    day
            );

            dialog.show();
        });
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
        String title = etEventTitle.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String capacityText = etCapacity.getText().toString().trim();
        String trailerUrl = etTrailerUrl.getText().toString().trim();

        if (TextUtils.isEmpty(title)
                || TextUtils.isEmpty(category)
                || TextUtils.isEmpty(venue)
                || TextUtils.isEmpty(description)
                || TextUtils.isEmpty(capacityText)
                || selectedTimestamp == null) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
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

        List<String> tags = new ArrayList<>();
        tags.add(category.toLowerCase(Locale.getDefault()));

        EventProposal proposal = new EventProposal();
        proposal.setTitle(title);
        proposal.setDescription(description);
        proposal.setCategory(category);
        proposal.setTags(tags);
        proposal.setDate(selectedTimestamp);
        proposal.setLocation(venue);
        proposal.setCapacity(capacity);
        proposal.setSponsors(new ArrayList<>());
        proposal.setFoodStalls(new ArrayList<>());
        proposal.setTrailerUrl(trailerUrl);
        proposal.setOrganizerId(currentUserId);
        proposal.setOrganizerName(TextUtils.isEmpty(currentOrganizerName) ? "Organizer" : currentOrganizerName);
        proposal.setStatus("pending");
        proposal.setAdminNote("");
        proposal.setSubmittedAt(Timestamp.now());
        proposal.setReviewedAt(null);

        showLoading(true);

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
}
