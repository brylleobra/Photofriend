# Product Requirements Document — Photofriend

**Product**: Photofriend
**Platform**: Android (Mobile)
**Version**: 1.0
**Status**: Draft
**Date**: 2026-03-05
**Owner**: Photofriend Product Team

---

## Table of Contents

1. [Vision & Goal](#1-vision--goal)
2. [Target Users](#2-target-users)
3. [Core User Stories](#3-core-user-stories)
4. [Feature List (MoSCoW)](#4-feature-list-moscow)
5. [AI Integration Points](#5-ai-integration-points)
6. [Supported Camera Models](#6-supported-camera-models)
7. [Film Simulation Recipes](#7-film-simulation-recipes)
8. [Success Metrics](#8-success-metrics)
9. [Out of Scope for v1](#9-out-of-scope-for-v1)

---

## 1. Vision & Goal

Photofriend exists to eliminate the frustration of fumbling through camera menus on location. Photographers who invest in dedicated cameras — particularly Fujifilm's renowned film-simulation ecosystem — frequently struggle to translate what they see with their eyes into optimal in-camera settings in the moment. Photofriend bridges that gap by turning the user's smartphone into an intelligent shooting companion: the user points their phone at the scene, selects their camera body, and receives AI-driven, scene-aware recommendations for exposure settings and film simulation recipes that they can dial directly into their dedicated camera. The goal for v1 is to make Fujifilm photographers of all skill levels more confident, more experimental, and more satisfied with their keeper rate — without requiring a laptop, a desktop preset tool, or prior deep knowledge of every camera parameter.

---

## 2. Target Users

### Persona 1 — The Curious Hobbyist

**Name**: Maya, 28
**Background**: Recently purchased her first Fujifilm camera (X-T30 II) after years of smartphone photography. She is drawn to the film simulation aesthetic but finds the menu system overwhelming. She shoots mostly street and travel.
**Goals**: Understand what settings to use, get consistent results that look like the film photos she admires online, and learn why certain settings work for certain scenes.
**Pain Points**: Does not know the difference between Highlight Tone +1 and +2. Has never touched Grain Effect. Frequently shoots in Auto and feels she is not getting the most from her camera.
**Tech Comfort**: High smartphone literacy; moderate app literacy; low dedicated-camera literacy.

### Persona 2 — The Weekend Enthusiast

**Name**: Kenji, 41
**Background**: Has owned several Fujifilm bodies over six years and currently shoots with an X-S20. Understands exposure fundamentals and has a handful of saved recipes from online communities (e.g., Fuji X Weekly). Shoots landscapes, portraits, and documentary family work.
**Goals**: Discover new recipes tailored to specific lighting conditions, rapidly iterate in the field without guessing, and have a reference tool when shooting in unfamiliar light.
**Pain Points**: Spends too much time post-processing when in-camera settings were not quite right. Wants suggestions that go beyond the standard "Velvia for landscapes" advice.
**Tech Comfort**: High across the board; happy to experiment with AI-assisted tools.

### Persona 3 — The Semi-Professional / Working Photographer

**Name**: Sofia, 35
**Background**: Uses a Fujifilm X-T5 and GFX 50S II professionally for editorial, product, and event work. Owns a large personal library of tested recipes and custom settings. Travels frequently.
**Goals**: A fast, reliable second opinion on settings in novel shooting environments; a way to brief assistants or second shooters on camera settings; a lightweight reference that does not require opening a laptop.
**Pain Points**: Lighting conditions at events are unpredictable. Existing recipe apps are static lookups with no scene awareness. Wants a tool that respects her existing knowledge rather than talking down to her.
**Tech Comfort**: Expert; values speed, accuracy, and minimal friction above all else.

---

## 3. Core User Stories

### 3.1 Loading / Selecting a Camera Model

**Story US-01: First-time camera selection**

> Given I have just installed Photofriend and opened it for the first time,
> When I am shown the camera selection screen,
> Then I can browse a list of supported Fujifilm camera models organized by series (X-T, X-S, X100, GFX),
> And I can select my camera body,
> And my selection is persisted so I do not have to re-select it on subsequent launches.

**Story US-02: Changing camera model**

> Given I have previously selected a camera model,
> When I navigate to Settings or tap a "Change Camera" affordance,
> Then I can search or browse for a different model,
> And selecting a new model immediately updates all settings references and AI suggestions throughout the app to reflect the capabilities of the newly selected body.

**Story US-03: Camera model detail confirmation**

> Given I have selected a camera model,
> When I view the camera detail screen,
> Then I see the model name, sensor size, and a summary of its supported film simulations and key settings ranges,
> So that I can confirm I have selected the correct body before proceeding.

---

### 3.2 Browsing Camera Settings for That Model

**Story US-04: Viewing all available settings**

> Given I have selected a camera model,
> When I navigate to the Settings Reference section,
> Then I see a complete, scrollable list of all user-adjustable parameters supported by that body (e.g., Film Simulation, Grain, Color Chrome Effect, White Balance, Highlight Tone, Shadow Tone, Color, Sharpness, Noise Reduction, Dynamic Range, ISO range, aperture range, shutter speed range),
> And each parameter displays its valid value range or option list specific to that model.

**Story US-05: Parameter detail view**

> Given I am viewing the settings list for my selected camera,
> When I tap on an individual parameter (e.g., Grain Effect),
> Then I see a description of what that parameter does, its full value range, and a visual or textual example of its effect,
> So that I can understand the parameter before dialing it in.

**Story US-06: Settings search**

> Given I am on the Settings Reference screen,
> When I type a query into the search field (e.g., "white balance"),
> Then the list filters in real time to show only matching parameters,
> So I can quickly locate a specific setting without scrolling.

---

### 3.3 Using the Phone Camera + AI to Get Setting Suggestions

**Story US-07: Launching the AI viewfinder**

> Given I have selected a camera model,
> When I tap the "Suggest Settings" button on the home screen,
> Then the app requests camera permission if not already granted,
> And opens a live viewfinder using the phone's rear camera,
> And displays a real-time analysis overlay once the scene has been evaluated.

**Story US-08: Receiving AI exposure suggestions**

> Given the AI viewfinder is active and a scene is visible,
> When I tap the "Analyze" button or the analysis completes automatically,
> Then the app displays a recommended set of exposure parameters (ISO, aperture, shutter speed, white balance) for my selected camera body,
> And each suggestion includes a brief rationale (e.g., "Low ambient light — ISO 1600 recommended to maintain 1/125s handheld"),
> And the values are constrained to the valid ranges of my selected camera model.

**Story US-09: Refreshing suggestions as conditions change**

> Given I am viewing AI-generated suggestions in the viewfinder,
> When I move to a new location or lighting changes significantly,
> Then I can tap "Re-analyze" to obtain fresh suggestions based on the current frame,
> And the previous suggestions are replaced with the new ones.

**Story US-10: Copying suggestions for reference**

> Given I am viewing AI-generated setting suggestions,
> When I tap "Copy" or "Share",
> Then the suggestion set is copied to my clipboard or shared as plain text (e.g., "ISO 400 | f/5.6 | 1/250s | WB: Daylight 5500K | Film Sim: Classic Chrome"),
> So I can paste it into a notes app or message it to a second shooter.

---

### 3.4 Getting Film Simulation Recipe Suggestions

**Story US-11: AI-driven recipe suggestion**

> Given the AI viewfinder has analyzed a scene,
> When the analysis is complete,
> Then the app suggests one or more film simulation recipes tailored to the scene's detected mood, color palette, and subject type,
> And each recipe includes all relevant parameters (Film Simulation, Grain Effect, Color Chrome, Color Chrome FX Blue, White Balance shift, Highlights, Shadows, Color, Sharpness, Noise Reduction),
> And each recipe is labeled with a descriptive name and a one-sentence rationale (e.g., "Golden Hour Velvia — enhances warm saturated tones for sunset scenes").

**Story US-12: Browsing multiple recipe suggestions**

> Given the AI has returned two or more recipe suggestions,
> When I swipe between recipe cards,
> Then each card shows the full parameter set for that recipe,
> And I can visually compare recipes side by side or in sequence without leaving the screen.

**Story US-13: Viewing recipe details**

> Given I am viewing a recipe suggestion card,
> When I tap "Details",
> Then I see every parameter in the recipe with its recommended value and a brief explanation of the creative intent behind that value,
> So I can understand how to replicate or adapt the recipe.

---

### 3.5 Saving / Favoriting Recipes

**Story US-14: Saving a recipe**

> Given I am viewing an AI-suggested or manually browsed recipe,
> When I tap the "Save" or heart icon,
> Then the recipe is added to my Saved Recipes library,
> And a confirmation toast or snackbar is shown,
> And the recipe persists across app restarts.

**Story US-15: Viewing saved recipes**

> Given I have saved one or more recipes,
> When I navigate to the "My Recipes" tab,
> Then I see a list of all saved recipes sorted by most recently saved,
> And each entry displays the recipe name, the film simulation used, and the date saved.

**Story US-16: Editing a saved recipe**

> Given I am viewing a saved recipe,
> When I tap "Edit",
> Then I can modify any parameter value within the valid range for my selected camera model,
> And save the edited version either as a new recipe (with a custom name) or as an update to the existing entry.

**Story US-17: Deleting a saved recipe**

> Given I am viewing my Saved Recipes list,
> When I long-press a recipe and select "Delete", or swipe-to-dismiss,
> Then the recipe is removed from my library after a confirmation prompt,
> And an "Undo" snackbar is offered for five seconds to allow accidental deletion recovery.

**Story US-18: Tagging and organizing saved recipes**

> Given I have multiple saved recipes,
> When I edit a recipe's metadata,
> Then I can assign one or more user-defined tags (e.g., "Street", "Portrait", "Golden Hour"),
> And I can filter my Saved Recipes list by tag.

---

## 4. Feature List (MoSCoW)

### Must Have (v1 Launch Blockers)

| ID | Feature | Notes |
|----|---------|-------|
| F-01 | Camera model selection and persistence | Covers US-01, US-02, US-03 |
| F-02 | Per-model settings reference (full parameter list with valid ranges) | Covers US-04, US-05 |
| F-03 | Live phone camera viewfinder | Requires CameraX integration; covers US-07 |
| F-04 | AI scene analysis (brightness, subject, mood, color palette) | Core differentiator; covers US-08 |
| F-05 | AI-generated exposure suggestions constrained to selected camera model | Covers US-08, US-09 |
| F-06 | AI-generated film simulation recipe suggestions | Covers US-11, US-12 |
| F-07 | Save / favorite a recipe to local storage | Covers US-14, US-15 |
| F-08 | View and browse saved recipes | Covers US-15 |
| F-09 | Camera permission handling (request, rationale, denial graceful handling) | Legal and UX requirement |
| F-10 | Support initial set of Fujifilm camera models (see Section 6) | Minimum viable model database |

### Should Have (High Value, Ship If Feasible)

| ID | Feature | Notes |
|----|---------|-------|
| F-11 | Settings search / filter on the settings reference screen | Covers US-06 |
| F-12 | Parameter detail / education view | Covers US-05 |
| F-13 | Copy / share suggestions as plain text | Covers US-10 |
| F-14 | Edit saved recipes with validation against camera model ranges | Covers US-16 |
| F-15 | Delete saved recipes with undo | Covers US-17 |
| F-16 | Multiple recipe cards per analysis with swipe navigation | Covers US-12 |
| F-17 | Recipe detail view with per-parameter rationale | Covers US-13 |
| F-18 | Descriptive AI rationale text alongside every suggestion | UX polish; increases trust |
| F-19 | Re-analyze on demand (manual trigger) | Covers US-09 |

### Could Have (Nice-to-Have for Future Sprints)

| ID | Feature | Notes |
|----|---------|-------|
| F-20 | Recipe tagging and tag-based filtering | Covers US-18 |
| F-21 | Recipe naming (custom user-defined names) | Covers US-16 |
| F-22 | Dark mode support | Theme already supports it via Material You |
| F-23 | Onboarding / tutorial flow for first-time users | Reduces activation friction |
| F-24 | Community-sourced recipe browser (read-only, curated) | Requires backend |
| F-25 | Export recipes as QR code or structured text for camera import | Fujifilm does not support programmatic import; informational only |
| F-26 | Shooting mode suggestions (e.g., Aperture Priority vs Manual) | Extends F-05 |
| F-27 | Scene type label displayed to user (e.g., "Low-light interior", "Bright outdoor portrait") | Transparency in AI reasoning |
| F-28 | Confidence indicator on AI suggestions | Transparency in AI reasoning |
| F-29 | Haptic feedback on analysis complete | Polish |

### Won't Have in v1

| ID | Feature | Reason |
|----|---------|--------|
| F-30 | Direct camera control / tethering via USB or WiFi | Requires proprietary Fujifilm SDK; out of scope |
| F-31 | In-app image capture or photo library editing | Photofriend is a reference/advisory tool, not a camera replacement |
| F-32 | Non-Fujifilm camera support (Sony, Nikon, Canon) | Intentionally narrow focus for v1 to ship a quality experience |
| F-33 | Cloud sync of saved recipes | Privacy and infrastructure cost; defer to v2 |
| F-34 | Social sharing / community features | Requires backend, moderation, and significant scope |
| F-35 | Video / cine settings suggestions | Different parameter space; defer to v2 |
| F-36 | RAW processing or LUT export | Out of scope for an advisory tool |
| F-37 | Paid subscription or in-app purchase | Monetization strategy not finalized for v1 |

---

## 5. AI Integration Points

### 5.1 Input: What the AI Analyzes

The AI pipeline receives a still frame (captured from the phone's live viewfinder) and extracts the following scene signals:

| Signal Category | Specific Inputs | Analysis Method |
|-----------------|----------------|-----------------|
| **Luminance / Exposure** | Overall scene brightness (EV estimate), highlight clipping risk, shadow detail, dynamic range estimate | Histogram analysis + luminance map |
| **Color Palette** | Dominant hue(s), color temperature estimate (warm/cool/neutral), color saturation level, presence of specific color casts | Color clustering + color temperature estimation |
| **Subject / Scene Type** | Detected subject category (person, landscape, architecture, food, flora, low-light, etc.) | On-device image classification model |
| **Mood / Atmosphere** | Inferred photographic mood (moody/dark, bright/airy, golden/warm, high-contrast, muted/cinematic) | Composite signal derived from luminance + palette + subject |
| **Depth / Bokeh Potential** | Foreground/background separation, subject distance estimate | Depth estimation or saliency map |
| **Motion Indicators** | Presence of motion blur in live frame (suggests subject or camera motion) | Frame-difference analysis |

### 5.2 Output: What the AI Recommends

**Exposure Parameters** (constrained to the valid ranges of the selected camera model):

| Parameter | Output Format | Example |
|-----------|--------------|---------|
| ISO | Specific ISO value from model's supported range | ISO 800 |
| Aperture | f-stop value within model's lens range (if known) | f/4.0 |
| Shutter Speed | Fractional or decimal seconds | 1/250s |
| White Balance | Preset name or Kelvin value | Daylight (5500K) |
| White Balance Shift | R/B axis shift values (-9 to +9) | R: +1, B: -2 |
| Dynamic Range Setting | DR100 / DR200 / DR400 (model-dependent) | DR200 |

**Film Simulation Recipe** (one or more per analysis):

| Parameter | Output Format | Example |
|-----------|--------------|---------|
| Film Simulation | Named simulation (model-dependent availability) | Classic Chrome |
| Grain Effect | Rough/Smooth + Size (Small/Large) or Off | Weak, Small |
| Color Chrome Effect | Off / Weak / Strong | Strong |
| Color Chrome FX Blue | Off / Weak / Strong (X-T4+ bodies) | Weak |
| White Balance | Preset or Kelvin | Cloudy |
| White Balance Shift | R/B values | R: +3, B: +1 |
| Highlight Tone | -2 to +4 (model-dependent range) | -1 |
| Shadow Tone | -2 to +4 (model-dependent range) | +1 |
| Color | -4 to +4 | -1 |
| Sharpness | -4 to +4 | 0 |
| Noise Reduction | -4 to +4 | -2 |
| Clarity | -5 to +5 (newer bodies only) | 0 |

**Rationale Text**: Each output block is accompanied by a 1–2 sentence natural-language explanation of why these settings were chosen, referencing the detected scene signals (e.g., "The scene has strong warm afternoon light and high dynamic range — DR200 is recommended to protect highlights, with Shadow Tone +1 to retain detail in the darker foreground.").

### 5.3 AI Architecture Notes

- **On-device inference preferred** for scene classification and luminance analysis to avoid latency and preserve user privacy. Consider TensorFlow Lite or ML Kit.
- **Cloud LLM call** (e.g., Claude API) for generating natural-language rationale text and assembling the final recipe output from structured scene signals. The structured signals are sent as a compact JSON payload; the LLM returns a structured recipe + rationale.
- **Model-aware constraint layer**: A local rules engine validates all AI-output parameter values against the selected camera model's known capabilities before displaying them to the user. This ensures correctness regardless of LLM hallucination risk.
- **Graceful degradation**: If the AI call fails or times out, the app falls back to a deterministic rule-based suggestion (scene brightness → ISO lookup table, etc.) rather than showing an error.

---

## 6. Supported Camera Models

The following Fujifilm camera bodies are targeted for the v1 model database. Each entry in the database stores: sensor size, available film simulations, parameter ranges, and notable feature flags (e.g., Clarity control, Color Chrome FX Blue availability).

### X-T Series (APS-C, Interchangeable Lens)

| Model | Sensor | Key Notes |
|-------|--------|-----------|
| X-T5 | 40.2 MP BSI CMOS | Latest X-T; Clarity, Color Chrome FX Blue, all current film sims |
| X-T4 | 26.1 MP BSI CMOS | IBIS; first body with Color Chrome FX Blue |
| X-T30 II | 26.1 MP BSI CMOS | No IBIS; compact; popular entry enthusiast body |
| X-T30 | 26.1 MP BSI CMOS | Predecessor to X-T30 II; slight sim differences |
| X-T20 | 24.3 MP CMOS | Older; missing newer sims (Eterna, Reala Ace) |

### X-S Series (APS-C, Interchangeable Lens, IBIS-focused)

| Model | Sensor | Key Notes |
|-------|--------|-----------|
| X-S20 | 26.1 MP BSI CMOS | Current X-S flagship; vlog-oriented; full modern sim set |
| X-S10 | 26.1 MP BSI CMOS | First X-S with IBIS; Eterna Cinema but no Reala Ace |

### X100 Series (APS-C, Fixed Lens)

| Model | Sensor | Key Notes |
|-------|--------|-----------|
| X100VI | 40.2 MP BSI CMOS | Latest; IBIS; full sim set including Reala Ace |
| X100V | 26.1 MP BSI CMOS | Weather-resistant; Color Chrome FX Blue; extremely popular |
| X100F | 24.3 MP CMOS | Acros simulation; no Color Chrome Effect |

### X-E Series (APS-C, Interchangeable Lens, Rangefinder-style)

| Model | Sensor | Key Notes |
|-------|--------|-----------|
| X-E4 | 26.1 MP BSI CMOS | Minimalist body; full modern sim set |

### X-Pro Series (APS-C, Interchangeable Lens, Hybrid OVF)

| Model | Sensor | Key Notes |
|-------|--------|-----------|
| X-Pro3 | 26.1 MP BSI CMOS | Hidden rear LCD; Eterna Cinema; Color Chrome FX Blue |

### GFX Series (Medium Format, Interchangeable Lens)

| Model | Sensor | Key Notes |
|-------|--------|-----------|
| GFX100S II | 102 MP BSI CMOS | Flagship medium format; IBIS; full sim set |
| GFX50S II | 51.4 MP BSI CMOS | More accessible medium format entry point |

> **Model database design note**: The database should be structured so that new models can be added via a data-only update (e.g., a JSON asset or remote config) without requiring an app update. Each model entry specifies its film simulation list by reference to a global simulation catalog, with an availability flag per model.

---

## 7. Film Simulation Recipes

### 7.1 What They Are

Film simulation recipes are sets of in-camera parameter values that, when applied together, emulate the look of a specific analog film stock or achieve a distinct photographic aesthetic. Fujifilm cameras expose a rich set of image processing parameters beyond the base film simulation, allowing photographers to fine-tune grain, color, contrast, and sharpness in ways that closely approximate beloved film stocks or create entirely original looks. The recipes are applied entirely in-camera at capture time: the resulting JPEG (or processed RAW) comes out of the camera already "developed," much like choosing a specific film roll and developing process in the analog era.

The recipe community around Fujifilm — exemplified by resources such as Fuji X Weekly — has grown large because these parameter combinations are highly reproducible, shareable as plain text, and instantly applicable. Photofriend treats recipes as a first-class object: they are suggested by AI, saved by users, and presented with enough contextual detail to be dialed into any compatible camera body within seconds.

### 7.2 Key Recipe Parameters

| Parameter | Description | Typical Range | Notes |
|-----------|-------------|---------------|-------|
| **Film Simulation** | The base look profile applied to all tones and colors. Each simulation models a specific analog film or Fujifilm original look. | Model-dependent (see catalog below) | The most impactful single parameter; all other settings build on top of this. |
| **Grain Effect** | Adds analog-style luminance grain. Controls both intensity (Off / Weak / Strong) and grain size (Small / Large). | Off; Weak Small; Weak Large; Strong Small; Strong Large | Older bodies support only Off/Weak/Strong without size control. |
| **Color Chrome Effect** | Increases color depth and gradation in highly saturated areas without blowing them out. Particularly effective on reds, oranges, and greens. | Off / Weak / Strong | Available from X-T3 / X-T30 era onward. |
| **Color Chrome FX Blue** | Specifically deepens blues and cyans, preventing sky highlights from washing out. | Off / Weak / Strong | Available from X-T4 / X100V era onward. |
| **White Balance** | Sets the color temperature reference point. Can be a named preset or a specific Kelvin value. | Auto; Daylight (5500K); Cloudy (6000K); Shade (7500K); Fluorescent 1–3; Incandescent (3000K); Kelvin 2500–10000K | WB shift (R/B axes) applies an additional color cast on top of the base WB. |
| **White Balance Shift** | Fine-tunes the WB along red-blue and magenta-green axes. | R: -9 to +9; B: -9 to +9 | Used to dial in precise color casts (e.g., warm orange cast for a vintage look). |
| **Highlight Tone** | Controls the brightness and contrast of highlight regions. Positive values brighten highlights; negative values compress them (protecting detail). | -2 to +4 (some bodies -2 to +2) | Negative values give a flatter, more film-like highlight roll-off. |
| **Shadow Tone** | Controls the brightness and contrast of shadow regions. Positive values deepen shadows; negative values lift them (reducing contrast). | -2 to +4 (some bodies -2 to +2) | Positive shadow tone adds drama and depth; negative lifts for a faded/matte look. |
| **Color** | Global color saturation adjustment. | -4 to +4 | 0 is the simulation default. Negative values produce muted/desaturated results; positive values intensify colors. |
| **Sharpness** | In-camera sharpening / detail enhancement. | -4 to +4 | Most recipe authors set this to 0 or -1 for a more organic look. |
| **Noise Reduction** | In-camera luminance noise smoothing. | -4 to +4 | Most recipe authors set this to -4 or -3 to preserve grain texture and avoid plasticky smoothing. |
| **Clarity** | Mid-tone contrast / micro-contrast enhancement. Differs from Sharpness in that it affects tonal separation rather than edge acuity. | -5 to +5 | Available on newer bodies only (X-T4, X-T5, X100VI, X-S20, GFX100S II, etc.). |
| **Dynamic Range** | In-camera highlight recovery. DR100 = no expansion; DR200 = ~1 stop highlight recovery; DR400 = ~2 stops. | DR100 / DR200 / DR400 | DR200 requires ISO ≥ 400; DR400 requires ISO ≥ 800. |

### 7.3 Film Simulation Catalog (All Current Fujifilm Simulations)

| Simulation | Character | Availability |
|-----------|-----------|-------------|
| Provia / Standard | Balanced, versatile, natural color | All modern Fujifilm bodies |
| Velvia / Vivid | Highly saturated, punchy, high contrast | All modern Fujifilm bodies |
| Astia / Soft | Soft skin tones, lower contrast, pastel palette | All modern Fujifilm bodies |
| Classic Chrome | Muted, faded, desaturated; inspired by Kodachrome slide film | X-T1 era onward |
| PRO Neg. Hi | Higher contrast portrait film; punchy but controlled skin | Most X-series bodies |
| PRO Neg. Std | Lower contrast portrait film; flat, controlled | Most X-series bodies |
| Acros | Black-and-white simulation with exceptional tonal gradation; sub-variants: Acros+Y, Acros+R, Acros+G | X-Pro2 era onward |
| Monochrome | Standard black-and-white conversion; sub-variants: +Y, +R, +G | All modern Fujifilm bodies |
| Sepia | Warm-toned black-and-white / sepia tone | All modern Fujifilm bodies |
| Eterna / Cinema | Low saturation, low contrast, wide dynamic range; cinematic flat look | X-T3 / X-H1 era onward |
| Eterna Bleach Bypass | Desaturated, high-contrast, silver-retention look | X-T4 era onward |
| Classic Neg. | Heavily muted greens and blues; warm, faded look; inspired by Superia print film | X-Pro3 era onward |
| Nostalgic Neg. | Warm, low-contrast, faded; inspired by 1970s American print film (Kodacolor analogue) | X-T5 / X-H2 era onward |
| Reala Ace | Natural, accurate skin tones, high detail; Fujifilm's most realistic simulation | X-T5 / X100VI / X-H2S era onward |

---

## 8. Success Metrics

### 8.1 Acquisition

| KPI | Target (90 days post-launch) | Measurement |
|-----|------------------------------|-------------|
| Total installs | 10,000 | Google Play Console |
| Day-1 retention | ≥ 40% | Firebase Analytics |
| Day-7 retention | ≥ 20% | Firebase Analytics |
| Organic search rank for "Fujifilm recipe app" | Top 5 in Google Play | Play Store ranking tracker |

### 8.2 Engagement

| KPI | Target | Measurement |
|-----|--------|-------------|
| AI analyses per DAU | ≥ 3 | Custom event: `ai_analysis_triggered` |
| Recipes saved per user (median, users with ≥ 1 session) | ≥ 2 within first week | Custom event: `recipe_saved` |
| Session length (median) | ≥ 4 minutes | Firebase Analytics |
| % of users who use viewfinder ≥ 3 times in first week | ≥ 30% | Custom event: `viewfinder_opened` |
| Camera model selection completion rate | ≥ 90% of new users | Funnel analysis: `camera_selected` |

### 8.3 Quality

| KPI | Target | Measurement |
|-----|--------|-------------|
| AI suggestion acceptance rate (user does not immediately re-analyze) | ≥ 60% | Custom event: `suggestion_accepted` vs `reanalyze_tapped` |
| App crash-free session rate | ≥ 99.5% | Firebase Crashlytics |
| AI analysis latency (p95, end-to-end) | < 4 seconds | Custom performance trace |
| Play Store rating (after 100+ reviews) | ≥ 4.3 stars | Google Play Console |

### 8.4 Business

| KPI | Target | Measurement |
|-----|--------|-------------|
| Monthly Active Users (MAU) at month 3 | ≥ 3,000 | Firebase Analytics |
| Net Promoter Score (in-app survey, n ≥ 200) | ≥ 40 | In-app survey tool |
| Feature request volume for v2 priorities | Qualitative signal | App store reviews + in-app feedback |

---

## 9. Out of Scope for v1

The following items have been explicitly deferred to v2 or later, or excluded entirely. They are documented here to prevent scope creep and to communicate clear boundaries to engineering, design, and stakeholders.

| Item | Rationale |
|------|-----------|
| **Non-Fujifilm camera brands** (Sony, Canon, Nikon, OM System, Leica, etc.) | v1 is intentionally scoped to Fujifilm to deliver a deep, accurate, well-tested experience. Expanding to other brands requires significant parameter research and testing for each body family. |
| **Direct camera connectivity** (USB tethering, WiFi remote control, Fujifilm Camera Remote integration) | Requires Fujifilm's proprietary connectivity SDK and is a significantly different product scope. |
| **In-app photo capture and gallery** | Photofriend is an advisory tool. Users take photos on their dedicated camera, not in the app. |
| **RAW file processing, LUT export, or desktop integration** | Post-processing workflow tools are a separate product category. |
| **Video / cine settings suggestions** | Cine shooting involves shutter angle, frame rate, and log profiles — a distinct parameter space requiring separate AI training and UX. Defer to v2. |
| **Cloud sync and multi-device recipe library** | Requires backend infrastructure, authentication, and privacy engineering. Local-only storage is sufficient for v1. |
| **Social features** (sharing recipes publicly, following other users, community feed) | Requires backend, moderation, trust & safety, and significant product design. Out of scope for v1. |
| **Subscription / monetization** | Monetization strategy to be determined post-launch based on retention and engagement data. |
| **Localization / internationalization** | English-only for v1. Localization to Japanese, French, German, Spanish, and Portuguese planned for v2 based on download geography. |
| **Accessibility audit** | Basic Compose accessibility defaults apply, but a full WCAG 2.1 AA audit and remediation is deferred to v1.1. |
| **Tablet / large-screen layout optimization** | Phone form factor is the primary target. Tablet layout improvements deferred to v1.1. |
| **Offline AI suggestions** (fully on-device LLM) | On-device LLMs of sufficient quality for recipe generation are not yet practical on mid-range Android hardware. Cloud fallback with on-device graceful degradation is the v1 architecture. |

---

*Document ends. For questions or change requests, open a discussion in the project repository or contact the Product Owner.*
