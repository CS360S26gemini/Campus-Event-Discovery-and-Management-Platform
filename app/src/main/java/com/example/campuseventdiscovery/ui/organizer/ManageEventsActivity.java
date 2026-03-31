package com.example.campuseventdiscovery.ui.organizer;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscovery.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * ManageEventsActivity.java
 *
 * Organizer manage events screen.
 */
public class ManageEventsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        MaterialToolbar toolbar = findViewById(R.id.toolbarManageEvents);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
}