package com.example.CampusEventDiscovery.ui.favourites;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FavouritesFragment.java
 *
 * Displays the current user's saved events from Firestore.
 */
public class FavouritesFragment extends Fragment {

    private TextView tvFavouritesBadge;
    private TextView tvEmptyFavourites;
    private ProgressBar progressBarFavourites;
    private RecyclerView rvFavourites;

    private EventRepository repository;
    private EventAdapter adapter;

    private final List<Event> favouriteEvents = new ArrayList<>();
    private final Set<String> savedEventIds = new HashSet<>();

    private String currentUserId;

    public FavouritesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null
                ? currentUser.getUid()
                : DevSessionManager.getEffectiveUserId(requireContext());

        tvFavouritesBadge = view.findViewById(R.id.tvFavouritesBadge);
        tvEmptyFavourites = view.findViewById(R.id.tvEmptyFavourites);
        progressBarFavourites = view.findViewById(R.id.progressBarFavourites);
        rvFavourites = view.findViewById(R.id.rvFavourites);

        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavourites();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(
                favouriteEvents,
                savedEventIds,
                currentUserId,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        if (event == null || event.getEventId() == null || !canUseView()) {
                            return;
                        }

                        Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                        intent.putExtra("eventId", event.getEventId());
                        startActivity(intent);
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                        if (currentUserId == null || event == null || event.getEventId() == null || !canUseView()) {
                            return;
                        }

                        repository.unsaveEvent(currentUserId, event.getEventId(), () -> {
                            if (canUseView()) {
                                removeEventFromList(event.getEventId());
                            }
                        });
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        // no-op for favourites screen
                    }
                },
                true
        );

        rvFavourites.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavourites.setAdapter(adapter);
    }

    private void loadFavourites() {
        showLoading(true);

        if (currentUserId == null) {
            favouriteEvents.clear();
            savedEventIds.clear();
            adapter.updateSavedIds(savedEventIds);
            adapter.updateData(favouriteEvents);
            updateUiState();
            showLoading(false);
            return;
        }

        repository.getSavedEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!canUseView()) {
                    return;
                }

                favouriteEvents.clear();
                savedEventIds.clear();

                if (events != null) {
                    favouriteEvents.addAll(events);
                    for (Event event : events) {
                        if (event.getEventId() != null) {
                            savedEventIds.add(event.getEventId());
                        }
                    }
                }

                adapter.updateSavedIds(savedEventIds);
                adapter.updateData(new ArrayList<>(favouriteEvents));

                updateUiState();
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                if (!canUseView()) {
                    return;
                }

                favouriteEvents.clear();
                savedEventIds.clear();
                adapter.updateSavedIds(savedEventIds);
                adapter.updateData(favouriteEvents);

                updateUiState();
                showLoading(false);
            }
        });
    }

    private void removeEventFromList(String eventId) {
        Event target = null;

        for (Event event : favouriteEvents) {
            if (eventId.equals(event.getEventId())) {
                target = event;
                break;
            }
        }

        if (target != null) {
            favouriteEvents.remove(target);
        }

        savedEventIds.remove(eventId);

        adapter.updateSavedIds(savedEventIds);
        adapter.updateData(new ArrayList<>(favouriteEvents));
        updateUiState();
    }

    private void updateUiState() {
        tvFavouritesBadge.setText(getString(R.string.favourites_badge_count, favouriteEvents.size()));

        if (favouriteEvents.isEmpty()) {
            tvEmptyFavourites.setVisibility(View.VISIBLE);
            rvFavourites.setVisibility(View.GONE);
        } else {
            tvEmptyFavourites.setVisibility(View.GONE);
            rvFavourites.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBarFavourites != null) {
            progressBarFavourites.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private boolean canUseView() {
        return isAdded() && getView() != null;
    }
}
