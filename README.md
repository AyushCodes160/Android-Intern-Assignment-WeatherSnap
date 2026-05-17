# WeatherSnap

Small Android app that searches live weather for a city, captures a photo with a
custom CameraX screen, compresses it, attaches notes, and stores the report locally.

## Setup

1. Open the project root (`ANDROID_INTERN_ASSIGNMENT/`) in **Android Studio Hedgehog (or newer)**.
2. Let Gradle sync. The wrapper pins **Gradle 8.7** and **AGP 8.5.2** and will be
   downloaded automatically on first sync.
3. Required JDK: **17** (Android Studio bundles one — set it in
   *Settings → Build, Execution, Deployment → Build Tools → Gradle*).
4. Press **Run** on any Android 8.0+ device or emulator (`minSdk 26`, `targetSdk 34`).

No API key is required — weather comes from Open-Meteo's public endpoints.

To run from the CLI instead:

```bash
./gradlew assembleDebug
./gradlew installDebug   # device or emulator must be attached
```

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM · ViewModel + StateFlow · Coroutines ·
Hilt · Navigation Compose · Retrofit · Gson · OkHttp logging (debug-only) ·
Room · CameraX · Coil · kotlinx-serialization (nav-args + draft snapshots).

## Architecture

```
ui/      Composable screens + ViewModels (one per screen)
domain/  City, WeatherSnapshot, WeatherCode mapper
data/
  remote/      Retrofit DTOs + GeocodingApi/ForecastApi
  local/       Room entities, DAOs, WeatherSnapDatabase
  cache/       CitySuggestionCache (LRU, 64 entries)
  repository/  WeatherRepository, ReportRepository, DraftRepository
di/      NetworkModule, DatabaseModule (Hilt)
util/    ImageCompressor, TempFileSweeper
```

Strict MVVM: composables observe `StateFlow` from ViewModels via
`collectAsStateWithLifecycle()`. ViewModels never touch Compose, View, or the
database directly — only repositories. All I/O (Room, network, file work,
bitmap encode) runs on `Dispatchers.IO`.

### Caching

- `CitySuggestionCache` (`android.util.LruCache`, key = normalized query) prevents
  repeat geocoding hits for the same prefix.
- `OkHttp` is shared across both Retrofit instances and constructed once.

### Networking

Two Retrofit instances behind `@GeocodingClient` / `@ForecastClient` qualifiers,
both sharing the same `OkHttpClient`. `HttpLoggingInterceptor(BODY)` is added
**only when `BuildConfig.DEBUG`** — release builds carry no network logging.

### Image compression

`ImageCompressor` does a two-pass decode: it first reads the bounds to pick an
`inSampleSize`, then decodes the bitmap, applies EXIF rotation, downscales the
longest edge to 1600px, and writes a JPEG at quality 70 to
`cacheDir/captures/compressed_<uuid>.jpg`. Both the original and compressed byte
sizes are tracked and shown in the report card.

### Room

Two tables — `reports` (saved reports) and `report_drafts` (in-progress drafts).
DAOs expose `suspend` writes and `Flow<List<…>>` reads; the Flow is wrapped in
`flowOn(Dispatchers.IO)` at the repository boundary. Saving runs inside
`db.withTransaction { … }` so insert + draft-delete are atomic.

## Developer-judgment challenge — recovery without duplicates

The Create Report flow must survive rotation, backgrounding, and process death
without (a) losing the in-progress report and (b) ever duplicating a saved report.
The approach:

**1. Snapshot is frozen, not refetched.** When the user taps *Create Report*,
the `WeatherSnapshot` is `kotlinx-serialization`-encoded into JSON and passed
into the nav-arg. `CreateReportViewModel.init()` decodes it once and writes it
to a `report_drafts` row keyed by a UUID generated at navigation time. After
that, the report screen never re-fetches weather — the snapshot inside the
saved report is byte-identical to what the user originally saw.

**2. One draft, one UUID, one save.** The UUID created when *Create Report* is
tapped is reused for the draft row, the captured-image filename (indirectly via
the draft), and the eventual `ReportEntity.id` primary key. Re-entering the
flow (rotation, navigation pop, process restart) calls `getOrCreate(draftId, …)`,
which returns the existing row instead of creating a second one. Save uses
`@Insert(onConflict = REPLACE)` inside `db.withTransaction`, so even a retried
save can never produce two reports.

**3. Notes survive rotation and process death.** Notes are debounced (400 ms) and
persisted into the draft row from a `StateFlow.onEach` pipeline. On re-entering
the screen the notes are reloaded from the draft.

**4. The captured image survives rotation too.** When the camera screen returns,
it writes the file path into the report screen's nav `SavedStateHandle`. The
ViewModel observes that key as a `StateFlow` (so a second capture from the same
screen is also picked up) and consumes it idempotently: compress → write file
path back to the draft row → clear the savedStateHandle key. The image path
lives in the draft, so rotation can't lose it.

**5. Temp files are not allowed to leak.**
- The raw capture file is deleted as soon as compression succeeds.
- When the draft image is replaced, the previous draft-only file is deleted.
- On `WeatherSnapApp.onCreate`, `TempFileSweeper.sweep()` lists every file in
  `cacheDir/captures/` and deletes anything not referenced by either a saved
  report or an in-progress draft. This catches files orphaned by a process kill
  between capture and the compress callback.

**Tradeoffs.** The draft is in Room (not `SavedStateHandle` alone) because
`SavedStateHandle` doesn't survive a full process death after the activity has
been removed from recents. Using Room costs a single small write per
notes-debounce + one per capture, in exchange for full process-death recovery
without ever creating a duplicate. We deliberately do not surface a "you have
an in-progress report — resume?" dialog; the recovery is automatic and silent,
which matches the assignment's wording ("must still be recoverable without
creating duplicate saved reports").

## What is intentionally not included

No splash screen, login, onboarding, or settings page — per the spec.

## Bonus items present

- Unit test for `WeatherCode.describe` (`app/src/test`).
- Debug-only network logging (gated by `BuildConfig.DEBUG`).
- Room writes/reads run on `Dispatchers.IO` end-to-end.

## Screen recording

Record `device-2024…mp4` from Android Studio's *Logcat → Screen Record* showing:
city autocomplete → weather search → create report → custom camera → image
compression sizes (visible on the saved report card) → notes entry → save →
saved reports list.
