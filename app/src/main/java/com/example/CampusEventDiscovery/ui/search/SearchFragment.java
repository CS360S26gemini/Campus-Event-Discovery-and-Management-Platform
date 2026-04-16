package com.example.CampusEventDiscovery.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.EventAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SearchFragment.java
 *
 * Firestore-backed event search screen with text debounce and category filters.
 */
public class SearchFragment extends Fragment {

    private static final long SEARCH_DEBOUNCE_MILLIS = 300L;

    private EditText etSearch;
    private TextView tvEventsCount;
    private TextView tvEmptySearch;
    private ProgressBar progressBarSearch;
    private RecyclerView rvResults;

    private Chip chipAll;
    private Chip chipMusic;
    private Chip chipSports;
    private Chip chipCareer;
    private Chip chipAcademic;
    private Chip chipArts;
    private Chip chipBusiness;
    private Chip chipFoodBev;

    private EventRepository repository;
    private EventAdapter adapter;

    private final List<Event> resultList = new ArrayList<>();
    private final Set<String> savedEventIds = new HashSet<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private String selectedCategory = "All";
    private String currentUserId;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(requireContext());

        etSearch = view.findViewById(R.id.etSearch);
        tvEventsCount = view.findViewById(R.id.tvEventsCount);
        tvEmptySearch = view.findViewById(R.id.tvEmptySearch);
        progressBarSearch = view.findViewById(R.id.progressBarSearch);
        rvResults = view.findViewById(R.id.rvResults);

        chipAll = view.findViewById(R.id.chipAll);
        chipMusic = view.findViewById(R.id.chipMusic);
        chipSports = view.findViewById(R.id.chipSports);
        chipCareer = view.findViewById(R.id.chipCareer);
        chipAcademic = view.findViewById(R.id.chipAcademic);
        chipArts = view.findViewById(R.id.chipArts);
        chipBusiness = view.findViewById(R.id.chipBusiness);
        chipFoodBev = view.findViewById(R.id.chipFoodBev);

        setupRecyclerView();
        setupSearchInput();
        setupCategoryChips();
        loadSavedEvents();
        chipAll.setChecked(true);
        performSearch();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadSavedEvents();
            performSearch();
        }
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(
                resultList,
                savedEventIds,
                currentUserId,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        if (event == null || event.getEventId() == null || !isAdded()) {
                            return;
                        }

                        Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                        intent.putExtra("eventId", event.getEventId());
                        startActivity(intent);
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                        if (currentUserId == null || event == null || event.getEventId() == null || !isAdded()) {
                            return;
                        }

                        if (isCurrentlySaved) {
                            repository.unsaveEvent(currentUserId, event.getEventId(), () -> {
                                if (isAdded()) {
                                    savedEventIds.remove(event.getEventId());
                                    adapter.updateSavedIds(savedEventIds);
                                }
                            });
                        } else {
                            repository.saveEvent(currentUserId, event, new EventRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    if (isAdded()) {
                                        savedEventIds.add(event.getEventId());
                                        adapter.updateSavedIds(savedEventIds);
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        // no-op for search screen
                    }
                }
        );

        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(adapter);
    }

    private void setupSearchInput() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                debounceSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });
    }

    private void setupCategoryChips() {
        chipAll.setOnClickListener(v -> selectCategory("All"));
        chipMusic.setOnClickListener(v -> selectCategory("Music"));
        chipSports.setOnClickListener(v -> selectCategory("Sports"));
        chipCareer.setOnClickListener(v -> selectCategory("Career"));
        chipAcademic.setOnClickListener(v -> selectCategory("Academic"));
        chipArts.setOnClickListener(v -> selectCategory("Arts"));
        chipBusiness.setOnClickListener(v -> selectCategory("Business"));
        chipFoodBev.setOnClickListener(v -> selectCategory("Food & Bev"));
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        performSearch();
    }

    private void debounceSearch() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        searchRunnable = this::performSearch;
        searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MILLIS);
    }

    private void performSearch() {
        showLoading(true);

        String query = etSearch.getText().toString().trim();

        repository.searchEvents(query, selectedCategory, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!isAdded()) return;
                showLoading(false);

                adapter.updateData(events);
                tvEventsCount.setText(getString(R.string.events_count_label, events.size()));

                if (events.isEmpty()) {
                    tvEmptySearch.setVisibility(View.VISIBLE);
                } else {
                    tvEmptySearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
                adapter.updateData(new ArrayList<>());
                tvEventsCount.setText(getString(R.string.events_count_label, 0));
                tvEmptySearch.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadSavedEvents() {
        if (currentUserId == null) {
            adapter.updateSavedIds(savedEventIds);
            return;
        }

        repository.getSavedEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!isAdded()) return;
                savedEventIds.clear();

                for (Event event : events) {
                    if (event.getEventId() != null) {
                        savedEventIds.add(event.getEventId());
                    }
                }

                adapter.updateSavedIds(savedEventIds);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                savedEventIds.clear();
                adapter.updateSavedIds(savedEventIds);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBarSearch != null) {
            progressBarSearch.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
