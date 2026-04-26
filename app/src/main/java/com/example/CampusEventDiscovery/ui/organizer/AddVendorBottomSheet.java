package com.example.CampusEventDiscovery.ui.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.User;
import com.example.CampusEventDiscovery.model.Vendor;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

public class AddVendorBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_EVENT_ID = "eventId";

    private EditText etVendorName;
    private EditText etPhoneNumber;
    private EditText etVendorType;
    private Button btnSubmit;
    private Button btnCancel;

    private EventRepository repository;
    private String eventId;
    private String organizerName;

    public static AddVendorBottomSheet newInstance(String eventId) {
        AddVendorBottomSheet fragment = new AddVendorBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_vendor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        repository = new EventRepository();

        etVendorName = view.findViewById(R.id.etVendorName);
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        etVendorType = view.findViewById(R.id.etVendorType);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnCancel = view.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());

        fetchOrganizerData();
    }

    private void fetchOrganizerData() {
        String organizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        repository.getUserData(organizerId, new EventRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    organizerName = user.getFullName();
                }
            }

            @Override
            public void onError(Exception e) {
                // If we can't fetch user data, we'll use a fallback or handle it during submission
            }
        });
    }

    private void validateAndSubmit() {
        String vendorName = etVendorName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String vendorType = etVendorType.getText().toString().trim();

        if (TextUtils.isEmpty(vendorName)) {
            Toast.makeText(getContext(), "Vendor name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(getContext(), "Phone number is required", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        Vendor vendor = new Vendor();
        vendor.setVendorName(vendorName);
        vendor.setPhoneNumber(phoneNumber);
        vendor.setVendorType(vendorType);
        vendor.setEventId(eventId);
        vendor.setOrganizerId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        vendor.setOrganizerName(organizerName != null ? organizerName : "Organizer");

        repository.submitVendorRequest(vendor, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Vendor request submitted", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    setLoadingState(false);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting...");
        } else {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit");
        }
    }
}
