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

function buildDescription(alert) {
  const parts = [];
  if (alert.eventName) parts.push(`Event: ${alert.eventName}`);
  if (alert.latitude != null && alert.longitude != null) {
    parts.push(`Location: ${alert.latitude}, ${alert.longitude}`);
  }
  if (alert.mapsUrl) parts.push(alert.mapsUrl);
  return parts.join("\n");
}
