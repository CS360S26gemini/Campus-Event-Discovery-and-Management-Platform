package com.example.CampusEventDiscovery.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.WelcomeActivity;
import com.example.CampusEventDiscovery.model.AvatarConfig;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.ui.calendar.EventCalendarFragment;
import com.example.CampusEventDiscovery.ui.myevents.MyEventsFragment;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.util.AvatarRenderer;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.NavigationTransitions;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * ProfileFragment.java
 *
 * Profile and settings screen.
 */
public class ProfileFragment extends Fragment {

    private static final long TAP_FEEDBACK_DELAY_MS = 140L;
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
    private MaterialButton tvEditPhoto;
    private TextView tvFullName;
    private TextView tvEmail;
    private Chip chipRole;
    private Switch switchDarkMode;
    private View cardDarkMode;
    private View rowMyEvents;
    private View rowMemories;
    private View rowManageEvents;
    private View rowCalendar;
    private View rowNotifications;
    private View rowAccountSettings;
    private MaterialButton btnHelp;
    private MaterialButton btnLogout;
    private ProgressBar progressBarProfile;

    private EventRepository repository;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private String currentUserId;
    private String currentRole = "attendee";
    private String currentDisplayName = "";
    private AvatarConfig currentAvatarConfig;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
        setupStaticActions();
        loadProfile();
    }

    private void bindViews(View view) {
        ivProfile = view.findViewById(R.id.ivProfile);
        tvEditPhoto = view.findViewById(R.id.tvEditPhoto);
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        chipRole = view.findViewById(R.id.chipRole);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        cardDarkMode = view.findViewById(R.id.cardDarkMode);
        rowMyEvents = view.findViewById(R.id.rowMyEvents);
        rowMemories = view.findViewById(R.id.rowMemories);
        rowManageEvents = view.findViewById(R.id.rowManageEvents);
        rowCalendar = view.findViewById(R.id.rowCalendar);
        rowNotifications = view.findViewById(R.id.rowNotifications);
        rowAccountSettings = view.findViewById(R.id.rowAccountSettings);
        btnHelp = view.findViewById(R.id.btn_help);
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBarProfile = view.findViewById(R.id.progressBarProfile);
    }

    private void setupStaticActions() {
        tvEditPhoto.setOnClickListener(v -> runAfterTouchFeedback(v, this::showProfileImageOptions));

        cardDarkMode.setOnClickListener(v -> switchDarkMode.toggle());

        bindDarkModeListener();

        rowMyEvents.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                NavigationTransitions.replace(
                        requireActivity().getSupportFragmentManager(),
                        R.id.fragmentContainer,
                        MyEventsFragment.newInstance(true),
                        true,
                        true
                )
        ));

        rowMemories.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            Intent intent = new Intent(requireContext(), MemoriesActivity.class);
            startActivity(intent);
        }));

        rowManageEvents.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            Intent intent = new Intent(requireContext(), ManageEventsActivity.class);
            startActivity(intent);
        }));

        rowCalendar.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                NavigationTransitions.replace(
                        requireActivity().getSupportFragmentManager(),
                        R.id.fragmentContainer,
                        new EventCalendarFragment(),
                        true,
                        true
                )
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
                NavigationTransitions.replace(
                        requireActivity().getSupportFragmentManager(),
                        R.id.fragmentContainer,
                        new HelpFragment(),
                        true,
                        true
                )
        ));

        btnLogout.setOnClickListener(v -> runAfterTouchFeedback(v, this::showLogoutDialog));
    }

    private void showProfileImageOptions() {
        if (!isAdded() || currentUserId == null) {
            return;
        }

        String[] options = {
                getString(R.string.upload_photo),
                getString(R.string.create_avatar)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_visual_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openImagePicker();
                    } else {
                        showAvatarCreatorDialog();
                    }
                })
                .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
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

    private void loadProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isDevBypass = firebaseUser == null && DevSessionManager.shouldUseBypass(requireContext());

        if (isDevBypass) {
            currentRole = DevSessionManager.getBypassRole(requireContext());
            currentDisplayName = DevSessionManager.getDisplayName(requireContext());
            currentAvatarConfig = AvatarConfig.defaultsFor(currentDisplayName, currentRole);
            bindRoleBadge();
            tvFullName.setText(currentDisplayName);
            tvEmail.setText(DevSessionManager.getDisplayEmail(requireContext()));
            switchDarkMode.setOnCheckedChangeListener(null);
            switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
            bindDarkModeListener();
            rowMyEvents.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
            rowMemories.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
            rowManageEvents.setVisibility(UserRoles.isOrganizer(currentRole) ? View.VISIBLE : View.GONE);
            bindAvatar(currentAvatarConfig);
            tvEditPhoto.setEnabled(false);
            tvEditPhoto.setAlpha(0.6f);
            return;
        }

        if (firebaseUser == null || currentUserId == null) {
            currentDisplayName = getString(R.string.guest_user);
            tvFullName.setText(getString(R.string.guest_user));
            tvEmail.setText(getString(R.string.unknown_email));
            currentRole = UserRoles.ATTENDEE;
            currentAvatarConfig = AvatarConfig.defaultsFor(currentDisplayName, currentRole);
            bindAvatar(currentAvatarConfig);
            bindRoleBadge();
            switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
            rowMyEvents.setVisibility(View.GONE);
            rowMemories.setVisibility(View.GONE);
            rowManageEvents.setVisibility(View.GONE);
            tvEditPhoto.setEnabled(false);
            tvEditPhoto.setAlpha(0.6f);
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
                currentAvatarConfig = AvatarConfig.fromMap(user.getAvatarConfig(), currentDisplayName, currentRole);
                bindRoleBadge();
                rowMyEvents.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
                rowMemories.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
                rowManageEvents.setVisibility(UserRoles.isOrganizer(currentRole) ? View.VISIBLE : View.GONE);

                switchDarkMode.setOnCheckedChangeListener(null);
                switchDarkMode.setChecked(user.isDarkMode());
                bindDarkModeListener();

                bindProfileImage(user);
                tvEditPhoto.setEnabled(true);
                tvEditPhoto.setAlpha(1f);
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
        tvFullName.setText(getString(R.string.guest_user));
        if (firebaseUser != null && firebaseUser.getEmail() != null) {
            tvEmail.setText(firebaseUser.getEmail());
        } else {
            tvEmail.setText(getString(R.string.unknown_email));
        }
        rowMyEvents.setVisibility(View.GONE);
        rowMemories.setVisibility(View.GONE);
        rowManageEvents.setVisibility(View.GONE);
        currentRole = UserRoles.ATTENDEE;
        currentAvatarConfig = AvatarConfig.defaultsFor(currentDisplayName, currentRole);
        bindAvatar(currentAvatarConfig);
        bindRoleBadge();
        switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
        tvEditPhoto.setEnabled(false);
        tvEditPhoto.setAlpha(0.6f);
    }

    private void bindProfileImage(User user) {
        if (user != null && user.isAvatarEnabled()) {
            bindAvatar(currentAvatarConfig);
            return;
        }

        if (user != null && !TextUtils.isEmpty(user.getProfilePicUrl())) {
            Glide.with(requireContext())
                    .load(user.getProfilePicUrl())
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .centerCrop()
                    .into(ivProfile);
            return;
        }

        bindAvatar(currentAvatarConfig);
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
        tvEditPhoto.setEnabled(!isLoading);
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
            ThemeManager.applyThemePreference(requireContext(), isChecked);
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                repository.updateDarkMode(currentUserId, isChecked);
            }
        });
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
}
