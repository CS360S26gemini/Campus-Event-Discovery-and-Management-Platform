package com.example.campuseventdiscovery.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    private EventAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();
    private final Set<String> savedEventIds = new HashSet<>();

    private String currentUserId;
    private User currentUser;
    private Event featuredEvent;
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = false;
                if (result != null) {
                    for (Boolean value : result.values()) {
                        if (Boolean.TRUE.equals(value)) {
                            granted = true;
                            break;
                        }
                    }
                }

                if (!isAdded()) {
                    return;
                }

                if (!granted) {
                    Toast.makeText(requireContext(), getString(R.string.sos_location_unavailable), Toast.LENGTH_SHORT).show();
                }

                sendSosWithLocation();
            });

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
        currentUserId = firebaseUser != null ? firebaseUser.getUid() : DevSessionManager.getEffectiveUserId(requireContext());

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
            tvWelcome.setText(getString(R.string.welcome_back));
            btnSos.setEnabled(false);
            featuredCardContainer.setVisibility(View.GONE);
            showLoading(false);
            updateEventList(new ArrayList<>());
            tvEmpty.setText(getString(R.string.no_events_found));
            tvEmpty.setVisibility(View.VISIBLE);
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
                bindFeaturedEvent(featuredEvent);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                savedEventIds.clear();
                adapter.updateSavedIds(savedEventIds);
            }
        });

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;
                currentUser = user;

                String name = user.getFullName();
                if (TextUtils.isEmpty(name)) {
                    tvWelcome.setText(getString(R.string.welcome_back));
                } else {
                    tvWelcome.setText(getString(R.string.welcome_back) + "\n" + name);
                }

                loadFeaturedEvent();

                repository.getUpcomingEvents(new EventRepository.EventListCallback() {
                    @Override
                    public void onSuccess(List<Event> events) {
                        if (!isAdded()) return;
                        showLoading(false);
                        updateEventList(events);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        showLoading(false);
                        tvEmpty.setVisibility(View.VISIBLE);
                        updateEventList(new ArrayList<>());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                if (DevSessionManager.shouldUseBypass(requireContext())) {
                    tvWelcome.setText(getString(R.string.welcome_back) + "\n" + DevSessionManager.getDisplayName(requireContext()));
                } else {
                    tvWelcome.setText(getString(R.string.welcome_back));
                }
                loadFeaturedEvent();

                repository.getUpcomingEvents(new EventRepository.EventListCallback() {
                    @Override
                    public void onSuccess(List<Event> events) {
                        if (!isAdded()) return;
                        showLoading(false);
                        updateEventList(events);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        showLoading(false);
                        tvEmpty.setVisibility(View.VISIBLE);
                        updateEventList(new ArrayList<>());
                    }
                });
            }
        });
    }

    private void loadFeaturedEvent() {
        repository.getFeaturedEventIds(new EventRepository.FeaturedEventIdsCallback() {
            @Override
            public void onSuccess(List<String> featuredIds) {
                if (!isAdded()) return;
                if (featuredIds == null || featuredIds.isEmpty()) {
                    featuredCardContainer.setVisibility(View.GONE);
                    return;
                }

                repository.getFeaturedEvents(featuredIds, new EventRepository.EventListCallback() {
                    @Override
                    public void onSuccess(List<Event> events) {
                        if (!isAdded()) return;
                        if (events == null || events.isEmpty()) {
                            featuredCardContainer.setVisibility(View.GONE);
                            return;
                        }

                        featuredEvent = events.get(0);
                        bindFeaturedEvent(featuredEvent);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        featuredCardContainer.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                featuredCardContainer.setVisibility(View.GONE);
            }
        });
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
                    .setPositiveButton(R.string.send, (dialog, which) -> requestLocationAndSendSos())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void requestLocationAndSendSos() {
        if (hasAnyLocationPermission()) {
            sendSosWithLocation();
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void sendSosWithLocation() {
        Location location = getBestLastKnownLocation();
        double latitude = location != null ? location.getLatitude() : 0.0;
        double longitude = location != null ? location.getLongitude() : 0.0;

        repository.sendSosReport(
                currentUserId,
                currentUser != null ? currentUser.getFullName() : DevSessionManager.getDisplayName(requireContext()),
                "SOS triggered",
                latitude,
                longitude,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), getString(R.string.sos_sent), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(), "Failed to send SOS.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private boolean hasAnyLocationPermission() {
        Context context = requireContext();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private Location getBestLastKnownLocation() {
        if (!hasAnyLocationPermission()) {
            return null;
        }

        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        Location bestLocation = null;
        try {
            List<String> providers = locationManager.getProviders(true);
            for (String provider : providers) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) {
                    continue;
                }

                if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = location;
                }
            }
        } catch (SecurityException ignored) {
            return null;
        }

        return bestLocation;
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
