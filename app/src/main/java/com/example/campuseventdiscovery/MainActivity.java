package com.example.campuseventdiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.ui.favourites.FavouritesFragment;
import com.example.campuseventdiscovery.ui.home.HomeAdminFragment;
import com.example.campuseventdiscovery.ui.home.HomeFragment;
import com.example.campuseventdiscovery.ui.home.HomeOrganizerFragment;
import com.example.campuseventdiscovery.ui.organizer.CreateEventActivity;
import com.example.campuseventdiscovery.ui.profile.ProfileFragment;
import com.example.campuseventdiscovery.ui.search.SearchFragment;
import com.example.campuseventdiscovery.ui.calendar.EventCalendarFragment;
import com.example.campuseventdiscovery.ui.myevents.MyEventsFragment;
import com.example.campuseventdiscovery.util.DevSessionManager;
import com.example.campuseventdiscovery.util.NavigationTransitions;
import com.example.campuseventdiscovery.util.ThemeManager;
import com.example.campuseventdiscovery.util.UserRoles;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

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

    private final Map<String, Fragment> fragmentCache = new HashMap<>();

    private String currentRole = "attendee";

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
                loadFragment(getOrCreateFragment("profile"));
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                return;
            }
        }
        
        // If no specific tab requested, ensure we load initial if empty
        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {
            onStart(); // Trigger initial load logic
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If a fragment is already loaded (e.g. user returning from another activity),
        // don't reset the UI to the initial home state.
        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) {
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (DevSessionManager.shouldUseBypass(this)) {
                currentRole = DevSessionManager.getBypassRole(this);
                updateBottomNavMenu();
                loadInitialFragment();
            } else {
                Intent intent = new Intent(MainActivity.this, TempLoginActivity.class);
                startActivity(intent);
                finish();
            }
            return;
        }

        showLoading(true);

        repository.getUserData(currentUser.getUid(), new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.campuseventdiscovery.model.User user) {
                showLoading(false);

                String resolvedRole = UserRoles.sanitize(user.getRole());
                currentRole = resolvedRole.isEmpty() ? UserRoles.ATTENDEE : resolvedRole;

                if (ThemeManager.syncThemePreference(MainActivity.this, user.isDarkMode())) {
                    return;
                }

                updateBottomNavMenu();
                loadInitialFragment();
            }

            @Override
            public void onError(Exception e) {
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

        Fragment initialFragment;
        if (UserRoles.isAdmin(currentRole)) {
            initialFragment = getOrCreateFragment("home_admin");
        } else if (UserRoles.isOrganizer(currentRole)) {
            initialFragment = getOrCreateFragment("home_organizer");
        } else {
            initialFragment = getOrCreateFragment("home_attendee");
        }

        loadFragment(initialFragment);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(this::handleBottomNavSelection);
    }

    private boolean handleBottomNavSelection(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            if (UserRoles.isAdmin(currentRole)) {
                loadFragment(getOrCreateFragment("home_admin"));
            } else if (UserRoles.isOrganizer(currentRole)) {
                loadFragment(getOrCreateFragment("home_organizer"));
            } else {
                loadFragment(getOrCreateFragment("home_attendee"));
            }
            return true;
        }

        if (itemId == R.id.nav_search) {
            loadFragment(getOrCreateFragment("search"));
            return true;
        }

        if (itemId == R.id.nav_action) {
            if (UserRoles.isOrganizer(currentRole)) {
                startActivity(new Intent(this, CreateEventActivity.class));
            } else if (UserRoles.isAdmin(currentRole)) {
                loadFragment(getOrCreateFragment("home_admin"));
            } else {
                loadFragment(getOrCreateFragment("my_events"));
            }
            return true;
        }

        if (itemId == R.id.nav_favourites) {
            loadFragment(getOrCreateFragment("favourites"));
            return true;
        }

        if (itemId == R.id.nav_profile) {
            loadFragment(getOrCreateFragment("profile"));
            return true;
        }

        return false;
    }

    private Fragment getOrCreateFragment(String key) {
        if (fragmentCache.containsKey(key)) {
            return fragmentCache.get(key);
        }

        Fragment fragment;

        switch (key) {
            case "home_admin":
                fragment = new HomeAdminFragment();
                break;

            case "home_organizer":
                fragment = new HomeOrganizerFragment();
                break;

            case "search":
                fragment = new SearchFragment();
                break;

            case "calendar":
                fragment = new EventCalendarFragment();
                break;

            case "my_events":
                fragment = new MyEventsFragment();
                break;

            case "favourites":
                fragment = new FavouritesFragment();
                break;

            case "profile":
                fragment = new ProfileFragment();
                break;

            case "home_attendee":
            default:
                fragment = new HomeFragment();
                break;
        }

        fragmentCache.put(key, fragment);
        return fragment;
    }

    private void loadFragment(Fragment fragment) {
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
}
