# Personalised Recommendations Update

This document summarizes the personalised event recommendation and interest-selection work added to the Campus Event Discovery Android app.

## Goal

The app now supports deterministic personalised event recommendations for logged-in users. Recommendations are based only on existing app data:

- The logged-in user's saved interests from `User.interests`
- The categories of up to 5 recently viewed events
- Event RSVP count
- Event date proximity

No AI, ML APIs, external recommendation APIs, or new Gradle dependencies were added.

## Interest Buckets

The app uses the same fixed interest/category buckets as `arrays.xml`:

- Music
- Sports
- Career
- Academic
- Arts
- Business
- Food & Bev
- Social

`Education` is treated as an alias for `Academic`.

## Recommendation Scoring

Each active upcoming event receives a deterministic score.

Interest category weights:

| Category | Weight |
| --- | ---: |
| Sports | 10 |
| Music | 9 |
| Career | 8 |
| Business | 8 |
| Arts | 7 |
| Social | 7 |
| Food & Bev | 6 |
| Academic | 5 |
| Education | 5, normalized to Academic |

Scoring formula:

```text
score = 0

if event.category matches a selected user interest:
    score += category weight

if event.category matches a recently viewed event category:
    score += 6

score += min(5, event.rsvpCount / 10)

if event.date is within the next 7 days:
    score += 4
else if event.date is within the next 30 days:
    score += 2
```

Past events are excluded from recommendations.

Sorting:

1. Higher score first
2. Earlier event date first
3. Higher RSVP count second
4. Title alphabetical last, null-safe

If every active upcoming event scores `0`, the carousel falls back to trending events:

- Sort by RSVP count descending
- Return top 5
- Show the carousel as `Trending on Campus`

## Repository Changes

Updated:

- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`

Removed/retired the old tag-based recommendation behavior:

- `getPersonalisedEvents(List<String> interests, EventListCallback cb)`
- Old `whereArrayContainsAny("tags", ...)` recommendation query

Added:

- `RecommendationCallback`
- `getScoredRecommendations(List<String> interests, List<String> recentlyViewedIds, RecommendationCallback cb)`
- `scoreEvent(...)`
- `normalizeCategory(...)`
- `categoryWeight(...)`
- `isUpcoming(...)`
- `dateProximityScore(...)`
- Ranking helpers for deterministic sorting and fallback trending

Firestore behavior:

- Fetches active events from `Constants.COLLECTION_EVENTS`
- Excludes past events
- Fetches up to 5 recently viewed events using `whereIn(FieldPath.documentId(), ids)`
- Does not perform one read per candidate event

## Home Screen Changes

Updated:

- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeFragment.java`
- `app/src/main/res/layout/fragment_home.xml`

The old Popular Events carousel was replaced with a personalised recommendation carousel.

New UI fields:

- `tvRecommendedLabel`
- `tvRecommendedSubtitle`
- `recommendedEventsScroll`
- `recommendedEventsContainer`

Behavior:

- Hidden when no user is logged in
- Hidden when no recommendations or trending fallback events are returned
- Uses `Recommended for You` when personalised results exist
- Uses `Trending on Campus` when fallback trending is used
- Opens `EventDetailActivity` when a recommendation card is tapped
- Main home feed still loads via `getUpcomingEvents`
- Recommendation failure does not blank the main home screen

## Recently Viewed Constants

Updated:

- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/EventDetailActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/myevents/MyEventsFragment.java`

Recent-view SharedPreferences now use:

- `Constants.PREFS_RECENTLY_VIEWED`
- `Constants.PREFS_RECENTLY_VIEWED_KEY`

The existing behavior is preserved:

- Newest event first
- Deduplicated
- Maximum 5 event IDs

## Interest Chip Redesign

Updated layouts:

- `app/src/main/res/layout/activity_sign_up.xml`
- `app/src/main/res/layout/activity_account_settings.xml`

Updated Java:

- `app/src/main/java/com/example/CampusEventDiscovery/SignUpActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/profile/AccountSettingsActivity.java`

The freeform `MultiAutoCompleteTextView` interest input was replaced with fixed Material filter chips.

Chips added:

- `chipInterestMusic`
- `chipInterestSports`
- `chipInterestCareer`
- `chipInterestAcademic`
- `chipInterestArts`
- `chipInterestBusiness`
- `chipInterestFoodBev`
- `chipInterestSocial`

Validation:

- Users must select at least 3 interests during signup
- Users must select at least 3 interests before saving account settings
- Freeform interests are no longer allowed
- Stored interests use the displayed bucket strings

## Strings Added

Updated:

- `app/src/main/res/values/strings.xml`

Added strings:

- `recommended_for_you`
- `trending_on_campus`
- `based_on_interest`
- `popular_events_across_campus`
- `select_at_least_three_interests`
- `interests_chip_helper`
- `social`

## Validation Helper

Updated:

- `app/src/main/java/com/example/CampusEventDiscovery/util/SignupValidator.java`

Added:

- `hasMinimumSelectedInterests(List<String> interests)`

This helper keeps interest-count validation testable outside UI-only tests.

## Tests Added

Added:

- `app/src/test/java/com/example/CampusEventDiscovery/repository/EventRepositoryPersonalisationTest.java`

Updated:

- `app/src/test/java/com/example/CampusEventDiscovery/SignupValidatorTest.java`

Test coverage includes:

- Sports interest + Sports event scoring
- Academic scoring lower than Sports
- Interest match and recently viewed category match combining
- RSVP popularity scoring and cap
- Date proximity scoring for 7 days and 30 days
- Past event exclusion
- Null category/date safety
- Trending fallback sorting
- Top 5 recommendation limit
- `Education` normalization to `Academic`
- Interest validation for fewer than 3 and exactly 3 selected interests

## Build Notes

Run these from the project root:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

If Gradle fails because `JAVA_HOME` points to an invalid Android Studio JBR path, use the installed JBR path:

```powershell
$env:JAVA_HOME='D:\Program Files\Android Studio\jbr'
```

If Gradle tries to use a non-writable sandbox Gradle cache, point it to the normal user cache:

```powershell
$env:GRADLE_USER_HOME='C:\Users\hbaqa\.gradle'
```

## Assumptions

- Event categories stored in Firestore match the app buckets or can be normalized with the `Education -> Academic` alias.
- `Event.date` is the source of truth for deciding upcoming versus past events.
- `Event.rsvpCount` may be missing or zero, and zero is treated safely.
- Recently viewed IDs are stored as a comma-separated list in SharedPreferences.
- Search personalization was left out because the requested core Home recommendations and interest data cleanup were the priority.
