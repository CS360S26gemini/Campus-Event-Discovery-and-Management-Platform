package com.example.CampusEventDiscovery;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.favourites.FavouritesFragment;
import com.example.CampusEventDiscovery.ui.home.HomeAdminFragment;
import com.example.CampusEventDiscovery.ui.home.HomeFragment;
import com.example.CampusEventDiscovery.ui.home.HomeOrganizerFragment;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.profile.HelpFragment;
import com.example.CampusEventDiscovery.ui.profile.ProfileFragment;
import com.example.CampusEventDiscovery.ui.search.SearchFragment;
import com.example.CampusEventDiscovery.ui.calendar.EventCalendarFragment;
import com.example.CampusEventDiscovery.ui.myevents.MyEventsFragment;
import com.example.CampusEventDiscovery.ui.vendors.VendorManagementFragment;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.NavigationTransitions;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

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

    private String currentRole = UserRoles.ATTENDEE;
    private String currentNavigationKey;
    private boolean initialLoadStarted;
    private boolean suppressBottomNavSelection;

    // List of keys representing main screens where bottom navigation should be visible.
    private static final List<String> MAIN_SCREEN_KEYS = Arrays.asList(
            "home_admin", "home_organizer", "home_attendee", "search", "favourites", "profile", "my_events", "vendors"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        progressBarMain = findViewById(R.id.progressBarMain);

        if (savedInstanceState != null) {
            currentRole = savedInstanceState.getString("currentRole", UserRoles.ATTENDEE);
            currentNavigationKey = savedInstanceState.getString("currentNavigationKey");
        }

        // Apply role-based UI and theme colors immediately
        updateBottomNavMenu();

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        setupBottomNavigation();
        
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentRole", currentRole);
        outState.putString("currentNavigationKey", currentNavigationKey);
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
            if ("vendors".equals(tab)) {
                navigateTo("vendors", R.id.nav_favourites);
                return;
            }
        }

        if (intent != null && intent.getBooleanExtra("OPEN_HELP", false)) {
            openHelpSupport();
            return;
        }

        if (intent != null && intent.getBooleanExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, false)) {
            openWalkthroughScreen(intent.getStringExtra("WALKTHROUGH_SCREEN"));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

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

    @Override
    protected void onResume() {
        super.onResume();
        ThemeManager.applyAccentToMainNavigation(this, bottomNavigationView, null);
        ThemeManager.applyAccentToActivity(this);
    }

    private void updateBottomNavMenu() {
        Menu menu = bottomNavigationView.getMenu();
        MenuItem actionItem = menu.findItem(R.id.nav_action);
        MenuItem favouritesItem = menu.findItem(R.id.nav_favourites);
        if (actionItem == null) {
            return;
        }

        if (UserRoles.isOrganizer(currentRole)) {
            actionItem.setVisible(true);
            actionItem.setIcon(R.drawable.ic_add);
            actionItem.setTitle(R.string.create_event);
            if (favouritesItem != null) {
                favouritesItem.setIcon(R.drawable.ic_person);
                favouritesItem.setTitle(R.string.vendors);
            }
        } else if (UserRoles.isAdmin(currentRole)) {
            actionItem.setVisible(false);
            if (favouritesItem != null) {
                favouritesItem.setIcon(R.drawable.ic_person);
                favouritesItem.setTitle(R.string.vendors);
            }
        } else {
            actionItem.setVisible(true);
            actionItem.setIcon(R.drawable.ic_pin);
            actionItem.setTitle(R.string.my_events_row);
            if (favouritesItem != null) {
                favouritesItem.setIcon(R.drawable.ic_heart_outline);
                favouritesItem.setTitle(R.string.favourites);
            }
        }
        ThemeManager.applyAccentToMainNavigation(this, bottomNavigationView, null);
        ThemeManager.applyAccentToActivity(this);
    }

    private void loadInitialFragment() {
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
        bottomNavigationView.setOnItemReselectedListener(item -> handleBottomNavSelection(item));
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
                updateSelectedNavItem(R.id.nav_home);
                return true;
            } else if (!UserRoles.canManageEvents(currentRole)) {
                return navigateTo("my_events", R.id.nav_action);
            }
            return true;
        }

        if (itemId == R.id.nav_favourites) {
            if (UserRoles.canManageEvents(currentRole)) {
                return navigateTo("vendors", R.id.nav_favourites);
            }
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

        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (key.equals(currentNavigationKey)
                && getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) {
            updateSelectedNavItem(selectedItemId);
            updateBottomNavVisibility(key);
            return true;
        }

        loadFragment(createFragment(key));
        currentNavigationKey = key;
        updateSelectedNavItem(selectedItemId);
        updateBottomNavVisibility(key);
        return true;
    }

    public void openWalkthroughScreen(String screen) {
        if (screen == null) {
            return;
        }
        switch (screen) {
            case "search":
                navigateTo("search", R.id.nav_search);
                break;
            case "my_events":
                navigateTo("my_events", R.id.nav_action);
                break;
            case "memories":
            case "profile":
                navigateTo("profile", R.id.nav_profile);
                break;
            case "home_admin":
            case "home_organizer":
            case "home_attendee":
            default:
                navigateTo(screen, R.id.nav_home);
                break;
        }
    }

    private void openHelpSupport() {
        if (!canUpdateUi() || getSupportFragmentManager().isStateSaved()) {
            return;
        }
        navigateTo("profile", R.id.nav_profile);
        NavigationTransitions.replace(
                getSupportFragmentManager(),
                R.id.fragmentContainer,
                new HelpFragment(),
                true,
                true
        );
        currentNavigationKey = "help";
        updateBottomNavVisibility(currentNavigationKey);
    }

    public void returnFromHelp() {
        if (!canUpdateUi() || getSupportFragmentManager().isStateSaved()) {
            return;
        }
        getSupportFragmentManager().popBackStackImmediate();
        currentNavigationKey = "profile";
        updateSelectedNavItem(R.id.nav_profile);
        updateBottomNavVisibility("profile");
    }

    public void openVendorManagement() {
        navigateTo("vendors", R.id.nav_favourites);
    }

    private void updateBottomNavVisibility(String key) {
        if (MAIN_SCREEN_KEYS.contains(key)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        } else {
            bottomNavigationView.setVisibility(View.GONE);
        }
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
            case "vendors":
                return VendorManagementFragment.newInstance(currentRole);
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
        fragmentContainerPostAccent();
    }

    private void fragmentContainerPostAccent() {
        bottomNavigationView.postDelayed(() -> ThemeManager.applyAccentToActivity(this), 160L);
    }

    private void showLoading(boolean isLoading) {
        progressBarMain.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        bottomNavigationView.setEnabled(!isLoading);
    }

    private boolean canUpdateUi() {
        return !isFinishing() && !isDestroyed();
    }
}
