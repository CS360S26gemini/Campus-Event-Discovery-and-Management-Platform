package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.favourites.FavouritesFragment;
import com.example.CampusEventDiscovery.ui.home.HomeAdminFragment;
import com.example.CampusEventDiscovery.ui.home.HomeFragment;
import com.example.CampusEventDiscovery.ui.home.HomeOrganizerFragment;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.profile.ProfileFragment;
import com.example.CampusEventDiscovery.ui.search.SearchFragment;
import com.example.CampusEventDiscovery.ui.calendar.EventCalendarFragment;
import com.example.CampusEventDiscovery.ui.myevents.MyEventsFragment;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.NavigationTransitions;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * MainActivity.java
 *
 * Main host activity containing the bottom navigation bar and fragment container.
 * It decides which home fragment to load based on the user's Firestore role.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBarMain;

    private FirebaseAuth auth;
    private EventRepository repository;

    private String currentRole = "attendee";
    private String currentNavigationKey;
    private boolean initialLoadStarted;
    private boolean suppressBottomNavSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        progressBarMain = findViewById(R.id.progressBarMain);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        setupBottomNavigation();
        
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("OPEN_TAB")) {
            String tab = intent.getStringExtra("OPEN_TAB");
            if ("profile".equals(tab)) {
                navigateTo("profile", R.id.nav_profile);
                return;
            }
        }
        
        // No explicit tab requested; the normal onStart lifecycle loads the initial screen.
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If a fragment is already loaded (e.g. user returning from another activity),
        // don't reset the UI to the initial home state.
        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) {
            return;
        }

        if (initialLoadStarted) {
            return;
        }
        initialLoadStarted = true;

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (DevSessionManager.shouldUseBypass(this)) {
                currentRole = DevSessionManager.getBypassRole(this);
                updateBottomNavMenu();
                loadInitialFragment();
                return;
            }

            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        showLoading(true);

        repository.getUserData(currentUser.getUid(), new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                if (!canUpdateUi()) {
                    return;
                }
                showLoading(false);

                String resolvedRole = UserRoles.sanitize(user == null ? null : user.getRole());
                currentRole = resolvedRole.isEmpty() ? UserRoles.ATTENDEE : resolvedRole;

                if (user != null && ThemeManager.syncThemePreference(MainActivity.this, user.isDarkMode())) {
                    return;
                }

                updateBottomNavMenu();
                loadInitialFragment();
            }

            @Override
            public void onError(Exception e) {
                if (!canUpdateUi()) {
                    return;
                }
                showLoading(false);

                currentRole = UserRoles.ATTENDEE;
                updateBottomNavMenu();
                loadInitialFragment();
            }
        });
    }

    private void updateBottomNavMenu() {
        Menu menu = bottomNavigationView.getMenu();
        MenuItem actionItem = menu.findItem(R.id.nav_action);
        if (actionItem == null) {
            return;
        }

        if (UserRoles.isOrganizer(currentRole)) {
            actionItem.setIcon(R.drawable.ic_add);
            actionItem.setTitle(R.string.create_event);
        } else if (UserRoles.isAdmin(currentRole)) {
            actionItem.setIcon(R.drawable.ic_verified);
            actionItem.setTitle(R.string.approvals);
        } else {
            actionItem.setIcon(R.drawable.ic_pin);
            actionItem.setTitle(R.string.my_events_row);
        }
    }

    private void loadInitialFragment() {
        // Double check we don't overwrite existing
        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) {
            return;
        }
        if (getSupportFragmentManager().isStateSaved()) {
            initialLoadStarted = false;
            return;
        }

        if (UserRoles.isAdmin(currentRole)) {
            navigateTo("home_admin", R.id.nav_home);
        } else if (UserRoles.isOrganizer(currentRole)) {
            navigateTo("home_organizer", R.id.nav_home);
        } else {
            navigateTo("home_attendee", R.id.nav_home);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(this::handleBottomNavSelection);
    }

    private boolean handleBottomNavSelection(@NonNull MenuItem item) {
        if (suppressBottomNavSelection) {
            return true;
        }

        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            if (UserRoles.isAdmin(currentRole)) {
                return navigateTo("home_admin", R.id.nav_home);
            } else if (UserRoles.isOrganizer(currentRole)) {
                return navigateTo("home_organizer", R.id.nav_home);
            } else {
                return navigateTo("home_attendee", R.id.nav_home);
            }
        }

        if (itemId == R.id.nav_search) {
            return navigateTo("search", R.id.nav_search);
        }

        if (itemId == R.id.nav_action) {
            if (UserRoles.isOrganizer(currentRole)) {
                startActivity(new Intent(this, CreateEventActivity.class));
            } else if (UserRoles.isAdmin(currentRole)) {
                return navigateTo("home_admin", R.id.nav_action);
            } else {
                return navigateTo("my_events", R.id.nav_action);
            }
            return true;
        }

        if (itemId == R.id.nav_favourites) {
            return navigateTo("favourites", R.id.nav_favourites);
        }

        if (itemId == R.id.nav_profile) {
            return navigateTo("profile", R.id.nav_profile);
        }

        return false;
    }

    private boolean navigateTo(String key, int selectedItemId) {
        if (!canUpdateUi() || getSupportFragmentManager().isStateSaved()) {
            return false;
        }

        if (key.equals(currentNavigationKey)
                && getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) {
            updateSelectedNavItem(selectedItemId);
            return true;
        }

        loadFragment(createFragment(key));
        currentNavigationKey = key;
        updateSelectedNavItem(selectedItemId);
        return true;
    }

    private Fragment createFragment(String key) {
        switch (key) {
            case "home_admin":
                return new HomeAdminFragment();

            case "home_organizer":
                return new HomeOrganizerFragment();

            case "search":
                return new SearchFragment();

            case "calendar":
                return new EventCalendarFragment();

            case "my_events":
                return new MyEventsFragment();

            case "favourites":
                return new FavouritesFragment();

            case "profile":
                return new ProfileFragment();

            case "home_attendee":
            default:
                return new HomeFragment();
        }
    }

    private void updateSelectedNavItem(int selectedItemId) {
        if (bottomNavigationView.getSelectedItemId() == selectedItemId) {
            return;
        }

        suppressBottomNavSelection = true;
        bottomNavigationView.setSelectedItemId(selectedItemId);
        suppressBottomNavSelection = false;
    }

    private void loadFragment(Fragment fragment) {
        if (!canUpdateUi() || getSupportFragmentManager().isStateSaved()) {
            return;
        }
        boolean animate = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null;
        NavigationTransitions.replace(
                getSupportFragmentManager(),
                R.id.fragmentContainer,
                fragment,
                false,
                animate
        );
    }

    private void showLoading(boolean isLoading) {
        progressBarMain.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        bottomNavigationView.setEnabled(!isLoading);
    }

    private boolean canUpdateUi() {
        return !isFinishing() && !isDestroyed();
    }
}
