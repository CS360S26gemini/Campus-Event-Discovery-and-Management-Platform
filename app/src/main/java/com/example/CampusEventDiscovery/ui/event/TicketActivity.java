package com.example.CampusEventDiscovery.ui.event;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.util.QRCodeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

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

    private ImageView ivQrCode;
    private MaterialToolbar toolbarTicket;
    private TextView tvEventName, tvEventDate, tvTxnId;
    private MaterialButton btnDone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        Intent intent = getIntent();
        rsvpId = intent.getStringExtra("rsvpId");
        eventName = intent.getStringExtra("eventName");
        eventDate = intent.getStringExtra("eventDate");
        transactionId = intent.getStringExtra("transactionId");
        qrPayload = intent.getStringExtra("qrPayload");

        bindViews();
        setupUI();
    }

    private void bindViews() {
        toolbarTicket = findViewById(R.id.toolbarTicket);
        ivQrCode = findViewById(R.id.ivTicketQrCode);
        tvEventName = findViewById(R.id.tvTicketEventName);
        tvEventDate = findViewById(R.id.tvTicketEventDate);
        tvTxnId = findViewById(R.id.tvTicketTxnId);
        btnDone = findViewById(R.id.btnTicketDone);
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

        toolbarTicket.setNavigationOnClickListener(v -> finish());

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
