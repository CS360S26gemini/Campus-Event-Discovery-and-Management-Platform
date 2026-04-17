package com.example.CampusEventDiscovery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.text.TextUtils;

import com.example.CampusEventDiscovery.model.SosAlert;
import com.example.CampusEventDiscovery.repository.SosRepository;
import com.example.CampusEventDiscovery.util.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SosRepositoryTest.java
 *
 * Mockito-based unit tests for {@link SosRepository}. All Firestore calls are
 * mocked, and {@link TextUtils} is stubbed statically so the repository's
 * empty-string guards behave identically to the Android runtime.
 */
@RunWith(MockitoJUnitRunner.class)
public class SosRepositoryTest {

    @Mock
    private FirebaseFirestore db;

    @Mock
    private WriteBatch batch;

    @Mock
    private CollectionReference collectionRef;

    @Mock
    private DocumentReference documentRef;

    @Mock
    private Task<Void> commitTask;

    private MockedStatic<TextUtils> textUtilsMock;

    private SosRepository repository;

    @Before
    public void setUp() {
        // Replicate TextUtils.isEmpty semantics on the JVM — android.jar stubs
        // throw "not mocked" without this override.
        textUtilsMock = mockStatic(TextUtils.class);
        textUtilsMock.when(() -> TextUtils.isEmpty(any()))
                .thenAnswer(invocation -> {
                    CharSequence seq = invocation.getArgument(0);
                    return seq == null || seq.length() == 0;
                });

        when(db.batch()).thenReturn(batch);
        when(db.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document()).thenReturn(documentRef);
        when(collectionRef.document(anyString())).thenReturn(documentRef);
        when(documentRef.collection(anyString())).thenReturn(collectionRef);
        when(batch.commit()).thenReturn(commitTask);
        when(commitTask.addOnSuccessListener(any(OnSuccessListener.class)))
                .thenReturn(commitTask);
        when(commitTask.addOnFailureListener(any(OnFailureListener.class)))
                .thenReturn(commitTask);

        repository = new SosRepository(db);
    }

    @After
    public void tearDown() {
        if (textUtilsMock != null) {
            textUtilsMock.close();
        }
    }

    private SosAlert buildAlert() {
        return new SosAlert(
                "uid-1",
                "Jane Doe",
                "event-42",
                "Spring Gala",
                "org-7",
                31.4808,
                74.3035,
                "https://maps.google.com/?q=31.4808,74.3035",
                1_700_000_000_000L,
                "ACTIVE");
    }

    /** Verifies that a successful batch commit propagates to SosCallback.onSuccess exactly once. */
    @Test
    public void testSendSosAlert_success_callsOnSuccess() {
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return commitTask;
        }).when(commitTask).addOnSuccessListener(any(OnSuccessListener.class));

        SosRepository.SosCallback callback = org.mockito.Mockito.mock(SosRepository.SosCallback.class);
        repository.sendSosAlert(buildAlert(), "org-7",
                Collections.singletonList("admin-1"), callback);

        verify(callback, times(1)).onSuccess();
        verify(callback, never()).onFailure(any(Exception.class));
    }

    /** Verifies that a failed batch commit surfaces the same exception via onFailure. */
    @Test
    public void testSendSosAlert_failure_callsOnFailure() {
        Exception boom = new RuntimeException("firestore down");
        doAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(boom);
            return commitTask;
        }).when(commitTask).addOnFailureListener(any(OnFailureListener.class));

        SosRepository.SosCallback callback = org.mockito.Mockito.mock(SosRepository.SosCallback.class);
        repository.sendSosAlert(buildAlert(), "org-7",
                Collections.singletonList("admin-1"), callback);

        verify(callback, times(1)).onFailure(boom);
        verify(callback, never()).onSuccess();
    }

    /** Verifies that an empty organizerId skips the organizer notification write. */
    @Test
    public void testSendSosAlert_emptyOrganizerId_skipsOrganizerNotification() {
        repository.sendSosAlert(buildAlert(), "", null, null);

        // Only the sos_alerts document should be staged.
        verify(batch, times(1)).set(any(DocumentReference.class), any());
        verify(db, times(1)).collection(Constants.COLLECTION_SOS_ALERTS);
        verify(db, never()).collection(Constants.COLLECTION_NOTIFICATIONS);
    }

    /** Verifies that a null adminIds list is treated as empty and does not throw. */
    @Test
    public void testSendSosAlert_nullAdminIds_doesNotCrash() {
        repository.sendSosAlert(buildAlert(), "org-7", null, null);

        // Commit must still run even with no admins.
        verify(batch, times(1)).commit();
    }

    /** Verifies only the sos_alerts and organizer notification are staged when adminIds is empty. */
    @Test
    public void testSendSosAlert_emptyAdminIds_onlyWritesSosAndOrganizer() {
        repository.sendSosAlert(buildAlert(), "org-7", new ArrayList<>(), null);

        verify(batch, times(2)).set(any(DocumentReference.class), any());
    }

    /** Verifies a sos_alerts write plus one notification per admin when multiple admins exist. */
    @Test
    public void testSendSosAlert_multipleAdmins_writesAllNotifications() {
        List<String> admins = Arrays.asList("admin-1", "admin-2", "admin-3");
        repository.sendSosAlert(buildAlert(), "org-7", admins, null);

        // 1 sos_alerts + 1 organizer + 3 admins = 5 batch.set calls.
        verify(batch, times(5)).set(any(DocumentReference.class), any());
    }

    /** Verifies a null SosCallback never causes an NPE when the success path fires. */
    @Test
    public void testSendSosAlert_nullCallback_doesNotCrash() {
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return commitTask;
        }).when(commitTask).addOnSuccessListener(any(OnSuccessListener.class));

        repository.sendSosAlert(buildAlert(), "org-7",
                Collections.singletonList("admin-1"), null);

        verify(batch, times(1)).commit();
    }

    /** Verifies the repository uses Constants (not literal strings) for Firestore collection names. */
    @Test
    public void testSendSosAlert_usesConstantsForCollectionNames() {
        repository.sendSosAlert(buildAlert(), "org-7",
                Collections.singletonList("admin-1"), null);

        verify(db).collection(Constants.COLLECTION_SOS_ALERTS);
        verify(db, times(2)).collection(Constants.COLLECTION_NOTIFICATIONS);
    }
}
