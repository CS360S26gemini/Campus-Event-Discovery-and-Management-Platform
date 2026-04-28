package com.example.CampusEventDiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.model.Payment;
import com.example.CampusEventDiscovery.model.Rsvp;
import com.example.CampusEventDiscovery.model.TicketTier;

import org.junit.Test;

public class TicketTierFieldsTest {

    @Test
    public void rsvpStoresTierSnapshotAndAmount() {
        Rsvp rsvp = new Rsvp();
        rsvp.setTierId("vip");
        rsvp.setTierName("VIP");
        rsvp.setTierPrice(4500.0);
        rsvp.setAmount(4500.0);

        assertEquals("vip", rsvp.getTierId());
        assertEquals("VIP", rsvp.getTierName());
        assertEquals(4500.0, rsvp.getTierPrice(), 0.001);
        assertEquals(4500.0, rsvp.getAmount(), 0.001);
    }

    @Test
    public void paymentStoresTierMetadata() {
        Payment payment = new Payment();
        payment.setTierId("early");
        payment.setTierName("Early Bird");

        assertEquals("early", payment.getTierId());
        assertEquals("Early Bird", payment.getTierName());
    }

    @Test
    public void ticketTierComputesRemainingCapacityAndSoldOut() {
        TicketTier tier = new TicketTier("general", "General", 2500.0, 100, 99, "");
        assertEquals(1L, tier.getRemainingCapacity());
        assertFalse(tier.isSoldOut());

        tier.setRsvpCount(100);
        assertEquals(0L, tier.getRemainingCapacity());
        assertTrue(tier.isSoldOut());
    }
}
