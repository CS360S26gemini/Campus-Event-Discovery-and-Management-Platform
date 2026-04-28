package com.example.CampusEventDiscovery.ui.organizer;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.CampusEventDiscovery.R;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * ZXing capture activity with an explicit in-camera back affordance.
 */
public class TicketCaptureActivity extends CaptureActivity {

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_ticket_capture);
        return findViewById(R.id.zxing_barcode_scanner);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);

        View backButton = findViewById(R.id.btnScannerCameraBack);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }
}
