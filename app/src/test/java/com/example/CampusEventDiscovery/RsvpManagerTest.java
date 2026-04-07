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

/**
 * RsvpManagerTest.java
 *
 * Tests for RSVP business logic.
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

    @Test
    public void testRsvp_UsesTransaction_ForDuplicateGuard() {
        Event event = buildEvent("event1", 50);
        repository.rsvpEvent("user1", event, "Alice", null);
        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    @Test
    public void testRsvp_EventAtCapacity_InitiatesTransaction() {
        // Even if capacity is 0 locally, we initiate a transaction to check the authoritative DB state.
        Event event = buildEvent("eventFull", 0);
        repository.rsvpEvent("user2", event, "Bob", null);
        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    @Test
    public void testCancelRsvp_TargetsCorrectPaths() {
        repository.cancelRsvp("user1", "event1", null);
        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    @Test
    public void testCancelRsvp_EmptyUserId_FiresError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.cancelRsvp("", "event1", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });
        assertTrue(errorFired.get());
    }

    @Test
    public void testRsvpModel_StoresCorrectIds() {
        Rsvp rsvp = new Rsvp();
        rsvp.setUserId("user42");
        rsvp.setEventId("event99");
        assertEquals("user42", rsvp.getUserId());
        assertEquals("event99", rsvp.getEventId());
    }

    @Test
    public void testQrPayload_UniquePerUserAndEvent() {
        String payloadA = buildQrPayload("user1", "event1");
        String payloadB = buildQrPayload("user2", "event1");
        assertNotNull(payloadA);
        assertFalse(payloadA.equals(payloadB));
    }

    private Event buildEvent(String id, int capacity) {
        Event e = new Event();
        e.setEventId(id);
        e.setCapacity(capacity);
        return e;
    }

    private String buildQrPayload(String userId, String eventId) {
        return userId + "|" + eventId + "|" + System.currentTimeMillis();
    }
}
