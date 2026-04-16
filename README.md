# Campus Event Discovery & Management Platform

**Team Gemini — CS360 Android Project (Level 2, Sprint 1)**
LUMS Computer Science, Spring 2026

---

## 🚀 Live Payment Integration (Yahya's Update)

This project has been upgraded from a mock payment system to a **Live Stripe Test-Mode** integration using **Supabase Edge Functions**.

### **How the Payment Flow Works**

1.  **Android App (`CheckoutActivity`)**: When the user selects a payment method and clicks "Pay", the app collects the `userId`, `eventId`, and `amount`.
2.  **Supabase Edge Function**: The app sends an authenticated POST request to a custom Supabase Edge Function (`create-payment-intent`).
3.  **Stripe API**: The Edge Function securely communicates with the Stripe API using a Secret Key (hidden from the app) to create a `PaymentIntent`.
4.  **Transaction ID**: Stripe returns a real `paymentIntentId` (starting with `pi_...`).
5.  **Firestore Sync**: 
    -   The app receives the ID and creates a **Payment Receipt** in the `payments` collection.
    -   It then updates the User's **RSVP ticket** in the `users/{id}/rsvps` sub-collection with the status `CONFIRMED` and the real Stripe `transactionId`.
6.  **Verification**: You can see these payments live in the [Stripe Dashboard](https://dashboard.stripe.com/test/payments) and matching records in the Firebase Console.

### **Tech Stack Added**
-   **Backend**: Supabase Edge Functions (Deno/TypeScript).
-   **Payments**: Stripe API (Test Mode).
-   **Networking**: OkHttp3 for secure communication.
-   **Security**: Secret keys are stored in the Cloud (Supabase), never on the Android device.

---

## 🛠 Setup & Deployment

If you are setting this up on a new machine, you must deploy the Edge Function:

```bash
# From the project root
npx supabase login
npx supabase link --project-ref jhvujgiusemenimbzmil
npx supabase functions deploy create-payment-intent --no-verify-jwt
```

---

## 🧪 Testing
-   **Test Card**: `4242 4242 4242 4242`
-   **Expiry/CVV**: Any future date and any 3 digits.
-   **Success State**: A QR code ticket is generated only after Stripe confirms the intent.

---

— *Yahya, Team Gemini*
