package com.example.CampusEventDiscovery.ui.event;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.util.QRCodeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * TicketActivity.java
 *
 * Activity for displaying the generated QR code ticket to the attendee.
 */
public class TicketActivity extends AppCompatActivity {

    private String rsvpId;
    private String eventName;
    private String eventDate;
    private String transactionId;
    private String qrPayload;
    private String paymentMethod;
    private String paymentProofUrl;

    private ImageView ivQrCode;
    private TextView tvEventName, tvEventDate, tvTxnId;
    private MaterialButton btnDone;

    // Bank Transfer receipt views
    private MaterialCardView cardBankTransferReceipt;
    private ImageView ivPaymentProof;
    private TextView tvBankTxnRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        Intent intent = getIntent();
        rsvpId = intent.getStringExtra("rsvpId");
        eventName = intent.getStringExtra("eventName");
        eventDate = intent.getStringExtra("eventDate");
        transactionId = intent.getStringExtra("transactionId");
        qrPayload = intent.getStringExtra("qrPayload");
        paymentMethod = intent.getStringExtra("paymentMethod");
        paymentProofUrl = intent.getStringExtra("paymentProofUrl");

        bindViews();
        setupUI();
    }

    private void bindViews() {
        ivQrCode = findViewById(R.id.ivTicketQrCode);
        tvEventName = findViewById(R.id.tvTicketEventName);
        tvEventDate = findViewById(R.id.tvTicketEventDate);
        tvTxnId = findViewById(R.id.tvTicketTxnId);
        btnDone = findViewById(R.id.btnTicketDone);
        cardBankTransferReceipt = findViewById(R.id.cardBankTransferReceipt);
        ivPaymentProof = findViewById(R.id.ivPaymentProof);
        tvBankTxnRef = findViewById(R.id.tvBankTxnRef);
    }

    private void setupUI() {
        tvEventName.setText(eventName);
        tvEventDate.setText(eventDate);
        tvTxnId.setText("Txn ID: " + transactionId);

        if (qrPayload != null) {
            // Generate QR code (400x400dp is roughly 1200x1200px on high density, 
            // but we'll use a standard size and scale it via ImageView)
            Bitmap qrBitmap = QRCodeHelper.generateQRCode(qrPayload, 800, 800);
            if (qrBitmap != null) {
                ivQrCode.setImageBitmap(qrBitmap);
            }
        }

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Show EasyPaisa/JazzCash-style receipt card for Bank Transfer payments
        if ("BANK_TRANSFER".equals(paymentMethod)) {
            cardBankTransferReceipt.setVisibility(View.VISIBLE);
            tvBankTxnRef.setText(TextUtils.isEmpty(transactionId) ? "N/A" : transactionId);

            if (!TextUtils.isEmpty(paymentProofUrl)) {
                Glide.with(this)
                        .load(paymentProofUrl)
                        .placeholder(R.drawable.bg_placeholder_image)
                        .into(ivPaymentProof);
            }
        } else {
            cardBankTransferReceipt.setVisibility(View.GONE);
        }
    }
}
