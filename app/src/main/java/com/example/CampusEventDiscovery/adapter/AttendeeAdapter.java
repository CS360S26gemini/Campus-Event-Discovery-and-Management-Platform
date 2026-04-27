package com.example.CampusEventDiscovery.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.EventAttendee;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AttendeeAdapter.java
 *
 * RecyclerView adapter for organizer attendee management.
 */
public class AttendeeAdapter extends ListAdapter<EventAttendee, AttendeeAdapter.AttendeeViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<EventAttendee> allAttendees = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private final OnSelectionChangedListener selectionChangedListener;
    private final SimpleDateFormat checkedInDateFormat =
            new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public AttendeeAdapter(OnSelectionChangedListener selectionChangedListener) {
        super(DIFF_CALLBACK);
        this.selectionChangedListener = selectionChangedListener;
    }

    @NonNull
    @Override
    public AttendeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendee, parent, false);
        return new AttendeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendeeViewHolder holder, int position) {
        EventAttendee attendee = getItem(position);
        String attendeeId = resolveSelectionId(attendee);

        holder.tvName.setText(TextUtils.isEmpty(attendee.getFullName())
                ? holder.itemView.getContext().getString(R.string.participant_name_fallback)
                : attendee.getFullName());
        holder.tvStatus.setText(buildStatusText(holder.itemView, attendee));

        boolean isSelected = selectedIds.contains(attendeeId);
        holder.checkSelected.setChecked(isSelected);

        View.OnClickListener toggleSelection = v -> {
            if (selectedIds.contains(attendeeId)) {
                selectedIds.remove(attendeeId);
            } else {
                selectedIds.add(attendeeId);
            }
            notifyItemChanged(holder.getBindingAdapterPosition());
            notifySelectionChanged();
        };

        holder.itemView.setOnClickListener(toggleSelection);
        holder.cardAttendee.setOnClickListener(toggleSelection);
        holder.checkSelected.setOnClickListener(toggleSelection);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    public void updateData(List<EventAttendee> attendees) {
        allAttendees.clear();
        if (attendees != null) {
            allAttendees.addAll(attendees);
        }
        filter("");
    }

    public void filter(String query) {
        String safeQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());

        List<EventAttendee> filteredAttendees = new ArrayList<>();
        for (EventAttendee attendee : allAttendees) {
            String fullName = attendee.getFullName() == null ? "" : attendee.getFullName().toLowerCase(Locale.getDefault());
            if (safeQuery.isEmpty() || fullName.contains(safeQuery)) {
                filteredAttendees.add(attendee);
            }
        }

        pruneSelectionToKnownAttendees();
        submitList(new ArrayList<>(filteredAttendees));
        notifySelectionChanged();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public List<EventAttendee> getSelectedAttendees() {
        List<EventAttendee> selectedAttendees = new ArrayList<>();
        for (EventAttendee attendee : allAttendees) {
            if (selectedIds.contains(resolveSelectionId(attendee))) {
                selectedAttendees.add(attendee);
            }
        }
        return selectedAttendees;
    }

    private void pruneSelectionToKnownAttendees() {
        Set<String> validIds = new HashSet<>();
        for (EventAttendee attendee : allAttendees) {
            validIds.add(resolveSelectionId(attendee));
        }
        selectedIds.retainAll(validIds);
    }

    private void notifySelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedIds.size());
        }
    }

    private String resolveSelectionId(EventAttendee attendee) {
        if (!TextUtils.isEmpty(attendee.getUserId())) {
            return attendee.getUserId();
        }
        if (!TextUtils.isEmpty(attendee.getQrToken())) {
            return attendee.getQrToken();
        }
        return attendee.getFullName() == null ? "" : attendee.getFullName();
    }

    private String buildStatusText(View itemView, EventAttendee attendee) {
        if (attendee.isBlacklisted()) {
            Timestamp blacklistedAt = attendee.getBlacklistedAt();
            if (blacklistedAt == null) {
                return itemView.getContext().getString(R.string.blacklisted_attendees);
            }
            return itemView.getContext().getString(
                    R.string.participant_blacklisted_at,
                    checkedInDateFormat.format(blacklistedAt.toDate())
            );
        }

        if (!attendee.isCheckedIn()) {
            return itemView.getContext().getString(R.string.participant_registered_status);
        }

        Timestamp checkedInAt = attendee.getCheckedInAt();
        if (checkedInAt == null) {
            return itemView.getContext().getString(R.string.participant_checked_in_status);
        }

        return itemView.getContext().getString(
                R.string.participant_checked_in_at,
                checkedInDateFormat.format(checkedInAt.toDate())
        );
    }

    private static final DiffUtil.ItemCallback<EventAttendee> DIFF_CALLBACK = new DiffUtil.ItemCallback<EventAttendee>() {
        @Override
        public boolean areItemsTheSame(@NonNull EventAttendee oldItem, @NonNull EventAttendee newItem) {
            return TextUtils.equals(resolveSelectionIdStatic(oldItem), resolveSelectionIdStatic(newItem));
        }

        @Override
        public boolean areContentsTheSame(@NonNull EventAttendee oldItem, @NonNull EventAttendee newItem) {
            return TextUtils.equals(oldItem.getFullName(), newItem.getFullName())
                    && TextUtils.equals(oldItem.getUserId(), newItem.getUserId())
                    && TextUtils.equals(oldItem.getQrToken(), newItem.getQrToken())
                    && oldItem.isBlacklisted() == newItem.isBlacklisted()
                    && oldItem.isCheckedIn() == newItem.isCheckedIn()
                    && timestampMillis(oldItem.getBlacklistedAt()) == timestampMillis(newItem.getBlacklistedAt())
                    && timestampMillis(oldItem.getCheckedInAt()) == timestampMillis(newItem.getCheckedInAt());
        }
    };

    private static String resolveSelectionIdStatic(EventAttendee attendee) {
        if (attendee == null) {
            return "";
        }
        if (!TextUtils.isEmpty(attendee.getUserId())) {
            return attendee.getUserId();
        }
        if (!TextUtils.isEmpty(attendee.getQrToken())) {
            return attendee.getQrToken();
        }
        return attendee.getFullName() == null ? "" : attendee.getFullName();
    }

    private static long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? Long.MIN_VALUE : timestamp.toDate().getTime();
    }

    static class AttendeeViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardAttendee;
        final TextView tvName;
        final TextView tvStatus;
        final CheckBox checkSelected;

        AttendeeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAttendee = itemView.findViewById(R.id.cardAttendee);
            tvName = itemView.findViewById(R.id.tvAttendeeName);
            tvStatus = itemView.findViewById(R.id.tvAttendeeStatus);
            checkSelected = itemView.findViewById(R.id.checkSelected);
        }
    }
}
