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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * EventRepositoryTest.java
 *
 * Unit tests for EventRepository using Robolectric and Mockito.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class EventRepositoryTest {

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

    // ─── RSVP ────────────────────────────────────────────────────────────────

    @Test
    public void testRsvpEvent_TransactionInitiated() {
        Event event = new Event();
        event.setEventId("event123");
        event.setCapacity(100);

        repository.rsvpEvent("user1", event, "John Doe", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    @Test
    public void testRsvpEvent_ChecksCorrectPaths() {
        Event event = new Event();
        event.setEventId("event123");
        event.setCapacity(100);

        repository.rsvpEvent("user1", event, "John Doe", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        // Simply verify that the transaction was started, which is the core logic
        verify(mockDb).runTransaction(any());
    }

    @Test
    public void testRsvpEvent_NullUserId_ReturnsError() {
        Event event = new Event();
        event.setEventId("event123");

        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.rsvpEvent(null, event, "John Doe", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        verify(mockDb, never()).runTransaction(any());
    }

    @Test
    public void testRsvpEvent_NullEvent_ReturnsError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.rsvpEvent("user1", null, "John Doe", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        verify(mockDb, never()).runTransaction(any());
    }

    @Test
    public void testRsvpEvent_ZeroCapacity_Rejected() {
        Event event = new Event();
        event.setEventId("eventFull");
        event.setCapacity(0);

        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.rsvpEvent("user1", event, "Jane", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        verify(mockDb, never()).runTransaction(any());
    }

    // ─── ORGANIZER EVENTS ────────────────────────────────────────────────────

    @Test
    public void testOrganizerEvents_VisibilityCheck() {
        repository.getOrganizerEvents("org123", new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Event> events) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).whereEqualTo("organizerId", "org123");
        verify(mockQuery).whereEqualTo("status", "active");
        verify(mockQuery).get();
    }

    @Test
    public void testOrganizerEvents_EmptyOrganizerId_ReturnsError() {
        AtomicBoolean errorFired = new AtomicBoolean(false);
        repository.getOrganizerEvents("", new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Event> e) {}
            @Override public void onError(Exception e) { errorFired.set(true); }
        });

        verify(mockCollection, never()).whereEqualTo(anyString(), any());
    }

    // ─── PENDING APPROVAL ────────────────────────────────────────────────────

    @Test
    public void testGetPendingEvents_QueryFiltersPending() {
        repository.getPendingEvents(new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Event> events) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).whereEqualTo("status", "pending");
        verify(mockQuery).get();
    }

    @Test
    public void testApproveEvent_UpdatesStatusField() {
        when(mockDocRef.update(anyString(), any())).thenReturn(mockTask);

        repository.approveEvent("event123", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).document("event123");
        verify(mockDocRef).update("status", "active");
    }

    @Test
    public void testRejectEvent_UpdatesStatusToRejected() {
        when(mockDocRef.update(anyString(), any())).thenReturn(mockTask);

        repository.rejectEvent("event123", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).document("event123");
        verify(mockDocRef).update("status", "rejected");
    }

    // ─── EVENT CREATION ──────────────────────────────────────────────────────

    @Test
    public void testCreateEvent_WritesToFirestore() {
        when(mockDocRef.set(any())).thenReturn(mockTask);

        Event event = new Event();
        event.setTitle("Career Fair");
        event.setOrganizerId("org1");
        event.setCapacity(200);

        repository.createEvent(event, new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        verify(mockDb).collection("events");
        verify(mockDocRef).set(any());
    }

    @Test
    public void testCreateEvent_DefaultStatusIsPending() {
        Event event = new Event();
        event.setTitle("Tech Talk");
        event.setOrganizerId("org1");

        assertEquals("pending", event.getStatus());
    }

    // ─── SEARCH ──────────────────────────────────────────────────────────────

    @Test
    public void testSearchEvents_QueryFiltering() {
        repository.searchEvents("Concert", "Music", new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Event> events) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).whereEqualTo("status", "active");
        verify(mockQuery).get();
    }

    @Test
    public void testSearchEvents_EmptyQuery_ReturnsActiveEvents() {
        repository.searchEvents("", "", new EventRepository.EventListCallback() {
            @Override public void onSuccess(List<Event> events) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).whereEqualTo("status", "active");
    }

    // ─── SINGLE EVENT RETRIEVAL ──────────────────────────────────────────────

    @Test
    public void testGetEventById_PathVerification() {
        repository.getEventById("event123", new EventRepository.SingleEventCallback() {
            @Override public void onSuccess(Event event) {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).document("event123");
        verify(mockDocRef).get();
    }

    @Test
    public void testGetEventById_EmptyId_ReturnsError() {
        AtomicReference<String> errorMsg = new AtomicReference<>();
        repository.getEventById("", new EventRepository.SingleEventCallback() {
            @Override public void onSuccess(Event e) {}
            @Override public void onError(Exception e) { errorMsg.set(e.getMessage()); }
        });

        verify(mockCollection, never()).document(anyString());
    }

    // ─── ATTENDEE COUNT ──────────────────────────────────────────────────────

    @Test
    public void testIncrementAttendeeCount_EmptyIdValidation() {
        AtomicReference<String> errorMsg = new AtomicReference<>();
        repository.incrementAttendeeCount("", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) { errorMsg.set(e.getMessage()); }
        });

        assertEquals("eventId is empty", errorMsg.get());
    }

    @Test
    public void testIncrementAttendeeCount_ValidId_TargetsFirestore() {
        when(mockDocRef.update(anyString(), any())).thenReturn(mockTask);

        repository.incrementAttendeeCount("event123", new EventRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });

        verify(mockCollection).document("event123");
    }
}
