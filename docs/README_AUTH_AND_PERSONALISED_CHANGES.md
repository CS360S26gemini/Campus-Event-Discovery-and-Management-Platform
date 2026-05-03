# Auth, Sign-In, and Personalised Suggestions Changes

This document summarizes the recent changes on the `personalised_suggestions` branch for:

- Personalised event suggestions
- Forgot Password
- Strong password validation
- Remember Me
- Sign In page navigation changes

## Personalised Suggestions

The Home screen now supports deterministic personalised event recommendations for signed-in users.

Recommendations use existing app data only:

- User interests from `User.interests`
- Up to 5 recently viewed event categories
- Event RSVP count
- Event date proximity

No AI service, ML API, external recommendation API, or new Gradle dependency was added.

Main files:

- `app/src/main/java/com/example/CampusEventDiscovery/repository/EventRepository.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/home/HomeFragment.java`
- `app/src/main/res/layout/fragment_home.xml`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/event/EventDetailActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/ui/myevents/MyEventsFragment.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/Constants.java`

Recommendation behavior:

- Past events are excluded.
- Events matching selected interests score higher.
- Events matching recently viewed categories get an additional score.
- Popular events get a capped RSVP score.
- Events happening soon get a date-proximity boost.
- If no personalised event scores above zero, the carousel falls back to trending events.

Interest selection was also standardized with fixed Material chips:

- Music
- Sports
- Career
- Academic
- Arts
- Business
- Food & Bev
- Social

Users must select at least 3 interests during sign-up and account settings updates.

More detail is available in:

- `README_PERSONALISED_RECOMMENDATIONS.md`

## Forgot Password

The Sign In screen now includes a `Forgot Password?` link.

Main files:

- `app/src/main/java/com/example/CampusEventDiscovery/SignInActivity.java`
- `app/src/main/res/layout/activity_sign_in.xml`
- `app/src/main/res/values/strings.xml`

Behavior:

- User taps `Forgot Password?`.
- App opens a reset-password dialog.
- The dialog pre-fills the email from the Sign In email field when available.
- Email is validated with `SignupValidator.validateEmail(...)`.
- On valid input, the app calls:

```java
FirebaseAuth.sendPasswordResetEmail(email)
```

Security behavior:

- The app does not reveal whether an email is registered.
- For user-not-found cases, the UI still shows the generic message:

```text
If an account exists for this email, a password reset link has been sent.
```

The app does not store reset tokens, passwords, password hashes, or reset email data in Firestore.

Firebase Console setup remains required:

- Email/Password auth enabled
- Password reset email template configured
- Firebase Password Policy configured separately for reset-link password strength enforcement

## Strong Password Validation

Password validation is centralized in:

- `app/src/main/java/com/example/CampusEventDiscovery/util/SignupValidator.java`

Required password rule:

- At least 8 characters
- At least 1 uppercase letter
- At least 1 number
- At least 1 special/non-alphanumeric character

The app does not require lowercase.

Validation is enforced before Firebase calls in:

- `SignUpActivity` before `createUserWithEmailAndPassword(...)`
- `AccountSettingsActivity` before reauth and `updatePassword(...)`
- `FirebaseAuthRepository.signup(...)` before `createUserWithEmailAndPassword(...)`

Password policy message:

```text
Password must be at least 8 characters and include 1 uppercase letter, 1 number, and 1 special character.
```

Firebase hosted reset-link pages are outside the Android app, so reset-link password strength must be enforced in Firebase Console:

```text
Authentication -> Settings -> Password policy
Minimum length: 8
Uppercase required
Numeric required
Non-alphanumeric required
Enforcement mode: Require
```

## Remember Me

The existing Sign In `Remember me` switch now persists only the user's email address.

Main files:

- `app/src/main/java/com/example/CampusEventDiscovery/SignInActivity.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/AuthPreferenceManager.java`
- `app/src/main/java/com/example/CampusEventDiscovery/util/Constants.java`
- `app/src/main/res/layout/activity_sign_in.xml`

SharedPreferences storage:

```java
Constants.PREFS_AUTH
Constants.PREF_REMEMBER_ME
Constants.PREF_REMEMBERED_EMAIL
```

Behavior when Remember Me is enabled:

- After successful Firebase sign-in and email verification, the app saves the email locally.
- On the next Sign In screen load, the email field is pre-filled.
- The password field stays blank.
- The Remember Me switch is checked.
- The user must still type their password.

Behavior when Remember Me is disabled:

- After successful Firebase sign-in and email verification, any previously remembered email is cleared.
- The next Sign In screen opens with a blank email field and unchecked switch.

Security constraints:

- Password is never stored.
- No auto-login is performed.
- Firebase session behavior is unchanged.
- Failed sign-ins do not save email.
- Unverified email sign-ins do not save email.
- `admin/admin` developer bypass does not save or clear remembered real-user email.

Logout behavior:

- `ProfileFragment.showLogoutDialog()` still calls `FirebaseAuth.getInstance().signOut()`.
- It still clears developer bypass state with `DevSessionManager.clearBypass(...)`.
- It does not clear Remember Me preferences, so remembered email survives logout.

## Sign In Page Changes

Main files:

- `app/src/main/java/com/example/CampusEventDiscovery/SignInActivity.java`
- `app/src/main/res/layout/activity_sign_in.xml`
- `app/src/main/res/values/strings.xml`

The Sign In page now includes:

- Email field
- Password field
- `Forgot Password?` link
- Remember Me switch
- Sign In button
- `New here? Create an account` row
- Developer Bypass button

Create-account navigation:

- Tapping `Create an account` opens `SignUpActivity`.
- `SignInActivity` is not finished, so Back returns to Sign In.

Preserved behavior:

- Existing Firebase email/password sign-in
- Email verification handling
- Resend verification email dialog
- Forgot Password flow
- Remember Me email prefill
- `admin/admin` bypass
- Developer role bypass
- Theme preference sync
- MainActivity navigation after sign-in

## Tests

Relevant test files:

- `app/src/test/java/com/example/CampusEventDiscovery/SignupValidatorTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/util/AuthPreferenceManagerTest.java`
- `app/src/test/java/com/example/CampusEventDiscovery/repository/EventRepositoryPersonalisationTest.java`

Test coverage includes:

- Valid and invalid emails
- Strong and weak password cases
- Password confirmation match and mismatch
- Remember Me enabled stores email and true flag
- Remember Me disabled clears stored email and stores false flag
- Empty remembered email default
- Password is not stored in auth preferences
- Personalised recommendation scoring and fallback behavior

## Manual QA Checklist

Forgot Password:

1. Open Sign In.
2. Tap `Forgot Password?`.
3. Confirm email dialog opens.
4. Confirm existing email is pre-filled when available.
5. Invalid email keeps the dialog open.
6. Valid email sends Firebase reset email.
7. App shows the generic success message.

Remember Me:

1. Sign in with Remember Me enabled.
2. Sign out from Profile.
3. Return to Sign In.
4. Confirm email is pre-filled, password is blank, and switch is checked.
5. Sign in with Remember Me disabled.
6. Sign out and return to Sign In.
7. Confirm email is blank and switch is unchecked.
8. Confirm failed login does not save email.
9. Confirm unverified login does not save email.
10. Confirm `admin/admin` is never remembered as an email.

Create Account Link:

1. Open Sign In.
2. Tap `Create an account`.
3. Confirm `SignUpActivity` opens.
4. Press Back and confirm Sign In is still available.

Password Policy:

1. Sign-up rejects `Camp1!`.
2. Sign-up rejects `campus1!`.
3. Sign-up rejects `Campus!!`.
4. Sign-up rejects `Campus12`.
5. Sign-up accepts `Campus1!`.
6. Account Settings password change rejects weak new passwords before reauthentication.

Personalised Suggestions:

1. Create or use a user with at least 3 interests.
2. Open Home.
3. Confirm recommendation carousel appears when matching active upcoming events exist.
4. Open event details for several events.
5. Return Home and confirm recently viewed categories influence recommendations.
6. Confirm fallback title changes to `Trending on Campus` when no personalised event scores above zero.

## Build Commands

From the project root:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

If `JAVA_HOME` points to an invalid Android Studio JBR path, set it for the current PowerShell session:

```powershell
$env:JAVA_HOME='D:\Program Files\Android Studio\jbr'
```

If Gradle cannot write to its default cache path, use a writable Gradle cache:

```powershell
$env:GRADLE_USER_HOME='C:\Users\hbaqa\.gradle'
```

## Security Notes

- Passwords are never stored in Firestore or SharedPreferences.
- Remember Me stores only an email address and a boolean flag.
- Forgot Password uses Firebase Authentication's built-in password reset email flow.
- No custom reset-token collection or custom email delivery was added.
- Firebase sign-out remains the source of truth for ending authenticated sessions.
- Developer bypass remains separate from real Firebase authentication.
