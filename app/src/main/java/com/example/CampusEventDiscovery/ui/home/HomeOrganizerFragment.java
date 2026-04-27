package com.example.CampusEventDiscovery.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.sos.SOSDashboardActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HomeOrganizerFragment.java
 *
 * Organizer home screen showing featured events, personalized recommendations,
 * and a quick access button to create a new event.
 */
public class HomeOrganizerFragment extends Fragment {

    private TextView tvOrganizerWelcome;
    private MaterialButton btnCreateEvent;
    private MaterialButton btnManageEvents;
    private MaterialButton btnScanTickets;
    private MaterialButton btnSosDashboard;
    private TextView tvPopularLabel;
    private HorizontalScrollView popularEventsScroll;
    private LinearLayout popularEventsContainer;
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
    private String currentUserId;
    private User currentUser;
    private Event featuredEvent;
    private final Handler popularCarouselHandler = new Handler(Looper.getMainLooper());
    private final Runnable popularCarouselRunnable = new Runnable() {
        @Override
        public void run() {
            advancePopularCarousel();
            popularCarouselHandler.postDelayed(this, 3000L);
        }
    };

    public HomeOrganizerFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_organizer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        tvOrganizerWelcome = view.findViewById(R.id.tvOrganizerWelcome);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        btnManageEvents = view.findViewById(R.id.btnManageEvents);
        btnScanTickets = view.findViewById(R.id.btnScanTickets);
        btnSosDashboard = view.findViewById(R.id.btnSosDashboard);
        tvPopularLabel = view.findViewById(R.id.tvPopularLabel);
        popularEventsScroll = view.findViewById(R.id.popularEventsScroll);
        popularEventsContainer = view.findViewById(R.id.popularEventsContainer);
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
        setupCreateEventButton();
        loadHomeData();
        WalkthroughManager.maybeShow(requireActivity(), view, "home_organizer");
    }

    @Override
    public void onResume() {
        super.onResume();
        startPopularCarousel();
        if (getView() != null) {
            loadHomeData();
            WalkthroughManager.maybeShow(requireActivity(), getView(), "home_organizer");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        popularCarouselHandler.removeCallbacks(popularCarouselRunnable);
    }

    @Override
    public void onDestroyView() {
        popularCarouselHandler.removeCallbacks(popularCarouselRunnable);
        if (rvEvents != null) {
            rvEvents.setAdapter(null);
        }
        if (popularEventsContainer != null) {
            popularEventsContainer.removeAllViews();
        }
        adapter = null;
        repository = null;
        super.onDestroyView();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(
                eventList,
                new java.util.HashSet<>(),
                currentUserId,
                new EventAdapter.OnEventClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        openEventDetail(event);
                    }

                    @Override
                    public void onHeartClick(Event event, boolean isCurrentlySaved) {
                        // Favourites are attendee-only.
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        // no-op
                    }
                },
                false,
                false
        );

        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setNestedScrollingEnabled(false);
        rvEvents.setAdapter(adapter);
    }

    private void setupCreateEventButton() {
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });
        btnManageEvents.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageEventsActivity.class)));
        btnScanTickets.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ScannerActivity.class)));
        btnSosDashboard.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SOSDashboardActivity.class)));
    }

    private void loadHomeData() {
        showLoading(true);

        if (currentUserId == null) {
            tvOrganizerWelcome.setText(getString(R.string.organizer_welcome_back));
            featuredCardContainer.setVisibility(View.GONE);
            showLoading(false);
            updateEventList(new ArrayList<>());
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;
                currentUser = user;

                String name = user == null ? null : user.getFullName();
                if (TextUtils.isEmpty(name)) {
                    tvOrganizerWelcome.setText(getString(R.string.organizer_welcome_back));
                } else {
                    tvOrganizerWelcome.setText(getString(R.string.organizer_welcome_back) + "\n" + name);
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
                    tvOrganizerWelcome.setText(getString(R.string.organizer_welcome_back) + "\n" + DevSessionManager.getDisplayName(requireContext()));
                } else {
                    tvOrganizerWelcome.setText(getString(R.string.organizer_welcome_back));
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

        ivBannerHeart.setVisibility(View.GONE);

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

    private void openEventDetail(Event event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        Intent intent = new Intent(requireContext(), EventDetailActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }

    private void updateEventList(List<Event> events) {
        adapter.updateData(events);
        bindPopularEvents(events);

        if (events == null || events.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void bindPopularEvents(List<Event> events) {
        if (popularEventsContainer == null || popularEventsScroll == null || tvPopularLabel == null) {
            return;
        }

        popularEventsContainer.removeAllViews();

        if (events == null || events.isEmpty()) {
            tvPopularLabel.setVisibility(View.GONE);
            popularEventsScroll.setVisibility(View.GONE);
            popularCarouselHandler.removeCallbacks(popularCarouselRunnable);
            return;
        }

        tvPopularLabel.setVisibility(View.VISIBLE);
        popularEventsScroll.setVisibility(View.VISIBLE);

        List<Event> popular = new ArrayList<>(events);
        popular.sort((first, second) -> Long.compare(second.getRsvpCount(), first.getRsvpCount()));
        int max = Math.min(5, popular.size());

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < max; i++) {
            Event event = popular.get(i);
            View card = inflater.inflate(R.layout.item_event_card_carousel, popularEventsContainer, false);
            bindPopularCard(card, event, i < max - 1);
            popularEventsContainer.addView(card);
        }

        popularEventsScroll.post(() -> popularEventsScroll.scrollTo(0, 0));
        startPopularCarousel();
    }

    private void bindPopularCard(View card, Event event, boolean addTrailingGap) {
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

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
        layoutParams.setMarginEnd(addTrailingGap ? dpToPx(12) : 0);
        card.setLayoutParams(layoutParams);
        card.setOnClickListener(v -> openEventDetail(event));
    }

    private void startPopularCarousel() {
        popularCarouselHandler.removeCallbacks(popularCarouselRunnable);
        popularCarouselHandler.postDelayed(popularCarouselRunnable, 3000L);
    }

    private void advancePopularCarousel() {
        if (popularEventsScroll == null || popularEventsContainer == null || popularEventsContainer.getChildCount() < 2) {
            return;
        }

        int maxScroll = Math.max(0, popularEventsContainer.getWidth() - popularEventsScroll.getWidth());
        if (maxScroll == 0) {
            return;
        }

        int cardSpan = getResources().getDimensionPixelSize(R.dimen.event_carousel_card_width) + dpToPx(12);
        int nextScrollX = popularEventsScroll.getScrollX() + cardSpan;
        if (nextScrollX >= maxScroll) {
            nextScrollX = 0;
        }
        popularEventsScroll.smoothScrollTo(nextScrollX, 0);
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
