package com.example.CampusEventDiscovery.ui.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.CampusEventDiscovery.R;

public class AdminHomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.CampusEventDiscovery.util.ThemeManager.applyAccentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);
    }
}
