package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;

import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventProposal;

import org.junit.Test;

public class OrganizerContactFieldsTest {

    @Test
    public void eventStoresOrganizerContact() {
        Event event = new Event();
        event.setOrganizerName("Demo Organizer");
        event.setOrganizerEmail("organizer.demo@campus.test");

        assertEquals("Demo Organizer", event.getOrganizerName());
        assertEquals("organizer.demo@campus.test", event.getOrganizerEmail());
    }

    @Test
    public void eventProposalStoresOrganizerContact() {
        EventProposal proposal = new EventProposal();
        proposal.setOrganizerName("Demo Organizer");
        proposal.setOrganizerEmail("organizer.demo@campus.test");

        assertEquals("Demo Organizer", proposal.getOrganizerName());
        assertEquals("organizer.demo@campus.test", proposal.getOrganizerEmail());
    }
}
