# Validation

## 1.0.2 — Galaxy S25 frame pacing, 25 October 2026

The RGB visual is preserved. The old offscreen RuntimeShader was replaced by three direct glyph draws (green coverage plus additive red/blue offsets), with the same movement-dependent separation. Terrain, horizon, pulse, palette and lock animation remain enabled.

A separate minified `.benchmark` APK replays the actual UI through identical fast pitch/roll poses at 100 Hz. All comparisons below use the same physical SM-S931B and 120 Hz display. This is a UI workload replay, not a sensor-accuracy test.

| Variant | Rendered frames/s | Frames missing the deadline (gfxinfo) |
| --- | ---: | ---: |
| Original offscreen RGB, run 1 | 71.01 | 21.51% |
| Original offscreen RGB, warmed repeat | 53.64 | 29.74% |
| RGB disabled, live terrain kept | 120.05 | 0.00% |
| Direct RGB glyphs, live terrain kept | 120.06 | 0.00% |
| Direct RGB + adaptive background, normal replay | 120.06 | 0.07% |
| Direct RGB + adaptive background, 3× background pixel workload | 119.90 | 0.14% |

The original rendered-frame regression fails the gate `--max-jank-percent 5 --min-fps 110`; direct RGB passes. SurfaceFlinger FrameTimeline additionally records 716 benchmark frames over a six-second trace as **On-time Present** (about 119/s). The trace labels them `Buffer Stuffing`, so this is not a claim that every queue/pipeline metric is zero. CPU work was around 2–4 ms; the expensive offscreen text effect introduced long GPU/queue stalls during movement.

For lower-powered GPUs, sustained missed display frames reduce only the background texture scale: 0.66 → 0.5 → 0.4. Text, RGB, dial and sensor cadence stay full quality. Three unit tests cover smooth 60/120 Hz, sustained missed frames, resume gaps and display-rate changes. A higher background pixel-work multiplier exists only in the separate benchmark. It simulates GPU pressure, not a physical budget phone; such hardware remains to be checked.

Release, benchmark, 14 unit tests and Lint complete successfully. Version 1.0.2 (code 3) was installed and launched on the S25; the separate benchmark APK and on-device trace files were removed after testing. The final adaptive build passes the same FPS/jank gate both normally and under a threefold background pixel workload (see table). The stress changes only benchmark texture work; this is a synthetic headroom check, not a promise of 120 fps on every phone.

## 1.0.1

Checked on 24–25 October 2026 for 1.0.1:

- Release APK (R8), unit tests and Android Lint: pass. 11 unit tests, 0 failures; Lint: 0 errors, 2 dependency/tooling update warnings.
- The actual dial interpolation is extracted into `InstrumentResponse` and tested at 60 and 120 Hz. With a 0° → 30° step, after 100 ms the old response showed 16.96°; the new response shows 29.45°. The 90% settling time of this UI filter falls from about 276 ms to 58 ms. These are filter response times, not physical sensor-to-display measurements.
- Raw accelerometer fallback: a 30° step reaches 28.87° after 100 ms; alternating stationary noise stays below 0.03°. The fused gravity sensor is still used when available and receives no second low-pass filter.
- Screen-up, left-edge and right-edge offsets are independent. Tests cover camera-bump compensation staying off the edges, inclined poses, resetting one side, rejection of moving/steep/unsupported calibration poses.
- The existing screen-up `zero` preference is retained; the journal preference format is unchanged.
- SensorService on the emulator confirms the new gravity request is 10,000 µs with 0 µs batching, versus 20,000 µs before.
- Russian guide and calibration checked at 360 × 640 dp with 1.3× system font. The guide scrolls; sheet actions use a minimum height rather than clipping longer labels.
- Regression script: `python3 scripts/verify-emulator.py --serial emulator-5554`. On 1.0.0 it reproduces Journal → Settings → Zero waiting forever with an inactive sensor. On 1.0.1 the same script passes: fresh stable readings arrive and Set zero is enabled from the journal. The full script also passes: held angle stays fixed after a tilt, keyboard entry works, rapid double save creates one entry, the entry survives a process restart and deleting it requires confirmation. The calibration sheet and first-run guide were also visually inspected on the emulator.

Not verified on physical hardware for this update: sensor accuracy and latency on S25. No phone was connected during development. Emulator frame timings depend heavily on the host GPU/backend and are not S25 performance results.

## Previous release

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
