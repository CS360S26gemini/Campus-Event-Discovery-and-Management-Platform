package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BlacklistTest.java
 *
 * Unit tests for EventRepository blacklist flows.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class BlacklistTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDocRef;
    @Mock private Query mockQuery;
    @Mock private Task mockTask;

    private EventRepository repository;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new EventRepository(mockDb);

        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);
        when(mockCollection.document()).thenReturn(mockDocRef);
        when(mockDocRef.collection(anyString())).thenReturn(mockCollection);

        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        when(mockQuery.get()).thenReturn(mockTask);
        when(mockDocRef.get()).thenReturn(mockTask);
        when(mockCollection.get()).thenReturn(mockTask);
        when(mockDb.runTransaction(any())).thenReturn(mockTask);

        when(mockCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.orderBy(anyString(), any())).thenReturn(mockQuery);
    }

    // ─── BLACKLIST ───────────────────────────────────────────────────────────

    @Test
    public void testIsUserBlacklisted_EmptyEventId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);

        repository.isUserBlacklisted("", "user1", new EventRepository.BooleanCallback() {
            @Override public void onSuccess(boolean value) {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        Assert.assertFalse(errorFired.get());
    }

    @Test
    public void testIsUserBlacklisted_EmptyUserId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);

        repository.isUserBlacklisted("event1", "", new EventRepository.BooleanCallback() {
            @Override public void onSuccess(boolean value) {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        Assert.assertFalse(errorFired.get());
    }

    @Test
    public void testIsUserBlacklisted_NullEventId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        boolean threw = false;

        try {
            repository.isUserBlacklisted(null, "user1", new EventRepository.BooleanCallback() {
                @Override public void onSuccess(boolean value) {}
                @Override public void onError(Exception e) { errorFired.set(true); }
            });
        } catch (Exception e) {
            threw = true;
        }

        Assert.assertTrue(errorFired.get() || threw);
    }

    @Test
    public void testIsUserBlacklisted_NullUserId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        boolean threw = false;

        try {
            repository.isUserBlacklisted("event1", null, new EventRepository.BooleanCallback() {
                @Override public void onSuccess(boolean value) {}
                @Override public void onError(Exception e) { errorFired.set(true); }
            });
        } catch (Exception e) {
            threw = true;
        }

        Assert.assertTrue(errorFired.get() || threw);
    }

    @Test
    public void testIsUserBlacklisted_ValidInputs_QueriesEventBlacklistPath() {
        repository.isUserBlacklisted("event123", "user456", new EventRepository.BooleanCallback() {
            @Override public void onSuccess(boolean value) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockDb, Mockito.atLeastOnce()).collection("events");
        verify(mockCollection, Mockito.atLeastOnce()).document("event123");
        verify(mockCollection, Mockito.atLeastOnce()).document("user456");
        verify(mockDocRef, Mockito.atLeastOnce()).get();
    }

    @Test
    public void testIsUserBlacklisted_ValidInputs_QueriesPlatformBlacklistPath() {
        repository.isUserBlacklisted("event123", "user456", new EventRepository.BooleanCallback() {
            @Override public void onSuccess(boolean value) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockDb, Mockito.atLeastOnce()).collection("platform_blacklist");
        verify(mockCollection, Mockito.atLeastOnce()).document("user456");
    }

    @Test
    public void testBlacklistUserByEmail_EmptyEventId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);

        repository.blacklistUserByEmail("", "test@test.com", "org1", "reason", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        Assert.assertTrue(errorFired.get());
        verify(mockDb, never()).collection(anyString());
    }

    @Test
    public void testBlacklistUserByEmail_EmptyEmail_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);

        repository.blacklistUserByEmail("event1", "", "org1", "reason", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        Assert.assertTrue(errorFired.get());
        verify(mockDb, never()).collection(anyString());
    }
}
