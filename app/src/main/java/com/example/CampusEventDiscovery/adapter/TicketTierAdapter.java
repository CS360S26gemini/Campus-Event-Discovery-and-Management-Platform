package com.example.CampusEventDiscovery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;

import java.util.List;

/**
 * TicketTierAdapter.java
 *
 * Adapter for displaying selectable ticket tiers and quantity counters.
 */
public class TicketTierAdapter extends RecyclerView.Adapter<TicketTierAdapter.TierViewHolder> {

    public interface OnTotalChangedListener {
        void onTotalChanged(int total);
    }

    public static class TicketTier {
        private final String name;
        private final String description;
        private final String dateRange;
        private final int pricePerUnit;
        private int quantity;

        public TicketTier(String name, String description, String dateRange, int pricePerUnit, int quantity) {
            this.name = name;
            this.description = description;
            this.dateRange = dateRange;
            this.pricePerUnit = pricePerUnit;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getDateRange() {
            return dateRange;
        }

        public int getPricePerUnit() {
            return pricePerUnit;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public int getSubtotal() {
            return pricePerUnit * quantity;
        }
    }

    private final List<TicketTier> tiers;
    private final OnTotalChangedListener onTotalChangedListener;

    public TicketTierAdapter(List<TicketTier> tiers, OnTotalChangedListener onTotalChangedListener) {
        this.tiers = tiers;
        this.onTotalChangedListener = onTotalChangedListener;
    }

    @NonNull
    @Override
    public TierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_tier, parent, false);
        return new TierViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TierViewHolder holder, int position) {
        TicketTier tier = tiers.get(position);

        holder.tvTierName.setText(tier.getName());
        holder.tvTierDescription.setText(tier.getDescription());
        holder.tvTierDateRange.setText(tier.getDateRange());
        holder.tvPricePerUnit.setText("PKR " + tier.getPricePerUnit());
        holder.tvQuantity.setText(String.valueOf(tier.getQuantity()));
        holder.tvSubtotal.setText("PKR " + tier.getSubtotal());

        holder.btnMinus.setOnClickListener(v -> {
            if (tier.getQuantity() > 0) {
                tier.setQuantity(tier.getQuantity() - 1);
                notifyItemChanged(position);
                notifyTotalChanged();
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            tier.setQuantity(tier.getQuantity() + 1);
            notifyItemChanged(position);
            notifyTotalChanged();
        });
    }

    @Override
    public int getItemCount() {
        return tiers != null ? tiers.size() : 0;
    }

    public int getTotalPrice() {
        int total = 0;
        for (TicketTier tier : tiers) {
            total += tier.getSubtotal();
        }
        return total;
    }

    private void notifyTotalChanged() {
        if (onTotalChangedListener != null) {
            onTotalChangedListener.onTotalChanged(getTotalPrice());
        }
    }

    public static class TierViewHolder extends RecyclerView.ViewHolder {

        TextView tvTierName;
        TextView tvTierDescription;
        TextView tvTierDateRange;
        TextView tvPricePerUnit;
        TextView tvQuantity;
        TextView tvSubtotal;
        Button btnMinus;
        Button btnPlus;

        public TierViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTierName = itemView.findViewById(R.id.tvTierName);
            tvTierDescription = itemView.findViewById(R.id.tvTierDescription);
            tvTierDateRange = itemView.findViewById(R.id.tvTierDateRange);
            tvPricePerUnit = itemView.findViewById(R.id.tvPricePerUnit);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
        }
    }
}