# 501MTB App

Native Android application for the 501MTB mountain-bike community in Madagascar.

## Project status

Development is taking place on the `initial/android-skeleton` branch and remains independent from the official website repository, `501mtb/501mtb.github.io`. The current build is an early functional foundation, not a production release.

## Implemented

- Kotlin, Jetpack Compose and Material 3 application shell.
- Bottom navigation for Home, Rides, Events and Riders.
- Firebase Authentication with Google Credential Manager sign-in.
- Automatic creation of a pending USER account record after Google sign-in, when Firestore is available.
- Firestore repository and data models for accounts, rides, events, public member profiles and club summary.
- Firestore-backed Rides, Events and Riders screens.
- Ride registration restricted to signed-in, administrator-approved members.
- Firestore rules that prevent users from self-approving, assigning themselves a member link, or promoting their own role.

## Still to complete

- Firestore database creation and region selection in Firebase Console.
- Trusted initial administrator bootstrap.
- Admin screens for approving accounts, linking existing club members, publishing rides/events and recording attendance.
- Migration/import of verified club data.
- End-to-end tests with Firebase Emulator Suite and a real test account.
- Offline and error-state testing before release.

## Technology

- Kotlin and Jetpack Compose
- Firebase Authentication (Google)
- Cloud Firestore
- GitHub Actions Android debug build

## Firebase setup

1. Open Firebase project `mtb-app-3fb1d`.
2. Confirm Google is enabled under Authentication providers.
3. Create Firestore Database in a region selected deliberately; the region cannot be changed after creation.
4. Review and test `firestore.rules` with the Firebase Emulator Suite before deploying them.
5. Bootstrap the first ADMIN user through a trusted administrator-only process. Never grant ADMIN from the Android client.

The Android application uses Firestore's local read cache where available. A pending offline write must not be presented as server-confirmed until Firestore acknowledges it.

## Security

- Never commit service-account keys, passwords or private credentials.
- Keep `google-services.json` private to the Android app setup; do not publish its contents in documentation.
- A member badge (for example, Leader or Admin) is not the same as the app security role.
- Account approval, member linking and administrator permissions are enforced by Firestore Security Rules, not merely by UI checks.
- The official website repository must remain separate and unchanged.
