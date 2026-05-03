package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.example.CampusEventDiscovery.util.DevSessionManager;
import com.example.CampusEventDiscovery.util.UserRoles;
import com.example.CampusEventDiscovery.util.WalkthroughManager;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class SecurityBehaviorTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        FirebaseAuth.getInstance().signOut();
        DevSessionManager.clearBypass(context);
    }

    @After
    public void tearDown() {
        DevSessionManager.clearBypass(context);
    }

    @Test
    public void devBypass_invalidRoleFallsBackToAttendee() {
        DevSessionManager.enableBypass(context, "super-admin");

        assertEquals(UserRoles.ATTENDEE, DevSessionManager.getBypassRole(context));
        assertEquals("demo-attendee-user", DevSessionManager.getEffectiveUserId(context));
    }

    @Test
    public void devBypass_effectiveIdentityMatchesAuthorizedRole() {
        DevSessionManager.enableBypass(context, UserRoles.ORGANIZER);
        assertEquals("demo-organizer-user", DevSessionManager.getEffectiveUserId(context));
        assertEquals("Test Organizer", DevSessionManager.getDisplayName(context));

        DevSessionManager.enableBypass(context, UserRoles.ADMIN);
        assertEquals("demo-admin-user", DevSessionManager.getEffectiveUserId(context));
        assertEquals("admin@test.local", DevSessionManager.getDisplayEmail(context));
    }

    @Test
    public void roleChecks_rejectUnknownAndLimitAdminPowers() {
        assertEquals("", UserRoles.sanitize("root"));
        assertEquals(UserRoles.ADMIN, UserRoles.sanitize(" ADMIN "));
        assertFalse(UserRoles.canUseAdminPowers("organizer"));
        assertTrue(UserRoles.canUseAdminPowers("admin"));
        assertTrue(UserRoles.canManageEvents("admin"));
        assertTrue(UserRoles.canManageEvents("organizer"));
        assertFalse(UserRoles.canManageEvents("attendee"));
    }

    @Test
    public void walkthroughIntent_flagMustBeExplicitlyPresent() {
        Intent regularIntent = new Intent();
        Intent walkthroughIntent = new Intent().putExtra(WalkthroughManager.EXTRA_WALKTHROUGH_MODE, true);

        assertFalse(WalkthroughManager.isWalkthroughIntent(regularIntent));
        assertFalse(WalkthroughManager.isWalkthroughIntent(null));
        assertTrue(WalkthroughManager.isWalkthroughIntent(walkthroughIntent));
    }

    @Test
    public void bypassDisabled_cannotProduceSyntheticIdentity() {
        assertFalse(DevSessionManager.isBypassEnabled(context));
        assertNull(DevSessionManager.getEffectiveUserId(context));
    }
}
