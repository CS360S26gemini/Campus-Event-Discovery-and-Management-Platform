package com.example.CampusEventDiscovery.ui.admin;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class AdminHomeActivity extends AppCompatActivity {

    private EventRepository repository;
    private RecyclerView rvVendorRequests;
    private TextView tvNoVendorRequests;
    private ProgressBar progressBarVendors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        repository = new EventRepository();

        rvVendorRequests = findViewById(R.id.rvVendorRequests);
        tvNoVendorRequests = findViewById(R.id.tvNoVendorRequests);
        progressBarVendors = findViewById(R.id.progressBarVendors);

        rvVendorRequests.setLayoutManager(new LinearLayoutManager(this));

        loadPendingVendorRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingVendorRequests();
    }

    private void loadPendingVendorRequests() {
        progressBarVendors.setVisibility(View.VISIBLE);
        rvVendorRequests.setVisibility(View.GONE);
        tvNoVendorRequests.setVisibility(View.GONE);

        repository.getPendingVendorRequests(new EventRepository.VendorListCallback() {
            @Override
            public void onSuccess(List<Vendor> vendors) {
                progressBarVendors.setVisibility(View.GONE);
                if (vendors == null || vendors.isEmpty()) {
                    tvNoVendorRequests.setVisibility(View.VISIBLE);
                } else {
                    rvVendorRequests.setVisibility(View.VISIBLE);
                    rvVendorRequests.setAdapter(new AdminVendorAdapter(vendors));
                }
            }

            @Override
            public void onError(Exception e) {
                progressBarVendors.setVisibility(View.GONE);
                tvNoVendorRequests.setVisibility(View.VISIBLE);
                Toast.makeText(AdminHomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class AdminVendorAdapter extends RecyclerView.Adapter<AdminVendorAdapter.ViewHolder> {

        private final List<Vendor> vendorList;

        AdminVendorAdapter(List<Vendor> vendorList) {
            this.vendorList = vendorList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            container.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvEvent = new TextView(parent.getContext());
            tvEvent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            TextView tvOrganizer = new TextView(parent.getContext());
            tvOrganizer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            TextView tvPhone = new TextView(parent.getContext());
            tvPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            TextView tvType = new TextView(parent.getContext());
            tvType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            LinearLayout buttonContainer = new LinearLayout(parent.getContext());
            buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
            buttonContainer.setPadding(0, dpToPx(8), 0, 0);

            Button btnApprove = new Button(parent.getContext());
            btnApprove.setText("Approve");
            btnApprove.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button btnReject = new Button(parent.getContext(), null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
            btnReject.setText("Reject");
            btnReject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnReject.getLayoutParams();
            lp.setMarginStart(dpToPx(8));

            buttonContainer.addView(btnApprove);
            buttonContainer.addView(btnReject);

            container.addView(tvName);
            container.addView(tvEvent);
            container.addView(tvOrganizer);
            container.addView(tvPhone);
            container.addView(tvType);
            container.addView(buttonContainer);

            return new ViewHolder(container, tvName, tvEvent, tvOrganizer, tvPhone, tvType, btnApprove, btnReject);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Vendor vendor = vendorList.get(position);
            holder.tvName.setText(vendor.getVendorName());
            holder.tvEvent.setText("Event: " + vendor.getEventTitle());
            holder.tvOrganizer.setText("Organizer: " + vendor.getOrganizerName());
            holder.tvPhone.setText("Phone: " + vendor.getPhoneNumber());

            if (TextUtils.isEmpty(vendor.getVendorType())) {
                holder.tvType.setVisibility(View.GONE);
            } else {
                holder.tvType.setVisibility(View.VISIBLE);
                holder.tvType.setText("Type: " + vendor.getVendorType());
            }

            holder.btnApprove.setOnClickListener(v -> {
                repository.approveVendorRequest(vendor.getVendorId(), new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            vendorList.remove(pos);
                            notifyItemRemoved(pos);
                            Toast.makeText(AdminHomeActivity.this, "Vendor approved", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(AdminHomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });

            holder.btnReject.setOnClickListener(v -> {
                repository.rejectVendorRequest(vendor.getVendorId(), new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            vendorList.remove(pos);
                            notifyItemRemoved(pos);
                            Toast.makeText(AdminHomeActivity.this, "Vendor rejected", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(AdminHomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return vendorList.size();
        }

        private int dpToPx(int dp) {
            return (int) (dp * getResources().getDisplayMetrics().density);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvName, tvEvent, tvOrganizer, tvPhone, tvType;
            final Button btnApprove, btnReject;

            ViewHolder(@NonNull View itemView, TextView tvName, TextView tvEvent, TextView tvOrganizer,
                       TextView tvPhone, TextView tvType, Button btnApprove, Button btnReject) {
                super(itemView);
                this.tvName = tvName;
                this.tvEvent = tvEvent;
                this.tvOrganizer = tvOrganizer;
                this.tvPhone = tvPhone;
                this.tvType = tvType;
                this.btnApprove = btnApprove;
                this.btnReject = btnReject;
            }
        }
    }
}
