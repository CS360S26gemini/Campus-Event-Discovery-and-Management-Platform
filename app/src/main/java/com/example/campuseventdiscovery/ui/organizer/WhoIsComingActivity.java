package com.example.campuseventdiscovery.ui.organizer;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscovery.R;
import com.google.android.material.button.MaterialButton;

/**
 * WhoIsComingActivity.java
 *
 * Screen for organizers to see the list of registered participants and take actions like blacklisting.
 */
public class WhoIsComingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private RecyclerView rvParticipants;
    private MaterialButton btnBlacklist;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_who_is_coming);

        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        rvParticipants = findViewById(R.id.rvParticipants);
        btnBlacklist = findViewById(R.id.btnBlacklist);

        btnBack.setOnClickListener(v -> finish());
        
        // Participants list logic would go here
    }
}