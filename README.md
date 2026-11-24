# Bubblant 🫧🐜

A tilt-controlled Android game: the whole screen is water, a tiny air bubble floats on it and a living ant stands on top. Tilt the phone to carry the ant through 60 maze levels to a safe lily pad — but bump a wall too fast and the bubble pops!

## Features
- **60 levels in 6 worlds** (Sunny Pond → Lazy River → Windy Lake → Dark Cave → Thunder Storm → The Abyss), generated deterministically and verified solvable by unit tests.
- **Talking ant**: gives directions (left/right/up/down), shakes its head and says "other way!" when you go wrong, gets angry if you keep ignoring it, gives a 👍 when you do well, thanks you at the end. Speech bubbles + real voice (text-to-speech).
- **Hazards**: fragile bubble (speed-based popping), sea urchins, patrolling fish / piranhas, water currents, wind and gusts, whirlpools, rain ripples, lightning, darkness, hunting eels.
- **Special levels**: rescue other ants stranded on leaves (they ride on your bubble), shrinking (leaking) bubble with O₂ pickups, race against the clock.
- **Procedural music**: cheerful in the pond, adventurous on the river, mysterious in the cave, action in the storm, horror in the abyss — with extra tension when time or air is running out. No audio files, no licensing issues.
- **Collectibles**: 3 sugar cubes per level = stars; stars unlock hats for the ant.
- **Settings**: music, sound effects, ant voice, speech bubbles, vibration, touch control, sensitivity, language (English default; Türkçe, Español, Deutsch, Français, Português), privacy options.
- **AdMob**: adaptive banners on menus, frequency-capped interstitials (every 3rd level end, ≥75 s apart, not in levels 1-3), rewarded ads for *Revive* and *Skip level*. Google UMP consent form (GDPR).

## Build
Requirements: Android Studio (its bundled JDK) and Android SDK 36.

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest assembleDebug     # debug APK with Google TEST ads
.\gradlew.bat bundleRelease                        # app-release.aab for Play Console
```
Outputs: `app/build/outputs/apk/debug/app-debug.apk`, `app/build/outputs/bundle/release/app-release.aab`.

## Before publishing on Google Play
1. **Signing key** – run `tools\create-keystore.ps1` once. Back up `upload.jks` + `keystore.properties` (they are git-ignored). Enroll in *Play App Signing* when you upload the first bundle.
2. **AdMob ids** – done: app *Bubblant* (`ca-app-pub-1210933263813150~3717822413`) with Banner, Interstitial and Rewarded units, wired into release builds in `app/build.gradle`. After publishing, link the Play Store listing to the app in AdMob (Apps → Bubblant → Add store) so ad serving is no longer limited. Debug builds always use Google's test ids — never click your own live ads.
3. **Privacy policy** – enable GitHub Pages (Settings → Pages → branch `main`, folder `/docs`) and put the URL `https://<user>.github.io/Bubblant/privacy.html` into `SettingsActivity.PRIVACY_URL` and the Play Console.
4. **Consent message** – in AdMob → Privacy & messaging, create a GDPR message for the app.
5. Play Console: content rating questionnaire, *Contains ads = Yes*, Data safety (advertising ID, collected by AdMob), target audience 13+.
6. Bump `versionCode` in `app/build.gradle` for every upload.

## GitHub Actions
`.github/workflows/android.yml` runs the level tests and builds the APK + AAB on every push. Optional repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `ADMOB_APP_ID`, `ADMOB_BANNER`, `ADMOB_INTERSTITIAL`, `ADMOB_REWARDED`.

## Project layout
```
app/src/main/java/com/bubblant/game/
  engine/   LevelSpec (difficulty per level), Maze, Level (hazard placement), GameWorld (physics), AntBrain (personality)
  view/     GameView (loop + tilt), GameRenderer, AntPainter, MenuBubblesView
  audio/    AudioEngine (mixer), Sequencer + MusicStyle (generative music), Sfx, AntVoice (TTS)
  *.java    Activities, AdManager, Prefs
```
