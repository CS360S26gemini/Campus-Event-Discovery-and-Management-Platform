package com.example.campuseventdiscovery.ui.organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.adapter.AttendeeAdapter;
import com.example.campuseventdiscovery.model.EventAttendee;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * WhoIsComingActivity.java
 *
 * Screen for organizers to see the list of registered participants and take actions like blacklisting.
 */
public class WhoIsComingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private EditText etSearchParticipants;
    private RecyclerView rvParticipants;
    private TextView tvEmptyParticipants;
    private MaterialButton btnBlacklist;
    private EventRepository repository;
    private AttendeeAdapter adapter;
    private String eventId;
    private String eventTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_who_is_coming);

        repository = new EventRepository();
        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");

        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        etSearchParticipants = findViewById(R.id.etSearchParticipants);
        rvParticipants = findViewById(R.id.rvParticipants);
        tvEmptyParticipants = findViewById(R.id.tvEmptyParticipants);
        btnBlacklist = findViewById(R.id.btnBlacklist);

        btnBack.setOnClickListener(v -> finish());
        tvTitle.setText(TextUtils.isEmpty(eventTitle) ? getString(R.string.who_is_coming) : eventTitle);

        setupRecyclerView();
        setupSearch();
        setupBlacklistAction();
        loadParticipants();
    }

    private void setupRecyclerView() {
        adapter = new AttendeeAdapter(selectedCount -> btnBlacklist.setEnabled(selectedCount > 0));
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        rvParticipants.setAdapter(adapter);
        btnBlacklist.setEnabled(false);
    }

    private void setupSearch() {
        etSearchParticipants.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });
    }

    private void setupBlacklistAction() {
        btnBlacklist.setOnClickListener(v -> Toast.makeText(
                this,
                getString(R.string.blacklist_unavailable),
                Toast.LENGTH_SHORT
        ).show());
    }

    private void loadParticipants() {
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, getString(R.string.error_loading_participants), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository.getEventAttendees(eventId, new EventRepository.AttendeeListCallback() {
            @Override
            public void onSuccess(List<EventAttendee> attendees) {
                adapter.updateData(attendees);
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(WhoIsComingActivity.this, getString(R.string.error_loading_participants), Toast.LENGTH_SHORT).show();
                adapter.updateData(null);
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        tvEmptyParticipants.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvParticipants.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
