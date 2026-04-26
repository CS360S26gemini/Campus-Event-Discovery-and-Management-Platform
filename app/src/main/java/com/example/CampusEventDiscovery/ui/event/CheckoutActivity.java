package com.example.CampusEventDiscovery.ui.event;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.adapter.TicketTierAdapter;
import com.example.CampusEventDiscovery.callback.FirestoreCallback;
import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.repository.PaymentRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.MockPaymentService;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CheckoutActivity.java
 *
 * Collects attendee payment information, processes a demo payment via
 * MockPaymentService, creates an RSVP with a QR payload, and navigates
 * to TicketActivity. Enforces one RSVP per user per event.
 */
public class CheckoutActivity extends AppCompatActivity {

    private MaterialToolbar toolbarCheckout;
    private TextView tvCheckoutEventTitle;
    private TextView tvCheckoutSubtitle;
    private EditText etFullName;
    private EditText etLastName;
    private LinearLayout layoutTiersSelection;
    private RecyclerView rvCheckoutTiers;
    private LinearLayout layoutPaymentSection;
    private RadioGroup radioGroupPayment;
    private TextInputLayout tilCardNumber;
    private EditText etCardNumber;
    private TextView tvCheckoutTotal;
    private ProgressBar progressBarCheckout;
    private MaterialButton btnPay;

    private EventRepository eventRepository;
    private PaymentRepository paymentRepository;
    private FirebaseFirestore db;
    private TicketTierAdapter tierAdapter;

    private String eventId;
    private String eventTitle;
    private long eventDateMillis;
    private String eventVenue;
    private double totalPrice;
    private String effectiveUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        eventRepository = new EventRepository();
        paymentRepository = new PaymentRepository();
        db = FirebaseFirestore.getInstance();

        bindViews();
        readIntentExtras();
        resolveUserId();
        setupToolbar();
        bindStaticUi();
        setupTierSelection();
        setupPaymentMethodToggle();
        enforceAttendeeAccess();
        setupPayButton();
    }

    private void bindViews() {
        toolbarCheckout      = findViewById(R.id.toolbarCheckout);
        tvCheckoutEventTitle = findViewById(R.id.tvCheckoutEventTitle);
        tvCheckoutSubtitle   = findViewById(R.id.tvCheckoutSubtitle);
        etFullName           = findViewById(R.id.etFirstName);
        etLastName           = findViewById(R.id.etLastName);
        layoutTiersSelection = findViewById(R.id.layoutTiersSelection);
        rvCheckoutTiers      = findViewById(R.id.rvCheckoutTiers);
        layoutPaymentSection = findViewById(R.id.layoutPaymentSection);
        radioGroupPayment    = findViewById(R.id.radioGroupPayment);
        tilCardNumber        = findViewById(R.id.tilCardNumber);
        etCardNumber         = findViewById(R.id.etCardNumber);
        tvCheckoutTotal      = findViewById(R.id.tvCheckoutTotal);
        progressBarCheckout  = findViewById(R.id.progressBarCheckout);
        btnPay               = findViewById(R.id.btnPay);
    }

    private void readIntentExtras() {
        eventId         = getIntent().getStringExtra("eventId");
        eventTitle      = getIntent().getStringExtra("eventTitle");
        eventDateMillis = getIntent().getLongExtra("eventDateMillis", -1L);
        eventVenue      = getIntent().getStringExtra("eventVenue");
        totalPrice      = getIntent().getDoubleExtra("totalPrice", 0.0);
    }

    private void resolveUserId() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        effectiveUserId = firebaseUser != null
                ? firebaseUser.getUid()
                : DevSessionManager.getEffectiveUserId(this);
    }

    private void setupToolbar() {
        toolbarCheckout.setNavigationOnClickListener(v -> finish());
    }

    private void bindStaticUi() {
        tvCheckoutEventTitle.setText(eventTitle != null ? eventTitle : getString(R.string.app_name));
        tvCheckoutSubtitle.setText(getString(R.string.secure_your_spot_subtitle));
        updateTotalDisplay(totalPrice);
    }

    private void setupTierSelection() {
        if (eventId == null) return;

        eventRepository.getTiersForEvent(eventId, new EventRepository.TierListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> tiersData) {
                if (tiersData == null || tiersData.isEmpty()) {
                    layoutTiersSelection.setVisibility(View.GONE);
                    return;
                }

                layoutTiersSelection.setVisibility(View.VISIBLE);
                List<TicketTierAdapter.TicketTier> tiers = new ArrayList<>();
                for (Map<String, Object> data : tiersData) {
                    String id = (String) data.get("tierId");
                    String name = (String) data.get("name");
                    String desc = (String) data.get("description");
                    Object pObj = data.get("price");
                    int p = pObj instanceof Number ? ((Number) pObj).intValue() : 0;
                    
                    tiers.add(new TicketTierAdapter.TicketTier(id, name, desc, "", p, 0));
                }

                tierAdapter = new TicketTierAdapter(tiers, total -> {
                    totalPrice = (double) total;
                    updateTotalDisplay(totalPrice);
                });

                rvCheckoutTiers.setLayoutManager(new LinearLayoutManager(CheckoutActivity.this));
                rvCheckoutTiers.setAdapter(tierAdapter);
            }

            @Override
            public void onError(Exception e) {
                layoutTiersSelection.setVisibility(View.GONE);
            }
        });
    }

    private void updateTotalDisplay(double total) {
        if (total <= 0.0) {
            tvCheckoutTotal.setText(getString(R.string.checkout_total_free));
            btnPay.setText(getString(R.string.register_free));
            layoutPaymentSection.setVisibility(View.GONE);
        } else {
            tvCheckoutTotal.setText(getString(R.string.checkout_total_pkr, total));
            btnPay.setText(getString(R.string.pay_now));
            layoutPaymentSection.setVisibility(View.VISIBLE);
        }
    }

    private void setupPaymentMethodToggle() {
        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCreditCard || checkedId == R.id.rbDebitCard) {
                tilCardNumber.setVisibility(View.VISIBLE);
            } else {
                tilCardNumber.setVisibility(View.GONE);
                etCardNumber.setText("");
            }
        });
    }

    private void setupPayButton() {
        btnPay.setOnClickListener(v -> attemptPayment());
    }

    private void attemptPayment() {
        if (!validateForm()) return;

        if (effectiveUserId == null) {
            Toast.makeText(this, getString(R.string.login_required_for_checkout), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        db.collection("users").document(effectiveUserId)
                .collection("rsvps").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Rsvp existingRsvp = snapshot.toObject(Rsvp.class);
                        if (existingRsvp != null && !"cancelled".equalsIgnoreCase(existingRsvp.getStatus())) {
                            showLoading(false);
                            Toast.makeText(CheckoutActivity.this, getString(R.string.already_registered_for_event), Toast.LENGTH_LONG).show();
                            navigateToTicket(existingRsvp);
                            return;
                        }
                    }
                    processPayment();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.checkout_rsvp_check_failed), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateForm() {
        String fullName = etFullName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (tierAdapter != null && tierAdapter.getSelectedTier() == null) {
            Toast.makeText(this, getString(R.string.please_select_ticket), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (totalPrice > 0.0) {
            if (radioGroupPayment.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, getString(R.string.please_select_payment_method), Toast.LENGTH_SHORT).show();
                return false;
            }

            int checkedId = radioGroupPayment.getCheckedRadioButtonId();
            if (checkedId == R.id.rbCreditCard || checkedId == R.id.rbDebitCard) {
                String cardNumber = etCardNumber.getText().toString().trim();
                if (cardNumber.length() != 16) {
                    Toast.makeText(this, getString(R.string.invalid_card_number), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }

        return true;
    }

    private void processPayment() {
        Payment demoPayment = MockPaymentService.processPayment(effectiveUserId, eventId, totalPrice);
        paymentRepository.savePayment(demoPayment, new FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                runRsvpTransaction((Payment) result);
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(CheckoutActivity.this, getString(R.string.payment_failed_message, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runRsvpTransaction(Payment payment) {
        com.example.CampusEventDiscovery.model.Event eventShell = new com.example.CampusEventDiscovery.model.Event();
        eventShell.setEventId(eventId);
        eventShell.setTitle(eventTitle);
        if (eventDateMillis > 0L) {
            eventShell.setDate(new Timestamp(new java.util.Date(eventDateMillis)));
        }

        String fullName = etFullName.getText().toString().trim() + " " + etLastName.getText().toString().trim();
        
        Map<String, Object> selectedTierMap = null;
        if (tierAdapter != null) {
            TicketTierAdapter.TicketTier selectedTier = tierAdapter.getSelectedTier();
            if (selectedTier != null) {
                selectedTierMap = new HashMap<>();
                selectedTierMap.put("tierId", selectedTier.getTierId());
                selectedTierMap.put("name", selectedTier.getName());
                selectedTierMap.put("price", selectedTier.getPricePerUnit());
            }
        }

        eventRepository.rsvpEvent(effectiveUserId, eventShell, fullName, selectedTierMap,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        mergePaymentOntoRsvp(payment);
                    }

                    @Override
                    public void onError(Exception e) {
                        showLoading(false);
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        Toast.makeText(CheckoutActivity.this, getString(R.string.rsvp_failed_message, msg), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mergePaymentOntoRsvp(Payment payment) {
        JSONObject qrJson = new JSONObject();
        try {
            qrJson.put("userId", effectiveUserId);
            qrJson.put("eventId", eventId);
            qrJson.put("transactionId", payment.getTransactionId());
        } catch (JSONException e) { e.printStackTrace(); }

        String qrPayload = qrJson.toString();
        Map<String, Object> paymentMerge = new HashMap<>();
        paymentMerge.put("paymentStatus", "SUCCESS");
        paymentMerge.put("transactionId", payment.getTransactionId());
        paymentMerge.put("qrPayload", qrPayload);
        paymentMerge.put("qrExpired", false);

        db.collection("users").document(effectiveUserId)
                .collection("rsvps").document(eventId)
                .update(paymentMerge)
                .addOnSuccessListener(unused -> {
                    Rsvp rsvp = new Rsvp();
                    rsvp.setTransactionId(payment.getTransactionId());
                    rsvp.setQrPayload(qrPayload);
                    showLoading(false);
                    navigateToTicket(rsvp);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(CheckoutActivity.this, getString(R.string.rsvp_failed_message, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToTicket(Rsvp rsvp) {
        String formattedDate = "Date TBD";
        if (eventDateMillis > 0L) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
            formattedDate = sdf.format(new java.util.Date(eventDateMillis));
        }

        Intent intent = new Intent(this, TicketActivity.class);
        intent.putExtra("rsvpId", eventId);
        intent.putExtra("eventName", eventTitle);
        intent.putExtra("eventDate", formattedDate);
        intent.putExtra("transactionId", rsvp.getTransactionId());
        intent.putExtra("qrPayload", rsvp.getQrPayload());
        startActivity(intent);
        setResult(RESULT_OK);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBarCheckout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!isLoading);
    }

    private void enforceAttendeeAccess() {
        if (DevSessionManager.shouldUseBypass(this)) {
            if (!UserRoles.isAttendee(DevSessionManager.getBypassRole(this))) finish();
            return;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        eventRepository.getUserData(currentUser.getUid(), new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
<<<<<<< Updated upstream
                if (!UserRoles.isAttendee(user.getRole())) {
                    Toast.makeText(CheckoutActivity.this,
                            getString(R.string.attendee_only_registration_message),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
=======
                if (user == null || !UserRoles.isAttendee(user.getRole())) finish();
>>>>>>> Stashed changes
            }
            @Override
<<<<<<< Updated upstream
            public void onError(Exception e) {
                Toast.makeText(CheckoutActivity.this,
                        getString(R.string.attendee_only_registration_message),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
=======
            public void onError(Exception e) { finish(); }
>>>>>>> Stashed changes
        });
    }
}
