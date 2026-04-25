package com.example.CampusEventDiscovery.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.CampusEventDiscovery.MainActivity;
import com.example.CampusEventDiscovery.R;
import com.example.CampusEventDiscovery.model.Event;
import com.example.CampusEventDiscovery.model.EventProposal;
import com.example.CampusEventDiscovery.ui.event.CheckoutActivity;
import com.example.CampusEventDiscovery.ui.event.EventApprovalActivity;
import com.example.CampusEventDiscovery.ui.event.EventDetailActivity;
import com.example.CampusEventDiscovery.ui.event.TicketActivity;
import com.example.CampusEventDiscovery.ui.organizer.CreateEventActivity;
import com.example.CampusEventDiscovery.ui.organizer.ManageEventsActivity;
import com.example.CampusEventDiscovery.ui.organizer.ScannerActivity;
import com.example.CampusEventDiscovery.ui.profile.MemoriesActivity;
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
            showOverlay(activity, target, step);
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
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("OPEN_TAB", "profile");
        intent.putExtra("OPEN_HELP", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        if (!(activity instanceof MainActivity)) {
            activity.finish();
        }
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
        } else if ("create_event".equals(step.screen)) {
            intent = new Intent(activity, CreateEventActivity.class);
        } else if ("manage_events".equals(step.screen)) {
            intent = new Intent(activity, ManageEventsActivity.class);
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
        if (TextUtils.isEmpty(id)) return list;
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
            case "attendee_ticket":
                list.add(new Step("my_events", R.id.rvSection1, "Registered events", "Your RSVP events appear on the real My Events screen."));
                list.add(new Step("ticket", R.id.ivTicketQrCode, "Ticket QR", "This QR is what attendees show at check-in."));
                break;
            case "attendee_memories":
                list.add(new Step("memories", R.id.rvMemories, "Memory folders", "Attended events appear here as memory folders."));
                break;
            case "attendee_sos":
                list.add(new Step("home_attendee", R.id.btnSos, "SOS button", "This is where attendees request urgent help. Walkthrough mode never sends an alert."));
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
            case "organizer_scan":
                list.add(new Step("home_organizer", R.id.btnScanTickets, "Scan tickets", "Open scanner tools from the organizer dashboard."));
                list.add(new Step("scanner", R.id.btnStartScanner, "Start scanner", "Use this to scan attendee QR codes."));
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
            case "admin_sos":
                list.add(new Step("home_admin", R.id.btnSosDashboard, "SOS dashboard", "Admins can open campus-wide SOS monitoring here."));
                list.add(new Step("sos_dashboard", R.id.rvSosAlerts, "SOS list", "Active SOS alerts appear in this real dashboard list."));
                break;
            default:
                break;
        }
        return list;
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
            MaterialButton back = actionButton(getContext().getString(R.string.walkthrough_back), false);
            MaterialButton next = actionButton(position == total - 1 ? getContext().getString(R.string.walkthrough_done) : getContext().getString(R.string.walkthrough_next), true);
            exit.setOnClickListener(v -> cancel((Activity) getContext()));
            back.setVisibility(position == 0 ? GONE : VISIBLE);
            back.setOnClickListener(v -> back((Activity) getContext()));
            next.setOnClickListener(v -> next((Activity) getContext()));
            actions.addView(exit);
            actions.addView(back);
            actions.addView(next);

            content.addView(counter);
            content.addView(title);
            content.addView(body);
            content.addView(actions);
            panel.addView(content);
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
            int maxPanelHeight = Math.max(dp(120), Math.round(Math.max(availableAbove, availableBelow)));
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
            if (!filled) {
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
