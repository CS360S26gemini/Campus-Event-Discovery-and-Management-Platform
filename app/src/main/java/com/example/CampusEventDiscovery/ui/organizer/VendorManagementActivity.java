package com.example.CampusEventDiscovery.ui.organizer;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Vendor;
import com.example.CampusEventDiscovery.repository.EventRepository;

import java.util.List;

/**
 * VendorManagementActivity.java
 *
 * Organizer screen showing approved vendors for a specific event.
 */
public class VendorManagementActivity extends AppCompatActivity {

    private EventRepository repository;
    private RecyclerView rvVendors;
    private TextView tvEmpty;
    private TextView tvEventTitle;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ImageButton btnAdd;
    private String eventId;
    private String eventTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_management);

        repository = new EventRepository();
        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");

        bindViews();
        tvEventTitle.setText(eventTitle);
        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> showAddVendorBottomSheet());

        setupRecyclerView();
        loadVendors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVendors();
    }

    private void bindViews() {
        rvVendors = findViewById(R.id.rvVendors);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnAdd = findViewById(R.id.btnAdd);
    }

    private void setupRecyclerView() {
        rvVendors.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showAddVendorBottomSheet() {
        AddVendorBottomSheet bottomSheet = AddVendorBottomSheet.newInstance(eventId);
        bottomSheet.show(getSupportFragmentManager(), "AddVendorBottomSheet");
    }

    private void loadVendors() {
        progressBar.setVisibility(View.VISIBLE);
        rvVendors.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        repository.getApprovedVendorsForEvent(eventId, new EventRepository.VendorListCallback() {
            @Override
            public void onSuccess(List<Vendor> vendors) {
                progressBar.setVisibility(View.GONE);

                if (vendors == null || vendors.isEmpty()) {
                    rvVendors.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvVendors.setVisibility(View.VISIBLE);
                    rvVendors.setAdapter(new VendorAdapter(vendors));
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                rvVendors.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void bindOptionalText(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }

    private class VendorAdapter extends RecyclerView.Adapter<VendorAdapter.VendorViewHolder> {

        private final List<Vendor> vendors;

        VendorAdapter(List<Vendor> vendors) {
            this.vendors = vendors;
        }

        @NonNull
        @Override
        public VendorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tvVendorName = new TextView(parent.getContext());
            tvVendorName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            tvVendorName.setTypeface(tvVendorName.getTypeface(), Typeface.BOLD);

            TextView tvVendorType = new TextView(parent.getContext());
            tvVendorType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            TextView tvPhoneNumber = new TextView(parent.getContext());
            tvPhoneNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            row.addView(tvVendorName);
            row.addView(tvVendorType);
            row.addView(tvPhoneNumber);

            return new VendorViewHolder(row, tvVendorName, tvVendorType, tvPhoneNumber);
        }

        @Override
        public void onBindViewHolder(@NonNull VendorViewHolder holder, int position) {
            Vendor vendor = vendors.get(position);
            bindOptionalText(holder.tvVendorName, vendor.getVendorName());
            bindOptionalText(holder.tvVendorType, vendor.getVendorType());
            bindOptionalText(holder.tvPhoneNumber, vendor.getPhoneNumber());
        }

        @Override
        public int getItemCount() {
            return vendors == null ? 0 : vendors.size();
        }

        class VendorViewHolder extends RecyclerView.ViewHolder {

            private final TextView tvVendorName;
            private final TextView tvVendorType;
            private final TextView tvPhoneNumber;

            VendorViewHolder(@NonNull View itemView,
                             TextView tvVendorName,
                             TextView tvVendorType,
                             TextView tvPhoneNumber) {
                super(itemView);
                this.tvVendorName = tvVendorName;
                this.tvVendorType = tvVendorType;
                this.tvPhoneNumber = tvPhoneNumber;
            }
        }
    }
}
