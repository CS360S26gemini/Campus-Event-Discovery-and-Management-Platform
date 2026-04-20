package com.example.CampusEventDiscovery.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.MemoryAdapter;
import com.example.CampusEventDiscovery.model.Memory;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the current user's saved memories.
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
        loadMemories();
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
        adapter = new MemoryAdapter(memories);
        rvMemories.setLayoutManager(new LinearLayoutManager(this));
        rvMemories.setAdapter(adapter);
    }

    private void loadMemories() {
        setLoading(true);

        if (TextUtils.isEmpty(currentUserId)) {
            bindMemories(new ArrayList<>());
            return;
        }

        repository.getMemories(currentUserId, new EventRepository.MemoryListCallback() {
            @Override
            public void onSuccess(List<Memory> memories) {
                bindMemories(memories);
            }

            @Override
            public void onError(Exception e) {
                bindMemories(new ArrayList<>());
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

    private void setLoading(boolean isLoading) {
        progressBarMemories.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
