package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

/**
 * EventRepositoryTest.java
 *
 * Unit tests for EventRepository using Robolectric and Mockito.
 * Tests focus on repository interaction logic, including RSVP transactions,
 * visibility filters, and search behavior.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class EventRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private CollectionReference mockCollection;
    @Mock
    private DocumentReference mockDocRef;
    @Mock
    private Query mockQuery;
    @Mock
    private Task mockTask; // Raw Task used to avoid generic type mismatch in thenReturn()

    private EventRepository repository;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.print("RUNNING: " + description.getMethodName() + " ... ");
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println("PASS");
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println("FAIL (" + e.getMessage() + ")");
        }
    };

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new EventRepository(mockDb);
        
        // Setup default mock behaviors for Firestore collection/document pathing
        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);
        when(mockCollection.document()).thenReturn(mockDocRef);
        when(mockDocRef.collection(anyString())).thenReturn(mockCollection);
        
        // Mock chainable Task methods (onSuccess, onFailure) to return the task itself
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);
        
        // Mock Firestore methods that return a Task
        when(mockQuery.get()).thenReturn(mockTask);
        when(mockDocRef.get()).thenReturn(mockTask);
        when(mockCollection.get()).thenReturn(mockTask);
        when(mockDb.runTransaction(any())).thenReturn(mockTask);
        
        // Mock query filters to return the query mock for chaining
        when(mockCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.orderBy(anyString(), any())).thenReturn(mockQuery);
    }

    /**
     * Test RSVP Transaction Initiation.
     * Ensures that the repository uses a transaction for RSVP to handle
     * capacity tracking and prevent multiple registrations.
     */
    @Test
    public void testRsvpEvent_TransactionInitiated() {
        Event event = new Event();
        event.setEventId("event123");
        event.setCapacity(100);

        repository.rsvpEvent("user1", event, "John Doe", new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(Exception e) {}
        });

        // Verify that it attempted to start a transaction
        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Test Organizer Events Visibility.
     * Verifies that the query for an organizer's events includes the correct filters.
     */
    @Test
    public void testOrganizerEvents_VisibilityCheck() {
        repository.getOrganizerEvents("org123", new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {}
            @Override
            public void onError(Exception e) {}
        });

        verify(mockCollection).whereEqualTo("organizerId", "org123");
        verify(mockQuery).whereEqualTo("status", "active");
        verify(mockQuery).get();
    }

    /**
     * Test Search Filtering.
     * Ensures search queries target active events and initiate a fetch.
     */
    @Test
    public void testSearchEvents_QueryFiltering() {
        repository.searchEvents("Concert", "Music", new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {}
            @Override
            public void onError(Exception e) {}
        });
        
        verify(mockCollection).whereEqualTo("status", "active");
        verify(mockQuery).get();
    }

    /**
     * Test Event Retrieval by ID.
     * Verifies correctly targeting the specific document path.
     */
    @Test
    public void testGetEventById_PathVerification() {
        repository.getEventById("event123", new EventRepository.SingleEventCallback() {
            @Override
            public void onSuccess(Event event) {}
            @Override
            public void onError(Exception e) {}
        });

        verify(mockCollection).document("event123");
        verify(mockDocRef).get();
    }

    /**
     * Test Input Validation for Attendee Increment.
     * Verifies error handling for empty input.
     */
    @Test
    public void testIncrementAttendeeCount_EmptyIdValidation() {
        repository.incrementAttendeeCount("", new EventRepository.ActionCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(Exception e) {
                assertEquals("eventId is empty", e.getMessage());
            }
        });
    }

    /**
     * Test RSVP Pathing.
     * Verifies that the repository targets the correct Firestore locations for RSVPs.
     */
    @Test
    public void testRsvpEvent_ChecksCorrectPaths() {
        Event event = new Event();
        event.setEventId("event123");

        repository.rsvpEvent("user1", event, "John Doe", null);

        verify(mockDb).collection("users");
        verify(mockDb).collection("events");
    }
}
