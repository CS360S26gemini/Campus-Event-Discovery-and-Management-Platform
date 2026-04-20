package com.example.CampusEventDiscovery.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.CampusEventDiscovery.R;
import com.google.android.material.button.MaterialButton;

public class HelpFragment extends Fragment {

    public HelpFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton btnGotIt = view.findViewById(R.id.btnGotIt);
        btnGotIt.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }
}
