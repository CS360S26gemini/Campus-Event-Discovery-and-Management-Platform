package com.example.CampusEventDiscovery.ui.calendar;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageButton;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * EventCalendarFragment.java
 *
 * Calendar screen showing RSVP'd events for personal scheduling.
 */
public class EventCalendarFragment extends Fragment {

    private ImageButton btnBackCalendar;
    private TextView tvMonthLabel;
    private TextView tvDatesWithEvents;
    private ProgressBar progressBarCalendar;
    private RecyclerView rvCalendarEvents;
    private TextView tvEmptyCalendar;
    private CalendarView calendarView;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;

    private EventRepository repository;
    private EventAdapter adapter;

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> selectedDayEvents = new ArrayList<>();
    private final Set<String> savedEventIds = new HashSet<>();

    private Calendar currentMonthCalendar;
    private Calendar selectedDateCalendar;
    private String currentUserId;

    public EventCalendarFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(requireContext());

        btnBackCalendar = view.findViewById(R.id.btnBackCalendar);
        tvMonthLabel = view.findViewById(R.id.tvMonthLabel);
        tvDatesWithEvents = view.findViewById(R.id.tvDatesWithEvents);
        progressBarCalendar = view.findViewById(R.id.progressBarCalendar);
        rvCalendarEvents = view.findViewById(R.id.rvCalendarEvents);
        tvEmptyCalendar = view.findViewById(R.id.tvEmptyCalendar);
        calendarView = view.findViewById(R.id.calendarView);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);

        currentMonthCalendar = Calendar.getInstance();
        selectedDateCalendar = Calendar.getInstance();

        setupRecyclerView();
        setupCalendar();
        setupMonthButtons();
        setupBackButton();
        updateMonthLabel();
        loadEvents();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(
                selectedDayEvents,
                savedEventIds,
                null,
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
                        // no-op in calendar
                    }

                    @Override
                    public void onItemLongClick(Event event) {
                        if (event == null) {
                            return;
                        }
                        addEventToGoogleCalendar(event);
                    }
                }
        );

        rvCalendarEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCalendarEvents.setNestedScrollingEnabled(false);
        rvCalendarEvents.setAdapter(adapter);
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, month);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            currentMonthCalendar.set(Calendar.YEAR, year);
            currentMonthCalendar.set(Calendar.MONTH, month);

            updateMonthLabel();
            filterEventsForSelectedDay();
            updateDatesWithEventsSummary();
        });
    }

    private void setupMonthButtons() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            moveCalendarToCurrentMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            moveCalendarToCurrentMonth();
        });
    }

    private void setupBackButton() {
        btnBackCalendar.setOnClickListener(v -> {
            if (isAdded()) {
                if (getActivity() != null) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavigationView);
                    if (nav != null) {
                        nav.setSelectedItemId(R.id.nav_profile);
                    } else {
                        getParentFragmentManager().popBackStack();
                    }
                }
            }
        });
    }

    private void moveCalendarToCurrentMonth() {
        Calendar temp = (Calendar) currentMonthCalendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);

        selectedDateCalendar.set(Calendar.YEAR, temp.get(Calendar.YEAR));
        selectedDateCalendar.set(Calendar.MONTH, temp.get(Calendar.MONTH));
        selectedDateCalendar.set(Calendar.DAY_OF_MONTH, 1);

        calendarView.setDate(temp.getTimeInMillis(), true, true);
        updateMonthLabel();
        filterEventsForSelectedDay();
        updateDatesWithEventsSummary();
    }

    private void loadEvents() {
        if (TextUtils.isEmpty(currentUserId)) {
            tvEmptyCalendar.setVisibility(View.VISIBLE);
            return;
        }

        showLoading(true);

        // Changed from getUpcomingEvents to getRsvps to implement "personal scheduling"
        repository.getRsvps(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!isAdded()) return;
                allEvents.clear();
                if (events != null) {
                    allEvents.addAll(events);
                }

                filterEventsForSelectedDay();
                updateDatesWithEventsSummary();
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                allEvents.clear();
                filterEventsForSelectedDay();
                updateDatesWithEventsSummary();
                showLoading(false);
            }
        });
    }

    private void filterEventsForSelectedDay() {
        selectedDayEvents.clear();

        for (Event event : allEvents) {
            if (isSameDay(event.getDate(), selectedDateCalendar)) {
                selectedDayEvents.add(event);
            }
        }

        adapter.updateData(new ArrayList<>(selectedDayEvents));

        if (selectedDayEvents.isEmpty()) {
            tvEmptyCalendar.setVisibility(View.VISIBLE);
            tvEmptyCalendar.setText(getString(R.string.no_rsvpd_events_for_day));
            rvCalendarEvents.setVisibility(View.GONE);
        } else {
            tvEmptyCalendar.setVisibility(View.GONE);
            rvCalendarEvents.setVisibility(View.VISIBLE);
        }
    }

    private void updateDatesWithEventsSummary() {
        LinkedHashSet<String> daySet = new LinkedHashSet<>();

        for (Event event : allEvents) {
            Timestamp timestamp = event.getDate();
            if (timestamp == null) {
                continue;
            }

            Calendar eventCal = Calendar.getInstance();
            eventCal.setTime(timestamp.toDate());

            if (eventCal.get(Calendar.YEAR) == currentMonthCalendar.get(Calendar.YEAR)
                    && eventCal.get(Calendar.MONTH) == currentMonthCalendar.get(Calendar.MONTH)) {
                daySet.add(String.valueOf(eventCal.get(Calendar.DAY_OF_MONTH)));
            }
        }

        if (daySet.isEmpty()) {
            tvDatesWithEvents.setText(getString(R.string.dates_with_events, "-"));
            return;
        }

        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String day : daySet) {
            builder.append(day);
            if (index < daySet.size() - 1) {
                builder.append(", ");
            }
            index++;
        }

        tvDatesWithEvents.setText(getString(R.string.dates_with_events, builder.toString()));
    }

    private void updateMonthLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthLabel.setText(sdf.format(currentMonthCalendar.getTime()));
    }

    private boolean isSameDay(Timestamp timestamp, Calendar selectedCalendar) {
        if (timestamp == null || selectedCalendar == null) {
            return false;
        }

        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(timestamp.toDate());

        return eventCal.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR)
                && eventCal.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)
                && eventCal.get(Calendar.DAY_OF_MONTH) == selectedCalendar.get(Calendar.DAY_OF_MONTH);
    }

    private void addEventToGoogleCalendar(Event event) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE,
                            safeText(event.getTitle(), getString(R.string.app_name)))
                    .putExtra(CalendarContract.Events.EVENT_LOCATION,
                            safeText(event.getLocation(), getString(R.string.placeholder_venue)));

            if (event.getDate() != null) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        event.getDate().toDate().getTime());
            }

            long endMillis = resolveEventEndMillis(event);
            if (endMillis > 0L) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
            }

            startActivity(intent);
            repository.markRsvpAddedToCalendar(currentUserId, event.getEventId(), "");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), getString(R.string.calendar_add_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }

    private void showLoading(boolean isLoading) {
        if (progressBarCalendar != null) {
            progressBarCalendar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private long resolveEventEndMillis(Event event) {
        if (event == null) {
            return 0L;
        }

        Timestamp end = event.getEndTime();
        if (end != null) {
            return end.toDate().getTime();
        }

        Timestamp start = event.getDate();
        if (start == null) {
            return 0L;
        }

        return start.toDate().getTime() + 2L * 60L * 60L * 1000L;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
