package com.example.CampusEventDiscovery.ui.calendar;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.EventAdapter;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormatSymbols;
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
 * Calendar screen that shows all active events and overlays the current
 * user's registered or organizer-owned approved events.
 */
public class EventCalendarFragment extends Fragment {

    private ImageButton btnBackCalendar;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private TextView tvMonthLabel;
    private TextView tvDatesWithEvents;
    private TextView tvSelectedDayHeader;
    private TextView tvEmptyCalendar;
    private TextView tvLegendPersonal;
    private ProgressBar progressBarCalendar;
    private RecyclerView rvCalendarEvents;
    private MaterialButtonToggleGroup toggleCalendarEventScope;
    private MaterialButton btnCalendarFilterPersonal;
    private LinearLayout layoutWeekdayHeader;
    private LinearLayout layoutLegendPersonal;
    private GridLayout gridCalendarDays;
    private View viewLegendAll;
    private View viewLegendPersonal;

    private EventRepository repository;
    private EventAdapter adapter;

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> personalEvents = new ArrayList<>();
    private final List<Event> selectedDayEvents = new ArrayList<>();
    private final Set<String> savedEventIds = new HashSet<>();
    private final Set<Long> allEventDayKeys = new HashSet<>();
    private final Set<Long> personalEventDayKeys = new HashSet<>();

    private Calendar currentMonthCalendar;
    private Calendar selectedDateCalendar;
    private String currentUserId;
    private String currentUserRole = "";
    private boolean showingPersonalEventsOnly;

    public EventCalendarFragment() {
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

        bindViews(view);

        currentMonthCalendar = Calendar.getInstance();
        currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
        selectedDateCalendar = Calendar.getInstance();

        setupRecyclerView();
        buildWeekdayHeader();
        setupLegendMarkers();
        setupFilterToggle();
        setupMonthButtons();
        setupBackButton();
        updateMonthLabel();
        renderCalendarDays();
        loadEvents();
    }

    private void bindViews(View view) {
        btnBackCalendar = view.findViewById(R.id.btnBackCalendar);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvMonthLabel = view.findViewById(R.id.tvMonthLabel);
        tvDatesWithEvents = view.findViewById(R.id.tvDatesWithEvents);
        tvSelectedDayHeader = view.findViewById(R.id.tvSelectedDayHeader);
        tvEmptyCalendar = view.findViewById(R.id.tvEmptyCalendar);
        tvLegendPersonal = view.findViewById(R.id.tvLegendPersonal);
        progressBarCalendar = view.findViewById(R.id.progressBarCalendar);
        rvCalendarEvents = view.findViewById(R.id.rvCalendarEvents);
        toggleCalendarEventScope = view.findViewById(R.id.toggleCalendarEventScope);
        btnCalendarFilterPersonal = view.findViewById(R.id.btnCalendarFilterPersonal);
        layoutWeekdayHeader = view.findViewById(R.id.layoutWeekdayHeader);
        layoutLegendPersonal = view.findViewById(R.id.layoutLegendPersonal);
        gridCalendarDays = view.findViewById(R.id.gridCalendarDays);
        viewLegendAll = view.findViewById(R.id.viewLegendAll);
        viewLegendPersonal = view.findViewById(R.id.viewLegendPersonal);
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
                        if (WalkthroughManager.isActive()) {
                            Toast.makeText(requireContext(), "Walkthrough mode: device calendar was not opened.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (event == null) {
                            return;
                        }
                        addEventToGoogleCalendar(event);
                    }
                },
                true
        );

        rvCalendarEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCalendarEvents.setNestedScrollingEnabled(false);
        rvCalendarEvents.setAdapter(adapter);
    }

    private void setupLegendMarkers() {
        int primary = ThemeManager.getAccentColor(requireContext());
        int secondary = ContextCompat.getColor(requireContext(), R.color.colorSecondary);

        styleLegendDot(viewLegendAll, ColorUtils.setAlphaComponent(secondary, 64), secondary);
        styleLegendDot(viewLegendPersonal, ColorUtils.setAlphaComponent(primary, 48), primary);
        updateLegend();
    }

    private void setupFilterToggle() {
        toggleCalendarEventScope.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            showingPersonalEventsOnly = checkedId == R.id.btnCalendarFilterPersonal;
            filterEventsForSelectedDay();
        });
    }

    private void buildWeekdayHeader() {
        layoutWeekdayHeader.removeAllViews();

        String[] weekdays = DateFormatSymbols.getInstance(Locale.getDefault()).getShortWeekdays();
        int firstDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();

        for (int offset = 0; offset < 7; offset++) {
            int weekdayIndex = ((firstDayOfWeek - 1 + offset) % 7) + 1;
            String label = weekdays[weekdayIndex];
            if (label == null) {
                label = "";
            }

            TextView weekdayView = new TextView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            );
            weekdayView.setLayoutParams(params);
            weekdayView.setGravity(Gravity.CENTER);
            weekdayView.setText(label.toUpperCase(Locale.getDefault()));
            weekdayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary));
            weekdayView.setTypeface(Typeface.DEFAULT_BOLD);
            weekdayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            layoutWeekdayHeader.addView(weekdayView);
        }
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
            if (!isAdded()) {
                return;
            }

            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
                return;
            }

            if (getActivity() != null) {
                BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavigationView);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_profile);
                } else {
                    getParentFragmentManager().popBackStack();
                }
            }
        });
    }

    private void moveCalendarToCurrentMonth() {
        currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);

        Calendar today = Calendar.getInstance();
        if (!isSameMonth(selectedDateCalendar, currentMonthCalendar)) {
            int targetDay = 1;
            if (isSameMonth(today, currentMonthCalendar)) {
                targetDay = today.get(Calendar.DAY_OF_MONTH);
            }
            selectedDateCalendar.set(Calendar.YEAR, currentMonthCalendar.get(Calendar.YEAR));
            selectedDateCalendar.set(Calendar.MONTH, currentMonthCalendar.get(Calendar.MONTH));
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, targetDay);
        }

        renderCalendarDays();
        updateMonthLabel();
        filterEventsForSelectedDay();
        updateDatesWithEventsSummary();
    }

    private void loadEvents() {
        if (WalkthroughManager.isActive()) {
            currentUserRole = DevSessionManager.shouldUseBypass(requireContext())
                    ? UserRoles.sanitize(DevSessionManager.getBypassRole(requireContext()))
                    : UserRoles.ATTENDEE;
            allEvents.clear();
            allEvents.addAll(WalkthroughManager.getDemoEvents());
            applyCalendarData(WalkthroughManager.getDemoEvents());
            WalkthroughManager.maybeShow(requireActivity(), getView(), "calendar");
            return;
        }

        showLoading(true);

        repository.getUpcomingEvents(new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!isAdded()) {
                    return;
                }
                allEvents.clear();
                if (events != null) {
                    allEvents.addAll(events);
                }
                resolveRoleAndLoadPersonalEvents();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }
                allEvents.clear();
                resolveRoleAndLoadPersonalEvents();
            }
        });
    }

    private void resolveRoleAndLoadPersonalEvents() {
        if (!isAdded()) {
            return;
        }

        if (DevSessionManager.shouldUseBypass(requireContext())) {
            currentUserRole = UserRoles.sanitize(DevSessionManager.getBypassRole(requireContext()));
            loadPersonalEventsForCurrentRole();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            currentUserRole = "";
            applyCalendarData(new ArrayList<>());
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                if (!isAdded()) {
                    return;
                }
                currentUserRole = user == null ? "" : UserRoles.sanitize(user.getRole());
                loadPersonalEventsForCurrentRole();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }
                currentUserRole = "";
                applyCalendarData(new ArrayList<>());
            }
        });
    }

    private void loadPersonalEventsForCurrentRole() {
        if (UserRoles.isAttendee(currentUserRole) && !TextUtils.isEmpty(currentUserId)) {
            repository.getRsvps(currentUserId, new EventRepository.EventListCallback() {
                @Override
                public void onSuccess(List<Event> events) {
                    if (!isAdded()) {
                        return;
                    }
                    applyCalendarData(events);
                }

                @Override
                public void onError(Exception e) {
                    if (!isAdded()) {
                        return;
                    }
                    applyCalendarData(new ArrayList<>());
                }
            });
            return;
        }

        if (UserRoles.isOrganizer(currentUserRole) && !TextUtils.isEmpty(currentUserId)) {
            repository.getOrganizerEvents(currentUserId, new EventRepository.EventListCallback() {
                @Override
                public void onSuccess(List<Event> events) {
                    if (!isAdded()) {
                        return;
                    }
                    applyCalendarData(events);
                }

                @Override
                public void onError(Exception e) {
                    if (!isAdded()) {
                        return;
                    }
                    applyCalendarData(new ArrayList<>());
                }
            });
            return;
        }

        applyCalendarData(new ArrayList<>());
    }

    private void applyCalendarData(List<Event> personalEvents) {
        this.personalEvents.clear();
        allEventDayKeys.clear();
        personalEventDayKeys.clear();

        for (Event event : allEvents) {
            long key = dayKey(event.getDate());
            if (key > 0L) {
                allEventDayKeys.add(key);
            }
        }

        if (personalEvents != null) {
            for (Event event : personalEvents) {
                this.personalEvents.add(event);
                long key = dayKey(event.getDate());
                if (key > 0L) {
                    personalEventDayKeys.add(key);
                }
            }
        }

        updateLegend();
        updateFilterToggle();
        renderCalendarDays();
        filterEventsForSelectedDay();
        updateDatesWithEventsSummary();
        showLoading(false);
    }

    private void renderCalendarDays() {
        if (!isAdded()) {
            return;
        }

        gridCalendarDays.removeAllViews();

        Calendar firstOfMonth = (Calendar) currentMonthCalendar.clone();
        firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        normalizeToStartOfDay(firstOfMonth);

        int offset = calculateMonthOffset(firstOfMonth);
        Calendar cellDate = (Calendar) firstOfMonth.clone();
        cellDate.add(Calendar.DAY_OF_MONTH, -offset);

        for (int index = 0; index < 42; index++) {
            Calendar day = (Calendar) cellDate.clone();
            if (isSameMonth(day, currentMonthCalendar)) {
                gridCalendarDays.addView(createDayCell(day));
            } else {
                gridCalendarDays.addView(createEmptyCell());
            }
            cellDate.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private View createDayCell(Calendar day) {
        TextView cell = new TextView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(48);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        cell.setLayoutParams(params);
        cell.setGravity(Gravity.CENTER);
        cell.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        cell.setClickable(true);
        cell.setFocusable(true);

        int primary = ThemeManager.getAccentColor(requireContext());
        int onBackground = ContextCompat.getColor(requireContext(), R.color.colorOnBackground);
        int secondary = ContextCompat.getColor(requireContext(), R.color.colorSecondary);

        boolean isToday = isSameDay(day, Calendar.getInstance());
        boolean isSelected = isSameDay(day, selectedDateCalendar);
        long dayKey = dayKey(day);
        boolean hasAllEvents = allEventDayKeys.contains(dayKey);
        boolean hasPersonalEvents = personalEventDayKeys.contains(dayKey);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dpToPx(14));
        background.setColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
        background.setStroke(0, ContextCompat.getColor(requireContext(), android.R.color.transparent));

        if (hasPersonalEvents) {
            background.setColor(ColorUtils.setAlphaComponent(primary, 48));
            background.setStroke(dpToPx(isSelected ? 2 : 1), primary);
        } else if (hasAllEvents) {
            background.setColor(ColorUtils.setAlphaComponent(secondary, 40));
            background.setStroke(dpToPx(isSelected ? 2 : 1), ColorUtils.setAlphaComponent(secondary, 140));
        } else if (isSelected) {
            background.setStroke(dpToPx(2), primary);
        }

        cell.setBackground(background);
        cell.setTextColor(isToday ? primary : onBackground);
        cell.setTypeface(Typeface.DEFAULT, isToday || isSelected ? Typeface.BOLD : Typeface.NORMAL);

        cell.setOnClickListener(v -> {
            selectedDateCalendar.set(Calendar.YEAR, day.get(Calendar.YEAR));
            selectedDateCalendar.set(Calendar.MONTH, day.get(Calendar.MONTH));
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH));
            renderCalendarDays();
            filterEventsForSelectedDay();
            updateDatesWithEventsSummary();
        });

        return cell;
    }

    private View createEmptyCell() {
        TextView spacer = new TextView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(48);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        spacer.setLayoutParams(params);
        spacer.setText("");
        return spacer;
    }

    private void filterEventsForSelectedDay() {
        selectedDayEvents.clear();

        List<Event> source = shouldUsePersonalFilter() ? personalEvents : allEvents;
        for (Event event : source) {
            if (isSameDay(event.getDate(), selectedDateCalendar)) {
                selectedDayEvents.add(event);
            }
        }

        adapter.updateData(new ArrayList<>(selectedDayEvents));

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
        tvSelectedDayHeader.setText(getString(R.string.selected_day_events) + " - " + sdf.format(selectedDateCalendar.getTime()));

        if (selectedDayEvents.isEmpty()) {
            tvEmptyCalendar.setVisibility(View.VISIBLE);
            tvEmptyCalendar.setText(resolveEmptyStateMessage());
            rvCalendarEvents.setVisibility(View.GONE);
        } else {
            tvEmptyCalendar.setVisibility(View.GONE);
            rvCalendarEvents.setVisibility(View.VISIBLE);
        }
    }

    private boolean shouldUsePersonalFilter() {
        return showingPersonalEventsOnly
                && (UserRoles.isAttendee(currentUserRole) || UserRoles.isOrganizer(currentUserRole));
    }

    private String resolveEmptyStateMessage() {
        if (shouldUsePersonalFilter()) {
            if (UserRoles.isAttendee(currentUserRole)) {
                return getString(R.string.no_rsvpd_events_for_day);
            }
            if (UserRoles.isOrganizer(currentUserRole)) {
                return getString(R.string.no_approved_events_for_day);
            }
        }
        return getString(R.string.no_events_this_day);
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

            if (isSameMonth(eventCal, currentMonthCalendar)) {
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

    private void updateFilterToggle() {
        if (toggleCalendarEventScope == null) {
            return;
        }

        if (UserRoles.isAttendee(currentUserRole)) {
            toggleCalendarEventScope.setVisibility(View.VISIBLE);
            btnCalendarFilterPersonal.setText(R.string.calendar_filter_registered);
        } else if (UserRoles.isOrganizer(currentUserRole)) {
            toggleCalendarEventScope.setVisibility(View.VISIBLE);
            btnCalendarFilterPersonal.setText(R.string.calendar_filter_approved);
        } else {
            toggleCalendarEventScope.setVisibility(View.GONE);
            showingPersonalEventsOnly = false;
            toggleCalendarEventScope.check(R.id.btnCalendarFilterAll);
            return;
        }

        if (showingPersonalEventsOnly) {
            toggleCalendarEventScope.check(R.id.btnCalendarFilterPersonal);
        } else {
            toggleCalendarEventScope.check(R.id.btnCalendarFilterAll);
        }
    }

    private void updateLegend() {
        if (UserRoles.isAttendee(currentUserRole)) {
            layoutLegendPersonal.setVisibility(View.VISIBLE);
            tvLegendPersonal.setText(R.string.calendar_registered_events_legend);
        } else if (UserRoles.isOrganizer(currentUserRole)) {
            layoutLegendPersonal.setVisibility(View.VISIBLE);
            tvLegendPersonal.setText(R.string.calendar_approved_events_legend);
        } else {
            layoutLegendPersonal.setVisibility(View.GONE);
        }
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
        progressBarCalendar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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

    private void styleLegendDot(View target, int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        drawable.setStroke(dpToPx(1), strokeColor);
        target.setBackground(drawable);
    }

    private int calculateMonthOffset(Calendar firstOfMonth) {
        int firstDayOfWeek = firstOfMonth.getFirstDayOfWeek();
        int currentDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK);
        int offset = currentDayOfWeek - firstDayOfWeek;
        return offset < 0 ? offset + 7 : offset;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void normalizeToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private long dayKey(Timestamp timestamp) {
        if (timestamp == null) {
            return -1L;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(timestamp.toDate());
        normalizeToStartOfDay(calendar);
        return calendar.getTimeInMillis();
    }

    private long dayKey(Calendar calendar) {
        Calendar copy = (Calendar) calendar.clone();
        normalizeToStartOfDay(copy);
        return copy.getTimeInMillis();
    }

    private boolean isSameDay(Timestamp timestamp, Calendar selectedCalendar) {
        if (timestamp == null || selectedCalendar == null) {
            return false;
        }

        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(timestamp.toDate());

        return isSameDay(eventCal, selectedCalendar);
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        if (first == null || second == null) {
            return false;
        }

        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
                && first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isSameMonth(Calendar first, Calendar second) {
        if (first == null || second == null) {
            return false;
        }

        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH);
    }
}
