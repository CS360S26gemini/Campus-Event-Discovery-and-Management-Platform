package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.example.CampusEventDiscovery.model.VendorProposal;

import org.junit.Test;

public class VendorProposalTest {

    @Test
    public void defaultState_isPendingAndUnreadByAdmin() {
        VendorProposal proposal = new VendorProposal();

        assertEquals("pending", proposal.getStatus());
        assertFalse(proposal.isReadByAdmin());
    }

    @Test
    public void settersAndGetters_preserveVendorWorkflowFields() {
        VendorProposal proposal = new VendorProposal();
        proposal.setProposalId("proposal123");
        proposal.setVendorName("Campus Cafe");
        proposal.setDescription("Coffee and snacks");
        proposal.setPhone("5551234");
        proposal.setEventId("event123");
        proposal.setEventTitle("Career Fair");
        proposal.setOrganizerId("organizer123");
        proposal.setOrganizerName("Organizer");
        proposal.setStatus("approved");
        proposal.setAdminNote("Approved");
        proposal.setReadByAdmin(true);

        assertEquals("proposal123", proposal.getProposalId());
        assertEquals("Campus Cafe", proposal.getVendorName());
        assertEquals("Coffee and snacks", proposal.getDescription());
        assertEquals("5551234", proposal.getPhone());
        assertEquals("event123", proposal.getEventId());
        assertEquals("Career Fair", proposal.getEventTitle());
        assertEquals("organizer123", proposal.getOrganizerId());
        assertEquals("Organizer", proposal.getOrganizerName());
        assertEquals("approved", proposal.getStatus());
        assertEquals("Approved", proposal.getAdminNote());
        assertEquals(true, proposal.isReadByAdmin());
    }
}
