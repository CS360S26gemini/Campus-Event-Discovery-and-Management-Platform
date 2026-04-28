package com.example.CampusEventDiscovery.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VendorRepositoryContractTest {

    @Test
    public void repositoryDefinesVendorProposalCollectionAndCallbacks() throws Exception {
        String source = readRepository();

        assertTrue(source.contains("COLLECTION_VENDOR_PROPOSALS = \"vendorProposals\""));
        assertTrue(source.contains("interface VendorProposalListCallback"));
        assertTrue(source.contains("interface IntegerCallback"));
    }

    @Test
    public void proposeVendorInitializesPendingUnreadSubmission() throws Exception {
        String source = readRepository();

        assertTrue(source.contains("public void proposeVendor(VendorProposal proposal"));
        assertTrue(source.contains("proposal.setStatus(\"pending\")"));
        assertTrue(source.contains("proposal.setReadByAdmin(false)"));
        assertTrue(source.contains("proposal.setCreatedAt(Timestamp.now())"));
        assertTrue(source.contains("db.collection(COLLECTION_VENDOR_PROPOSALS)"));
        assertTrue(source.contains(".add(proposal)"));
    }

    @Test
    public void vendorFetchAndUnreadCountQueriesUseExpectedFields() throws Exception {
        String source = readRepository();

        assertTrue(source.contains("public void getVendorProposalsForEvent"));
        assertTrue(source.contains(".whereEqualTo(\"eventId\", eventId)"));
        assertTrue(source.contains("public void getAllVendorProposals"));
        assertTrue(source.contains("observeUnreadPendingVendorProposalCount"));
        assertTrue(source.contains(".whereEqualTo(\"status\", \"pending\")"));
        assertTrue(source.contains(".whereEqualTo(\"readByAdmin\", false)"));
    }

    @Test
    public void vendorReviewWritesApprovedRejectedStateAndOrganizerNotification() throws Exception {
        String source = readRepository();

        assertTrue(source.contains("approveVendorProposal"));
        assertTrue(source.contains("rejectVendorProposal"));
        assertTrue(source.contains("reviewVendorProposal"));
        assertTrue(source.contains("\"status\", status"));
        assertTrue(source.contains("\"reviewedAt\", Timestamp.now()"));
        assertTrue(source.contains("\"vendor_\" + status"));
    }

    private static String readRepository() throws Exception {
        Path fromRoot = Paths.get("app", "src", "main", "java", "com", "example", "CampusEventDiscovery", "repository", "EventRepository.java");
        if (Files.exists(fromRoot)) {
            return new String(Files.readAllBytes(fromRoot), StandardCharsets.UTF_8);
        }
        Path fromModule = Paths.get("src", "main", "java", "com", "example", "CampusEventDiscovery", "repository", "EventRepository.java");
        return new String(Files.readAllBytes(fromModule), StandardCharsets.UTF_8);
    }
}
