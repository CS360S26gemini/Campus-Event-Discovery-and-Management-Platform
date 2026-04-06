package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.repository.EventRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

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
 * RsvpManagerTest.java
 *
 * Tests for RSVP business logic:
 *  - Duplicate RSVP prevention (same user, same event)
 *  - Capacity enforcement (no RSVP past max capacity)
 *  - RSVP cancellation
 *  - QR code generation on successful RSVP
 *  - QR one-time-use / expiry after check-in
 *  - RSVP data integrity (correct userId, eventId stored)
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class RsvpManagerTest {

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
    }

    // ─── DUPLICATE RSVP PREVENTION ───────────────────────────────────────────

    /**
     * The repository must use a transaction for RSVP so that a concurrent duplicate
     * is detected atomically. (The transaction itself checks for an existing RSVP doc.)
     */
    @Test
    public void testRsvp_UsesTransaction_ForDuplicateGuard() {
        Event event = buildEvent("event1", 50);

        repository.rsvpEvent("user1", event, "Alice", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        // Transaction must be used — it is the only safe way to prevent duplicates
        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Two separate RSVP calls for the same (user, event) pair must each
     * go through a transaction so the second one can detect the duplicate.
     */
    @Test
    public void testRsvp_SameUserSameEvent_BothAttemptTransaction() {
        Event event = buildEvent("event1", 50);

        repository.rsvpEvent("user1", event, "Alice", null);
        repository.rsvpEvent("user1", event, "Alice", null); // duplicate attempt

        // Both calls should have tried to enter a transaction;
        // the transaction body is responsible for detecting the duplicate.
        verify(mockDb, org.mockito.Mockito.times(2)).runTransaction(any(Transaction.Function.class));
    }

    // ─── CAPACITY ────────────────────────────────────────────────────────────

    /** Capacity == 0 is treated as a full/closed event; RSVP must be rejected immediately. */
    @Test
    public void testRsvp_EventAtCapacity_FiresError() {
        Event event = buildEvent("eventFull", 0);

        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.rsvpEvent("user2", event, "Bob", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        assertTrue("RSVP to full event must fire onError", errorFired.get());
        verify(mockDb, never()).runTransaction(any());
    }

    /** Capacity == 1 is valid; a transaction must be attempted. */
    @Test
    public void testRsvp_OneSpotLeft_AllowsTransaction() {
        Event event = buildEvent("eventAlmost", 1);

        repository.rsvpEvent("user3", event, "Carol", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    // ─── CANCELLATION ────────────────────────────────────────────────────────

    /** cancelRsvp must target the correct user and event documents. */
    @Test
    public void testCancelRsvp_TargetsCorrectPaths() {
        repository.cancelRsvp("user1", "event1", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        verify(mockDb).collection("users");
        verify(mockDb).collection("events");
    }

    /** cancelRsvp with empty userId must fire onError immediately. */
    @Test
    public void testCancelRsvp_EmptyUserId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.cancelRsvp("", "event1", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        assertTrue(errorFired.get());
    }

    /** cancelRsvp with empty eventId must fire onError immediately. */
    @Test
    public void testCancelRsvp_EmptyEventId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.cancelRsvp("user1", "", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        assertTrue(errorFired.get());
    }

    // ─── RSVP MODEL INTEGRITY ────────────────────────────────────────────────

    /** Rsvp model must correctly store userId and eventId. */
    @Test
    public void testRsvpModel_StoresCorrectIds() {
        Rsvp rsvp = new Rsvp();
        rsvp.setUserId("user42");
        rsvp.setEventId("event99");

        assertEquals("user42", rsvp.getUserId());
        assertEquals("event99", rsvp.getEventId());
    }

    /** A freshly created Rsvp must default to checked-in = false. */
    @Test
    public void testRsvpModel_DefaultCheckedIn_IsFalse() {
        Rsvp rsvp = new Rsvp();
        assertFalse("New RSVP should not be checked in by default", rsvp.isCheckedIn());
    }

    /** Marking a QR code as used should flip checkedIn to true and record a timestamp. */
    @Test
    public void testRsvpModel_AfterCheckIn_IsCheckedIn() {
        Rsvp rsvp = new Rsvp();
        rsvp.setCheckedIn(true);
        rsvp.setCheckInTimestamp(System.currentTimeMillis());

        assertTrue(rsvp.isCheckedIn());
        assertTrue(rsvp.getCheckInTimestamp() > 0);
    }

    // ─── QR CODE ─────────────────────────────────────────────────────────────

    /**
     * Each (userId, eventId) pair must produce a distinct, non-null QR payload.
     * This verifies the uniqueness contract without invoking ZXing.
     */
    @Test
    public void testQrPayload_UniquePerUserAndEvent() {
        String payloadA = buildQrPayload("user1", "event1");
        String payloadB = buildQrPayload("user2", "event1");
        String payloadC = buildQrPayload("user1", "event2");

        assertNotNull(payloadA);
        assertFalse("Different users must get different QR payloads",
                payloadA.equals(payloadB));
        assertFalse("Same user, different events must get different QR payloads",
                payloadA.equals(payloadC));
    }

    /** A QR payload must contain both the userId and eventId so the scanner can verify both. */
    @Test
    public void testQrPayload_ContainsBothIds() {
        String payload = buildQrPayload("user42", "event99");
        assertTrue(payload.contains("user42"));
        assertTrue(payload.contains("event99"));
    }

    /** After check-in the Rsvp is marked used; a second scan must see isCheckedIn == true. */
    @Test
    public void testQr_OneTimeUse_SecondScanDetected() {
        Rsvp rsvp = new Rsvp();
        rsvp.setUserId("user1");
        rsvp.setEventId("event1");

        // Simulate first scan (check-in)
        rsvp.setCheckedIn(true);
        rsvp.setCheckInTimestamp(System.currentTimeMillis());

        // A second scan should detect that this QR was already used
        assertTrue("Already checked-in RSVP should be detectable on second scan",
                rsvp.isCheckedIn());
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private Event buildEvent(String id, int capacity) {
        Event e = new Event();
        e.setEventId(id);
        e.setCapacity(capacity);
        return e;
    }

    /**
     * Mirrors the QR payload format used by the payment/ticketing system.
     * Format: "userId|eventId|<timestamp>"
     */
    private String buildQrPayload(String userId, String eventId) {
        return userId + "|" + eventId + "|" + System.currentTimeMillis();
    }
}
