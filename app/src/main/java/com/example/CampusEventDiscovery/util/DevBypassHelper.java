package com.example.CampusEventDiscovery.util;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Shared helper for launching the developer bypass role picker.
 */
public final class DevBypassHelper {

    private DevBypassHelper() {
    }

    public static void showRolePicker(@NonNull Activity activity) {
        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_dev_bypass_role_picker, null, false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();

        MaterialButton btnAttendee = dialogView.findViewById(R.id.btnDevAttendee);
        MaterialButton btnOrganizer = dialogView.findViewById(R.id.btnDevOrganizer);
        MaterialButton btnAdmin = dialogView.findViewById(R.id.btnDevAdmin);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDevCancel);

        btnAttendee.setOnClickListener(v -> launchDevBypass(activity, dialog, UserRoles.ATTENDEE));
        btnOrganizer.setOnClickListener(v -> launchDevBypass(activity, dialog, UserRoles.ORGANIZER));
        btnAdmin.setOnClickListener(v -> launchDevBypass(activity, dialog, UserRoles.ADMIN));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static void launchDevBypass(@NonNull Activity activity,
                                        @NonNull AlertDialog dialog,
                                        @NonNull String role) {
        dialog.dismiss();
        FirebaseAuth.getInstance().signOut();
        DevSessionManager.enableBypass(activity, role);

        Toast.makeText(
                activity,
                activity.getString(R.string.dev_bypass_enabled, getRoleLabel(activity, role)),
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private static String getRoleLabel(@NonNull Activity activity, @NonNull String role) {
        if (UserRoles.isOrganizer(role)) {
            return activity.getString(R.string.organizer);
        }
        if (UserRoles.isAdmin(role)) {
            return activity.getString(R.string.admin);
        }
        return activity.getString(R.string.attendee);
    }
}
