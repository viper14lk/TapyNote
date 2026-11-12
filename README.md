# TapyNote

Minimal sticky notes for Android (8.0+), in the app and on your home screen.

- **App:** lists all notes. Tap **+** to add, tap a note to edit, long-press to delete.
- **Home screen:** long-press home screen → Widgets → TapyNote → drag it out (creates a new note), or open a note in the app and tap **Pin to home**.
- **Colour:** pick one of the swatches in the editor; the widget updates too.
- Edits save automatically when you close the editor. Removing a widget keeps the note in the app.

## Build
`./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
Every push also builds the APK in GitHub Actions (download it from the run's Artifacts).
