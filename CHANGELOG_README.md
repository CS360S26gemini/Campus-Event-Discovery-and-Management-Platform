# Change Log

This file records implementation changes made from this point forward.

## 2026-05-04

- Updated checkout payment methods to remove Apple Pay and JazzCash.
- Combined separate credit/debit card choices into one Credit/Debit Card option.
- Added selected-state highlighting for checkout payment methods.
- Added organizer refund settings on event creation:
  - Allow attendee refunds toggle.
  - Refund penalty percentage input, constrained to 0-50%.
- Stored refund settings on event proposals and approved events.
- Applied organizer refund policy to attendee cancellation refunds.
- Added required attendee CNIC, country code, and phone number fields to checkout.
- Stored attendee identity/contact fields on RSVP and event attendee backend records.
- Added unit coverage for refund penalty calculations and attendee contact model fields.
- Added ticket-screen actions to add the event to calendar and view its campus location on the internal map.
- Rolled back the global button selected/pressed highlight override so button controls use their previous styling behavior again.
- Updated event share buttons to open the Android share sheet directly with only the shareable event link.
- Fixed the calendar event-scope segmented control so only the checked half is highlighted while admin-style selected buttons keep the same accent treatment.
- Reworded authentication errors so invalid passwords, invalid emails, duplicate emails, and account-update failures show clear user-facing messages, with password rules visible under password creation fields.
- Updated the calendar event-scope segmented control so its selected state uses the active accent instead of the default grey Material toggle fill.
- Added organizer names to event/proposal cards and event details, and exposed organizer name plus email on admin proposal review/detail views for pending, approved, and rejected proposals.
