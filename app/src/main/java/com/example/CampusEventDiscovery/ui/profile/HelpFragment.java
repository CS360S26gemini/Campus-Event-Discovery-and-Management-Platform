package com.example.CampusEventDiscovery.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class HelpFragment extends Fragment {

    private LinearLayout containerHelpGuides;
    private EventRepository repository;
    private String currentUserId;
    private String currentRole = UserRoles.ATTENDEE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new EventRepository();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : DevSessionManager.getEffectiveUserId(requireContext());

        MaterialToolbar toolbar = view.findViewById(R.id.toolbarHelp);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        containerHelpGuides = view.findViewById(R.id.containerHelpGuides);
        resolveRole();
    }

    private void resolveRole() {
        if (DevSessionManager.shouldUseBypass(requireContext())) {
            currentRole = UserRoles.sanitize(DevSessionManager.getBypassRole(requireContext()));
            bindGuides();
            return;
        }
        if (TextUtils.isEmpty(currentUserId)) {
            bindGuides();
            return;
        }
        repository.getUserData(currentUserId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;
                currentRole = user == null ? UserRoles.ATTENDEE : UserRoles.sanitize(user.getRole());
                if (TextUtils.isEmpty(currentRole)) currentRole = UserRoles.ATTENDEE;
                bindGuides();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                currentRole = UserRoles.ATTENDEE;
                bindGuides();
            }
        });
    }

    private void bindGuides() {
        containerHelpGuides.removeAllViews();
        for (Guide guide : guidesForRole(currentRole)) {
            containerHelpGuides.addView(createGuideCard(guide));
        }
        ThemeManager.applyAccentToActivity(requireActivity());
    }

    private View createGuideCard(Guide guide) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.spacing_sm));
        card.setLayoutParams(cardParams);
        card.setClickable(true);
        card.setFocusable(true);
        card.setCardElevation(0f);
        card.setStrokeWidth(1);
        card.setRadius(getResources().getDimension(R.dimen.event_card_radius));
        card.setStrokeColor(resolveAttrColor(com.google.android.material.R.attr.colorOutlineVariant));
        card.setCardBackgroundColor(resolveAttrColor(com.google.android.material.R.attr.colorSurface));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        content.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(requireContext());
        title.setText(guide.title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        title.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurface));

        TextView subtitle = new TextView(requireContext());
        subtitle.setText(guide.subtitle);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        subtitle.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        subtitle.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_xs), 0, 0);

        TextView action = new TextView(requireContext());
        action.setText(R.string.start_walkthrough);
        action.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        action.setTextColor(ThemeManager.getAccentColor(requireContext()));
        action.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_sm), 0, 0);

        content.addView(title);
        content.addView(subtitle);
        content.addView(action);
        card.addView(content);
        card.setOnClickListener(v -> WalkthroughManager.start(requireActivity(), guide.id));
        return card;
    }

    private List<Guide> guidesForRole(String role) {
        List<Guide> guides = new ArrayList<>();
        if (UserRoles.isAdmin(role)) {
            guides.add(new Guide("admin_review", "Review pending proposals", "Walk through the real admin review queue and approval screen."));
            guides.add(new Guide("admin_dashboard", "Use admin dashboard filters", "Highlight pending/rejected filters and the real proposal list."));
            guides.add(new Guide("admin_sos", "Monitor SOS alerts", "Open the real SOS dashboard and highlight alert monitoring."));
            return guides;
        }
        if (UserRoles.isOrganizer(role)) {
            guides.add(new Guide("organizer_create", "Create an event proposal", "Walk through the actual proposal form safely."));
            guides.add(new Guide("organizer_manage", "Manage events", "Open the real management screen and explain event sections."));
            guides.add(new Guide("organizer_scan", "Scan tickets", "Highlight the real scanner controls."));
            guides.add(new Guide("organizer_sos", "Monitor event SOS alerts", "Open the real SOS dashboard for organizers."));
            return guides;
        }
        guides.add(new Guide("attendee_register", "Register and RSVP for an event", "Use real Search, Event Detail, and Checkout screens with local demo data."));
        guides.add(new Guide("attendee_search", "Search and filter events", "Highlight the real search field, filters, sort, and result list."));
        guides.add(new Guide("attendee_ticket", "Find your ticket QR", "Open My Events and the real ticket screen in safe demo mode."));
        guides.add(new Guide("attendee_memories", "Use memory folders", "Open the real memories page and highlight attended-event folders."));
        guides.add(new Guide("attendee_sos", "Use SOS safely", "Highlight the real SOS button without sending an alert."));
        return guides;
    }

    private int resolveAttrColor(int attr) {
        android.util.TypedValue value = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    private static final class Guide {
        final String id;
        final String title;
        final String subtitle;

        Guide(String id, String title, String subtitle) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
        }
    }
}
