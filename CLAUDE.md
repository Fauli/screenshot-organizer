# Screenshot Vault (Android / Kotlin)

You are Claude Code acting as a senior Android engineer. Implement this project incrementally with clean, production-quality Kotlin code, strong separation of concerns, and testable architecture.

If anything is ambiguous, choose the simplest reasonable default that supports the MVP and document the assumption in the relevant file.

---

## 0) Project Summary

**Goal:** An Android app that ingests screenshots from a user-selected folder (SAF), extracts an “essence” (title, summary bullets, topics, entities, etc.) using OCR + optional cloud Vision AI, and presents them in a timeline feed with search, topics, and “Solved” hiding.

**MVP priorities:**
1. SAF folder selection + persisted permissions
2. Scan & store screenshot items (dedup)
3. Timeline feed (paging)
4. Background processing pipeline (WorkManager)
5. Essence extraction (start with OCR-only; then add cloud adapter)
6. Search (Room FTS5)
7. Solved toggle (hidden by default)

---

## 1) Tech Stack (Must Use)

- Kotlin
- Jetpack Compose (Material 3)
- Navigation Compose
- Room + FTS5
- DataStore (Preferences)
- WorkManager
- Paging 3
- Coil (images)
- Coroutines + Flow
- Hilt (DI)
- kotlinx.serialization (strict JSON)
- ML Kit Text Recognition (OCR) as baseline extraction

**Cloud AI:** implement as a clean adapter interface. App talks to a backend via HTTPS (Ktor/Retrofit). Do not embed API keys in the app.

---

## 2) Codebase Principles

### 2.1 Quality Bar
- Small, composable functions
- Clear naming; avoid cleverness
- Strict null-safety; handle errors explicitly
- No UI logic in repositories
- Side effects only in UseCases / Workers / Repos
- UI uses state holders (ViewModels) with immutable UI state
- Keep the app usable without cloud mode (OCR-only fallback)

### 2.2 Default Architecture
Use **MVVM + Repository + UseCases**.

Recommended packages:

```

app/
core/
di/
logging/
time/
result/
util/
data/
db/
entities/
dao/
fts/
migrations/
prefs/
repository/
model/
domain/
model/
usecase/
ai/
ingest/
scanner/
hashing/
workers/
ui/
navigation/
theme/
screens/
feed/
detail/
topics/
search/
settings/

````

---

## 3) Data Model (Canonical)

### 3.1 Entities (Room)

**ScreenshotItemEntity**
- id: String (UUID)
- contentUri: String (unique)
- sha256: String (unique)
- displayName: String
- mimeType: String
- sizeBytes: Long
- width: Int
- height: Int
- capturedAt: Long (epoch millis; best effort)
- ingestedAt: Long
- status: ProcessingStatus (NEW/PROCESSING/DONE/FAILED)
- errorMessage: String?
- solved: Boolean
- domain: String?
- type: String?
- ocrText: String? (optional but useful for search)

**EssenceEntity** (1:1)
- screenshotId: String (FK)
- title: String
- summaryBulletsJson: String
- topicsJson: String
- entitiesJson: String
- suggestedAction: String?
- confidence: Float
- modelName: String
- createdAt: Long

**TagEntity** + ScreenshotTagCrossRef (optional for MVP; topics may be enough)
- Keep “topics” as AI strings first; add manual tags later.

### 3.2 Search (FTS5)
Create an FTS table `search_index` containing:
- title, summary, entities, topics, ocr_text, domain, type

Update index after essence processing and after user edits (later).

---

## 4) AI Extraction: Contract First

### 4.1 Domain Model
Create a `domain.model.Essence`:

- title: String
- type: ContentType (enum)
- domain: String?
- summaryBullets: List<String>
- topics: List<String>
- entities: List<EntityRef>
- suggestedAction: SuggestedAction
- confidence: Float

### 4.2 Extractor Interface
In `domain.ai`:

```kotlin
interface EssenceExtractor {
  suspend fun extract(input: ExtractionInput): ExtractionResult
}

data class ExtractionInput(
  val screenshotId: String,
  val imageBytes: ByteArray,
  val ocrText: String?,
)

sealed interface ExtractionResult {
  data class Success(val essence: Essence, val modelName: String): ExtractionResult
  data class Failure(val reason: String, val retryable: Boolean): ExtractionResult
}
````

Implementations:

* `MlKitOcrEssenceExtractor` (baseline)
* `CloudVisionEssenceExtractor` (later; uses backend)

### 4.3 JSON Schema for Cloud

Cloud extractor returns strict JSON. Validate with kotlinx.serialization. On parse error: treat as retryable false (unless network).

---

## 5) Background Jobs

### 5.1 Workers

**ScanScreenshotsWorker**

* Lists folder files via SAF document tree
* For each item: compute sha256 and insert if new
* Mark rows NEW

**ProcessScreenshotsWorker**

* Fetch batch of NEW items (limit e.g. 10)
* Mark PROCESSING
* Run OCR + extraction
* Save Essence + set DONE
* Update FTS index
* On failure: set FAILED with message; allow retry

### 5.2 Scheduling

* Periodic scan: e.g. every 12 hours (configurable later)
* One-time processing chain triggered after scan and on app open
* Respect constraints: Wi-Fi only / charging only toggles later (Settings)

---

## 6) UI Requirements

### 6.1 Screens (MVP)

* **FeedScreen**: timeline list (Paging), solved hidden by default
* **DetailScreen**: essence + button to open original screenshot full-screen
* **SearchScreen**: query + results; filters minimal
* **TopicsScreen**: list topics + counts; tap topic shows filtered feed
* **SettingsScreen**: folder selection + AI mode toggle + rescan button

### 6.2 Compose Rules

* No DB calls inside Composables
* ViewModel exposes `StateFlow<UiState>`
* Use `collectAsStateWithLifecycle`
* Use Paging Compose for lists
* Stable keys for LazyColumn items

---

## 7) Preferences & Settings

Store in DataStore:

* selectedFolderUri: String?
* aiMode: enum { OCR_ONLY, CLOUD }
* hideSolvedByDefault: Boolean (true)
* lastScanAt: Long

Do not store secrets.

---

## 8) Networking (Cloud Mode)

* Prefer **Retrofit + OkHttp** (simple) OR Ktor client (also fine).

* Endpoints:

  * `POST /extract` with image (base64 or multipart) + ocrText + metadata
  * returns Essence JSON

* Add timeouts, retry policy for network failures.

* Keep payload small: downscale before upload.

---

## 9) Error Handling Expectations

* Missing URI permission → show a clear UI state + “Re-select folder”
* File removed → mark item FAILED with reason “file missing”
* Worker exceptions must not crash app; always persist state
* Extraction parse errors should be visible in debug logs and stored in `errorMessage`
* Add “Retry” action in Detail screen for FAILED items (post-MVP ok)

---

## 10) Testing Strategy (Minimum)

* Unit tests:

  * hashing/dedup
  * date extraction helper
  * JSON parsing for cloud essence
  * repository logic (Room in-memory)
* Instrumented tests (later):

  * basic navigation
  * Room migrations

Keep tests lean but meaningful.

---

## 11) Implementation Order (Follow This)

### Phase 1 — Storage + DB + Feed Skeleton

1. Create Room DB + entities + DAO
2. SAF folder picker + persist permission + store in DataStore
3. Minimal scanner (manual “Scan now” button) inserts items
4. Feed screen shows items with thumbnails (no essence yet)

### Phase 2 — Background + OCR Extraction

1. Add WorkManager workers
2. Integrate ML Kit OCR
3. Implement `MlKitOcrEssenceExtractor`:

   * domain from OCR (regex)
   * title heuristic (largest-looking line fallback: first non-empty)
   * topics: keyword extraction from OCR (simple frequency; exclude stopwords)
   * summary bullets: first 2–3 meaningful lines
4. Show essence on feed cards

### Phase 3 — Search + Topics + Solved

1. FTS table and query APIs
2. Search UI
3. Topics aggregation query
4. Solved flag toggle + hide by default

### Phase 4 — Cloud Adapter (Optional)

1. Add `CloudVisionEssenceExtractor` + backend client
2. Switch extraction by aiMode
3. Add settings toggle and clear UX messaging

---

## 12) Style & Conventions

* Kotlin: 4-space indent, trailing commas enabled
* Prefer immutable data classes
* Use `Result`-like sealed types (avoid throwing across layers)
* Do not introduce extra frameworks unless necessary
* Keep build.gradle dependencies minimal and explain additions in PR-style notes

---

## 13) Deliverables Per Change

When implementing, always provide:

* New/changed files list
* Brief reasoning for architecture choices
* Any TODOs and why they remain
* How to run/build
* Any migrations required

---

## 14) Guardrails (Do NOT Do)

* Don’t request broad storage permissions if SAF works.
* Don’t block the UI thread with file IO or hashing.
* Don’t put API keys in the app.
* Don’t make the AI pipeline mandatory for usability.
* Don’t “over-engineer” clustering; keep MVP simple.

---

## 15) “Definition of Done” for MVP

* User selects folder; app persists it.
* App scans and shows screenshots in a timeline.
* Each item gets an essence (OCR-only at minimum).
* Search works across essence fields.
* Topic browsing works.
* Solved items are hidden by default but recoverable.
* Original screenshot view works.

---

