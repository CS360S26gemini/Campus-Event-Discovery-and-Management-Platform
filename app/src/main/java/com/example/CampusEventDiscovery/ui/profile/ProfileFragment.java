package com.example.CampusEventDiscovery.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.WelcomeActivity;
import com.example.CampusEventDiscovery.model.AvatarConfig;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.sos.SOSDashboardActivity;
import com.example.CampusEventDiscovery.util.AvatarRenderer;
import com.example.CampusEventDiscovery.util.Constants;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.ScrollResettable;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * ProfileFragment.java
 *
 * Profile and settings screen.
 */
public class ProfileFragment extends Fragment implements ScrollResettable {

    private static final String TAG = "ProfileFragment";
    private static final long TAP_FEEDBACK_DELAY_MS = 140L;
    private static final String STATE_SCROLL_Y = "profileScrollY";
    private static final String SCROLL_PREFS_NAME = "profile_scroll_state";
    private static final String KEY_RESTORE_SCROLL = "restoreProfileScroll";
    private static final String KEY_RESTORE_SCROLL_Y = "restoreProfileScrollY";
    private static final int SCROLL_RESTORE_MAX_ATTEMPTS = 8;
    private static final long SCROLL_RESTORE_RETRY_MS = 80L;
    private static final int PROFILE_AVATAR_SIZE_DP = 96;
    private static final int DIALOG_AVATAR_SIZE_DP = 112;
    private static final int[] SKIN_SWATCHES = {
            Color.rgb(255, 224, 189),
            Color.rgb(241, 194, 125),
            Color.rgb(224, 172, 105),
            Color.rgb(198, 134, 66),
            Color.rgb(141, 85, 36)
    };
    private static final int[] HAIR_SWATCHES = {
            Color.rgb(35, 31, 32),
            Color.rgb(88, 56, 39),
            Color.rgb(143, 80, 49),
            Color.rgb(214, 170, 92),
            Color.rgb(113, 75, 145)
    };
    private static final int[] BACKGROUND_SWATCHES = {
            Color.rgb(123, 47, 190),
            Color.rgb(32, 122, 104),
            Color.rgb(35, 105, 176),
            Color.rgb(190, 85, 70),
            Color.rgb(88, 91, 105)
    };
    private static final int[] SHIRT_SWATCHES = {
            Color.rgb(64, 74, 190),
            Color.rgb(39, 137, 90),
            Color.rgb(212, 118, 44),
            Color.rgb(135, 62, 158),
            Color.rgb(33, 36, 46)
    };

    private CircleImageView ivProfile;
    private NestedScrollView profileScrollView;
    private MaterialButton tvEditPhoto;
    private TextView tvFullName;
    private TextView tvEmail;
    private TextView tvCreditBalance;
    private View layoutCreditBalance;
    private Chip chipRole;
    private Switch switchDarkMode;
    private View cardDarkMode;
    private View cardAccentColor;
    private View viewAccentPreview;
    private TextView tvAccentColorValue;
    private View rowMyEvents;
    private View rowMemories;
    private View rowManageEvents;
    private View rowCreateEvent;
    private View rowScanTickets;
    private View rowSosDashboard;
    private TextView tvSosBadge;
    private View sectionAttendeeTools;
    private View sectionOrganizerTools;
    private View rowCalendar;
    private View rowNotifications;
    private View rowAccountSettings;
    private MaterialButton btnHelp;
    private MaterialButton btnLogout;
    private ProgressBar progressBarProfile;

    private EventRepository repository;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ListenerRegistration sosListener;

    private String currentUserId;
    private String currentRole = UserRoles.ATTENDEE;
    private String currentDisplayName = "";
    private String currentProfilePicUrl = "";
    private boolean currentAvatarEnabled;
    private boolean hasSavedAvatar;
    private AvatarConfig currentAvatarConfig;
    private int pendingScrollY;
    private static int lastKnownScrollY;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null) {
            currentRole = savedInstanceState.getString("currentRole", UserRoles.ATTENDEE);
            currentDisplayName = savedInstanceState.getString("currentDisplayName", "");
            pendingScrollY = savedInstanceState.getInt(STATE_SCROLL_Y, lastKnownScrollY);
        } else {
            pendingScrollY = lastKnownScrollY;
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadProfilePicture(imageUri);
                        }
                    }
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentRole", currentRole);
        outState.putString("currentDisplayName", currentDisplayName);
        preserveScrollPosition();
        outState.putInt(STATE_SCROLL_Y, pendingScrollY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : DevSessionManager.getEffectiveUserId(requireContext());

        bindViews(view);
        loadPendingScrollRestoreRequest();
        setupStaticActions();
        
        // Apply existing state if we have it (e.g. during recreation)
        if (!currentDisplayName.isEmpty()) {
            tvFullName.setText(currentDisplayName);
            bindRoleBadge();
            bindRoleNavigationRows();
        }
        
        loadProfile();
        setupSosBadgeListener();
        restoreScrollPositionSoon();
        WalkthroughManager.maybeShow(requireActivity(), view, "profile");
    }

    @Override
    public void onPause() {
        preserveScrollPosition();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sosListener != null) {
            sosListener.remove();
            sosListener = null;
        }
        profileScrollView = null;
    }

    private void bindViews(View view) {
        profileScrollView = view.findViewById(R.id.profileScrollView);
        ivProfile = view.findViewById(R.id.ivProfile);
        tvEditPhoto = view.findViewById(R.id.tvEditPhoto);
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCreditBalance = view.findViewById(R.id.tvCreditBalance);
        layoutCreditBalance = view.findViewById(R.id.layoutCreditBalance);
        chipRole = view.findViewById(R.id.chipRole);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        cardDarkMode = view.findViewById(R.id.cardDarkMode);
        cardAccentColor = view.findViewById(R.id.cardAccentColor);
        viewAccentPreview = view.findViewById(R.id.viewAccentPreview);
        tvAccentColorValue = view.findViewById(R.id.tvAccentColorValue);
        rowMyEvents = view.findViewById(R.id.rowMyEvents);
        rowMemories = view.findViewById(R.id.rowMemories);
        rowManageEvents = view.findViewById(R.id.rowManageEvents);
        rowCreateEvent = view.findViewById(R.id.rowCreateEvent);
        rowScanTickets = view.findViewById(R.id.rowScanTickets);
        rowSosDashboard = view.findViewById(R.id.rowSosDashboard);
        tvSosBadge = view.findViewById(R.id.tvSosBadge);
        sectionAttendeeTools = view.findViewById(R.id.sectionAttendeeTools);
        sectionOrganizerTools = view.findViewById(R.id.sectionOrganizerTools);
        rowCalendar = view.findViewById(R.id.rowCalendar);
        rowNotifications = view.findViewById(R.id.rowNotifications);
        rowAccountSettings = view.findViewById(R.id.rowAccountSettings);
        btnHelp = view.findViewById(R.id.btn_help);
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBarProfile = view.findViewById(R.id.progressBarProfile);
    }

    private void setupStaticActions() {
        tvEditPhoto.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            if (WalkthroughManager.isActive()) {
                Toast.makeText(requireContext(), "Walkthrough mode: profile image tools were not opened.", Toast.LENGTH_SHORT).show();
                return;
            }
            showProfileImageOptions();
        }));

        cardDarkMode.setOnClickListener(v -> {
            if (WalkthroughManager.isActive()) {
                Toast.makeText(requireContext(), "Walkthrough mode: theme mode was not changed.", Toast.LENGTH_SHORT).show();
                return;
            }
            switchDarkMode.toggle();
        });
        cardAccentColor.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            if (WalkthroughManager.isActive()) {
                Toast.makeText(requireContext(), "Walkthrough mode: accent color was not changed.", Toast.LENGTH_SHORT).show();
                return;
            }
            showAccentColorDialog();
        }));
        bindAccentPreference();

        bindDarkModeListener();

        rowMyEvents.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                ((com.example.CampusEventDiscovery.MainActivity) requireActivity()).openProfileMyEvents()
        ));

        rowMemories.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            Intent intent = new Intent(requireContext(), MemoriesActivity.class);
            startActivity(intent);
        }));

        rowManageEvents.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            Intent intent = new Intent(requireContext(), ManageEventsActivity.class);
            startActivity(intent);
        }));

        rowCreateEvent.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                startActivity(new Intent(requireContext(), CreateEventActivity.class))));

        rowScanTickets.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                startActivity(new Intent(requireContext(), ScannerActivity.class))));

        rowSosDashboard.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                startActivity(new Intent(requireContext(), SOSDashboardActivity.class))));

        rowCalendar.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                ((com.example.CampusEventDiscovery.MainActivity) requireActivity()).openCalendarScreen()
        ));

        rowNotifications.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            Intent intent = new Intent(requireContext(), NotificationCenterActivity.class);
            startActivity(intent);
        }));

        rowAccountSettings.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            Intent intent = new Intent(requireContext(), AccountSettingsActivity.class);
            startActivity(intent);
        }));

        btnHelp.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                ((com.example.CampusEventDiscovery.MainActivity) requireActivity()).openHelpSupport()
        ));

        btnLogout.setOnClickListener(v -> runAfterTouchFeedback(v, this::showLogoutDialog));
    }

    private void setupSosBadgeListener() {
        if (!UserRoles.canManageEvents(currentRole) || currentUserId == null) {
            if (tvSosBadge != null) tvSosBadge.setVisibility(View.GONE);
            return;
        }

        Query query = FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_SOS_ALERTS)
                .whereEqualTo("status", "active");

        // If organizer, only count alerts for their events
        if (UserRoles.isOrganizer(currentRole)) {
            query = query.whereEqualTo("organizerId", currentUserId);
        }

        sosListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }

            if (snapshots != null && !snapshots.isEmpty()) {
                int count = snapshots.size();
                tvSosBadge.setText(String.valueOf(count));
                tvSosBadge.setVisibility(View.VISIBLE);
            } else {
                tvSosBadge.setVisibility(View.GONE);
            }
        });
    }

    private void showProfileImageOptions() {
        if (!isAdded() || currentUserId == null) {
            return;
        }

        List<String> options = new ArrayList<>();
        List<ProfileVisualAction> actions = new ArrayList<>();

        boolean hasPhoto = !TextUtils.isEmpty(currentProfilePicUrl);
        if (hasPhoto && hasSavedAvatar) {
            options.add(getString(R.string.use_photo_as_main));
            actions.add(() -> setMainProfileImage(false));
            options.add(getString(R.string.use_avatar_as_main));
            actions.add(() -> setMainProfileImage(true));
        }

        options.add(getString(hasPhoto ? R.string.change_photo : R.string.add_profile_picture));
        actions.add(this::openImagePicker);
        options.add(getString(hasSavedAvatar ? R.string.edit_avatar : R.string.create_avatar));
        actions.add(this::showAvatarCreatorDialog);

        new AlertDialog.Builder(requireContext())
                .setTitle(hasPhoto && hasSavedAvatar
                        ? R.string.choose_main_profile_visual
                        : R.string.profile_visual_title)
                .setItems(options.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        imagePickerLauncher.launch(intent);
    }

    private void showAvatarCreatorDialog() {
        if (!isAdded() || currentUserId == null) {
            return;
        }

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_avatar_creator, null, false);
        CircleImageView preview = content.findViewById(R.id.ivAvatarPreview);
        EditText etInitials = content.findViewById(R.id.etAvatarInitials);

        AvatarConfig draft = currentAvatarConfig == null
                ? AvatarConfig.defaultsFor(currentDisplayName, currentRole)
                : currentAvatarConfig.copy();
        etInitials.setText(draft.getInitials());
        etInitials.setSelection(etInitials.getText() == null ? 0 : etInitials.getText().length());

        Runnable refreshPreview = () -> preview.setImageBitmap(
                AvatarRenderer.render(draft, dpToPx(DIALOG_AVATAR_SIZE_DP))
        );

        bindAvatarGroup(
                content.findViewById(R.id.chipGroupSkinTone),
                getResources().getStringArray(R.array.avatar_skin_tone_options),
                draft.getSkinTone(),
                SKIN_SWATCHES,
                draft::setSkinTone,
                refreshPreview
        );
        bindAvatarGroup(
                content.findViewById(R.id.chipGroupHairStyle),
                getResources().getStringArray(R.array.avatar_hair_style_options),
                draft.getHairStyle(),
                null,
                draft::setHairStyle,
                refreshPreview
        );
        bindAvatarGroup(
                content.findViewById(R.id.chipGroupHairColor),
                getResources().getStringArray(R.array.avatar_hair_color_options),
                draft.getHairColor(),
                HAIR_SWATCHES,
                draft::setHairColor,
                refreshPreview
        );
        bindAvatarGroup(
                content.findViewById(R.id.chipGroupBackground),
                getResources().getStringArray(R.array.avatar_background_options),
                draft.getBackground(),
                BACKGROUND_SWATCHES,
                draft::setBackground,
                refreshPreview
        );
        bindAvatarGroup(
                content.findViewById(R.id.chipGroupAccessory),
                getResources().getStringArray(R.array.avatar_accessory_options),
                draft.getAccessory(),
                null,
                draft::setAccessory,
                refreshPreview
        );
        bindAvatarGroup(
                content.findViewById(R.id.chipGroupShirt),
                getResources().getStringArray(R.array.avatar_outfit_options),
                draft.getShirtColor(),
                SHIRT_SWATCHES,
                draft::setShirtColor,
                refreshPreview
        );

        etInitials.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                draft.setInitials(s == null ? "" : s.toString());
                refreshPreview.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        refreshPreview.run();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.avatar_creator_title)
                .setView(content)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.ok, null)
                .create();

        dialog.setOnShowListener(unused ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> saveAvatar(draft, dialog))
        );
        dialog.show();
    }

    private void bindAvatarGroup(ChipGroup group,
                                 String[] labels,
                                 int selectedIndex,
                                 @Nullable int[] swatches,
                                 AvatarOptionSetter setter,
                                 Runnable refreshPreview) {
        group.removeAllViews();
        group.setSingleSelection(true);

        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked(i == selectedIndex);

            if (swatches != null && i < swatches.length) {
                int color = swatches[i];
                chip.setChipBackgroundColor(ColorStateList.valueOf(color));
                chip.setTextColor(isDarkColor(color) ? Color.WHITE : Color.rgb(35, 31, 32));
            }

            final int optionIndex = i;
            chip.setOnClickListener(v -> {
                setter.set(optionIndex);
                refreshPreview.run();
            });
            group.addView(chip);
        }
    }

    private void saveAvatar(AvatarConfig avatarConfig, AlertDialog dialog) {
        showLoading(true);
        repository.updateProfileAvatar(currentUserId, avatarConfig.toMap(), new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                showLoading(false);
                currentAvatarConfig = avatarConfig.copy();
                hasSavedAvatar = true;
                currentAvatarEnabled = true;
                bindAvatar(currentAvatarConfig);
                Toast.makeText(requireContext(), R.string.avatar_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), R.string.avatar_save_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadProfilePicture(Uri uri) {
        if (currentUserId == null) return;

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Some providers do not grant persistable access; the immediate upload can still proceed.
        }

        showLoading(true);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_pictures")
                .child(currentUserId + ".jpg");

        storageRef.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    if (!isAdded()) return;
                    String url = downloadUri.toString();
                    repository.updateProfilePic(currentUserId, url, new EventRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) return;
                            showLoading(false);
                            currentProfilePicUrl = url;
                            currentAvatarEnabled = false;
                            Glide.with(requireContext())
                                    .load(url)
                                    .placeholder(android.R.drawable.sym_def_app_icon)
                                    .centerCrop()
                                    .into(ivProfile);
                            Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            if (!isAdded()) return;
                            showLoading(false);
                            Toast.makeText(requireContext(), "Failed to update profile picture in database", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void setMainProfileImage(boolean useAvatar) {
        if (useAvatar && !hasSavedAvatar) {
            showAvatarCreatorDialog();
            return;
        }
        if (!useAvatar && TextUtils.isEmpty(currentProfilePicUrl)) {
            openImagePicker();
            return;
        }

        showLoading(true);
        repository.updateProfileVisualPreference(currentUserId, useAvatar, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                showLoading(false);
                currentAvatarEnabled = useAvatar;
                if (useAvatar) {
                    bindAvatar(currentAvatarConfig);
                } else {
                    bindPhoto(currentProfilePicUrl);
                }
                Toast.makeText(requireContext(), R.string.profile_image_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), R.string.profile_image_update_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isDevBypass = firebaseUser == null && DevSessionManager.shouldUseBypass(requireContext());

        if (isDevBypass) {
            currentRole = DevSessionManager.getBypassRole(requireContext());
            currentDisplayName = DevSessionManager.getDisplayName(requireContext());
            currentProfilePicUrl = "";
            hasSavedAvatar = false;
            currentAvatarEnabled = true;
            currentAvatarConfig = AvatarConfig.defaultsFor(currentDisplayName, currentRole);
            bindRoleBadge();
            tvFullName.setText(currentDisplayName);
            tvEmail.setText(DevSessionManager.getDisplayEmail(requireContext()));
            bindCreditBalance(0.0);
            switchDarkMode.setOnCheckedChangeListener(null);
            switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
            bindDarkModeListener();
            bindRoleNavigationRows();
            bindAvatar(currentAvatarConfig);
            bindProfileVisualButtonState(true);
            setupSosBadgeListener();
            restoreScrollPositionSoon();
            return;
        }

        if (firebaseUser == null || currentUserId == null) {
            currentDisplayName = getString(R.string.guest_user);
            currentProfilePicUrl = "";
            hasSavedAvatar = false;
            currentAvatarEnabled = false;
            tvFullName.setText(getString(R.string.guest_user));
            tvEmail.setText(getString(R.string.unknown_email));
            bindCreditBalance(0.0);
            currentRole = UserRoles.ATTENDEE;
            currentAvatarConfig = AvatarConfig.defaultsFor(currentDisplayName, currentRole);
            bindAvatar(currentAvatarConfig);
            bindRoleBadge();
            switchDarkMode.setOnCheckedChangeListener(null);
            switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
            bindDarkModeListener();
            hideRoleNavigationRows();
            bindProfileVisualButtonState(false);
            restoreScrollPositionSoon();
            return;
        }

        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;
                if (user == null) {
                    bindFallbackProfile(firebaseUser);
                    return;
                }

                if (!TextUtils.isEmpty(user.getFullName())) {
                    currentDisplayName = user.getFullName();
                    tvFullName.setText(user.getFullName());
                } else {
                    currentDisplayName = getString(R.string.guest_user);
                    tvFullName.setText(getString(R.string.guest_user));
                }

                if (!TextUtils.isEmpty(user.getEmail())) {
                    tvEmail.setText(user.getEmail());
                } else if (firebaseUser.getEmail() != null) {
                    tvEmail.setText(firebaseUser.getEmail());
                } else {
                    tvEmail.setText(getString(R.string.unknown_email));
                }

                String role = UserRoles.sanitize(user.getRole());
                currentRole = role.isEmpty() ? UserRoles.ATTENDEE : role;
                currentProfilePicUrl = user.getProfilePicUrl() == null ? "" : user.getProfilePicUrl();
                hasSavedAvatar = user.isAvatarEnabled()
                        || (user.getAvatarConfig() != null && !user.getAvatarConfig().isEmpty());
                currentAvatarEnabled = user.isAvatarEnabled();
                currentAvatarConfig = AvatarConfig.fromMap(user.getAvatarConfig(), currentDisplayName, currentRole);
                bindRoleBadge();
                bindRoleNavigationRows();
                bindCreditBalance(user.getCreditBalance());

                switchDarkMode.setOnCheckedChangeListener(null);
                switchDarkMode.setChecked(user.isDarkMode());
                bindDarkModeListener();

                bindProfileImage(user);
                bindProfileVisualButtonState(true);
                setupSosBadgeListener();
                restoreScrollPositionSoon();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                bindFallbackProfile(firebaseUser);
            }
        });
    }

    private void bindFallbackProfile(FirebaseUser firebaseUser) {
        currentDisplayName = getString(R.string.guest_user);
        currentProfilePicUrl = "";
        hasSavedAvatar = false;
        currentAvatarEnabled = false;
        tvFullName.setText(getString(R.string.guest_user));
        if (firebaseUser != null && firebaseUser.getEmail() != null) {
            tvEmail.setText(firebaseUser.getEmail());
        } else {
            tvEmail.setText(getString(R.string.unknown_email));
        }
        hideRoleNavigationRows();
        currentRole = UserRoles.ATTENDEE;
        currentAvatarConfig = AvatarConfig.defaultsFor(currentDisplayName, currentRole);
        bindAvatar(currentAvatarConfig);
        bindRoleBadge();
        switchDarkMode.setOnCheckedChangeListener(null);
        switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
        bindDarkModeListener();
        bindProfileVisualButtonState(currentUserId != null);
        restoreScrollPositionSoon();
    }

    private void bindProfileImage(User user) {
        if (currentAvatarEnabled) {
            bindAvatar(currentAvatarConfig);
            return;
        }

        if (!TextUtils.isEmpty(currentProfilePicUrl)) {
            bindPhoto(currentProfilePicUrl);
            return;
        }

        bindAvatar(currentAvatarConfig);
    }

    private void bindRoleNavigationRows() {
        boolean attendee = UserRoles.isAttendee(currentRole);
        boolean organizer = UserRoles.isOrganizer(currentRole);
        boolean admin = UserRoles.isAdmin(currentRole);

        sectionAttendeeTools.setVisibility(attendee ? View.VISIBLE : View.GONE);
        sectionOrganizerTools.setVisibility(organizer || admin ? View.VISIBLE : View.GONE);
        if (layoutCreditBalance != null) {
            layoutCreditBalance.setVisibility(attendee ? View.VISIBLE : View.GONE);
        }
    }

    private void hideRoleNavigationRows() {
        sectionAttendeeTools.setVisibility(View.GONE);
        sectionOrganizerTools.setVisibility(View.GONE);
        if (layoutCreditBalance != null) {
            layoutCreditBalance.setVisibility(View.GONE);
        }
    }

    private void bindCreditBalance(double creditBalance) {
        if (tvCreditBalance != null) {
            tvCreditBalance.setText(getString(R.string.credit_balance_format, creditBalance));
        }
        if (layoutCreditBalance != null) {
            layoutCreditBalance.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
        }
    }

    private void bindPhoto(String profilePicUrl) {
        Glide.with(requireContext())
                .load(profilePicUrl)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .centerCrop()
                .into(ivProfile);
    }

    private void bindAvatar(AvatarConfig avatarConfig) {
        AvatarConfig safeConfig = avatarConfig == null
                ? AvatarConfig.defaultsFor(currentDisplayName, currentRole)
                : avatarConfig;
        Bitmap avatarBitmap = AvatarRenderer.render(safeConfig, dpToPx(PROFILE_AVATAR_SIZE_DP));
        ivProfile.setImageBitmap(avatarBitmap);
    }

    private void showLoading(boolean isLoading) {
        if (progressBarProfile != null) {
            progressBarProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        bindProfileVisualButtonState(!isLoading && currentUserId != null);
    }

    private void bindProfileVisualButtonState(boolean enabled) {
        tvEditPhoto.setEnabled(enabled);
        tvEditPhoto.setAlpha(enabled ? 1f : 0.6f);
    }

    private void bindRoleBadge() {
        if (chipRole == null) {
            return;
        }

        if (UserRoles.isAdmin(currentRole)) {
            chipRole.setText(R.string.admin);
            chipRole.setChipBackgroundColorResource(R.color.role_admin_container);
            chipRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_admin_on_container));
        } else if (UserRoles.isOrganizer(currentRole)) {
            chipRole.setText(R.string.organizer);
            chipRole.setChipBackgroundColorResource(R.color.role_organizer_container);
            chipRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_organizer_on_container));
        } else {
            chipRole.setText(R.string.attendee);
            chipRole.setChipBackgroundColorResource(R.color.role_attendee_container);
            chipRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_attendee_on_container));
        }
    }

    private void bindDarkModeListener() {
        switchDarkMode.setOnCheckedChangeListener(null);
        switchDarkMode.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (WalkthroughManager.isActive()) {
                Toast.makeText(requireContext(), "Walkthrough mode: theme mode was not changed.", Toast.LENGTH_SHORT).show();
                switchDarkMode.setOnCheckedChangeListener(null);
                switchDarkMode.setChecked(!isChecked);
                bindDarkModeListener();
                return;
            }
            preserveScrollPositionForAppearanceChange();
            ThemeManager.applyThemePreference(requireContext(), isChecked);
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                repository.updateDarkMode(currentUserId, isChecked);
            }
        });
    }

    private void bindAccentPreference() {
        int accent = ThemeManager.getAccentPreference(requireContext());
        int labelResId;
        if (accent == ThemeManager.ACCENT_BLUE) {
            labelResId = R.string.accent_blue;
        } else if (accent == ThemeManager.ACCENT_GREEN) {
            labelResId = R.string.accent_green;
        } else if (accent == ThemeManager.ACCENT_ORANGE) {
            labelResId = R.string.accent_orange;
        } else if (accent == ThemeManager.ACCENT_PINK) {
            labelResId = R.string.accent_pink;
        } else if (accent == ThemeManager.ACCENT_TEAL) {
            labelResId = R.string.accent_teal;
        } else {
            labelResId = R.string.accent_purple;
        }

        tvAccentColorValue.setText(labelResId);
        ThemeManager.applyAccentPreview(viewAccentPreview, requireContext());
        applyHostAccent();
    }

    private void showAccentColorDialog() {
        String[] labels = {
                getString(R.string.accent_purple),
                getString(R.string.accent_blue),
                getString(R.string.accent_green),
                getString(R.string.accent_orange),
                getString(R.string.accent_pink),
                getString(R.string.accent_teal)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.accent_color)
                .setSingleChoiceItems(labels, ThemeManager.getAccentPreference(requireContext()), (dialog, which) -> {
                    preserveScrollPositionForAppearanceChange();
                    ThemeManager.setAccentPreference(requireContext(), which);
                    bindAccentPreference();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.accent_updated, Toast.LENGTH_SHORT).show();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyHostAccent() {
        if (!isAdded()) {
            return;
        }

        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
        ThemeManager.applyAccentToMainNavigation(requireContext(), bottomNavigationView, null);
    }

    private void preserveScrollPosition() {
        if (profileScrollView != null) {
            pendingScrollY = profileScrollView.getScrollY();
        }
        lastKnownScrollY = pendingScrollY;
    }

    private void preserveScrollPositionForAppearanceChange() {
        preserveScrollPosition();
        persistScrollRestoreRequest(pendingScrollY);
    }

    private void loadPendingScrollRestoreRequest() {
        SharedPreferences prefs = getScrollPrefs();
        if (prefs == null) {
            return;
        }
        if (!prefs.getBoolean(KEY_RESTORE_SCROLL, false)) {
            return;
        }

        pendingScrollY = Math.max(pendingScrollY, prefs.getInt(KEY_RESTORE_SCROLL_Y, pendingScrollY));
        lastKnownScrollY = pendingScrollY;
    }

    private void persistScrollRestoreRequest(int scrollY) {
        SharedPreferences prefs = getScrollPrefs();
        if (prefs == null) {
            return;
        }

        prefs.edit()
                .putBoolean(KEY_RESTORE_SCROLL, scrollY > 0)
                .putInt(KEY_RESTORE_SCROLL_Y, Math.max(0, scrollY))
                .apply();
    }

    private void clearScrollRestoreRequest() {
        SharedPreferences prefs = getScrollPrefs();
        if (prefs == null) {
            return;
        }

        prefs.edit()
                .remove(KEY_RESTORE_SCROLL)
                .remove(KEY_RESTORE_SCROLL_Y)
                .apply();
    }

    @Nullable
    private SharedPreferences getScrollPrefs() {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        return context
                .getApplicationContext()
                .getSharedPreferences(SCROLL_PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void restoreScrollPositionSoon() {
        if (profileScrollView == null || pendingScrollY <= 0) {
            return;
        }

        int targetScrollY = pendingScrollY;
        profileScrollView.post(() -> restoreScrollPositionWhenReady(targetScrollY, 0));
    }

    private void restoreScrollPositionWhenReady(int targetScrollY, int attempt) {
        if (profileScrollView == null) {
            return;
        }

        View content = profileScrollView.getChildCount() > 0 ? profileScrollView.getChildAt(0) : null;
        int maxScrollY = content == null ? 0 : Math.max(0, content.getHeight() - profileScrollView.getHeight());
        profileScrollView.scrollTo(0, Math.min(targetScrollY, maxScrollY));

        if (maxScrollY >= targetScrollY || attempt >= SCROLL_RESTORE_MAX_ATTEMPTS) {
            clearScrollRestoreRequest();
            return;
        }

        profileScrollView.postDelayed(
                () -> restoreScrollPositionWhenReady(targetScrollY, attempt + 1),
                SCROLL_RESTORE_RETRY_MS
        );
    }

    @Override
    public void resetScrollToTop() {
        pendingScrollY = 0;
        lastKnownScrollY = 0;
        clearScrollRestoreRequest();
        if (profileScrollView != null) {
            profileScrollView.post(() -> profileScrollView.scrollTo(0, 0));
        }
    }

    private void runAfterTouchFeedback(View sourceView, Runnable action) {
        sourceView.postDelayed(action, TAP_FEEDBACK_DELAY_MS);
    }

    private boolean isDarkColor(int color) {
        double luminance = (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return luminance < 0.55;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout_confirm_title))
                .setMessage(getString(R.string.logout_confirm_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    DevSessionManager.clearBypass(requireContext());

                    Intent intent = new Intent(requireContext(), WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private interface AvatarOptionSetter {
        void set(int index);
    }

    private interface ProfileVisualAction {
        void run();
    }
}
