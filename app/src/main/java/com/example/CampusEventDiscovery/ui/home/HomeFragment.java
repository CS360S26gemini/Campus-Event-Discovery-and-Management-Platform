package com.example.CampusEventDiscovery.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.EventAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private TextView tvRecommendedLabel;
    private TextView tvRecommendedSubtitle;
    private HorizontalScrollView recommendedEventsScroll;
    private LinearLayout recommendedEventsContainer;
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
    private final Handler recommendationCarouselHandler = new Handler(Looper.getMainLooper());
    private final Runnable recommendationCarouselRunnable = new Runnable() {
        @Override
        public void run() {
            advanceRecommendationCarousel();
            recommendationCarouselHandler.postDelayed(this, 3000L);
        }
    };
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
        tvRecommendedLabel = view.findViewById(R.id.tvRecommendedLabel);
        tvRecommendedSubtitle = view.findViewById(R.id.tvRecommendedSubtitle);
        recommendedEventsScroll = view.findViewById(R.id.recommendedEventsScroll);
        recommendedEventsContainer = view.findViewById(R.id.recommendedEventsContainer);
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
        WalkthroughManager.maybeShow(requireActivity(), view, "home_attendee");
    }

    @Override
    public void onResume() {
        super.onResume();
        startRecommendationCarousel();
        if (getView() != null) {
            loadHomeData();
            WalkthroughManager.maybeShow(requireActivity(), getView(), "home_attendee");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        recommendationCarouselHandler.removeCallbacks(recommendationCarouselRunnable);
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
            hideRecommendations();
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

                String name = user == null ? null : user.getFullName();
                if (TextUtils.isEmpty(name)) {
                    tvWelcome.setText(getString(R.string.welcome_back));
                } else {
                    tvWelcome.setText(getString(R.string.welcome_back) + "\n" + name);
                }

                loadFeaturedEvent();
                loadRecommendations(user == null ? null : user.getInterests());

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
                hideRecommendations();

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
        ivBannerHeart.setColorFilter(isSaved
                ? ThemeManager.getAccentColor(requireContext())
                : ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceVariant));

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
                if (!isAdded()) return;
                savedEventIds.remove(event.getEventId());
                adapter.updateSavedIds(savedEventIds);
                bindFeaturedEvent(featuredEvent);
            });
        } else {
            repository.saveEvent(currentUserId, event, new EventRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    savedEventIds.add(event.getEventId());
                    adapter.updateSavedIds(savedEventIds);
                    bindFeaturedEvent(featuredEvent);
                }

                @Override
                public void onError(Exception e) {
                    if (!isAdded()) return;
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
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), getString(R.string.sos_sent), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
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

    private void loadRecommendations(List<String> interests) {
        if (currentUserId == null) {
            hideRecommendations();
            return;
        }

        List<String> recentlyViewedIds = readRecentlyViewedIds();
        repository.getScoredRecommendations(interests, recentlyViewedIds, new EventRepository.RecommendationCallback() {
            @Override
            public void onSuccess(List<Event> events, boolean trendingFallback, String topCategory) {
                if (!isAdded()) return;
                bindRecommendedEvents(events, trendingFallback, topCategory);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                hideRecommendations();
            }
        });
    }

    private List<String> readRecentlyViewedIds() {
        String raw = requireContext()
                .getSharedPreferences(Constants.PREFS_RECENTLY_VIEWED, Context.MODE_PRIVATE)
                .getString(Constants.PREFS_RECENTLY_VIEWED_KEY, "");
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        if (!TextUtils.isEmpty(raw)) {
            String[] ids = raw.split(",");
            for (String id : ids) {
                String value = id == null ? "" : id.trim();
                if (!TextUtils.isEmpty(value)) {
                    deduped.add(value);
                }
                if (deduped.size() == 5) {
                    break;
                }
            }
        }
        return new ArrayList<>(deduped);
    }

    private void bindRecommendedEvents(List<Event> events, boolean trendingFallback, String topCategory) {
        if (recommendedEventsContainer == null
                || recommendedEventsScroll == null
                || tvRecommendedLabel == null
                || tvRecommendedSubtitle == null) {
            return;
        }

        recommendedEventsContainer.removeAllViews();

        if (events == null || events.isEmpty()) {
            hideRecommendations();
            return;
        }

        tvRecommendedLabel.setVisibility(View.VISIBLE);
        tvRecommendedSubtitle.setVisibility(View.VISIBLE);
        recommendedEventsScroll.setVisibility(View.VISIBLE);
        if (trendingFallback) {
            tvRecommendedLabel.setText(R.string.trending_on_campus);
            tvRecommendedSubtitle.setText(R.string.popular_events_across_campus);
        } else {
            tvRecommendedLabel.setText(R.string.recommended_for_you);
            tvRecommendedSubtitle.setText(getString(R.string.based_on_interest, safeText(topCategory, getString(R.string.app_name))));
        }

        int max = Math.min(5, events.size());

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < max; i++) {
            Event event = events.get(i);
            View card = inflater.inflate(R.layout.item_event_card_carousel, recommendedEventsContainer, false);
            bindRecommendationCard(card, event, i < max - 1);
            recommendedEventsContainer.addView(card);
        }

        recommendedEventsScroll.post(() -> recommendedEventsScroll.scrollTo(0, 0));
        startRecommendationCarousel();
    }

    private void bindRecommendationCard(View card, Event event, boolean addTrailingGap) {
        ImageView thumbnail = card.findViewById(R.id.ivCarouselThumbnail);
        ImageView placeholder = card.findViewById(R.id.ivCarouselPlaceholderIcon);
        TextView title = card.findViewById(R.id.tvCarouselTitle);
        TextView date = card.findViewById(R.id.tvCarouselDate);
        TextView attendees = card.findViewById(R.id.tvCarouselAttendees);

        title.setText(safeText(event.getTitle(), getString(R.string.app_name)));
        date.setText(formatDateTime(event.getDate()));
        attendees.setText(event.getRsvpCount() + " registered");

        if (!TextUtils.isEmpty(event.getThumbnailUrl())) {
            placeholder.setVisibility(View.GONE);
            Glide.with(requireContext())
                    .load(event.getThumbnailUrl())
                    .placeholder(R.drawable.bg_placeholder_image)
                    .centerCrop()
                    .into(thumbnail);
        } else {
            thumbnail.setImageResource(0);
            thumbnail.setBackgroundResource(R.drawable.bg_placeholder_image);
            placeholder.setVisibility(View.VISIBLE);
        }

        MarginLayoutParams layoutParams = (MarginLayoutParams) card.getLayoutParams();
        layoutParams.setMarginEnd(addTrailingGap ? dpToPx(12) : 0);
        card.setLayoutParams(layoutParams);
        card.setOnClickListener(v -> openEventDetail(event));
    }

    private void hideRecommendations() {
        if (tvRecommendedLabel != null) {
            tvRecommendedLabel.setVisibility(View.GONE);
        }
        if (tvRecommendedSubtitle != null) {
            tvRecommendedSubtitle.setVisibility(View.GONE);
        }
        if (recommendedEventsScroll != null) {
            recommendedEventsScroll.setVisibility(View.GONE);
        }
        if (recommendedEventsContainer != null) {
            recommendedEventsContainer.removeAllViews();
        }
        recommendationCarouselHandler.removeCallbacks(recommendationCarouselRunnable);
    }

    private void startRecommendationCarousel() {
        recommendationCarouselHandler.removeCallbacks(recommendationCarouselRunnable);
        if (recommendedEventsContainer != null && recommendedEventsContainer.getChildCount() > 1) {
            recommendationCarouselHandler.postDelayed(recommendationCarouselRunnable, 3000L);
        }
    }

    private void advanceRecommendationCarousel() {
        if (recommendedEventsScroll == null || recommendedEventsContainer == null || recommendedEventsContainer.getChildCount() < 2) {
            return;
        }

        int maxScroll = Math.max(0, recommendedEventsContainer.getWidth() - recommendedEventsScroll.getWidth());
        if (maxScroll == 0) {
            return;
        }

        int cardSpan = getResources().getDimensionPixelSize(R.dimen.event_carousel_card_width) + dpToPx(12);
        int nextScrollX = recommendedEventsScroll.getScrollX() + cardSpan;
        if (nextScrollX >= maxScroll) {
            nextScrollX = 0;
        }
        recommendedEventsScroll.smoothScrollTo(nextScrollX, 0);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
