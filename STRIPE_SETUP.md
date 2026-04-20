# Stripe + Supabase Setup — One Time Steps

## 1. Install Supabase CLI (run once)
npm install -g supabase

## 2. Login to Supabase
npx supabase login
# (opens browser — just authorize)

## 3. Link your project
npx supabase link --project-ref jhvujgiusemenimbzmil
# (enter your Supabase database password when prompted)

## 4. Deploy the Edge Function
npx supabase functions deploy create-payment-intent --no-verify-jwt

## 5. Done!
# Test it by going through checkout in the app.
# Real PaymentIntents will appear in your Stripe sandbox dashboard at:
# https://dashboard.stripe.com/test/payments

## Test Card to use in the app:
# Card number: 4242 4242 4242 4242
# Expiry: any future date (e.g. 12/26)
# CVC: any 3 digits (e.g. 123)
