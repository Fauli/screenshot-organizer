# Screenshot Vault - Implementation TODO

This document tracks the implementation progress for the Screenshot Vault Android app.
Follows the phased approach from CLAUDE.md Section 11.

---

## Phase 1: Storage + DB + Feed Skeleton

### 1.1 Project Setup
- [x] Initialize Android project with Kotlin DSL
- [x] Configure Gradle with all required dependencies:
  - Jetpack Compose (Material 3)
  - Room + KSP
  - Hilt
  - WorkManager
  - Paging 3
  - Coil
  - DataStore
  - kotlinx.serialization
  - Navigation Compose
  - ML Kit Text Recognition
- [x] Set up package structure per CLAUDE.md Section 2.2
- [x] Configure Hilt application class and modules

### 1.2 Room Database + Entities
- [x] Create `ScreenshotItemEntity` with all fields:
  - id, contentUri, sha256, displayName, mimeType, sizeBytes
  - width, height, capturedAt, ingestedAt
  - status (ProcessingStatus enum), errorMessage
  - solved, domain, type, ocrText
- [x] Create `EssenceEntity` (1:1 with screenshot):
  - screenshotId (FK), title, summaryBulletsJson, topicsJson
  - entitiesJson, suggestedAction, confidence, modelName, createdAt
- [x] Create `ProcessingStatus` enum: NEW, PROCESSING, DONE, FAILED
- [x] Create DAO interfaces:
  - `ScreenshotItemDao` (CRUD, paging queries, status updates)
  - `EssenceDao` (insert, query by screenshotId)
- [x] Create `AppDatabase` with proper type converters
- [ ] Write unit tests for DAOs (in-memory Room)

### 1.3 Domain Models
- [x] Create `domain.model.Essence` data class
- [x] Create `ContentType` enum (article, product, social, chat, code, recipe, map, unknown)
- [x] Create `EntityRef` data class (kind, name)
- [x] Create `SuggestedAction` enum (read, buy, try, reference, decide, idea, unknown)
- [x] Create `ScreenshotItem` domain model (mapped from entity)

### 1.4 DataStore Preferences
- [x] Create `PreferencesDataStore` wrapper class
- [x] Implement stored preferences:
  - selectedFolderUri: String?
  - aiMode: AiMode enum (OCR_ONLY, CLOUD)
  - hideSolvedByDefault: Boolean (default true)
  - lastScanAt: Long
- [x] Create Hilt module for DataStore

### 1.5 Repository Layer
- [x] Create `ScreenshotRepository` interface and implementation:
  - Insert/update screenshot items
  - Paging source for feed
  - Query by status
  - Update solved flag
- [x] Create `EssenceRepository` interface and implementation
- [x] Create `PreferencesRepository` for settings access

### 1.6 SAF Folder Selection
- [x] Create `FolderSelectionUseCase`
- [x] Implement SAF document picker intent
- [x] Persist URI permission (takePersistableUriPermission)
- [x] Store selected URI in DataStore
- [x] Handle permission revocation gracefully
- [x] Create UI state for "no folder selected" scenario

### 1.7 Scanner (Manual Trigger)
- [x] Create `ingest.scanner.ScreenshotScanner` class
- [x] Implement SAF DocumentFile tree traversal
- [x] Create `ingest.hashing.FileHasher` (SHA-256)
- [x] Extract file metadata: name, size, mimeType, dimensions
- [x] Implement date extraction logic (EXIF > MediaStore > lastModified)
- [x] Insert new items with status=NEW, skip duplicates by sha256
- [x] Create `ScanScreenshotsUseCase`
- [ ] Write unit tests for hashing and dedup logic

### 1.8 Feed Screen (Skeleton)
- [x] Set up Navigation Compose with bottom nav (Feed, Topics, Search, Settings)
- [x] Create `FeedViewModel` with Paging integration
- [x] Create `FeedUiState` sealed class
- [x] Implement `FeedScreen` composable with LazyColumn
- [x] Create `ScreenshotCard` composable (thumbnail + basic metadata)
- [x] Load thumbnails with Coil (from content URI)
- [x] Handle empty state and loading state
- [x] Add "Scan Now" FAB or button for manual scan trigger

### 1.9 Settings Screen (Minimal)
- [x] Create `SettingsViewModel`
- [x] Create `SettingsScreen` with folder selection button
- [x] Show currently selected folder path
- [x] Add "Rescan" button
- [x] Add placeholder for AI mode toggle (implement later)

---

## Phase 2: Background + OCR Extraction

### 2.1 WorkManager Setup
- [x] Create Hilt WorkerFactory
- [x] Configure WorkManager in Application class

### 2.2 ScanScreenshotsWorker
- [x] Create `ScanScreenshotsWorker` extending CoroutineWorker
- [x] Reuse scanner logic from Phase 1
- [x] Handle missing folder URI permission (return failure)
- [x] Return success with count of new items found
- [x] Schedule periodic work (every 12 hours default)

### 2.3 ML Kit OCR Integration
- [x] Add ML Kit Text Recognition dependency
- [x] Create `OcrService` wrapper class
- [x] Implement text extraction from Bitmap
- [x] Handle failures gracefully (return empty text)
- [ ] Write unit/instrumented tests for OCR

### 2.4 EssenceExtractor Interface
- [x] Create `domain.ai.EssenceExtractor` interface
- [x] Create `ExtractionInput` data class (screenshotId, imageBytes, ocrText)
- [x] Create `ExtractionResult` sealed interface (Success, Failure)

### 2.5 MlKitOcrEssenceExtractor
- [x] Implement `MlKitOcrEssenceExtractor`
- [x] Domain extraction: regex for URLs/domains in OCR text
- [x] Title heuristic: largest text block or first meaningful line
- [x] Summary bullets: first 2-3 non-trivial lines
- [x] Topics: simple keyword extraction (frequency-based, exclude stopwords)
- [x] Set ContentType based on keyword hints (heuristic)
- [x] Set low confidence (0.3-0.5) for OCR-only results
- [ ] Write unit tests with sample OCR outputs

### 2.6 ProcessScreenshotsWorker
- [x] Create `ProcessScreenshotsWorker` extending CoroutineWorker
- [x] Fetch batch of NEW items (limit 10)
- [x] Mark items as PROCESSING
- [x] Load image bytes from content URI
- [x] Run OCR extraction
- [x] Run EssenceExtractor
- [x] Save Essence to database
- [x] Update item status to DONE
- [x] On failure: set FAILED with errorMessage, allow retry
- [x] Chain worker after scan worker

### 2.7 Update Feed with Essence
- [x] Update `ScreenshotCard` to show:
  - Title (from essence)
  - Domain badge (if available)
  - Summary bullets (2-3)
  - Topic chips (up to 4)
- [x] Handle items without essence (show "Processing..." or metadata only)
- [x] Show processing status indicator for in-progress items

### 2.8 Detail Screen
- [x] Create `DetailViewModel` (load item + essence by ID)
- [x] Create `DetailScreen` composable:
  - Display full essence (title, all bullets, all topics, entities)
  - Show confidence score
  - Show model name and extraction timestamp
  - "View Original" button
- [ ] Implement full-screen image viewer (zoom/pan)
- [x] Add navigation from feed card to detail

---

## Phase 3: Search + Topics + Solved

### 3.1 FTS Search Index
- [x] Create `SearchIndexEntity` FTS4 virtual table
- [x] Define columns: title, summary, entities, topics, ocrText, domain, type
- [x] Create `SearchDao` with search query
- [x] Create `SearchableContentEntity` for content sync
- [x] Update `ProcessScreenshotsWorker` to index after essence save
- [ ] Write tests for FTS queries

### 3.2 Search Screen
- [x] Create `SearchViewModel` with query state
- [x] Implement debounced search input (300ms)
- [x] Query FTS table and map to domain models
- [x] Create `SearchScreen` composable:
  - Search input field
  - Results list (reuse ScreenshotCard)
  - Empty state / no results state
- [x] Add solved filter to search ("Include Solved" toggle)
- [ ] Add date filter (optional for MVP)

### 3.3 Topics Aggregation
- [x] Create `TopicsDao` with distinct topics + counts query
- [x] Store topics in searchable_content table
- [x] Create `TopicsRepository` with getTopicsWithCounts()
- [x] Create `TopicsViewModel`

### 3.4 Topics Screen
- [x] Create `TopicsScreen` composable:
  - List of topic cards with counts
  - Tap topic → shows screenshots for that topic
- [x] Implement topic detail view (inline, not separate screen)
- [x] Add navigation from topic screenshots to detail screen

### 3.5 Solved Toggle
- [x] Add "Mark Solved" action in detail screen
- [x] Add "Mark Solved" swipe action or button in feed card
- [x] Update repository with toggleSolved method
- [x] Update feed query to hide solved by default
- [x] Add "Show Solved" toggle in feed (toolbar or filter)
- [x] Add solved filter to search

### 3.6 Update Paging Queries
- [x] Modify feed paging source to respect hideSolvedByDefault
- [x] Ensure solved items can be retrieved when filter toggled
- [ ] Test paging with large datasets

---

## Phase 4: Cloud Adapter (Post-MVP)

### 4.1 Network Layer
- [x] Add Retrofit/OkHttp dependencies
- [ ] Create API interface for `/extract` endpoint
- [ ] Configure timeouts and retry policy
- [ ] Add network connectivity check

### 4.2 CloudVisionEssenceExtractor
- [ ] Implement `CloudVisionEssenceExtractor`
- [ ] Downscale image before upload (max 1280px dimension)
- [ ] Send image + OCR text to backend
- [ ] Parse strict JSON response with kotlinx.serialization
- [ ] Handle parse errors (non-retryable failure)
- [ ] Handle network errors (retryable failure)

### 4.3 Extractor Selection
- [ ] Create `EssenceExtractorFactory` based on aiMode preference
- [ ] Inject appropriate extractor in ProcessScreenshotsWorker
- [ ] Test switching between modes

### 4.4 Settings Enhancements
- [ ] Add AI mode toggle (OCR_ONLY / CLOUD)
- [ ] Add backend URL configuration (if needed)
- [ ] Add "Wi-Fi only" toggle for cloud mode
- [ ] Add "Charging only" toggle
- [ ] Show clear messaging about data usage

---

## Phase 5: Polish + Error Handling

### 5.1 Error States & Retry
- [x] Show failed items in feed with error indicator
- [ ] Add "Retry" button in detail screen for FAILED items
- [ ] Create use case to retry single item extraction
- [ ] Handle file-not-found errors (mark as FAILED, show message)

### 5.2 Permission Handling
- [x] Detect revoked folder permission
- [x] Show "Re-select folder" prompt
- [x] Handle permission denial gracefully

### 5.3 First Run Experience
- [ ] Create onboarding flow:
  - Welcome screen with privacy note
  - Folder selection step
  - AI mode selection step
  - Initial scan with progress
- [ ] Store "onboarding complete" flag

### 5.4 Performance Optimization
- [ ] Profile feed scrolling with 1000+ items
- [ ] Optimize thumbnail loading (Coil memory cache)
- [x] Ensure stable keys for LazyColumn
- [x] Index database columns used in WHERE clauses

### 5.5 Settings Enhancements
- [ ] Add "Clear database" option (keeps screenshots)
- [ ] Add "Rebuild index" option
- [ ] Add "Export metadata" option (JSON)
- [ ] Show storage usage stats

### 5.6 UI Polish
- [x] Implement Material 3 theming
- [x] Add dark mode support
- [ ] Add loading skeletons for cards
- [ ] Add pull-to-refresh on feed
- [ ] Add animations for solved toggle

---

## Testing Checklist

### Unit Tests
- [ ] FileHasher (SHA-256 correctness)
- [ ] Date extraction helper
- [ ] JSON parsing for Essence (kotlinx.serialization)
- [ ] MlKitOcrEssenceExtractor heuristics
- [ ] Repository logic with in-memory Room

### Integration Tests
- [ ] WorkManager workers execute correctly
- [ ] Paging loads data properly
- [ ] FTS search returns expected results

### Instrumented Tests
- [ ] Navigation between screens
- [ ] Room database migrations (when applicable)
- [ ] SAF folder selection flow

---

## Definition of Done (MVP)

All items below must be complete for MVP:

- [x] User can select folder; persists across restarts
- [x] App scans and shows screenshots in timeline
- [x] Each item gets essence (OCR-only minimum)
- [x] Search works across essence fields
- [x] Topic browsing works
- [x] Solved items hidden by default but recoverable
- [x] Original screenshot viewable from detail screen
- [x] App handles failures gracefully

---

## Open Decisions (Document Here)

| Question | Decision | Rationale |
|----------|----------|-----------|
| Topics: free-form vs normalized? | Free-form strings | Simpler for MVP; stored space-separated in search index |
| Dedup: SHA-256 vs perceptual hash? | SHA-256 | Simpler, exact dedup sufficient for MVP |
| Store OCR text always? | Yes | Needed for search; privacy acceptable for local-only |
| Cloud mode: API key vs backend? | Backend proxy | Security best practice per spec |
| FTS version? | FTS4 | Wider device compatibility than FTS5 |

---

## Notes

- Follow CLAUDE.md Section 2.1 quality bar for all code
- Keep the app usable without cloud mode (OCR-only fallback)
- No API keys in the app
- Use SAF, not broad storage permissions
- Side effects only in UseCases/Workers/Repos
