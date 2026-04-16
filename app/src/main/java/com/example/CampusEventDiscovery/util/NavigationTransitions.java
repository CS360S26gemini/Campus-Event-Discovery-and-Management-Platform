package com.example.CampusEventDiscovery.util;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.CampusEventDiscovery.R;

/**
 * Shared navigation motion for internal fragment changes.
 */
public final class NavigationTransitions {

    private NavigationTransitions() {
    }

    public static void replace(FragmentManager fragmentManager,
                               int containerId,
                               Fragment fragment,
                               boolean addToBackStack,
                               boolean animate) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (animate) {
            transaction.setCustomAnimations(
                    R.anim.screen_enter,
                    R.anim.screen_exit,
                    R.anim.screen_pop_enter,
                    R.anim.screen_pop_exit
            );
        }

        transaction.replace(containerId, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commit();
    }
}
