package com.example.CampusEventDiscovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * OrganizerVendorsFragment.java
 *
 * Organizer screen showing active events with quick access
 * to check vendor requests for each event.
 */
public class OrganizerVendorsFragment extends Fragment {

    private static final String VENDOR_MANAGEMENT_ACTIVITY =
            "com.example.CampusEventDiscovery.ui.organizer.VendorManagementActivity";

    private EventRepository repository;
    private RecyclerView rvEvents;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private String currentUserId;

    public OrganizerVendorsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_vendors, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        rvEvents = view.findViewById(R.id.rvEvents);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null ? firebaseUser.getUid() : null;

        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        loadEvents();
    }

    private void loadEvents() {
        progressBar.setVisibility(View.VISIBLE);
        rvEvents.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        repository.getApprovedOrganizerEvents(currentUserId, new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (events == null || events.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvEvents.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvEvents.setVisibility(View.VISIBLE);
                    rvEvents.setAdapter(new EventVendorAdapter(events));
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                rvEvents.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openVendorManagement(Event event) {
        if (event == null || TextUtils.isEmpty(event.getEventId()) || !isAdded()) {
            return;
        }

        Intent intent = new Intent();
        intent.setClassName(requireContext(), VENDOR_MANAGEMENT_ACTIVITY);
        intent.putExtra("eventId", event.getEventId());
        intent.putExtra("eventTitle", safeText(event.getTitle(), getString(R.string.app_name)));

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String safeText(String text, String fallback) {
        return TextUtils.isEmpty(text) ? fallback : text;
    }

    private class EventVendorAdapter extends RecyclerView.Adapter<EventVendorAdapter.EventVendorViewHolder> {

        private final List<Event> events;

        EventVendorAdapter(List<Event> events) {
            this.events = events;
        }

        @NonNull
        @Override
        public EventVendorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView title = new TextView(parent.getContext());
            title.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            ));
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setTextSize(16f);

            Button btnCheckVendors = new Button(parent.getContext());
            btnCheckVendors.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            btnCheckVendors.setAllCaps(false);
            btnCheckVendors.setText("Check Vendors");

            row.addView(title);
            row.addView(btnCheckVendors);

            return new EventVendorViewHolder(row, title, btnCheckVendors);
        }

        @Override
        public void onBindViewHolder(@NonNull EventVendorViewHolder holder, int position) {
            Event event = events.get(position);
            holder.tvTitle.setText(safeText(event.getTitle(), getString(R.string.app_name)));
            holder.btnCheckVendors.setOnClickListener(v -> openVendorManagement(event));
        }

        @Override
        public int getItemCount() {
            return events == null ? 0 : events.size();
        }

        class EventVendorViewHolder extends RecyclerView.ViewHolder {

            private final TextView tvTitle;
            private final Button btnCheckVendors;

            EventVendorViewHolder(@NonNull View itemView,
                                  TextView tvTitle,
                                  Button btnCheckVendors) {
                super(itemView);
                this.tvTitle = tvTitle;
                this.btnCheckVendors = btnCheckVendors;
            }
        }
    }
}
