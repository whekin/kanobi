# Validation

Checked on 3 October 2026 for 1.0.0:

- `:app:assembleDebug`, `:app:assembleRelease` (R8, 2.4 MB) — pass.
- `:app:testDebugUnitTest` — 4 tests: angle at known tilts and rolls, rejected zero and non-finite vectors, the stability window and its progress.
- `:app:lintDebug` — no errors; only “newer version available” warnings. The fixed portrait orientation is suppressed on purpose: the readout levels itself, and recreating the activity would reset the stability window mid-measurement.

Release build on a Pixel 9 Pro emulator (API 36), poses set with `adb emu sensor set acceleration`:

- Screen up at 22°: readout, slope 40 %, dial needle, shutter ring sweeping as the reading settles.
- On the edge: the readout and dial level with the horizon; edge mode fades in from about 80° and is complete at 75°.
- Settings: lime and Material You colours, zero calibration entry, haptics and live background switches, about with version 1.0.0 (1).
- English, Russian and Georgian via the per-app language (`cmd locale set-app-locales`). Georgian text uses the system font, since Golos Text has no Georgian glyphs.
- Journal: empty state, saving with and without a name, deleting with confirmation.

The previous build ran on a Galaxy S25 (Android 16): no crashes, no janky frames over 10 s at 120 Hz.

The emulator's gravity sensor is virtual: after an abrupt accelerometer change it settles over several seconds, while a real phone's gyroscope makes it immediate.

Not checked yet:

- Sensor accuracy on a real straightedge, screen up and on the edge.
- Shader cost on budget GPUs.
- Small screens and large system fonts.

## On a phone

1. Lay the phone screen up on a level table: about 0°, the shutter ring fills and turns to the accent colour.
2. Compare with known angles, e.g. 30° and 45°, and a negative one.
3. Stand it on the long edge: the readout and dial turn level, the horizon appears; the reading matches screen-up on the same straightedge, give or take the camera bump.
4. Move the phone: the shutter stays disabled until the ring closes; the digits split and refocus.
5. Lock, move the phone: the locked angle stays. Save, open the journal, restart: the entry is there.
