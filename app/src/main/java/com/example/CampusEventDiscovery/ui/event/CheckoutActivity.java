package com.example.CampusEventDiscovery.ui.event;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * CheckoutActivity.java
 *
 * Collects contact/payment info and confirms RSVP via the repository layer.
 */
public class CheckoutActivity extends AppCompatActivity {

    private MaterialToolbar toolbarCheckout;
    private TextView tvCheckoutEventTitle;
    private TextView tvCheckoutSubtitle;
    private EditText etFirstName;
    private EditText etLastName;
    private RadioGroup radioGroupPayment;
    @SuppressWarnings("unused")
    private RadioButton rbApplePay;
    @SuppressWarnings("unused")
    private RadioButton rbCreditCard;
    @SuppressWarnings("unused")
    private RadioButton rbMasterCard;
    @SuppressWarnings("unused")
    private RadioButton rbBitcoin;
    private EditText etCardNumber;
    private TextView tvCheckoutTotal;
    private ProgressBar progressBarCheckout;
    private MaterialButton btnPay;

    private EventRepository repository;

    private String eventId;
    private String eventTitle;
    private long eventDateMillis;
    private String eventVenue;
    private long eventCapacity;
    private long eventRsvpCount;
    private int totalPrice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        repository = new EventRepository();

        bindViews();
        readIntentExtras();
        setupToolbar();
        bindStaticUi();
        setupPaymentMethodToggle();
        enforceAttendeeAccess();
        setupPayButton();
    }

    private void bindViews() {
        toolbarCheckout = findViewById(R.id.toolbarCheckout);
        tvCheckoutEventTitle = findViewById(R.id.tvCheckoutEventTitle);
        tvCheckoutSubtitle = findViewById(R.id.tvCheckoutSubtitle);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        rbApplePay = findViewById(R.id.rbApplePay);
        rbCreditCard = findViewById(R.id.rbCreditCard);
        rbMasterCard = findViewById(R.id.rbMasterCard);
        rbBitcoin = findViewById(R.id.rbBitcoin);
        etCardNumber = findViewById(R.id.etCardNumber);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        progressBarCheckout = findViewById(R.id.progressBarCheckout);
        btnPay = findViewById(R.id.btnPay);
    }

    private void readIntentExtras() {
        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");
        eventDateMillis = getIntent().getLongExtra("eventDateMillis", -1L);
        eventVenue = getIntent().getStringExtra("eventVenue");
        eventCapacity = getIntent().getLongExtra("eventCapacity", 0L);
        eventRsvpCount = getIntent().getLongExtra("eventRsvpCount", 0L);
        totalPrice = getIntent().getIntExtra("totalPrice", 0);
    }

    private void setupToolbar() {
        toolbarCheckout.setNavigationOnClickListener(v -> finish());
    }

    private void bindStaticUi() {
        tvCheckoutEventTitle.setText(eventTitle != null ? eventTitle : getString(R.string.app_name));
        tvCheckoutSubtitle.setText(getString(R.string.secure_your_spot_subtitle));
        tvCheckoutTotal.setText(getString(R.string.total_label, totalPrice));
    }

    private void setupPaymentMethodToggle() {
        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCreditCard) {
                etCardNumber.setVisibility(View.VISIBLE);
            } else {
                etCardNumber.setVisibility(View.GONE);
            }
        });
    }

    private void setupPayButton() {
        btnPay.setOnClickListener(v -> attemptPayment());
    }

    private void attemptPayment() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (radioGroupPayment.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, getString(R.string.please_select_payment_method), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String effectiveUserId = firebaseUser != null ? firebaseUser.getUid() : DevSessionManager.getEffectiveUserId(this);
        if (effectiveUserId == null) {
            Toast.makeText(this, getString(R.string.login_required_for_checkout), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        Event event = new Event();
        event.setEventId(eventId);
        event.setTitle(eventTitle);
        event.setDate(eventDateMillis > 0L ? new Timestamp(new java.util.Date(eventDateMillis)) : null);
        event.setLocation(eventVenue);
        event.setCapacity(eventCapacity);
        event.setRsvpCount(eventRsvpCount);

        String fullName = firstName + " " + lastName;

        repository.rsvpEvent(effectiveUserId, event, fullName, new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);

                new AlertDialog.Builder(CheckoutActivity.this)
                        .setTitle(getString(R.string.checkout_success_title))
                        .setMessage(getString(R.string.checkout_success_message))
                        .setPositiveButton("OK", (dialog, which) -> {
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(CheckoutActivity.this, getString(R.string.payment_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBarCheckout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!isLoading);
    }

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

        repository.getUserData(currentUser.getUid(), new EventRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.CampusEventDiscovery.model.User user) {
                if (!UserRoles.isAttendee(user.getRole())) {
                    Toast.makeText(CheckoutActivity.this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CheckoutActivity.this, getString(R.string.attendee_only_registration_message), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
