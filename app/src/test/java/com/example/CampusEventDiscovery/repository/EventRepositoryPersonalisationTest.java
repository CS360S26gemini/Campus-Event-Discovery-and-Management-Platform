package com.example.CampusEventDiscovery.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.CampusEventDiscovery.model.Event;
import com.google.firebase.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30})
public class EventRepositoryPersonalisationTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = 24L * 60L * 60L * 1000L;

    @Test
    public void sportsInterestAndSportsEvent_scoresTenWithoutOtherBoosts() {
        Event event = event("1", "Match", "Sports", 0, 45);

        int score = EventRepository.scoreEvent(event, Collections.singletonList("Sports"), Collections.emptySet(), NOW);

        assertEquals(10, score);
    }

    @Test
    public void academicInterestScoresLowerThanSports() {
        Event sports = event("1", "Sports", "Sports", 0, 45);
        Event academic = event("2", "Lecture", "Academic", 0, 45);

        int sportsScore = EventRepository.scoreEvent(sports, Collections.singletonList("Sports"), Collections.emptySet(), NOW);
        int academicScore = EventRepository.scoreEvent(academic, Collections.singletonList("Academic"), Collections.emptySet(), NOW);

        assertTrue(academicScore < sportsScore);
    }

    @Test
    public void interestAndRecentlyViewedCategoryCombine() {
        Event event = event("1", "Concert", "Music", 0, 45);

        int score = EventRepository.scoreEvent(
                event,
                Collections.singletonList("Music"),
                new HashSet<>(Collections.singletonList("Music")),
                NOW
        );

        assertEquals(15, score);
    }

    @Test
    public void popularityAddsOnePerTenRsvpsAndCapsAtFive() {
        Event small = event("1", "Small", "Unknown", 19, 45);
        Event packed = event("2", "Packed", "Unknown", 99, 45);

        assertEquals(1, EventRepository.scoreEvent(small, Collections.emptyList(), Collections.emptySet(), NOW));
        assertEquals(5, EventRepository.scoreEvent(packed, Collections.emptyList(), Collections.emptySet(), NOW));
    }

    @Test
    public void eventWithinSevenDaysGetsFourDatePoints() {
        Event event = event("1", "Soon", "Unknown", 0, 3);

        assertEquals(4, EventRepository.dateProximityScore(event, NOW));
    }

    @Test
    public void eventWithinThirtyDaysGetsTwoDatePoints() {
        Event event = event("1", "This Month", "Unknown", 0, 20);

        assertEquals(2, EventRepository.dateProximityScore(event, NOW));
    }

    @Test
    public void pastEventGetsNoDatePointsAndIsExcluded() {
        Event past = event("1", "Past", "Sports", 0, -1);
        Event future = event("2", "Future", "Music", 0, 45);

        assertEquals(0, EventRepository.dateProximityScore(past, NOW));
        List<Event> ranked = EventRepository.rankRecommendationsForTesting(
                Arrays.asList(past, future),
                Collections.emptyList(),
                Collections.emptySet(),
                NOW
        );

        assertEquals(1, ranked.size());
        assertEquals("2", ranked.get(0).getEventId());
    }

    @Test
    public void nullCategoryOrNullDateDoesNotCrash() {
        Event event = new Event();
        event.setEventId("1");
        event.setTitle("Incomplete");

        assertEquals(0, EventRepository.scoreEvent(event, Collections.singletonList("Sports"), Collections.emptySet(), NOW));
        assertFalse(EventRepository.isUpcoming(event, NOW));
    }

    @Test
    public void fallbackTrendingSortsByRsvpDescendingWhenAllScoresAreZero() {
        Event first = event("1", "One", "Unknown", 3, 45);
        Event second = event("2", "Two", "Unknown", 9, 45);
        Event third = event("3", "Three", "Unknown", 1, 45);

        List<Event> ranked = EventRepository.rankRecommendationsForTesting(
                Arrays.asList(first, second, third),
                Collections.emptyList(),
                Collections.emptySet(),
                NOW
        );

        assertTrue(EventRepository.usesTrendingFallbackForTesting(
                Arrays.asList(first, second, third),
                Collections.emptyList(),
                Collections.emptySet(),
                NOW
        ));
        assertEquals("2", ranked.get(0).getEventId());
        assertEquals("1", ranked.get(1).getEventId());
        assertEquals("3", ranked.get(2).getEventId());
    }

    @Test
    public void recommendationsReturnTopFiveOnly() {
        List<Event> ranked = EventRepository.rankRecommendationsForTesting(
                Arrays.asList(
                        event("1", "One", "Unknown", 9, 45),
                        event("2", "Two", "Unknown", 8, 45),
                        event("3", "Three", "Unknown", 7, 45),
                        event("4", "Four", "Unknown", 6, 45),
                        event("5", "Five", "Unknown", 5, 45),
                        event("6", "Six", "Unknown", 4, 45)
                ),
                Collections.emptyList(),
                Collections.emptySet(),
                NOW
        );

        assertEquals(5, ranked.size());
    }

    @Test
    public void educationNormalizesToAcademic() {
        assertEquals("Academic", EventRepository.normalizeCategory("Education"));
        assertEquals(5, EventRepository.categoryWeight("Education"));
    }

    private Event event(String id, String title, String category, long rsvps, int daysFromNow) {
        Event event = new Event();
        event.setEventId(id);
        event.setTitle(title);
        event.setCategory(category);
        event.setRsvpCount(rsvps);
        event.setDate(new Timestamp(new Date(NOW + daysFromNow * DAY)));
        return event;
    }
}
