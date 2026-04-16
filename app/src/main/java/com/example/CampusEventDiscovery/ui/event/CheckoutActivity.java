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

import com.example.CampusEventDiscovery.R;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * CheckoutActivity.java
 *
 * Collects attendee payment information, processes a demo payment via
 * MockPaymentService, creates an RSVP with a QR payload, and navigates
 * to TicketActivity. Enforces one RSVP per user per event — if a valid
 * RSVP already exists, the user is sent directly to their existing ticket.
 */
public class CheckoutActivity extends AppCompatActivity {

    private MaterialToolbar toolbarCheckout;
    private TextView tvCheckoutEventTitle;
    private TextView tvCheckoutSubtitle;
    private EditText etFullName;
    private EditText etLastName;
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

        if (isFreeEvent()) {
            tvCheckoutTotal.setText(getString(R.string.checkout_total_free));
            btnPay.setText(getString(R.string.register_free));
            layoutPaymentSection.setVisibility(View.GONE);
            radioGroupPayment.clearCheck();
            etCardNumber.setText("");
            tilCardNumber.setVisibility(View.GONE);
        } else {
            tvCheckoutTotal.setText(getString(R.string.checkout_total_pkr, totalPrice));
            btnPay.setText(getString(R.string.pay_now));
            layoutPaymentSection.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows or hides the card number field based on the selected payment method.
     * Credit Card and Debit Card require a 16-digit card number.
     * JazzCash and Apple Pay do not.
     */
    private void setupPaymentMethodToggle() {
        if (isFreeEvent()) {
            return;
        }

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

    /**
     * Validates all form fields then checks Firestore for an existing RSVP
     * before processing payment. If the user is already registered, they are
     * redirected to their existing ticket without a new charge.
     */
    private void attemptPayment() {
        if (!validateForm()) return;

        if (effectiveUserId == null) {
            Toast.makeText(this, getString(R.string.login_required_for_checkout), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Check for an existing RSVP — enforce one ticket per user per event
        db.collection("users").document(effectiveUserId)
                .collection("rsvps").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Rsvp existingRsvp = snapshot.toObject(Rsvp.class);
                        if (existingRsvp != null
                                && !"cancelled".equalsIgnoreCase(existingRsvp.getStatus())
                                && existingRsvp.getQrPayload() != null
                                && !existingRsvp.getQrPayload().isEmpty()) {
                            showLoading(false);
                            Toast.makeText(CheckoutActivity.this,
                                    getString(R.string.already_registered_for_event),
                                    Toast.LENGTH_LONG).show();
                            navigateToTicket(existingRsvp);
                            return;
                        }
                    }
                    processPayment();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            getString(R.string.checkout_rsvp_check_failed),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Validates that all required form fields are correctly filled.
     *
     * @return true if the form is valid, false otherwise.
     */
    private boolean validateForm() {
        String fullName = etFullName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isFreeEvent()) {
            return true;
        }

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

        return true;
    }

    private boolean isFreeEvent() {
        return totalPrice <= 0.0;
    }

    /**
     * Processes the demo payment via MockPaymentService, saves the Payment
     * record, then delegates RSVP creation to EventRepository.rsvpEvent()
     * which runs an atomic Firestore transaction covering capacity check,
     * duplicate prevention, attendees subcollection write, and rsvpCount
     * increment. Payment fields are merged onto the RSVP document afterwards.
     */
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
                Toast.makeText(CheckoutActivity.this,
                        getString(R.string.payment_failed_message, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Calls EventRepository.rsvpEvent() which atomically:
     * - checks capacity
     * - prevents duplicate registration
     * - writes events/{eventId}/attendees/{userId} (so organizer list works)
     * - increments rsvpCount on the event document
     * - writes users/{userId}/rsvps/{eventId} with qrCodeToken
     *
     * On success, merges payment fields and qrPayload onto the RSVP document.
     *
     * @param payment The confirmed Payment returned after saving to Firestore.
     */
    private void runRsvpTransaction(Payment payment) {
        // Build a minimal Event shell — rsvpEvent() reads capacity/rsvpCount
        // from Firestore inside the transaction; it only needs eventId, title,
        // and date from the object itself.
        com.example.CampusEventDiscovery.model.Event eventShell =
                new com.example.CampusEventDiscovery.model.Event();
        eventShell.setEventId(eventId);
        eventShell.setTitle(eventTitle);
        if (eventDateMillis > 0L) {
            eventShell.setDate(new Timestamp(new java.util.Date(eventDateMillis)));
        }

        String fullName = etFullName.getText().toString().trim()
                + " " + etLastName.getText().toString().trim();

        eventRepository.rsvpEvent(effectiveUserId, eventShell, fullName,
                new EventRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        mergePaymentOntoRsvp(payment);
                    }

                    @Override
                    public void onError(Exception e) {
                        showLoading(false);
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.contains("Already registered")) {
                            // Transaction caught duplicate — fetch existing ticket
                            Toast.makeText(CheckoutActivity.this,
                                    getString(R.string.already_registered_for_event),
                                    Toast.LENGTH_LONG).show();
                            fetchExistingRsvpAndShowTicket();
                        } else if (msg.contains("Event full")) {
                            Toast.makeText(CheckoutActivity.this,
                                    getString(R.string.sold_out_toast),
                                    Toast.LENGTH_SHORT).show();
                        } else if (msg.contains("not allowed")) {
                            Toast.makeText(CheckoutActivity.this,
                                    "You are not allowed to register for this event.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CheckoutActivity.this,
                                    getString(R.string.rsvp_failed_message, msg),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Merges payment-specific fields onto the RSVP document that rsvpEvent()
     * already created. This adds paymentStatus, transactionId, qrPayload, and
     * qrExpired without overwriting the qrCodeToken or other fields written
     * by the transaction.
     *
     * @param payment The confirmed Payment whose details should be recorded.
     */
    private void mergePaymentOntoRsvp(Payment payment) {
        JSONObject qrJson = new JSONObject();
        try {
            qrJson.put("userId", effectiveUserId);
            qrJson.put("eventId", eventId);
            qrJson.put("transactionId", payment.getTransactionId());
            qrJson.put("timestamp", payment.getTimestamp());
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
                    rsvp.setUserId(effectiveUserId);
                    rsvp.setEventId(eventId);
                    rsvp.setTitle(eventTitle);
                    rsvp.setStatus("confirmed");
                    rsvp.setPaymentStatus("SUCCESS");
                    rsvp.setTransactionId(payment.getTransactionId());
                    rsvp.setQrPayload(qrPayload);
                    rsvp.setCheckedIn(false);
                    rsvp.setQrExpired(false);
                    if (eventDateMillis > 0L) {
                        rsvp.setDate(new Timestamp(new java.util.Date(eventDateMillis)));
                    }
                    showLoading(false);
                    navigateToTicket(rsvp);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(CheckoutActivity.this,
                            getString(R.string.rsvp_failed_message, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches the existing RSVP from Firestore and navigates to TicketActivity.
     * Called when the transaction detects the user is already registered.
     */
    private void fetchExistingRsvpAndShowTicket() {
        db.collection("users").document(effectiveUserId)
                .collection("rsvps").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Rsvp existingRsvp = snapshot.toObject(Rsvp.class);
                        if (existingRsvp != null) {
                            navigateToTicket(existingRsvp);
                            return;
                        }
                    }
                    // Fallback — just go back
                    finish();
                })
                .addOnFailureListener(e -> finish());
    }

    /**
     * Navigates to TicketActivity passing all required ticket data.
     *
     * @param rsvp The RSVP whose QR code ticket should be displayed.
     */
    private void navigateToTicket(Rsvp rsvp) {
        String formattedDate = "Date TBD";
        if (eventDateMillis > 0L) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
            formattedDate = sdf.format(new java.util.Date(eventDateMillis));
        } else if (rsvp.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault());
            formattedDate = sdf.format(rsvp.getDate().toDate());
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

    /**
     * Enforces that only attendees can access checkout.
     * Organizers and admins are redirected away immediately.
     */
    private void enforceAttendeeAccess() {
        if (DevSessionManager.shouldUseBypass(this)) {
            if (!UserRoles.isAttendee(DevSessionManager.getBypassRole(this))) {
                Toast.makeText(this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.login_required_for_checkout), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventRepository.getUserData(currentUser.getUid(), new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                if (!UserRoles.isAttendee(user.getRole())) {
                    Toast.makeText(CheckoutActivity.this,
                            getString(R.string.attendee_only_registration_message),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CheckoutActivity.this,
                        getString(R.string.attendee_only_registration_message),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
