# “Screenshot Vault” (working title)

An Android app that turns forgotten screenshots into a searchable, organized knowledge stream by extracting the “essence” of each screenshot using Vision AI.

---

## 1) Problem & User Story

**Problem:** I take many screenshots of websites/articles. They end up in the Screenshots folder and get lost. Later they’re hard to find and lack context.

**Primary user story:**
- As a user, I want my screenshot folder ingested automatically.
- Each screenshot should become a concise, searchable “card” that contains the extracted essence (title/summary/topics/people/products/decisions/etc.).
- I want to browse by **timeline**, **topics**, and **search**.
- I want to mark items **Solved** so they disappear from the default view, while still being retrievable.

**Key constraints:**
- The original screenshot must always remain viewable.
- The “main view” should be AI-extracted content, not the raw image.

---

## 2) Product Goals

1. **Zero-setup ingestion**: user selects a screenshot folder once.
2. **Fast retrieval**: search by keywords + filters (topic, date, source, solved).
3. **Meaning extraction**: cards show what matters (not just OCR).
4. **Timeline-first**: default view is a chronological feed.
5. **Privacy-aware**: clear controls, local storage, explicit opt-in for cloud AI.

---

## 3) Non-Goals (for v1)

- Full note-taking/annotation suite (keep it minimal; add later).
- Multi-device sync (future).
- Perfect OCR for every language/font (best effort).
- Browser extension / direct share intent integration (nice-to-have later).

---

## 4) Core Features (MVP)

### 4.1 Folder Selection & Permissions
- User selects screenshot folder via **Storage Access Framework** (persistable URI permission).
- App remembers folder, can re-scan on demand.
- Handle common default paths but do not hardcode (Android OEM differences).

### 4.2 Ingestion & Dedup
- Detect new screenshots:
  - Periodic scan using WorkManager (e.g., every few hours) OR on app open.
  - Hash (e.g., SHA-256) of image bytes for dedup.
- Track file metadata:
  - `contentUri`, filename, size, lastModified, dateTaken (if available), resolution.

### 4.3 AI Extraction Pipeline
For each screenshot, produce an **Essence**:
- Title (best guess)
- Type/classification (article, social post, chat, product page, code snippet, recipe, map, etc.)
- Short summary (2–5 bullets)
- Key entities (people/orgs/products/places)
- Topics/tags (3–10)
- Suggested “action” (optional): read later / buy / try / reference / decision / idea
- Confidence score
- Optional: extracted URL/domain if visible
- Optional: “todo questions” (e.g., “Find the source”, “Compare prices”)

### 4.4 Browse & Search
- **Timeline feed** (default):
  - Cards sorted by capture date.
  - Shows title, domain (if found), summary bullets, topics chips.
  - Quick actions: Open screenshot, Mark solved, Add tag, Share (later).
- **Search**:
  - Full-text over title/summary/entities/topics + optional OCR text.
  - Filters: date range, topic, type, solved/un-solved, domain.
- **Topics view**:
  - Topic list with counts.
  - Topic detail: items in that topic + sub-filters.

### 4.5 Solved / Hidden
- Toggle: “Solved”.
- Default views hide solved items.
- Separate “Solved” tab or filter switch to show them.

### 4.6 Original Screenshot Viewer
- Tap card → detail screen:
  - Top: extracted essence
  - Button: “View original screenshot” (full-screen zoom/pan)
  - Metadata: file date, domain, tags, model used, extraction timestamp

---

## 5) Nice-to-Have (Post-MVP)

- Share sheet target: “Save to Screenshot Vault” (ingest single item with immediate processing).
- Collections / folders inside the app (manual grouping).
- LLM-based clustering: “Projects” / “Threads” across time.
- Offline/on-device model option (limited) + hybrid mode.
- Export (JSON/Markdown) for backups.

---

## 6) UX / Screens

### 6.1 First Run
1. Welcome + privacy note (clear: screenshots may contain sensitive info).
2. Choose screenshot folder (SAF picker).
3. Choose AI mode:
   - **Local-only** (OCR + heuristics; weaker)
   - **Cloud Vision AI** (best results; requires API key / backend)
4. Start scan (progress + allow running in background).

### 6.2 Main Tabs
- **Feed** (timeline)
- **Topics**
- **Search**
- **Settings**

### 6.3 Card Layout (Feed)
- Title (1–2 lines)
- Domain/type (small)
- Summary bullets (2–3)
- Topic chips (up to 4 + “+N”)
- Actions: Solved ✅, Open

---

## 7) Architecture Overview

### 7.1 Modules
- **ui/**: Compose screens, navigation, viewmodels
- **data/**: Room DB entities + DAO, repository
- **ingest/**: folder scanning, hashing, dedup
- **ai/**: extraction orchestration, model adapters, prompt templates
- **workers/**: WorkManager background jobs
- **utils/**: image loading, hashing, EXIF/date parsing

### 7.2 Data Flow
1. Scanner finds files → creates `ScreenshotItem` rows (status = NEW).
2. Worker picks NEW items → runs AI extraction → stores `Essence`.
3. UI observes DB (Flow) → renders Feed/Topics/Search.
4. User actions update flags/tags → reflected immediately.

---

## 8) Data Model (Room)

### 8.1 Tables

#### `screenshot_items`
- `id` (UUID, PK)
- `content_uri` (String, unique)
- `sha256` (String, unique)
- `display_name` (String)
- `mime_type` (String)
- `size_bytes` (Long)
- `width` (Int)
- `height` (Int)
- `captured_at` (Instant/Long) — best-effort (EXIF > MediaStore > lastModified)
- `ingested_at` (Instant/Long)
- `status` (Enum: NEW, PROCESSING, DONE, FAILED)
- `error_message` (String?)
- `solved` (Boolean, default false)
- `domain` (String?) — derived
- `type` (String?) — derived by AI
- `ocr_text` (String?) — optional for search

#### `essences`
- `screenshot_id` (FK -> screenshot_items.id, unique)
- `title` (String)
- `summary_bullets_json` (String) — JSON array
- `entities_json` (String) — JSON object/array
- `topics_json` (String) — JSON array
- `suggested_action` (String?)
- `confidence` (Float)
- `model_name` (String)
- `created_at` (Instant/Long)
- `raw_response_json` (String?) — optional for debugging (off by default)

#### `tags`
- `id` (UUID, PK)
- `name` (String, unique)

#### `screenshot_tags` (many-to-many)
- `screenshot_id` (FK)
- `tag_id` (FK)

### 8.2 Indexing & Search
- Use SQLite FTS (FTS5) virtual table, e.g. `search_index`:
  - columns: title, summary, entities, topics, ocr_text, domain, type
  - triggers or manual updates after extraction/tag changes
- Queries should support:
  - keyword search
  - filters: date range, topic/tag, solved flag, domain, type

---

## 9) AI Extraction

### 9.1 Modes
**Mode A: Cloud Vision AI (recommended)**
- Input: screenshot image (downscaled) + optional OCR text
- Output: strict JSON matching schema

**Mode B: Local-only (fallback)**
- Use ML Kit Text Recognition (OCR) + heuristics:
  - Try detect URL/domain, headline-like text, bullet-like structure
  - Topics: keyword extraction (simple TF-IDF or RAKE-like)
  - Summary: first N salient lines

### 9.2 Image Preprocessing
- Downscale to max dimension (e.g., 1280px) for cost/speed.
- Compress JPEG ~80 quality if safe (don’t destroy small text too much).
- Keep original unmodified.

### 9.3 Output JSON Schema (strict)
The AI must return JSON only, matching:

```json
{
  "title": "string",
  "type": "article|product|social|chat|code|recipe|map|unknown",
  "domain": "string|null",
  "summary_bullets": ["string", "string"],
  "topics": ["string"],
  "entities": [
    {"kind":"person|org|product|place|other","name":"string"}
  ],
  "suggested_action": "read|buy|try|reference|decide|idea|unknown",
  "confidence": 0.0
}
````

### 9.4 Prompt Template (Cloud Mode)

**System (conceptual):**

* You extract the essence of screenshots.
* Be concise.
* Return valid JSON only.
* If unsure, set fields to `unknown`/`null` and lower confidence.

**User prompt (template):**

* “Analyze this screenshot. Extract title, type, domain (if visible), 2–5 bullet summary, 3–10 topics, key entities. Output JSON exactly matching the schema.”

### 9.5 Safety & Privacy

* In settings, show:

  * AI mode (Local-only vs Cloud)
  * “Process only on Wi-Fi” toggle
  * “Exclude screenshots containing sensitive apps” (optional later)
* Never upload unless user enabled cloud mode.

---

## 10) Background Work (WorkManager)

### 10.1 Workers

* `ScanScreenshotsWorker` (periodic)

  * Enumerate folder URIs
  * For each file: if unseen hash/URI → insert NEW row
* `ProcessScreenshotWorker` (chained or queue)

  * Picks batch of NEW rows
  * Marks PROCESSING
  * Runs extraction
  * Writes `essences` + updates item status DONE/FAILED
  * Updates FTS index

### 10.2 Scheduling

* Periodic scan: every 6–12 hours (configurable).
* Processing: triggered after scan; throttle for battery.

---

## 11) Settings

* Screenshot folder (change / re-authorize)
* AI mode:

  * Local-only
  * Cloud Vision AI
* Cloud configuration:

  * Option 1: user pastes API key (fast dev; not ideal)
  * Option 2: call your own backend (recommended)
* Processing rules:

  * Wi-Fi only
  * Charging only
  * Max items per batch
* Data:

  * Clear database (does not delete screenshots)
  * Rebuild index
  * Export metadata (JSON)

---

## 12) Error Handling

* If file disappears → mark as FAILED with message; allow “relink folder” or “remove item”.
* If AI fails:

  * exponential backoff retries (max N)
  * store last error
  * allow manual “Retry extraction”
* If extraction is partial:

  * still store what’s available with low confidence.

---

## 13) Performance Requirements

* UI scroll must remain smooth with 1000+ items:

  * Paging (Paging 3)
  * LazyColumn
  * Thumbnail caching (Coil)
* DB queries must be indexed and use FTS for search.

---

## 14) Security Requirements

* Use scoped storage via SAF; do not request broad file permissions unless absolutely required.
* Keep secrets (API keys) out of logs.
* If backend used: use HTTPS, auth tokens, and minimal telemetry.

---

## 15) Acceptance Criteria (MVP)

1. User can select a screenshot folder and the app remembers it across restarts.
2. App ingests existing screenshots and shows them as cards in a timeline.
3. Each card displays AI-derived title + 2–3 summary bullets + topic chips.
4. User can search and filter (at least: keyword + solved toggle + date).
5. User can mark an item as solved; solved items are hidden by default.
6. User can open the original screenshot from the detail view.
7. New screenshots added to the folder appear after the next scan (or manual refresh).
8. App handles failures gracefully and allows retry.

---

## 16) Implementation Notes (Suggested Stack)

* Kotlin + Jetpack Compose
* Room + FTS5
* Paging 3
* WorkManager
* Coil for images
* ML Kit Text Recognition (optional but helpful even in cloud mode)
* AI adapter interface:

  * `AiExtractor.extract(imageBytes, ocrText?): EssenceResult`

---

## 17) Milestones

### Milestone 1 — Local DB + Folder Scan + Feed

* SAF folder selection
* Scan & store items (no AI yet)
* Feed renders thumbnails + metadata

### Milestone 2 — AI Extraction + Detail View

* Worker processing pipeline
* Essence schema & storage
* Detail screen with essence + original

### Milestone 3 — Search/Topics/Solved

* FTS search
* Topics aggregation
* Solved behavior + filters

### Milestone 4 — Polish

* Settings
* Retry flows
* Performance pass

---

## 18) Open Questions (Decide during implementation)

* Do we treat “topics” as free-form strings or normalized tags?
* How aggressive should dedup be (hash vs perceptual hash)?
* Should we store OCR text always (privacy vs usability)?
* Cloud mode: direct API key vs backend proxy?

