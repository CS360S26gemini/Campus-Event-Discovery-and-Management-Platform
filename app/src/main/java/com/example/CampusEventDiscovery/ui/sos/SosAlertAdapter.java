package com.example.CampusEventDiscovery.ui.sos;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.SosAlert;

import java.util.ArrayList;
import java.util.List;

/** RecyclerView adapter that renders SOS alerts in the dashboard. */
public class SosAlertAdapter extends ListAdapter<SosAlert, SosAlertAdapter.AlertHolder> {

    public SosAlertAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setAlerts(List<SosAlert> newAlerts) {
        submitList(newAlerts == null ? null : new ArrayList<>(newAlerts));
    }

    @NonNull
    @Override
    public AlertHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sos_alert, parent, false);
        return new AlertHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertHolder holder, int position) {
        holder.bind(getItem(position));
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    private static final DiffUtil.ItemCallback<SosAlert> DIFF_CALLBACK = new DiffUtil.ItemCallback<SosAlert>() {
        @Override
        public boolean areItemsTheSame(@NonNull SosAlert oldItem, @NonNull SosAlert newItem) {
            return oldItem.getTimestamp() == newItem.getTimestamp()
                    && TextUtils.equals(oldItem.getEventId(), newItem.getEventId())
                    && TextUtils.equals(oldItem.getDisplayName(), newItem.getDisplayName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SosAlert oldItem, @NonNull SosAlert newItem) {
            return TextUtils.equals(oldItem.getDisplayName(), newItem.getDisplayName())
                    && TextUtils.equals(oldItem.getStatus(), newItem.getStatus())
                    && TextUtils.equals(oldItem.getEventName(), newItem.getEventName())
                    && TextUtils.equals(oldItem.getMapsUrl(), newItem.getMapsUrl())
                    && oldItem.getTimestamp() == newItem.getTimestamp();
        }
    };

    static class AlertHolder extends RecyclerView.ViewHolder {

        private final TextView tvReporter;
        private final TextView tvStatus;
        private final TextView tvEventName;
        private final TextView tvTimestamp;
        private final TextView tvMapsUrl;

        AlertHolder(@NonNull View itemView) {
            super(itemView);
            tvReporter = itemView.findViewById(R.id.tvItemReporter);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            tvEventName = itemView.findViewById(R.id.tvItemEventName);
            tvTimestamp = itemView.findViewById(R.id.tvItemTimestamp);
            tvMapsUrl = itemView.findViewById(R.id.tvItemMapsUrl);
        }

        void bind(SosAlert alert) {
            tvReporter.setText(TextUtils.isEmpty(alert.getDisplayName())
                    ? itemView.getContext().getString(R.string.sos_label_unknown_reporter)
                    : alert.getDisplayName());
            tvStatus.setText(TextUtils.isEmpty(alert.getStatus())
                    ? itemView.getContext().getString(R.string.sos_status_active)
                    : alert.getStatus());
            tvEventName.setText(TextUtils.isEmpty(alert.getEventName())
                    ? ""
                    : itemView.getContext().getString(R.string.sos_label_event, alert.getEventName()));

            if (alert.getTimestamp() > 0L) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        alert.getTimestamp(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                tvTimestamp.setText(relative);
            } else {
                tvTimestamp.setText("");
            }

            String mapsUrl = alert.getMapsUrl();
            if (TextUtils.isEmpty(mapsUrl)) {
                tvMapsUrl.setText("");
                tvMapsUrl.setVisibility(View.GONE);
            } else {
                tvMapsUrl.setText(mapsUrl);
                tvMapsUrl.setVisibility(View.VISIBLE);
                tvMapsUrl.setOnClickListener(v -> {
                    try {
                        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
                        browse.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        v.getContext().startActivity(browse);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }
}
