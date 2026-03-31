package com.example.campuseventdiscovery.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.adapter.EventAdapter;
import com.example.campuseventdiscovery.model.Event;
import com.example.campuseventdiscovery.model.User;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.ui.event.EventDetailActivity;
import com.example.campuseventdiscovery.util.DevSessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HomeFragment.java
 *
 * Student home screen showing featured events and personalized event recommendations.
 */
public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private MaterialButton btnSos;
    private View featuredCardContainer;
    private MaterialCardView cardFeaturedEvent;
    private RecyclerView rvEvents;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private ImageView ivBanner;
    private ImageView ivBannerHeart;
    private ImageView ivBannerShare;
    private TextView tvBannerTitle;
    private TextView tvBannerDate;
    private TextView tvBannerVenue;

    private EventRepository repository;
    private FirebaseFirestore db;

    private EventAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();
    private final Set<String> savedEventIds = new HashSet<>();

    private String currentUserId;
    private User currentUser;
    private Event featuredEvent;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();
        db = FirebaseFirestore.getInstance();

        tvWelcome = view.findViewById(R.id.tvWelcome);
        btnSos = view.findViewById(R.id.btnSos);
        featuredCardContainer = view.findViewById(R.id.featuredCardContainer);
        rvEvents = view.findViewById(R.id.rvEvents);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        ivBanner = featuredCardContainer.findViewById(R.id.ivBanner);
        cardFeaturedEvent = featuredCardContainer.findViewById(R.id.cardFeaturedEvent);
        ivBannerHeart = featuredCardContainer.findViewById(R.id.ivBannerHeart);
        ivBannerShare = featuredCardContainer.findViewById(R.id.ivBannerShare);
        tvBannerTitle = featuredCardContainer.findViewById(R.id.tvBannerTitle);
        tvBannerDate = featuredCardContainer.findViewById(R.id.tvBannerDate);
        tvBannerVenue = featuredCardContainer.findViewById(R.id.tvBannerVenue);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null
                ? firebaseUser.getUid()
                : DevSessionManager.getEffectiveUserId(requireContext());

        setupRecyclerView();
        setupSosButton();
        loadHomeData();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(
                eventList,
                savedEventIds,
                currentUserId,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        openEventDetail(event);
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                        toggleSaveEvent(event, isCurrentlySaved);
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        // no-op for home screen
                    }
                }
        );

        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setNestedScrollingEnabled(false);
        rvEvents.setAdapter(adapter);
    }

    private void loadHomeData() {
        showLoading(true);

        if (currentUserId == null) {
            tvWelcome.setText(getString(R.string.welcome_back) + "\nDev User");
            btnSos.setEnabled(false);
            featuredCardContainer.setVisibility(View.GONE);
            showLoading(false);
            updateEventList(new ArrayList<>());
            tvEmpty.setText("Dev bypass active - no Firebase user signed in.");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        repository.getSavedEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                savedEventIds.clear();
                for (Event event : events) {
                    if (event.getEventId() != null) {
                        savedEventIds.add(event.getEventId());
                    }
                }
                adapter.updateSavedIds(savedEventIds);
                bindFeaturedEvent(featuredEvent);
            }

            @Override
            public void onError(Exception e) {
                savedEventIds.clear();
                adapter.updateSavedIds(savedEventIds);
            }
        });

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;

                String name = user.getFullName();
                if (TextUtils.isEmpty(name)) {
                    tvWelcome.setText(getString(R.string.welcome_back));
                } else {
                    tvWelcome.setText(getString(R.string.welcome_back) + "\n" + name);
                }

                loadFeaturedEvent();

                repository.getPersonalisedEvents(user.getInterests(), new EventRepository.EventListCallback() {
                    @Override
                    public void onSuccess(List<Event> events) {
                        showLoading(false);
                        updateEventList(events);
                    }

                    @Override
                    public void onError(Exception e) {
                        showLoading(false);
                        tvEmpty.setVisibility(View.VISIBLE);
                        updateEventList(new ArrayList<>());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (DevSessionManager.shouldUseBypass(requireContext())) {
                    tvWelcome.setText(getString(R.string.welcome_back) + "\n" + DevSessionManager.getDisplayName(requireContext()));
                } else {
                    tvWelcome.setText(getString(R.string.welcome_back));
                }
                loadFeaturedEvent();

                repository.getUpcomingEvents(new EventRepository.EventListCallback() {
                    @Override
                    public void onSuccess(List<Event> events) {
                        showLoading(false);
                        updateEventList(events);
                    }

                    @Override
                    public void onError(Exception e) {
                        showLoading(false);
                        tvEmpty.setVisibility(View.VISIBLE);
                        updateEventList(new ArrayList<>());
                    }
                });
            }
        });
    }

    private void loadFeaturedEvent() {
        db.collection("app_config")
                .document("settings")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> featuredIds = (List<String>) documentSnapshot.get("featuredEventIds");

                    if (featuredIds == null || featuredIds.isEmpty()) {
                        featuredCardContainer.setVisibility(View.GONE);
                        return;
                    }

                    repository.getFeaturedEvents(featuredIds, new EventRepository.EventListCallback() {
                        @Override
                        public void onSuccess(List<Event> events) {
                            if (events == null || events.isEmpty()) {
                                featuredCardContainer.setVisibility(View.GONE);
                                return;
                            }

                            featuredEvent = events.get(0);
                            bindFeaturedEvent(featuredEvent);
                        }

                        @Override
                        public void onError(Exception e) {
                            featuredCardContainer.setVisibility(View.GONE);
                        }
                    });
                })
                .addOnFailureListener(e -> featuredCardContainer.setVisibility(View.GONE));
    }

    private void bindFeaturedEvent(Event event) {
        if (event == null) {
            featuredCardContainer.setVisibility(View.GONE);
            return;
        }

        featuredCardContainer.setVisibility(View.VISIBLE);

        tvBannerTitle.setText(safeText(event.getTitle(), getString(R.string.app_name)));
        tvBannerDate.setText(formatDateTime(event.getDate()));
        tvBannerVenue.setText(safeText(event.getLocation(), getString(R.string.placeholder_venue)));

        boolean isSaved = event.getEventId() != null && savedEventIds.contains(event.getEventId());
        ivBannerHeart.setImageResource(isSaved ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        if (!TextUtils.isEmpty(event.getThumbnailUrl())) {
            Glide.with(requireContext())
                    .load(event.getThumbnailUrl())
                    .placeholder(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(ivBanner);
        } else {
            ivBanner.setImageResource(0);
            ivBanner.setBackgroundResource(R.drawable.bg_placeholder_image);
        }

        cardFeaturedEvent.setOnClickListener(v -> openEventDetail(event));

        ivBannerHeart.setOnClickListener(v -> toggleSaveEvent(event, isSaved));

        ivBannerShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    safeText(event.getTitle(), getString(R.string.app_name))
                            + "\n"
                            + formatDateTime(event.getDate())
                            + "\n"
                            + safeText(event.getLocation(), getString(R.string.placeholder_venue)));
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
        });
    }

    private void toggleSaveEvent(Event event, boolean isCurrentlySaved) {
        if (currentUserId == null || event == null || event.getEventId() == null) {
            return;
        }

        if (isCurrentlySaved) {
            repository.unsaveEvent(currentUserId, event.getEventId(), () -> {
                savedEventIds.remove(event.getEventId());
                adapter.updateSavedIds(savedEventIds);
                bindFeaturedEvent(featuredEvent);
            });
        } else {
            repository.saveEvent(currentUserId, event, new EventRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    savedEventIds.add(event.getEventId());
                    adapter.updateSavedIds(savedEventIds);
                    bindFeaturedEvent(featuredEvent);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(requireContext(), "Failed to save event.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openEventDetail(Event event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        Intent intent = new Intent(requireContext(), EventDetailActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }

    private void setupSosButton() {
        btnSos.setOnClickListener(v -> {
            if (currentUserId == null) {
                return;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.send_sos_title))
                    .setMessage(getString(R.string.send_sos_message))
                    .setPositiveButton("Send", (dialog, which) -> sendSos())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void sendSos() {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reporterId", currentUserId);
        reportData.put("reporterName", currentUser != null ? currentUser.getFullName() : "");
        reportData.put("isAnonymous", false);
        reportData.put("description", "SOS triggered");
        reportData.put("status", "open");
        reportData.put("submittedAt", Timestamp.now());

        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(requireContext(), getString(R.string.sos_sent), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to send SOS.", Toast.LENGTH_SHORT).show());
    }

    private void updateEventList(List<Event> events) {
        adapter.updateData(events);

        if (events == null || events.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return getString(R.string.placeholder_date);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    private String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }
}
