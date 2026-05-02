package com.example.CampusEventDiscovery.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventAttendee;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.model.Notification;
import com.example.CampusEventDiscovery.model.VendorProposal;
import com.example.CampusEventDiscovery.ui.event.CheckoutActivity;
import com.example.CampusEventDiscovery.ui.event.EventFeedbackActivity;
import com.example.CampusEventDiscovery.ui.event.EventApprovalActivity;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.ui.event.TicketActivity;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.OrganizerEventDetailActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.organizer.WhoIsComingActivity;
import com.example.CampusEventDiscovery.ui.profile.AccountSettingsActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoryAlbumActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
import com.example.CampusEventDiscovery.ui.profile.NotificationCenterActivity;
import com.example.CampusEventDiscovery.ui.sos.SOSDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class WalkthroughManager {

    public static final String EXTRA_WALKTHROUGH_MODE = "walkthrough_mode";

    private static final List<Step> steps = new ArrayList<>();
    private static int index = -1;
    private static String guideId = "";
    private static WalkthroughOverlay overlay;
    private static Event demoEvent;
    private static EventProposal demoProposal;

    private WalkthroughManager() {
    }

    public static boolean isActive() {
        return index >= 0 && index < steps.size();
    }

    public static boolean isWalkthroughIntent(Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_WALKTHROUGH_MODE, false);
    }

    public static Event getDemoEvent() {
        if (demoEvent == null) {
            demoEvent = new Event();
            demoEvent.setEventId("walkthrough_event");
            demoEvent.setTitle("Demo Music Night");
            demoEvent.setDescription("A local walkthrough-only event used to show the app flow safely.");
            demoEvent.setCategory("Music");
            demoEvent.setLocation("Main Auditorium");
            demoEvent.setCapacity(100);
            demoEvent.setRsvpCount(24);
            demoEvent.setTicketPrice(500.0);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            demoEvent.setDate(new Timestamp(calendar.getTime()));
            demoEvent.setStatus("active");
        }
        return demoEvent;
    }

    public static EventProposal getDemoProposal() {
        if (demoProposal == null) {
            demoProposal = new EventProposal();
            demoProposal.setProposalId("walkthrough_proposal");
            demoProposal.setTitle("Demo Cultural Night");
            demoProposal.setDescription("A walkthrough-only proposal for admin review training.");
            demoProposal.setLocation("Student Center");
            demoProposal.setCapacity(150);
            demoProposal.setTicketPrice(0.0);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 14);
            demoProposal.setDate(new Timestamp(calendar.getTime()));
            demoProposal.setStatus("pending");
        }
        return demoProposal;
    }

    public static List<Event> getDemoEvents() {
        List<Event> events = new ArrayList<>();
        events.add(getDemoEvent());
        return events;
    }

    public static List<EventAttendee> getDemoAttendees() {
        List<EventAttendee> attendees = new ArrayList<>();
        EventAttendee checkedIn = new EventAttendee();
        checkedIn.setUserId("walkthrough_attendee_checked_in");
        checkedIn.setFullName("Demo Checked-In Attendee");
        checkedIn.setQrToken("DEMO-CHECKED-IN");
        checkedIn.setCheckedIn(true);
        checkedIn.setCheckedInAt(Timestamp.now());
        attendees.add(checkedIn);

        EventAttendee waiting = new EventAttendee();
        waiting.setUserId("walkthrough_attendee_waiting");
        waiting.setFullName("Demo Waiting Attendee");
        waiting.setQrToken("DEMO-WAITING");
        waiting.setCheckedIn(false);
        attendees.add(waiting);
        return attendees;
    }

    public static List<VendorProposal> getDemoVendorProposals() {
        List<VendorProposal> proposals = new ArrayList<>();
        VendorProposal pending = new VendorProposal();
        pending.setProposalId("walkthrough_vendor_pending");
        pending.setVendorName("Demo Campus Cafe");
        pending.setDescription("A walkthrough-only catering request for the demo event.");
        pending.setPhone("+92 300 0000000");
        pending.setEventId(getDemoEvent().getEventId());
        pending.setEventTitle(getDemoEvent().getTitle());
        pending.setOrganizerId("walkthrough_organizer");
        pending.setOrganizerName("Demo Organizer");
        pending.setStatus("pending");
        pending.setCreatedAt(Timestamp.now());
        proposals.add(pending);

        VendorProposal approved = new VendorProposal();
        approved.setProposalId("walkthrough_vendor_approved");
        approved.setVendorName("Demo Sound Booth");
        approved.setDescription("Approved walkthrough vendor for lighting and sound.");
        approved.setPhone("+92 300 1111111");
        approved.setEventId(getDemoEvent().getEventId());
        approved.setEventTitle(getDemoEvent().getTitle());
        approved.setOrganizerId("walkthrough_organizer");
        approved.setOrganizerName("Demo Organizer");
        approved.setStatus("approved");
        approved.setCreatedAt(Timestamp.now());
        proposals.add(approved);
        return proposals;
    }

    public static List<Notification> getDemoNotifications() {
        List<Notification> notifications = new ArrayList<>();
        Notification approval = new Notification(
                "Demo event approved",
                "Your walkthrough event has been approved.",
                "event",
                getDemoEvent().getEventId()
        );
        approval.setNotificationId("walkthrough_notification_approval");
        notifications.add(approval);

        Notification sos = new Notification(
                "Demo SOS update",
                "A walkthrough SOS alert has been resolved.",
                "sos",
                getDemoEvent().getEventId()
        );
        sos.setNotificationId("walkthrough_notification_sos");
        sos.setRead(true);
        notifications.add(sos);
        return notifications;
    }

    public static void start(Activity activity, String id) {
        guideId = id == null ? "" : id;
        steps.clear();
        steps.addAll(buildSteps(guideId));
        index = steps.isEmpty() ? -1 : 0;
        if (steps.isEmpty()) {
            Toast.makeText(activity, "No walkthrough available.", Toast.LENGTH_SHORT).show();
            return;
        }
        navigate(activity);
    }

    public static void cancel(Activity activity) {
        removeOverlay(activity);
        steps.clear();
        index = -1;
        guideId = "";
    }

    public static void maybeShow(Activity activity, View root, String screen) {
        if (!isActive() || activity == null || root == null) {
            return;
        }
        Step step = steps.get(index);
        if (!step.screen.equals(screen)) {
            return;
        }

        root.postDelayed(() -> {
            if (!isActive() || activity.isFinishing() || activity.isDestroyed()) return;
            View target = root.findViewById(step.targetId);
            if (target == null) {
                target = root;
            }
            final View resolvedTarget = resolveTargetView(target);
            long revealDelayMs = shouldAutoScrollBeforeOverlay(step.screen, step.targetId)
                    && requestRevealTarget(resolvedTarget) ? 260L : 0L;
            resolvedTarget.postDelayed(() -> {
                if (!isActive() || activity.isFinishing() || activity.isDestroyed()) return;
                showOverlay(activity, resolvedTarget, step);
            }, revealDelayMs);
        }, 220L);
    }

    private static void next(Activity activity) {
        if (index >= steps.size() - 1) {
            complete(activity);
            return;
        }
        index++;
        navigate(activity);
    }

    private static void complete(Activity activity) {
        cancel(activity);
        Intent intent = createCompletionIntent(activity);
        activity.startActivity(intent);
        if (!(activity instanceof MainActivity)) {
            activity.finish();
        }
    }

    static Intent createCompletionIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_HELP", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private static void back(Activity activity) {
        if (index <= 0) {
            return;
        }
        index--;
        navigate(activity);
    }

    private static void navigate(Activity activity) {
        removeOverlay(activity);
        if (!isActive()) return;
        Step step = steps.get(index);

        if (isMainScreen(step.screen)) {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra(EXTRA_WALKTHROUGH_MODE, true);
            intent.putExtra("WALKTHROUGH_SCREEN", step.screen);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            return;
        }

        Intent intent = null;
        if ("event_detail".equals(step.screen)) {
            intent = new Intent(activity, EventDetailActivity.class);
            intent.putExtra("eventId", getDemoEvent().getEventId());
        } else if ("checkout".equals(step.screen)) {
            intent = new Intent(activity, CheckoutActivity.class);
            intent.putExtra("eventId", getDemoEvent().getEventId());
            intent.putExtra("eventTitle", getDemoEvent().getTitle());
            intent.putExtra("totalPrice", getDemoEvent().getTicketPrice());
            intent.putExtra("eventDateMillis", getDemoEvent().getDate().toDate().getTime());
            intent.putExtra("eventVenue", getDemoEvent().getLocation());
        } else if ("ticket".equals(step.screen)) {
            intent = new Intent(activity, TicketActivity.class);
            intent.putExtra("eventName", getDemoEvent().getTitle());
            intent.putExtra("eventDate", "Demo date");
            intent.putExtra("transactionId", "demo_txn");
            intent.putExtra("qrPayload", "{\"demo\":true}");
        } else if ("memories".equals(step.screen)) {
            intent = new Intent(activity, MemoriesActivity.class);
        } else if ("memory_album".equals(step.screen)) {
            intent = new Intent(activity, MemoryAlbumActivity.class);
            intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_ID, getDemoEvent().getEventId());
            intent.putExtra(MemoryAlbumActivity.EXTRA_EVENT_TITLE, getDemoEvent().getTitle());
            intent.putStringArrayListExtra(MemoryAlbumActivity.EXTRA_PHOTO_URLS, new ArrayList<>());
        } else if ("notifications".equals(step.screen)) {
            intent = new Intent(activity, NotificationCenterActivity.class);
        } else if ("account_settings".equals(step.screen)) {
            intent = new Intent(activity, AccountSettingsActivity.class);
        } else if ("event_feedback".equals(step.screen)) {
            intent = new Intent(activity, EventFeedbackActivity.class);
            intent.putExtra("eventId", getDemoEvent().getEventId());
            intent.putExtra("eventTitle", getDemoEvent().getTitle());
        } else if ("create_event".equals(step.screen)) {
            intent = new Intent(activity, CreateEventActivity.class);
        } else if ("manage_events".equals(step.screen)) {
            intent = new Intent(activity, ManageEventsActivity.class);
        } else if ("organizer_event_detail".equals(step.screen)) {
            intent = new Intent(activity, OrganizerEventDetailActivity.class);
            intent.putExtra("eventId", getDemoEvent().getEventId());
        } else if ("who_is_coming".equals(step.screen)) {
            intent = new Intent(activity, WhoIsComingActivity.class);
            intent.putExtra("eventId", getDemoEvent().getEventId());
            intent.putExtra("eventTitle", getDemoEvent().getTitle());
        } else if ("scanner".equals(step.screen)) {
            intent = new Intent(activity, ScannerActivity.class);
        } else if ("approval".equals(step.screen)) {
            intent = new Intent(activity, EventApprovalActivity.class);
            intent.putExtra("proposalId", getDemoProposal().getProposalId());
        } else if ("sos_dashboard".equals(step.screen)) {
            intent = new Intent(activity, SOSDashboardActivity.class);
        }

        if (intent != null) {
            intent.putExtra(EXTRA_WALKTHROUGH_MODE, true);
            activity.startActivity(intent);
        }
    }

    private static boolean isMainScreen(String screen) {
        return "home_attendee".equals(screen)
                || "search".equals(screen)
                || "my_events".equals(screen)
                || "favourites".equals(screen)
                || "profile".equals(screen)
                || "calendar".equals(screen)
                || "vendors".equals(screen)
                || "home_organizer".equals(screen)
                || "home_admin".equals(screen);
    }

    private static void showOverlay(Activity activity, View target, Step step) {
        removeOverlay(activity);
        FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
        overlay = new WalkthroughOverlay(activity);
        overlay.bind(resolveTargetView(target), step, index, steps.size());
        decor.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private static View resolveTargetView(View target) {
        if (target instanceof androidx.recyclerview.widget.RecyclerView) {
            androidx.recyclerview.widget.RecyclerView recyclerView =
                    (androidx.recyclerview.widget.RecyclerView) target;
            if (recyclerView.getChildCount() > 0) {
                return recyclerView.getChildAt(0);
            }
        }
        return target;
    }

    private static void removeOverlay(@Nullable Activity activity) {
        if (overlay != null && overlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) overlay.getParent()).removeView(overlay);
        }
        overlay = null;
    }

    private static List<Step> buildSteps(String id) {
        List<Step> list = new ArrayList<>();
        if (id == null || id.isEmpty()) return list;
        switch (id) {
            case "attendee_register":
                list.add(new Step("search", R.id.etSearch, "Search for an event", "Use the real Search screen to find events by keyword."));
                list.add(new Step("search", R.id.rvResults, "Open an event card", "Event results appear here. In walkthrough mode, this uses local demo data."));
                list.add(new Step("event_detail", R.id.btnTickets, "Start registration", "This is the real event detail screen. Get tickets begins RSVP."));
                list.add(new Step("checkout", R.id.btnPay, "Confirm safely", "Checkout is shown with demo event data. This button is highlighted but not charged."));
                break;
            case "attendee_search":
                list.add(new Step("search", R.id.etSearch, "Search field", "Type an event, topic, or venue here."));
                list.add(new Step("search", R.id.chipMusic, "Category filters", "Use category chips to narrow results."));
                list.add(new Step("search", R.id.tvSortBy, "Sort control", "Sort results by relevance, date, or price."));
                list.add(new Step("search", R.id.rvResults, "Result cards", "Tap a result to inspect event details."));
                break;
            case "attendee_event_detail":
                list.add(new Step("event_detail", R.id.tvTitle, "Event overview", "Use the detail page to confirm the title, venue, date, and organizer before registering."));
                list.add(new Step("event_detail", R.id.tvViewOnMap, "Directions", "Open the campus map route for the selected venue."));
                list.add(new Step("event_detail", R.id.btnShare, "Share event", "Share the event with classmates from this action."));
                list.add(new Step("event_detail", R.id.btnTickets, "Ticket options", "Start ticket selection or free RSVP from here."));
                break;
            case "attendee_ticket":
                list.add(new Step("my_events", R.id.rvSection1, "Registered events", "Your RSVP events appear on the real My Events screen."));
                list.add(new Step("ticket", R.id.ivTicketQrCode, "Ticket QR", "This QR is what attendees show at check-in."));
                break;
            case "attendee_favourites":
                list.add(new Step("favourites", R.id.tvFavouritesBadge, "Saved count", "This shows how many events you have saved."));
                list.add(new Step("favourites", R.id.rvFavourites, "Saved events", "Saved event cards appear here so you can revisit them quickly."));
                break;
            case "attendee_calendar":
                list.add(new Step("calendar", R.id.tvMonthLabel, "Calendar month", "Browse campus events by date."));
                list.add(new Step("calendar", R.id.toggleCalendarEventScope, "Event scope", "Switch between all events and your registered events."));
                list.add(new Step("calendar", R.id.rvCalendarEvents, "Daily events", "Events for the selected date appear here. Long-press can add them to your device calendar."));
                break;
            case "attendee_feedback":
                list.add(new Step("event_feedback", R.id.ratingBarFeedback, "Rate event", "Give attended events a star rating."));
                list.add(new Step("event_feedback", R.id.etFeedbackReview, "Write review", "Add optional feedback for organizers."));
                list.add(new Step("event_feedback", R.id.btnSelectPhotos, "Attach photos", "Add photos that become memories for the event."));
                list.add(new Step("event_feedback", R.id.btnSubmitFeedback, "Submit feedback", "Walkthrough mode highlights this button but does not save anything."));
                break;
            case "attendee_memories":
                list.add(new Step("memories", R.id.rvMemories, "Memory folders", "Attended events appear here as memory folders."));
                list.add(new Step("memory_album", R.id.tvEmptyMemoryAlbum, "Memory album", "Open a memory folder to review photos. Long-press photos can be removed in normal use."));
                break;
            case "attendee_sos":
                list.add(new Step("home_attendee", R.id.btnSos, "SOS button", "This is where attendees request urgent help. Walkthrough mode never sends an alert."));
                break;
            case "profile_tools":
                list.add(new Step("profile", R.id.ivProfile, "Profile photo or avatar", "Update your profile visual from here."));
                list.add(new Step("profile", R.id.cardDarkMode, "Theme mode", "Switch between light and dark mode."));
                list.add(new Step("profile", R.id.cardAccentColor, "Accent color", "Customize the app accent color."));
                list.add(new Step("profile", R.id.btn_help, "Help walkthroughs", "Return here any time to replay role-specific guides."));
                break;
            case "profile_notifications":
                list.add(new Step("profile", R.id.rowNotifications, "Notifications", "Open event, approval, payment, and SOS updates from your profile."));
                list.add(new Step("notifications", R.id.rvNotifications, "Notification list", "Unread and read notifications appear here."));
                break;
            case "profile_settings":
                list.add(new Step("profile", R.id.rowAccountSettings, "Account settings", "Open editable account details from the profile screen."));
                list.add(new Step("account_settings", R.id.etFullName, "Profile details", "Update display name and profile details here."));
                list.add(new Step("account_settings", R.id.chipGroupInterests, "Interests", "Choose interests that personalize event discovery."));
                list.add(new Step("account_settings", R.id.btnChangePassword, "Security", "Use these controls to change email or password."));
                break;
            case "organizer_create":
                list.add(new Step("home_organizer", R.id.btnCreateEvent, "Create event", "Organizers start proposals from this real dashboard button."));
                list.add(new Step("create_event", R.id.etEventTitle, "Event title", "Fill the real proposal form fields."));
                list.add(new Step("create_event", R.id.btnPickDate, "Date and time", "Choose when the event will happen."));
                list.add(new Step("create_event", R.id.btnSubmitEvent, "Submit proposal", "Walkthrough mode highlights this but does not submit."));
                break;
            case "organizer_manage":
                list.add(new Step("home_organizer", R.id.btnManageEvents, "Manage events", "Open the real organizer management screen."));
                list.add(new Step("manage_events", R.id.rvSection1, "Approved events", "Approved events and attendee tools live here."));
                list.add(new Step("manage_events", R.id.rvSection2, "Pending proposals", "Pending proposals are grouped in this section."));
                break;
            case "organizer_event_tools":
                list.add(new Step("organizer_event_detail", R.id.tvRegCount, "Registration progress", "Track RSVP and check-in progress for the selected event."));
                list.add(new Step("organizer_event_detail", R.id.btnWhoIsComing, "Attendee list", "Open attendees, search participants, and check people in."));
                list.add(new Step("organizer_event_detail", R.id.btnAnnouncement, "Announcements", "Send event updates to registered attendees."));
                list.add(new Step("organizer_event_detail", R.id.btnPayments, "Payments", "Review payment confirmations for this event."));
                list.add(new Step("organizer_event_detail", R.id.btnBlacklisted, "Blacklisted attendees", "Review attendees blocked from the event."));
                break;
            case "organizer_attendees":
                list.add(new Step("who_is_coming", R.id.etSearchParticipants, "Search attendees", "Find participants by name."));
                list.add(new Step("who_is_coming", R.id.rvParticipants, "Participant list", "Registered attendees and check-in status appear here."));
                list.add(new Step("who_is_coming", R.id.btnScanQr, "QR scan", "Use scanner check-in for attendee tickets."));
                list.add(new Step("who_is_coming", R.id.btnCheckIn, "Manual check-in", "Enter a QR token manually if the camera is unavailable."));
                list.add(new Step("who_is_coming", R.id.btnBlacklist, "Blacklist selected", "Select attendees, then blacklist them when needed."));
                break;
            case "organizer_scan":
                list.add(new Step("home_organizer", R.id.btnScanTickets, "Scan tickets", "Open scanner tools from the organizer dashboard."));
                list.add(new Step("scanner", R.id.btnStartScanner, "Start scanner", "Use this to scan attendee QR codes."));
                break;
            case "organizer_vendors":
                list.add(new Step("vendors", R.id.cardVendorToggle, "Vendor status", "Review approved, pending, and rejected vendor requests for the selected event."));
                list.add(new Step("vendors", R.id.rvVendorProposals, "Vendor proposals", "Vendor requests for the selected event appear here."));
                list.add(new Step("vendors", R.id.fabAddVendor, "Add vendor", "Submit a vendor request for admin approval."));
                break;
            case "organizer_calendar":
                list.add(new Step("calendar", R.id.tvMonthLabel, "Organizer calendar", "See approved events by month."));
                list.add(new Step("calendar", R.id.toggleCalendarEventScope, "Approved filter", "Switch between all events and your approved events."));
                list.add(new Step("calendar", R.id.rvCalendarEvents, "Event list", "Events for the selected date appear here."));
                break;
            case "organizer_sos":
                list.add(new Step("home_organizer", R.id.btnSosDashboard, "SOS dashboard", "Organizers monitor SOS reports for their events here."));
                list.add(new Step("sos_dashboard", R.id.rvSosAlerts, "Active alerts", "Active reports appear in this real list."));
                break;
            case "admin_review":
                list.add(new Step("home_admin", R.id.btnPendingApprovals, "Pending approvals", "Admins review pending proposals from this real dashboard."));
                list.add(new Step("home_admin", R.id.rvAdminApprovals, "Proposal queue", "Proposal cards open the approval screen."));
                list.add(new Step("approval", R.id.btnApprove, "Approve", "Approve publishes in normal use. Walkthrough mode blocks the action."));
                list.add(new Step("approval", R.id.btnReject, "Reject", "Reject requires a note in normal use. Walkthrough mode blocks the action."));
                break;
            case "admin_dashboard":
                list.add(new Step("home_admin", R.id.btnPendingApprovals, "Pending filter", "Use this real toggle for pending proposals."));
                list.add(new Step("home_admin", R.id.btnRejectedEvents, "Rejected filter", "Use this real toggle for rejected proposals."));
                list.add(new Step("home_admin", R.id.rvAdminApprovals, "Approval list", "The admin review list appears here."));
                break;
            case "admin_events":
                list.add(new Step("manage_events", R.id.rvSection1, "Active event oversight", "Admins can review active events across organizers."));
                list.add(new Step("organizer_event_detail", R.id.btnDeleteEvent, "Cancel event", "Admins can cancel events when policy or safety requires it."));
                list.add(new Step("organizer_event_detail", R.id.btnWhoIsComing, "Attendee tools", "Admins can inspect attendee status when supporting organizers."));
                break;
            case "admin_vendors":
                list.add(new Step("vendors", R.id.cardVendorToggle, "Vendor review filters", "Switch between pending, approved, and rejected vendor requests."));
                list.add(new Step("vendors", R.id.rvVendorProposals, "Vendor requests", "Vendor proposal cards appear here for admin review."));
                break;
            case "admin_sos":
                list.add(new Step("home_admin", R.id.btnSosDashboard, "SOS dashboard", "Admins can open campus-wide SOS monitoring here."));
                list.add(new Step("sos_dashboard", R.id.rvSosAlerts, "SOS list", "Active SOS alerts appear in this real dashboard list."));
                break;
            default:
                break;
        }
        return list;
    }

    static List<String> getGuideIds() {
        List<String> guideIds = new ArrayList<>();
        guideIds.add("attendee_register");
        guideIds.add("attendee_search");
        guideIds.add("attendee_event_detail");
        guideIds.add("attendee_ticket");
        guideIds.add("attendee_favourites");
        guideIds.add("attendee_calendar");
        guideIds.add("attendee_feedback");
        guideIds.add("attendee_memories");
        guideIds.add("attendee_sos");
        guideIds.add("profile_tools");
        guideIds.add("profile_notifications");
        guideIds.add("profile_settings");
        guideIds.add("organizer_create");
        guideIds.add("organizer_manage");
        guideIds.add("organizer_event_tools");
        guideIds.add("organizer_attendees");
        guideIds.add("organizer_scan");
        guideIds.add("organizer_vendors");
        guideIds.add("organizer_calendar");
        guideIds.add("organizer_sos");
        guideIds.add("admin_review");
        guideIds.add("admin_dashboard");
        guideIds.add("admin_events");
        guideIds.add("admin_vendors");
        guideIds.add("admin_sos");
        return guideIds;
    }

    static List<StepInfo> getStepInfoForGuide(String id) {
        List<StepInfo> info = new ArrayList<>();
        for (Step step : buildSteps(id)) {
            info.add(new StepInfo(step.screen, step.targetId, step.title, step.body));
        }
        return info;
    }

    static boolean canRouteScreen(String screen) {
        return isMainScreen(screen)
                || "event_detail".equals(screen)
                || "checkout".equals(screen)
                || "ticket".equals(screen)
                || "memories".equals(screen)
                || "memory_album".equals(screen)
                || "notifications".equals(screen)
                || "account_settings".equals(screen)
                || "event_feedback".equals(screen)
                || "create_event".equals(screen)
                || "manage_events".equals(screen)
                || "organizer_event_detail".equals(screen)
                || "who_is_coming".equals(screen)
                || "scanner".equals(screen)
                || "approval".equals(screen)
                || "sos_dashboard".equals(screen);
    }

    static boolean shouldShowBackAction(int position) {
        return position >= 0;
    }

    static String resolveBackButtonLabel(Context context, int position) {
        return position <= 0
                ? context.getString(R.string.walkthrough_exit)
                : context.getString(R.string.walkthrough_back);
    }

    static boolean shouldAutoScrollBeforeOverlay(String screen, int targetId) {
        return targetId != 0;
    }

    private static boolean requestRevealTarget(View target) {
        if (target == null || target.getWidth() <= 0 || target.getHeight() <= 0) {
            return false;
        }
        int verticalPadding = Math.round(96 * target.getResources().getDisplayMetrics().density);
        Rect rect = new Rect(0, -verticalPadding, target.getWidth(), target.getHeight() + verticalPadding);
        return target.requestRectangleOnScreen(rect, true);
    }

    private static final class Step {
        final String screen;
        final int targetId;
        final String title;
        final String body;

        Step(String screen, int targetId, String title, String body) {
            this.screen = screen;
            this.targetId = targetId;
            this.title = title;
            this.body = body;
        }
    }

    static final class StepInfo {
        final String screen;
        final int targetId;
        final String title;
        final String body;

        StepInfo(String screen, int targetId, String title, String body) {
            this.screen = screen;
            this.targetId = targetId;
            this.title = title;
            this.body = body;
        }
    }

    private static final class WalkthroughOverlay extends FrameLayout {
        private final android.graphics.Paint dimPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint strokePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final RectF targetRect = new RectF();

        WalkthroughOverlay(Activity activity) {
            super(activity);
            setWillNotDraw(false);
            setClickable(true);
            dimPaint.setColor(Color.argb(180, 0, 0, 0));
            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(3));
            strokePaint.setColor(ThemeManager.getAccentColor(activity));
        }

        void bind(View target, Step step, int position, int total) {
            MaterialCardView panel = new MaterialCardView(getContext());
            panel.setCardBackgroundColor(resolveAttr(com.google.android.material.R.attr.colorSurface));
            panel.setRadius(dp(8));
            panel.setCardElevation(dp(8));

            LinearLayout content = new LinearLayout(getContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(16), dp(14), dp(16), dp(12));
            TextView counter = text((position + 1) + " / " + total, 12, resolveAttr(com.google.android.material.R.attr.colorOnSurfaceVariant), false);
            TextView title = text(step.title, 18, resolveAttr(com.google.android.material.R.attr.colorOnSurface), true);
            TextView body = text(step.body, 14, resolveAttr(com.google.android.material.R.attr.colorOnSurfaceVariant), false);

            LinearLayout actions = new LinearLayout(getContext());
            actions.setGravity(Gravity.END);
            MaterialButton exit = actionButton(getContext().getString(R.string.walkthrough_exit), false);
            MaterialButton back = actionButton(resolveBackButtonLabel(getContext(), position), false);
            MaterialButton next = actionButton(position == total - 1 ? getContext().getString(R.string.walkthrough_done) : getContext().getString(R.string.walkthrough_next), true);
            exit.setOnClickListener(v -> cancel((Activity) getContext()));
            back.setVisibility(shouldShowBackAction(position) ? VISIBLE : GONE);
            back.setOnClickListener(v -> {
                if (position == 0) {
                    cancel((Activity) getContext());
                } else {
                    back((Activity) getContext());
                }
            });
            next.setOnClickListener(v -> next((Activity) getContext()));
            actions.addView(exit);
            actions.addView(back);
            actions.addView(next);

            content.addView(counter);
            content.addView(title);
            content.addView(body);
            content.addView(actions);
            ScrollView scrollView = new ScrollView(getContext());
            scrollView.setFillViewport(false);
            scrollView.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
            scrollView.addView(content, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            panel.addView(scrollView);
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            params.setMargins(dp(16), dp(16), dp(16), dp(16));
            addView(panel, params);

            post(() -> {
                int[] rootLoc = new int[2];
                int[] viewLoc = new int[2];
                getLocationOnScreen(rootLoc);
                target.getLocationOnScreen(viewLoc);
                targetRect.set(
                        viewLoc[0] - rootLoc[0] - dp(8),
                        viewLoc[1] - rootLoc[1] - dp(8),
                        viewLoc[0] - rootLoc[0] + target.getWidth() + dp(8),
                        viewLoc[1] - rootLoc[1] + target.getHeight() + dp(8)
                );
                invalidate();
                panel.post(() -> positionPanel(panel));
            });
        }

        private void positionPanel(MaterialCardView panel) {
            if (targetRect.isEmpty() || getHeight() == 0) {
                return;
            }

            int margin = dp(16);
            int gap = dp(12);
            float availableAbove = targetRect.top - gap - margin;
            float availableBelow = getHeight() - targetRect.bottom - gap - margin;
            int usableHeight = Math.max(dp(180), getHeight() - (margin * 2));
            int maxPanelHeight = Math.min(
                    usableHeight,
                    Math.max(dp(180), Math.round(Math.max(availableAbove, availableBelow)))
            );
            panel.measure(
                    MeasureSpec.makeMeasureSpec(Math.max(1, getWidth() - (margin * 2)), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(maxPanelHeight, MeasureSpec.AT_MOST)
            );
            int panelHeight = panel.getMeasuredHeight() > 0 ? panel.getMeasuredHeight() : Math.min(dp(180), maxPanelHeight);

            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    panelHeight >= maxPanelHeight ? maxPanelHeight : ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.leftMargin = margin;
            params.rightMargin = margin;

            if (availableBelow >= panelHeight) {
                params.gravity = Gravity.TOP;
                params.topMargin = Math.round(targetRect.bottom + gap);
            } else if (availableAbove >= panelHeight) {
                params.gravity = Gravity.TOP;
                params.topMargin = Math.max(margin, Math.round(targetRect.top - gap - panelHeight));
            } else if (availableAbove >= availableBelow) {
                params.gravity = Gravity.TOP;
                params.topMargin = margin;
            } else {
                params.gravity = Gravity.TOP;
                params.topMargin = Math.min(
                        getHeight() - margin - panelHeight,
                        Math.round(targetRect.bottom + gap)
                );
            }

            panel.setLayoutParams(params);
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            if (targetRect.isEmpty()) {
                canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
                return;
            }
            canvas.drawRect(0, 0, getWidth(), targetRect.top, dimPaint);
            canvas.drawRect(0, targetRect.bottom, getWidth(), getHeight(), dimPaint);
            canvas.drawRect(0, targetRect.top, targetRect.left, targetRect.bottom, dimPaint);
            canvas.drawRect(targetRect.right, targetRect.top, getWidth(), targetRect.bottom, dimPaint);
            canvas.drawRoundRect(targetRect, dp(14), dp(14), strokePaint);
        }

        private TextView text(String value, int sp, int color, boolean bold) {
            TextView text = new TextView(getContext());
            text.setText(value);
            text.setTextSize(sp);
            text.setTextColor(color);
            text.setPadding(0, dp(3), 0, dp(3));
            if (bold) text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
            return text;
        }

        private MaterialButton actionButton(String text, boolean filled) {
            MaterialButton button = new MaterialButton(getContext());
            button.setText(text);
            button.setAllCaps(false);
            if (filled) {
                button.setTextColor(resolveAttr(com.google.android.material.R.attr.colorOnPrimary));
            } else {
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
                button.setTextColor(ThemeManager.getAccentColor(getContext()));
            }
            return button;
        }

        private int resolveAttr(int attr) {
            android.util.TypedValue value = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(attr, value, true);
            return value.data;
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }
}
