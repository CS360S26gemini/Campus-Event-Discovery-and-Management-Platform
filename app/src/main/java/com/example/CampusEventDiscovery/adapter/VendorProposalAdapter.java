package com.example.CampusEventDiscovery.adapter;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.VendorProposal;
import com.example.CampusEventDiscovery.util.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Locale;

public class VendorProposalAdapter extends ListAdapter<VendorProposal, VendorProposalAdapter.VendorViewHolder> {

    public interface OnVendorActionListener {
        void onApprove(VendorProposal proposal);
        void onReject(VendorProposal proposal);
    }

    private final boolean showAdminActions;
    private final OnVendorActionListener listener;

    public VendorProposalAdapter(List<VendorProposal> proposals, boolean showAdminActions, OnVendorActionListener listener) {
        super(DIFF_CALLBACK);
        this.showAdminActions = showAdminActions;
        this.listener = listener;
        submitList(proposals == null ? null : new java.util.ArrayList<>(proposals));
    }

    @NonNull
    @Override
    public VendorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vendor_proposal, parent, false);
        return new VendorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VendorViewHolder holder, int position) {
        VendorProposal proposal = getItem(position);
        holder.tvName.setText(safe(proposal.getVendorName(), holder.itemView.getContext().getString(R.string.vendor_name)));
        holder.tvDescription.setText(safe(proposal.getDescription(), holder.itemView.getContext().getString(R.string.vendor_description)));
        holder.tvEvent.setText(holder.itemView.getContext().getString(R.string.vendor_event_format, safe(proposal.getEventTitle(), "-")));
        holder.tvOrganizer.setText(holder.itemView.getContext().getString(R.string.vendor_organizer_format, safe(proposal.getOrganizerName(), "-")));
        holder.tvPhone.setText(holder.itemView.getContext().getString(R.string.vendor_phone_format, safe(proposal.getPhone(), "-")));
        bindStatus(holder, proposal);

        boolean canReview = showAdminActions && "pending".equalsIgnoreCase(safe(proposal.getStatus(), ""));
        holder.layoutActions.setVisibility(canReview ? View.VISIBLE : View.GONE);
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(proposal);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(proposal);
        });
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    public void updateData(List<VendorProposal> newProposals) {
        submitList(newProposals == null ? null : new java.util.ArrayList<>(newProposals));
    }

    private void bindStatus(VendorViewHolder holder, VendorProposal proposal) {
        String status = safe(proposal.getStatus(), "pending").trim().toLowerCase(Locale.getDefault());
        holder.chipStatus.setText(status.substring(0, 1).toUpperCase(Locale.getDefault()) + status.substring(1));

        if ("approved".equals(status)) {
            holder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ThemeManager.getAccentColor(holder.itemView.getContext())));
            holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnPrimary));
        } else if ("rejected".equals(status)) {
            holder.chipStatus.setChipBackgroundColorResource(R.color.colorErrorContainer);
            holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnErrorContainer));
        } else {
            holder.chipStatus.setChipBackgroundColorResource(R.color.colorSecondaryContainer);
            holder.chipStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOnSecondaryContainer));
        }
    }

    private String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private static final DiffUtil.ItemCallback<VendorProposal> DIFF_CALLBACK = new DiffUtil.ItemCallback<VendorProposal>() {
        @Override
        public boolean areItemsTheSame(@NonNull VendorProposal oldItem, @NonNull VendorProposal newItem) {
            return TextUtils.equals(oldItem.getProposalId(), newItem.getProposalId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull VendorProposal oldItem, @NonNull VendorProposal newItem) {
            return TextUtils.equals(oldItem.getProposalId(), newItem.getProposalId())
                    && TextUtils.equals(oldItem.getVendorName(), newItem.getVendorName())
                    && TextUtils.equals(oldItem.getDescription(), newItem.getDescription())
                    && TextUtils.equals(oldItem.getEventTitle(), newItem.getEventTitle())
                    && TextUtils.equals(oldItem.getOrganizerName(), newItem.getOrganizerName())
                    && TextUtils.equals(oldItem.getPhone(), newItem.getPhone())
                    && TextUtils.equals(oldItem.getStatus(), newItem.getStatus());
        }
    };

    static class VendorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDescription;
        TextView tvEvent;
        TextView tvOrganizer;
        TextView tvPhone;
        Chip chipStatus;
        LinearLayout layoutActions;
        MaterialButton btnApprove;
        MaterialButton btnReject;

        VendorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvVendorName);
            tvDescription = itemView.findViewById(R.id.tvVendorDescription);
            tvEvent = itemView.findViewById(R.id.tvVendorEvent);
            tvOrganizer = itemView.findViewById(R.id.tvVendorOrganizer);
            tvPhone = itemView.findViewById(R.id.tvVendorPhone);
            chipStatus = itemView.findViewById(R.id.chipVendorStatus);
            layoutActions = itemView.findViewById(R.id.layoutVendorActions);
            btnApprove = itemView.findViewById(R.id.btnApproveVendor);
            btnReject = itemView.findViewById(R.id.btnRejectVendor);
        }
    }
}
