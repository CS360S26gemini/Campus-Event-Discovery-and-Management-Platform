/**
 * SOS Alert Fan-out Cloud Function
 *
 * Trigger: onCreate of any document in `sos_alerts`.
 * Reads the alert, resolves the target recipients (event organizer + all admins),
 * fetches their FCM tokens from `users/{uid}.fcmToken`, and sends a high-priority
 * data message with type=SOS_ALERT so the app can render the full-screen alarm.
 *
 * Deploy:
 *   cd functions && npm install && firebase deploy --only functions:onSosAlertCreated
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();
const REMINDER_TIME_ZONE = "Asia/Karachi";
const REMINDER_WINDOW_DAYS = 3;

exports.onSosAlertCreated = functions.firestore
    .document("sos_alerts/{alertId}")
    .onCreate(async (snap, context) => {
      const alert = snap.data() || {};
      const alertId = context.params.alertId;

      const organizerId = alert.organizerId || null;
      const reporter = alert.displayName || "Unknown Reporter";
      const eventId = alert.eventId || "";
      const eventName = alert.eventName || "";
      const mapsUrl = alert.mapsUrl || "";
      const description = buildDescription(alert);

      // 1. Collect recipient UIDs: organizer + all admins.
      const recipientIds = new Set();
      if (organizerId) recipientIds.add(organizerId);

      const adminsSnap = await db.collection("users")
          .where("role", "==", "admin")
          .get();
      adminsSnap.forEach((doc) => recipientIds.add(doc.id));

      if (recipientIds.size === 0) {
        console.log(`[SOS ${alertId}] No recipients resolved.`);
        return null;
      }

      // 2. Fetch FCM tokens for each recipient.
      const tokens = [];
      await Promise.all(
          Array.from(recipientIds).map(async (uid) => {
            const userDoc = await db.collection("users").doc(uid).get();
            const token = userDoc.exists ? userDoc.get("fcmToken") : null;
            if (token) tokens.push(token);
          }),
      );

      if (tokens.length === 0) {
        console.log(`[SOS ${alertId}] No FCM tokens available for recipients.`);
        return null;
      }

      // 3. Send as a DATA-only multicast so the client-side service
      //    always runs onMessageReceived and can launch the full-screen intent
      //    even when the app is backgrounded.
      const message = {
        data: {
          type: "SOS_ALERT",
          alertId: alertId,
          reporter: reporter,
          description: description,
          eventId: eventId,
          eventName: eventName,
          mapsUrl: mapsUrl,
        },
        android: {
          priority: "high",
        },
        tokens: tokens,
      };

      const response = await messaging.sendEachForMulticast(message);
      console.log(
          `[SOS ${alertId}] sent=${response.successCount} failed=${response.failureCount}`,
      );

      // 4. Prune invalid tokens so stale devices stop receiving alerts.
      const invalid = [];
      response.responses.forEach((r, i) => {
        if (!r.success) {
          const code = r.error && r.error.code;
          if (
            code === "messaging/invalid-registration-token" ||
            code === "messaging/registration-token-not-registered"
          ) {
            invalid.push(tokens[i]);
          }
        }
      });

      if (invalid.length > 0) {
        const usersSnap = await db.collection("users")
            .where("fcmToken", "in", invalid.slice(0, 10))
            .get();
        const batch = db.batch();
        usersSnap.forEach((doc) =>
          batch.update(doc.ref, {fcmToken: admin.firestore.FieldValue.delete()}),
        );
        await batch.commit();
      }

      return null;
    });

exports.sendEventReminders = functions.pubsub
    .schedule("0 8 * * *")
    .timeZone(REMINDER_TIME_ZONE)
    .onRun(async () => {
      const now = new Date();
      const windowEnd = addDays(now, REMINDER_WINDOW_DAYS);

      const eventsSnap = await db.collection("events")
          .where("status", "==", "active")
          .where("date", ">=", admin.firestore.Timestamp.fromDate(now))
          .where("date", "<=", admin.firestore.Timestamp.fromDate(windowEnd))
          .get();

      if (eventsSnap.empty) {
        console.log("[EVENT_REMINDER] No active events inside reminder window.");
        return null;
      }

      for (const eventDoc of eventsSnap.docs) {
        const event = eventDoc.data() || {};
        const eventId = eventDoc.id;
        const eventTitle = event.title || "your event";
        const eventDate = event.date && typeof event.date.toDate === "function"
            ? event.date.toDate()
            : null;

        if (!eventDate) {
          console.log(`[EVENT_REMINDER ${eventId}] Missing event date, skipping.`);
          continue;
        }

        const daysRemaining = getDaysRemaining(now, eventDate);
        if (daysRemaining < 0 || daysRemaining > REMINDER_WINDOW_DAYS) {
          continue;
        }

        const attendeesSnap = await db.collection("events")
            .doc(eventId)
            .collection("attendees")
            .get();

        if (attendeesSnap.empty) {
          continue;
        }

        const userIds = attendeesSnap.docs.map((doc) => doc.id);
        const tokens = await fetchFcmTokens(userIds);
        if (tokens.length === 0) {
          continue;
        }

        const startTime = formatStartTime(eventDate);
        const title = buildReminderTitle(eventTitle, daysRemaining, startTime);
        const body = buildReminderBody(eventTitle, daysRemaining, startTime);

        await sendReminderMulticast(tokens, {
          type: "EVENT_REMINDER",
          eventId: eventId,
          title: title,
          body: body,
          destinationTab: "calendar",
        });
      }

      return null;
    });

function buildDescription(alert) {
  const parts = [];
  if (alert.eventName) parts.push(`Event: ${alert.eventName}`);
  if (alert.latitude != null && alert.longitude != null) {
    parts.push(`Location: ${alert.latitude}, ${alert.longitude}`);
  }
  if (alert.mapsUrl) parts.push(alert.mapsUrl);
  return parts.join("\n");
}

async function fetchFcmTokens(userIds) {
  const uniqueIds = Array.from(new Set(userIds)).filter(Boolean);
  if (uniqueIds.length === 0) {
    return [];
  }

  const tokenDocs = await Promise.all(
      uniqueIds.map(async (uid) => {
        const userDoc = await db.collection("users").doc(uid).get();
        const token = userDoc.exists ? userDoc.get("fcmToken") : null;
        return token || null;
      }),
  );

  return tokenDocs.filter(Boolean);
}

async function sendReminderMulticast(tokens, data) {
  const chunks = chunk(tokens, 500);
  for (const chunkTokens of chunks) {
    const response = await messaging.sendEachForMulticast({
      data: data,
      android: {
        priority: "normal",
      },
      tokens: chunkTokens,
    });

    const invalidTokens = [];
    response.responses.forEach((r, i) => {
      if (!r.success) {
        const code = r.error && r.error.code;
        if (
          code === "messaging/invalid-registration-token" ||
          code === "messaging/registration-token-not-registered"
        ) {
          invalidTokens.push(chunkTokens[i]);
        }
      }
    });

    if (invalidTokens.length > 0) {
      await pruneInvalidTokens(invalidTokens);
    }
  }
}

async function pruneInvalidTokens(invalidTokens) {
  const usersSnap = await db.collection("users")
      .where("fcmToken", "in", invalidTokens.slice(0, 10))
      .get();
  const batch = db.batch();
  usersSnap.forEach((doc) => {
    batch.update(doc.ref, {fcmToken: admin.firestore.FieldValue.delete()});
  });
  await batch.commit();
}

function buildReminderTitle(eventTitle, daysRemaining, startTime) {
  if (daysRemaining <= 0) {
    return `${eventTitle} commencing at ${startTime}`;
  }
  return `${daysRemaining} Days left to ${eventTitle}`;
}

function buildReminderBody(eventTitle, daysRemaining, startTime) {
  if (daysRemaining <= 0) {
    return `${eventTitle} commencing at ${startTime}`;
  }
  return `Reminder: ${daysRemaining} Days left to ${eventTitle}`;
}

function getDaysRemaining(now, eventDate) {
  const nowKey = localDayKey(now);
  const eventKey = localDayKey(eventDate);
  return Math.round((eventKey - nowKey) / 86400000);
}

function localDayKey(date) {
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: REMINDER_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  const parts = formatter.formatToParts(date);
  const year = Number(parts.find((part) => part.type === "year").value);
  const month = Number(parts.find((part) => part.type === "month").value);
  const day = Number(parts.find((part) => part.type === "day").value);
  return Date.UTC(year, month - 1, day);
}

function formatStartTime(date) {
  return new Intl.DateTimeFormat("en-PK", {
    timeZone: REMINDER_TIME_ZONE,
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(date);
}

function addDays(date, days) {
  return new Date(date.getTime() + (days * 24 * 60 * 60 * 1000));
}

function chunk(array, size) {
  const result = [];
  for (let i = 0; i < array.length; i += size) {
    result.push(array.slice(i, i + size));
  }
  return result;
}
