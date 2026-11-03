# 501MTB App

Native Android application for the 501MTB mountain-bike community in Madagascar.

## Project status

Initial Android starter. This project is independent from the official website repository, `501mtb/501mtb.github.io`.

## Technology

- Kotlin
- Jetpack Compose and Material 3
- Firebase Authentication (Google sign-in) — integration to configure
- Cloud Firestore — integration to configure
- GitHub Actions Android debug build

## Requirements

- Android Studio with JDK 17
- Android SDK Platform 37
- Gradle 9.6.0

## First setup

1. Open this repository in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Create a Firebase project and register the Android app with package name `mg.madagascar.mtb501`.
4. Download `google-services.json` into the `app/` directory. This file is intentionally ignored by Git.
5. Enable Google as a sign-in provider and create the Firestore database.
6. Review and test `firestore.rules` with the Firebase Emulator Suite before deploying.

The first screen is a UI skeleton only. Authentication and live Firestore data are not connected yet.

## Security

- Never commit `google-services.json`, service-account keys, passwords, or private credentials.
- A member badge (for example, Leader or Admin) is not the same as an application security role.
- Account approval and administrator permissions must be enforced by Firestore Security Rules and trusted server-side setup, not only by UI checks.
