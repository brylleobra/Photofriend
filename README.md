# Photofriend

A companion app for Fujifilm camera photographers. Use your phone's camera as a live viewfinder to preview and experiment with film simulation recipes — then let AI suggest the optimal settings for any scene.

> **Current version:** v0.4.0 — pre-release / internal testing

---

## What It Does

Fujifilm cameras are beloved for their in-camera JPEG film simulations — Velvia, Classic Chrome, Eterna, and others. Dialling in the right recipe for a scene usually means taking test shots, reviewing them on the camera's screen, adjusting settings, and repeating. Photofriend moves that iteration loop to your phone.

- **Live preview with film simulation** — see approximately how your Fujifilm settings will look before you even raise the camera, using your phone's camera as a proxy viewfinder
- **AI scene analysis** — point at a scene, tap Analyze, and get a complete suggested recipe with reasoning
- **Recipe library** — browse 15 built-in recipes or save your own; share any recipe as formatted text

---

## Features

### Viewfinder
- Full-screen CameraX live preview with film simulation applied in real time via GPU colour matrix
- **Tap to focus** — AF/AE metering point set at the tapped position with an animated focus ring; auto-releases after 3 seconds
- **Film simulation overlay** — all 20 Fujifilm film simulations rendered as composable `ColorMatrix` operations: Provia/Standard, Velvia/Vivid, Astia/Soft, Classic Chrome, Pro Neg. Hi, Pro Neg. Std, Classic Neg., Nostalgic Neg., Eterna/Cinema, Eterna Bleach Bypass, Acros (+R/G/Ye), Monochrome (+R/G/Ye), Sepia, Reala Ace
- **Grain overlay** — animated at ~12 fps with weak/strong and small/large size variants
- **Aperture simulation** — choose an f-stop from f/1.0 to f/16; adjusts real exposure compensation on the sensor and adds a vignette overlay scaled to the aperture width
- **Photo capture** — shutter button applies the current colour matrix to the captured frame and saves to `DCIM/Photofriend` in the device gallery
- **Hardware settings** — Noise Reduction and Sharpness are applied at the Camera2 ISP level (not just a preview filter)

### Camera Settings
- Per-camera setting editor with 14 settings for the X-T30 III: Film Simulation, Grain Effect, Color Chrome Effect, Color Chrome FX Blue, White Balance, Highlight Tone, Shadow Tone, Color, Sharpness, Noise Reduction, Clarity, Aperture, Dynamic Range, ISO
- **Apply button** — changes are staged locally and only written to DataStore when Apply is tapped; unsaved changes are shown clearly
- **Reset** — reverts all settings for the selected camera to defaults
- Changes immediately update the live viewfinder effects

### AI Scene Analysis (Ollama)
- Captures the current frame, encodes it, and sends it to **Ollama Cloud** (`qwen3-vl:235b-cloud`) with a structured system prompt
- Returns a full recipe: film simulation, grain, white balance, tone, color, sharpness, noise reduction, clarity, ISO, and a creative recipe name with reasoning
- **Apply to Camera Settings** — writes the AI-suggested values directly into the settings store so the viewfinder updates immediately
- **Save as Recipe** — saves the AI suggestion to the local recipe library

### Recipe Library
- 15 built-in recipes across 8 categories: Colour/Slide, Documentary/Street, Golden Hour, Portrait, Cinematic, Black & White, Vintage, Travel
- User-saved recipes from AI suggestions
- Swipe to delete with 4-second undo
- Share any recipe as formatted plain text via the Android share sheet

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (domain / data / ui) |
| DI | Hilt |
| Database | Room (version 4) |
| Settings persistence | DataStore Preferences |
| Camera | CameraX + Camera2 interop |
| Networking | Retrofit + OkHttp |
| AI | Ollama Cloud — `qwen3-vl:235b-cloud` |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 15) |

---

## Project Structure

```
app/src/main/java/com/example/photofriend/
├── camera/
│   ├── BitmapUtils.kt          # Base64 encoding, colour matrix apply, gallery save
│   ├── CameraManager.kt        # CameraX lifecycle, capture, focus, exposure, NR/sharpness
│   ├── FilmLook.kt             # All film simulation → ColorMatrix definitions + aperture params
│   └── ViewfinderEffectParams.kt
├── data/
│   ├── local/                  # Room entities, DAOs, database, seed data
│   ├── mapper/                 # Entity ↔ domain model mappers
│   ├── remote/                 # Retrofit API service + DTOs (Ollama)
│   └── repository/             # CameraRepositoryImpl, AIRepositoryImpl
├── di/                         # Hilt modules + in-memory stores (AISuggestionStore, SettingsStore)
├── domain/
│   ├── model/                  # CameraModel, CameraSetting, FilmSimulationRecipe, AISuggestion
│   ├── repository/             # Repository interfaces
│   └── usecase/                # One class per use case
└── ui/
    ├── component/              # CameraPreview (viewfinder + grain + vignette + focus ring)
    ├── screen/
    │   ├── cameraselect/
    │   ├── viewfinder/
    │   ├── settings/
    │   ├── aisuggestion/
    │   ├── recipes/
    │   └── recipedetail/
    └── theme/
```

---

## Setup

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 36
- A physical Android device or emulator (API 24+) — camera features require a real device for best results
- An [Ollama Cloud](https://ollama.com) account and API key for AI features

### API Key
Add your Ollama Cloud key to `local.properties` (never commit this file):

```properties
ollama.api.key=your_key_here
```

The key is baked into `BuildConfig.OLLAMA_API_KEY` at compile time and attached as a `Bearer` token on every request.

### Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run lint
./gradlew lint
```

---

## Supported Cameras

Settings are currently seeded for the **Fujifilm X-T30 III** only. The following models are in the database but have no settings seeded yet:

- Fujifilm X-T5
- Fujifilm X-S20
- Fujifilm X100VI
- Fujifilm X-Pro3

---

## Known Limitations

| Limitation | Detail |
|---|---|
| **Viewfinder is an approximation** | The colour matrix is a linear model. Real Fujifilm film simulations involve complex tone curves and non-linear processing that cannot be perfectly replicated in a 4×5 matrix |
| **Aperture is simulated** | Phone lenses have a fixed physical aperture. The f-stop setting adjusts exposure compensation (real) and corner vignetting (overlay). Depth-of-field / bokeh is not simulated |
| **Settings only for X-T30 III** | Other supported camera models show an empty settings screen |
| **Grain is static noise** | Film grain in the overlay is random per-frame noise rather than the structured clumping of real analog grain |
| **AI requires internet** | Scene analysis calls Ollama Cloud; no on-device model is used |

---

## Roadmap

### Near-term
- [ ] Seed full settings for X-T5, X-S20, X100VI, and X-Pro3
- [ ] WB Shift R/B live preview in the viewfinder colour matrix
- [ ] Exposure compensation slider directly in the viewfinder (independent of aperture simulation)
- [ ] Histogram overlay on the live preview
- [ ] Before/after toggle — switch between the raw preview and the film simulation overlay with one tap

### Medium-term
- [ ] Side-by-side comparison — capture two shots with different recipes and compare them in-app
- [ ] Recipe import — parse a recipe shared as text and load it directly into settings
- [ ] On-device AI fallback — use a smaller quantised vision model (e.g. via llama.cpp or MediaPipe) so analysis works offline
- [ ] Better grain simulation — structured clumping and halation to more closely replicate analog film texture
- [ ] Tone curve editor — visual S-curve control for highlights and shadows instead of integer sliders

### Long-term
- [ ] Support for additional brands — Sony Creative Looks, Nikon Picture Controls, Canon Picture Styles
- [ ] Community recipe sharing — browse and import recipes shared by other users
- [ ] Export to camera — generate QR codes or files compatible with Fujifilm's recipe import format (X-T5, X100VI support this via a third-party transfer workflow)
- [ ] Video mode — apply film simulation to short video clips captured in-app
- [ ] Depth-of-field simulation — use the Portrait Mode depth API (where available) to apply realistic background blur scaled to the chosen f-stop

---

## Contributing

This project is currently in private testing. Contributions are not open yet, but feedback from test users is welcome via the issue tracker.

---

## License

Private — all rights reserved. Not yet open-sourced.
