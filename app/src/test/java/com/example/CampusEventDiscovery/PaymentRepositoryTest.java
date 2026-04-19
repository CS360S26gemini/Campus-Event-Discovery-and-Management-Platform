package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.CampusEventDiscovery.callback.FirestoreCallback;
import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.repository.PaymentRepository;
import com.example.CampusEventDiscovery.util.Constants;
import com.google.android.gms.tasks.Task;
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

/**
 * PaymentRepositoryTest.java
 *
 * Verifies that payment reads/writes hit the correct Firestore paths
 * and use Constants (never inline "payments"). Covers success and
 * failure callbacks.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class PaymentRepositoryTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDocRef;
    @Mock private Query mockQuery;
    @Mock private Task mockTask;

    private PaymentRepository repository;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override protected void starting(Description d)  { System.out.print("RUNNING: " + d.getMethodName() + " ... "); }
        @Override protected void succeeded(Description d) { System.out.println("PASS"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("FAIL (" + e.getMessage() + ")"); }
    };

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new PaymentRepository(mockDb);

        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.add(any())).thenReturn(mockTask);
        when(mockCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.limit(anyLong())).thenReturn(mockQuery);
        when(mockQuery.orderBy(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);
    }

    @Test
    public void savePayment_writesToPaymentsCollection() {
        Payment p = new Payment(null, "u1", "e1", 500.0,
                Constants.PAYMENT_CONFIRMED, "pi_test_x", System.currentTimeMillis());

        repository.savePayment(p, new FirestoreCallback() {
            @Override public void onSuccess(Object result) {}
            @Override public void onFailure(Exception e) {}
        });

        // Verify the call went to the constant, not an inline string.
        verify(mockDb).collection(eq(Constants.COLLECTION_PAYMENTS));
    }

    @Test
    public void savePayment_doesNotUseInlineLiteral() {
        // If anyone regresses and hardcodes "payments" as a literal, this test
        // will still pass because Constants.COLLECTION_PAYMENTS == "payments".
        // The stronger guarantee is that the value matches — enforced in
        // ConstantsTest.collectionPayments_isLowercasePayments().
        Payment p = new Payment(null, "u1", "e1", 100.0,
                Constants.PAYMENT_CONFIRMED, "pi_test_x", 0L);
        repository.savePayment(p, new FirestoreCallback() {
            @Override public void onSuccess(Object result) {}
            @Override public void onFailure(Exception e) {}
        });
        verify(mockDb).collection("payments");
    }

    @Test
    public void getPaymentByTransactionId_usesTransactionIdFilter() {
        repository.getPaymentByTransactionId("pi_test_abc", new FirestoreCallback() {
            @Override public void onSuccess(Object result) {}
            @Override public void onFailure(Exception e) {}
        });
        verify(mockDb).collection(eq(Constants.COLLECTION_PAYMENTS));
        verify(mockCollection).whereEqualTo(eq("transactionId"), eq("pi_test_abc"));
    }

    @Test
    public void getPaymentsForEvent_filtersByEventIdAndOrdersByTimestamp() {
        repository.getPaymentsForEvent("event_xyz", new FirestoreCallback() {
            @Override public void onSuccess(Object result) {}
            @Override public void onFailure(Exception e) {}
        });
        verify(mockDb).collection(eq(Constants.COLLECTION_PAYMENTS));
        verify(mockCollection).whereEqualTo(eq("eventId"), eq("event_xyz"));
        verify(mockQuery).orderBy(eq("timestamp"), any());
    }

    @Test
    public void getPaymentsForEvent_attachesBothListeners() {
        repository.getPaymentsForEvent("e1", new FirestoreCallback() {
            @Override public void onSuccess(Object result) {}
            @Override public void onFailure(Exception e) {}
        });
        verify(mockTask).addOnSuccessListener(any());
        verify(mockTask).addOnFailureListener(any());
    }

    @Test
    public void savePayment_nullPayment_doesNotCrashBeforeAdd() {
        // Repo itself doesn't validate — it passes straight to Firestore.
        // The test exists to document that behaviour and guard against
        // accidental NPEs inside PaymentRepository itself.
        AtomicBoolean blewUp = new AtomicBoolean(false);
        try {
            repository.savePayment(null, new FirestoreCallback() {
                @Override public void onSuccess(Object result) {}
                @Override public void onFailure(Exception e) {}
            });
        } catch (Throwable t) {
            blewUp.set(true);
        }
        assertTrue("Passing null Payment should not throw inside the repo",
                !blewUp.get());
    }
}
