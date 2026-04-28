package com.example.CampusEventDiscovery.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.TicketTier;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Displays ticket tiers in either preview mode or single-select purchase mode.
 */
public class TicketTierOptionAdapter extends RecyclerView.Adapter<TicketTierOptionAdapter.TierViewHolder> {

    public interface OnTierSelectedListener {
        void onTierSelected(TicketTier tier);
    }

    private final List<TicketTier> tiers = new ArrayList<>();
    private final boolean selectionEnabled;
    private final OnTierSelectedListener listener;
    private String selectedTierId;

    public TicketTierOptionAdapter(boolean selectionEnabled, OnTierSelectedListener listener) {
        this.selectionEnabled = selectionEnabled;
        this.listener = listener;
    }

    public void submitList(List<TicketTier> newTiers) {
        tiers.clear();
        if (newTiers != null) {
            tiers.addAll(newTiers);
        }
        if (!containsTier(selectedTierId)) {
            selectedTierId = null;
        }
        notifyDataSetChanged();
    }

    public TicketTier getSelectedTier() {
        if (TextUtils.isEmpty(selectedTierId)) {
            return null;
        }
        for (TicketTier tier : tiers) {
            if (selectedTierId.equals(tier.getTierId())) {
                return tier;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public TierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_tier_option, parent, false);
        return new TierViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TierViewHolder holder, int position) {
        TicketTier tier = tiers.get(position);
        boolean soldOut = tier.isSoldOut();
        boolean selected = selectedTierId != null && selectedTierId.equals(tier.getTierId());

        holder.tvTierName.setText(tier.getName());
        holder.tvTierDescription.setVisibility(TextUtils.isEmpty(tier.getDescription()) ? View.GONE : View.VISIBLE);
        holder.tvTierDescription.setText(tier.getDescription());
        holder.tvTierPrice.setText(String.format(Locale.getDefault(), "PKR %.2f", tier.getPrice()));
        holder.tvTierCapacity.setText(holder.itemView.getContext().getString(
                R.string.ticket_tier_spots_remaining,
                (int) tier.getRemainingCapacity()
        ));

        if (soldOut) {
            holder.tvTierState.setVisibility(View.VISIBLE);
            holder.tvTierState.setText(R.string.sold_out);
        } else if (selected && selectionEnabled) {
            holder.tvTierState.setVisibility(View.VISIBLE);
            holder.tvTierState.setText(R.string.ticket_tier_selected);
        } else {
            holder.tvTierState.setVisibility(View.GONE);
        }

        holder.card.setAlpha(soldOut ? 0.55f : 1f);
        int strokeColor = selected && !soldOut
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOutlineVariant);
        holder.card.setStrokeColor(strokeColor);
        holder.card.setStrokeWidth(selected && !soldOut ? 3 : 1);
        holder.card.setClickable(selectionEnabled && !soldOut);
        holder.card.setFocusable(selectionEnabled && !soldOut);
        holder.card.setOnClickListener(v -> {
            if (!selectionEnabled || soldOut) {
                return;
            }
            selectedTierId = tier.getTierId();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onTierSelected(tier);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tiers.size();
    }

    private boolean containsTier(String tierId) {
        if (TextUtils.isEmpty(tierId)) {
            return false;
        }
        for (TicketTier tier : tiers) {
            if (tierId.equals(tier.getTierId())) {
                return true;
            }
        }
        return false;
    }

    static class TierViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvTierName;
        final TextView tvTierState;
        final TextView tvTierDescription;
        final TextView tvTierPrice;
        final TextView tvTierCapacity;

        TierViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvTierName = itemView.findViewById(R.id.tvTierName);
            tvTierState = itemView.findViewById(R.id.tvTierState);
            tvTierDescription = itemView.findViewById(R.id.tvTierDescription);
            tvTierPrice = itemView.findViewById(R.id.tvTierPrice);
            tvTierCapacity = itemView.findViewById(R.id.tvTierCapacity);
        }
    }
}
