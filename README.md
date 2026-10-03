<p align="center"><img src="docs/icon.svg" width="96" alt="Kanobi icon"></p>

# Kanobi

**Kanobi** (ქანობი, Georgian for “slope”) is an Android inclinometer for the trail: takeoffs, roll-ins, drops — any surface you can lay a straightedge on. One big number, a dial, and a spirit level when the phone stands on its edge.

<p align="center">
  <img src="docs/screenshots/measure.jpg" width="200" alt="Measuring screen up">
  <img src="docs/screenshots/edge.jpg" width="200" alt="Spirit level on the edge">
  <img src="docs/screenshots/material-you.jpg" width="200" alt="Material You colours">
  <img src="docs/screenshots/settings.jpg" width="200" alt="Settings">
</p>

## Features

- Angle of the phone's long side against the horizon, signed, in 0.1° steps: plus rises towards the top of the phone, minus falls. Slope in percent underneath.
- Screen up on a straightedge, or standing on its long edge. On the edge the phone turns into a spirit level: the readout and the dial follow the real horizon, the needle runs along the phone and the background shows the horizon line.
- Stability is a dwell, not a word: the reading must stay within 0.6° for 650 ms. The shutter ring fills while it does, the digits split into RGB fringes while the phone moves and snap into focus when it stops.
- Lock the angle and save it to a local journal with a name.
- Settings: lime or Material You colour, zero calibration on a level surface, haptics, and a “live background” switch to save battery.
- English, Russian and Georgian, following the device language or the per-app language setting.
- No account, no network, no permissions. The screen stays on while measuring.

The angle depends on how the phone sits on the straightedge, its case, camera bump and sensor: screen up, a camera bump can tilt it by around a degree, which zero calibration takes out; on the edge it does not matter. Check against a level surface or a known angle when in doubt. One angle says nothing about whether a jump is safe or about the curve of a takeoff.

## Under the hood

Kotlin and Jetpack Compose, Android 13+ (API 33).

- `measurement/` — gravity sensor (or a filtered accelerometer), the angle maths and the stability window, the journal.
- `ui/` — measure screen, dial, journal, sheets and settings.
- `fx/` — AGSL shaders: the topographic background (value noise with analytic derivatives, so contour lines and hillshade need no `fwidth`), the horizon, the lock pulse and the RGB split. The background renders at two-thirds resolution into a cached layer, follows motion at the display rate and drifts at about 30 fps when the phone is still.
- `theme/` — the palette, derived from the Material You accent when that is on.

## Build

JDK 17+ and Android SDK 36.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease
```

Release builds are minified with R8. They are signed with the key described in `keystore.properties` (kept out of git) when the file exists:

```properties
storeFile=release.jks
storePassword=…
keyAlias=kanobi
keyPassword=…
```

Without it they fall back to the debug key, which is fine for sideloading but not for a store. Ready-made APKs are attached to the [releases](https://github.com/whekin/kanobi/releases).

## License

MIT, see [LICENSE](LICENSE). Fonts: Barlow Condensed and Golos Text, SIL Open Font License, see `docs/licenses/`.
