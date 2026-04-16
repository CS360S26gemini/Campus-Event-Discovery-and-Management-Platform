package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.CampusEventDiscovery.callback.AuthCallback;
import com.example.CampusEventDiscovery.repository.MockAuthRepository;
import com.example.CampusEventDiscovery.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AuthRepositoryTest.java
 *
 * Unit tests for authentication flows.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class AuthRepositoryTest {

    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseFirestore mockDb;
    @Mock private FirebaseUser mockUser;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDocRef;
    @Mock private Query mockQuery;
    @Mock private Task mockAuthTask;
    @Mock private Task mockQueryTask;

    private MockAuthRepository mockAuthRepo;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockAuthRepo = new MockAuthRepository();

        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);
        when(mockCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);

        when(mockAuthTask.addOnSuccessListener(any())).thenReturn(mockAuthTask);
        when(mockAuthTask.addOnFailureListener(any())).thenReturn(mockAuthTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);
        when(mockQuery.get()).thenReturn(mockQueryTask);
    }

    @Test
    public void testLogin_ValidCredentials_Succeeds() {
        AtomicBoolean success = new AtomicBoolean(false);
        mockAuthRepo.login("test@lums.edu.pk", "Password123!", new AuthCallback() {
            @Override public void onSuccess(User user) { success.set(true); }
            @Override public void onFailure(String errorMessage) {}
        });

        assertTrue(success.get());
    }

    @Test
    public void testLogin_WrongPassword_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        mockAuthRepo.login("test@lums.edu.pk", "wrongpass", new AuthCallback() {
            @Override public void onSuccess(User user) {}
            @Override public void onFailure(String errorMessage) { errorFired.set(true); }
        });

        assertTrue(errorFired.get());
    }

    @Test
    public void testLogin_EmptyEmail_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        mockAuthRepo.login("", "Password123!", new AuthCallback() {
            @Override public void onSuccess(User user) {}
            @Override public void onFailure(String errorMessage) { errorFired.set(true); }
        });

        assertTrue(errorFired.get());
    }

    @Test
    public void testLogin_EmptyPassword_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        mockAuthRepo.login("test@lums.edu.pk", "", new AuthCallback() {
            @Override public void onSuccess(User user) {}
            @Override public void onFailure(String errorMessage) { errorFired.set(true); }
        });

        assertTrue(errorFired.get());
    }

    @Test
    public void testLogin_UnregisteredUser_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        mockAuthRepo.login("nobody@nowhere.com", "Password123!", new AuthCallback() {
            @Override public void onSuccess(User user) {}
            @Override public void onFailure(String errorMessage) { errorFired.set(true); }
        });

        assertTrue(errorFired.get());
    }

    @Test
    public void testSignup_ValidInput_Succeeds() {
        AtomicReference<User> createdUser = new AtomicReference<>();
        mockAuthRepo.signup("New User", "new@lums.edu.pk", "Password123!", "attendee", new AuthCallback() {
            @Override public void onSuccess(User user) { createdUser.set(user); }
            @Override public void onFailure(String errorMessage) {}
        });

        assertNotNull(createdUser.get());
        assertEquals("attendee", createdUser.get().getRole());
    }

    @Test
    public void testSignup_OrganizerRole_SetCorrectly() {
        AtomicReference<User> createdUser = new AtomicReference<>();
        mockAuthRepo.signup("Event Pro", "organizer@lums.edu.pk", "Password123!", "organizer", new AuthCallback() {
            @Override public void onSuccess(User user) { createdUser.set(user); }
            @Override public void onFailure(String errorMessage) {}
        });

        assertNotNull(createdUser.get());
        assertEquals("organizer", createdUser.get().getRole());
    }

    @Test
    public void testSignup_DuplicateEmail_FiresError() {
        mockAuthRepo.signup("First", "dup@lums.edu.pk", "Password123!", "attendee", new AuthCallback() {
            @Override public void onSuccess(User u) {}
            @Override public void onFailure(String errorMessage) {}
        });

        AtomicBoolean errorFired = new AtomicBoolean(false);
        mockAuthRepo.signup("Second", "dup@lums.edu.pk", "Password123!", "attendee", new AuthCallback() {
            @Override public void onSuccess(User u) {}
            @Override public void onFailure(String errorMessage) { errorFired.set(true); }
        });

        assertTrue("Duplicate email should fire onFailure", errorFired.get());
    }

    @Test
    public void testSignup_EmptyName_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        mockAuthRepo.signup("", "user@lums.edu.pk", "Password123!", "attendee", new AuthCallback() {
            @Override public void onSuccess(User u) {}
            @Override public void onFailure(String errorMessage) { errorFired.set(true); }
        });

        assertTrue(errorFired.get());
    }

    @Test
    public void testLogout_ClearsSession() {
        mockAuthRepo.login("test@lums.edu.pk", "Password123!", new AuthCallback() {
            @Override public void onSuccess(User u) {}
            @Override public void onFailure(String errorMessage) {}
        });

        mockAuthRepo.logout();

        assertFalse("User should not be authenticated after logout", mockAuthRepo.isLoggedIn());
    }

    @Test
    public void testLogout_WhenNotLoggedIn_NoException() {
        try {
            mockAuthRepo.logout();
        } catch (Exception e) {
            throw new AssertionError("logout() must not throw when no user is signed in", e);
        }
    }

    @Test
    public void testLogin_AdminCredentials_BypassesFirebase() {
        AtomicReference<User> adminUser = new AtomicReference<>();
        mockAuthRepo.login("admin", "admin123", new AuthCallback() {
            @Override public void onSuccess(User u) { adminUser.set(u); }
            @Override public void onFailure(String errorMessage) {}
        });

        assertNotNull(adminUser.get());
        assertEquals("admin", adminUser.get().getRole());
    }

    @Test
    public void testIsLoggedIn_AfterLogin_ReturnsTrue() {
        mockAuthRepo.login("test@lums.edu.pk", "Password123!", new AuthCallback() {
            @Override public void onSuccess(User u) {}
            @Override public void onFailure(String errorMessage) {}
        });

        assertTrue(mockAuthRepo.isLoggedIn());
    }

    @Test
    public void testIsLoggedIn_BeforeLogin_ReturnsFalse() {
        assertFalse(mockAuthRepo.isLoggedIn());
    }
}
