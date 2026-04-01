package com.example.campuseventdiscovery.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.campuseventdiscovery.R;
import com.example.campuseventdiscovery.WelcomeActivity;
import com.example.campuseventdiscovery.model.User;
import com.example.campuseventdiscovery.repository.EventRepository;
import com.example.campuseventdiscovery.ui.calendar.EventCalendarFragment;
import com.example.campuseventdiscovery.ui.myevents.MyEventsFragment;
import com.example.campuseventdiscovery.ui.organizer.ManageEventsActivity;
import com.example.campuseventdiscovery.util.DevSessionManager;
import com.example.campuseventdiscovery.util.NavigationTransitions;
import com.example.campuseventdiscovery.util.ThemeManager;
import com.example.campuseventdiscovery.util.UserRoles;
import com.google.android.material.button.MaterialButton;
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

    private CircleImageView ivProfile;
    private MaterialButton tvEditPhoto;
    private TextView tvFullName;
    private TextView tvEmail;
    private Switch switchDarkMode;
    private View cardDarkMode;
    private View rowMyEvents;
    private View rowMemories;
    private View rowManageEvents;
    private View rowCalendar;
    private View rowNotifications;
    private View rowAccountSettings;
    private MaterialButton btnLogout;
    private ProgressBar progressBarProfile;

    private EventRepository repository;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private String currentUserId;
    private String currentRole = "attendee";

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
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        cardDarkMode = view.findViewById(R.id.cardDarkMode);
        rowMyEvents = view.findViewById(R.id.rowMyEvents);
        rowMemories = view.findViewById(R.id.rowMemories);
        rowManageEvents = view.findViewById(R.id.rowManageEvents);
        rowCalendar = view.findViewById(R.id.rowCalendar);
        rowNotifications = view.findViewById(R.id.rowNotifications);
        rowAccountSettings = view.findViewById(R.id.rowAccountSettings);
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBarProfile = view.findViewById(R.id.progressBarProfile);
    }

    private void setupStaticActions() {
        tvEditPhoto.setOnClickListener(v -> runAfterTouchFeedback(v, () -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        }));

        cardDarkMode.setOnClickListener(v -> switchDarkMode.toggle());

        bindDarkModeListener();

        rowMyEvents.setOnClickListener(v -> runAfterTouchFeedback(v, () ->
                NavigationTransitions.replace(
                        requireActivity().getSupportFragmentManager(),
                        R.id.fragmentContainer,
                        new MyEventsFragment(),
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

        btnLogout.setOnClickListener(v -> runAfterTouchFeedback(v, this::showLogoutDialog));
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
                    String url = downloadUri.toString();
                    repository.updateProfilePic(currentUserId, url, new EventRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
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
                            showLoading(false);
                            Toast.makeText(requireContext(), "Failed to update profile picture in database", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isDevBypass = firebaseUser == null && DevSessionManager.shouldUseBypass(requireContext());

        if (isDevBypass) {
            currentRole = DevSessionManager.getBypassRole(requireContext());
            tvFullName.setText(DevSessionManager.getDisplayName(requireContext()));
            tvEmail.setText(DevSessionManager.getDisplayEmail(requireContext()));
            switchDarkMode.setOnCheckedChangeListener(null);
            switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
            bindDarkModeListener();
            rowMyEvents.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
            rowMemories.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
            rowManageEvents.setVisibility(UserRoles.isOrganizer(currentRole) ? View.VISIBLE : View.GONE);
            ivProfile.setImageResource(android.R.drawable.sym_def_app_icon);
            tvEditPhoto.setEnabled(false);
            tvEditPhoto.setAlpha(0.6f);
            return;
        }

        if (firebaseUser == null || currentUserId == null) {
            tvFullName.setText(getString(R.string.guest_user));
            tvEmail.setText(getString(R.string.unknown_email));
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
                if (user != null) {
                    if (!TextUtils.isEmpty(user.getFullName())) {
                        tvFullName.setText(user.getFullName());
                    } else {
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
                    rowMyEvents.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
                    rowMemories.setVisibility(UserRoles.isAttendee(currentRole) ? View.VISIBLE : View.GONE);
                    rowManageEvents.setVisibility(UserRoles.isOrganizer(currentRole) ? View.VISIBLE : View.GONE);

                    switchDarkMode.setOnCheckedChangeListener(null);
                    switchDarkMode.setChecked(user.isDarkMode());
                    bindDarkModeListener();

                    if (!TextUtils.isEmpty(user.getProfilePicUrl())) {
                        Glide.with(requireContext())
                                .load(user.getProfilePicUrl())
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .centerCrop()
                                .into(ivProfile);
                    } else {
                        ivProfile.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                    tvEditPhoto.setEnabled(true);
                    tvEditPhoto.setAlpha(1f);
                }
            }

            @Override
            public void onError(Exception e) {
                tvFullName.setText(getString(R.string.guest_user));
                if (firebaseUser.getEmail() != null) {
                    tvEmail.setText(firebaseUser.getEmail());
                } else {
                    tvEmail.setText(getString(R.string.unknown_email));
                }
                rowMyEvents.setVisibility(View.GONE);
                rowMemories.setVisibility(View.GONE);
                rowManageEvents.setVisibility(View.GONE);
                switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(requireContext()));
                tvEditPhoto.setEnabled(false);
                tvEditPhoto.setAlpha(0.6f);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBarProfile != null) {
            progressBarProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        tvEditPhoto.setEnabled(!isLoading);
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
}
