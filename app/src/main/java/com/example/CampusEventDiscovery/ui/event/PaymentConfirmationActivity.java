package com.example.CampusEventDiscovery.ui.event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.callback.FirestoreCallback;
import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.repository.PaymentRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PaymentConfirmationActivity.java
 *
 * Organizer-only view — lists every payment captured for an event
 * plus the running revenue total.
 */
public class PaymentConfirmationActivity extends AppCompatActivity {

    private TextView tvEventTitle;
    private TextView tvPaymentCount;
    private TextView tvTotalRevenue;
    private TextView tvEmpty;
    private RecyclerView rvPayments;
    private MaterialToolbar toolbarPayments;

    private String eventId;
    private String eventTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_confirmation);

        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");

        tvEventTitle    = findViewById(R.id.tvEventTitle);
        tvPaymentCount  = findViewById(R.id.tvPaymentCount);
        tvTotalRevenue  = findViewById(R.id.tvTotalRevenue);
        tvEmpty         = findViewById(R.id.tvEmpty);
        rvPayments      = findViewById(R.id.rvPayments);
        toolbarPayments = findViewById(R.id.toolbarPayments);

        toolbarPayments.setNavigationOnClickListener(v -> finish());
        tvEventTitle.setText(eventTitle != null ? eventTitle : "");

        rvPayments.setLayoutManager(new LinearLayoutManager(this));

        loadPayments();
    }

    private void loadPayments() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, getString(R.string.payment_missing_event), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new PaymentRepository().getPaymentsForEvent(eventId, new FirestoreCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSuccess(Object result) {
                List<Payment> payments = result != null ? (List<Payment>) result : new ArrayList<>();
                bindPayments(payments);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PaymentConfirmationActivity.this,
                        getString(R.string.payment_load_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindPayments(List<Payment> payments) {
        double total = 0.0;
        for (Payment p : payments) {
            total += p.getAmount();
        }

        tvPaymentCount.setText(getString(R.string.payment_count_label, payments.size()));
        tvTotalRevenue.setText(getString(R.string.payment_total_revenue, total));

        if (payments.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvPayments.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPayments.setVisibility(View.VISIBLE);
            rvPayments.setAdapter(new PaymentAdapter(payments));
        }
    }

    /**
     * Simple adapter — single file because it's read-only.
     */
    private static class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.VH> {
        private final List<Payment> items;
        private final SimpleDateFormat fmt =
                new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

        PaymentAdapter(List<Payment> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_payment, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Payment p = items.get(position);
            holder.tvAmount.setText(String.format(Locale.getDefault(), "PKR %.2f", p.getAmount()));
            holder.tvRef.setText(p.getTransactionId() != null ? p.getTransactionId() : "—");
            holder.tvStatus.setText(p.getStatus() != null ? p.getStatus() : "");
            holder.tvMethod.setText(formatMethod(p.getPaymentMethod()));
            holder.tvWhen.setText(fmt.format(new java.util.Date(p.getTimestamp())));

            String proofUrl = p.getProofUrl();
            if (proofUrl != null && !proofUrl.isEmpty()) {
                holder.ivProof.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(proofUrl)
                        .into(holder.ivProof);
            } else {
                holder.ivProof.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String formatMethod(String method) {
            if (method == null || method.isEmpty()) {
                return "";
            }
            return method.replace('_', ' ');
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAmount, tvRef, tvStatus, tvMethod, tvWhen;
            android.widget.ImageView ivProof;
            VH(@NonNull View itemView) {
                super(itemView);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvRef    = itemView.findViewById(R.id.tvRef);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvMethod = itemView.findViewById(R.id.tvMethod);
                tvWhen   = itemView.findViewById(R.id.tvWhen);
                ivProof  = itemView.findViewById(R.id.ivProof);
            }
        }
    }
}
