# Screenshot Vault - Implementation TODO

This document tracks the implementation progress for the Screenshot Vault Android app.
Follows the phased approach from CLAUDE.md Section 11.

---

## Phase 1: Storage + DB + Feed Skeleton

### 1.1 Project Setup
- [ ] Initialize Android project with Kotlin DSL
- [ ] Configure Gradle with all required dependencies:
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
- [ ] Set up package structure per CLAUDE.md Section 2.2
- [ ] Configure Hilt application class and modules

### 1.2 Room Database + Entities
- [ ] Create `ScreenshotItemEntity` with all fields:
  - id, contentUri, sha256, displayName, mimeType, sizeBytes
  - width, height, capturedAt, ingestedAt
  - status (ProcessingStatus enum), errorMessage
  - solved, domain, type, ocrText
- [ ] Create `EssenceEntity` (1:1 with screenshot):
  - screenshotId (FK), title, summaryBulletsJson, topicsJson
  - entitiesJson, suggestedAction, confidence, modelName, createdAt
- [ ] Create `ProcessingStatus` enum: NEW, PROCESSING, DONE, FAILED
- [ ] Create DAO interfaces:
  - `ScreenshotItemDao` (CRUD, paging queries, status updates)
  - `EssenceDao` (insert, query by screenshotId)
- [ ] Create `AppDatabase` with proper type converters
- [ ] Write unit tests for DAOs (in-memory Room)

### 1.3 Domain Models
- [ ] Create `domain.model.Essence` data class
- [ ] Create `ContentType` enum (article, product, social, chat, code, recipe, map, unknown)
- [ ] Create `EntityRef` data class (kind, name)
- [ ] Create `SuggestedAction` enum (read, buy, try, reference, decide, idea, unknown)
- [ ] Create `ScreenshotItem` domain model (mapped from entity)

### 1.4 DataStore Preferences
- [ ] Create `PreferencesDataStore` wrapper class
- [ ] Implement stored preferences:
  - selectedFolderUri: String?
  - aiMode: AiMode enum (OCR_ONLY, CLOUD)
  - hideSolvedByDefault: Boolean (default true)
  - lastScanAt: Long
- [ ] Create Hilt module for DataStore

### 1.5 Repository Layer
- [ ] Create `ScreenshotRepository` interface and implementation:
  - Insert/update screenshot items
  - Paging source for feed
  - Query by status
  - Update solved flag
- [ ] Create `EssenceRepository` interface and implementation
- [ ] Create `PreferencesRepository` for settings access

### 1.6 SAF Folder Selection
- [ ] Create `FolderSelectionUseCase`
- [ ] Implement SAF document picker intent
- [ ] Persist URI permission (takePersistableUriPermission)
- [ ] Store selected URI in DataStore
- [ ] Handle permission revocation gracefully
- [ ] Create UI state for "no folder selected" scenario

### 1.7 Scanner (Manual Trigger)
- [ ] Create `ingest.scanner.ScreenshotScanner` class
- [ ] Implement SAF DocumentFile tree traversal
- [ ] Create `ingest.hashing.FileHasher` (SHA-256)
- [ ] Extract file metadata: name, size, mimeType, dimensions
- [ ] Implement date extraction logic (EXIF > MediaStore > lastModified)
- [ ] Insert new items with status=NEW, skip duplicates by sha256
- [ ] Create `ScanScreenshotsUseCase`
- [ ] Write unit tests for hashing and dedup logic

### 1.8 Feed Screen (Skeleton)
- [ ] Set up Navigation Compose with bottom nav (Feed, Topics, Search, Settings)
- [ ] Create `FeedViewModel` with Paging integration
- [ ] Create `FeedUiState` sealed class
- [ ] Implement `FeedScreen` composable with LazyColumn
- [ ] Create `ScreenshotCard` composable (thumbnail + basic metadata)
- [ ] Load thumbnails with Coil (from content URI)
- [ ] Handle empty state and loading state
- [ ] Add "Scan Now" FAB or button for manual scan trigger

### 1.9 Settings Screen (Minimal)
- [ ] Create `SettingsViewModel`
- [ ] Create `SettingsScreen` with folder selection button
- [ ] Show currently selected folder path
- [ ] Add "Rescan" button
- [ ] Add placeholder for AI mode toggle (implement later)

---

## Phase 2: Background + OCR Extraction

### 2.1 WorkManager Setup
- [ ] Create Hilt WorkerFactory
- [ ] Configure WorkManager in Application class

### 2.2 ScanScreenshotsWorker
- [ ] Create `ScanScreenshotsWorker` extending CoroutineWorker
- [ ] Reuse scanner logic from Phase 1
- [ ] Handle missing folder URI permission (return failure)
- [ ] Return success with count of new items found
- [ ] Schedule periodic work (every 12 hours default)

### 2.3 ML Kit OCR Integration
- [ ] Add ML Kit Text Recognition dependency
- [ ] Create `OcrService` wrapper class
- [ ] Implement text extraction from Bitmap
- [ ] Handle failures gracefully (return empty text)
- [ ] Write unit/instrumented tests for OCR

### 2.4 EssenceExtractor Interface
- [ ] Create `domain.ai.EssenceExtractor` interface
- [ ] Create `ExtractionInput` data class (screenshotId, imageBytes, ocrText)
- [ ] Create `ExtractionResult` sealed interface (Success, Failure)

### 2.5 MlKitOcrEssenceExtractor
- [ ] Implement `MlKitOcrEssenceExtractor`
- [ ] Domain extraction: regex for URLs/domains in OCR text
- [ ] Title heuristic: largest text block or first meaningful line
- [ ] Summary bullets: first 2-3 non-trivial lines
- [ ] Topics: simple keyword extraction (frequency-based, exclude stopwords)
- [ ] Set ContentType based on keyword hints (heuristic)
- [ ] Set low confidence (0.3-0.5) for OCR-only results
- [ ] Write unit tests with sample OCR outputs

### 2.6 ProcessScreenshotsWorker
- [ ] Create `ProcessScreenshotsWorker` extending CoroutineWorker
- [ ] Fetch batch of NEW items (limit 10)
- [ ] Mark items as PROCESSING
- [ ] Load image bytes from content URI
- [ ] Run OCR extraction
- [ ] Run EssenceExtractor
- [ ] Save Essence to database
- [ ] Update item status to DONE
- [ ] On failure: set FAILED with errorMessage, allow retry
- [ ] Chain worker after scan worker

### 2.7 Update Feed with Essence
- [ ] Update `ScreenshotCard` to show:
  - Title (from essence)
  - Domain badge (if available)
  - Summary bullets (2-3)
  - Topic chips (up to 4)
- [ ] Handle items without essence (show "Processing..." or metadata only)
- [ ] Show processing status indicator for in-progress items

### 2.8 Detail Screen
- [ ] Create `DetailViewModel` (load item + essence by ID)
- [ ] Create `DetailScreen` composable:
  - Display full essence (title, all bullets, all topics, entities)
  - Show confidence score
  - Show model name and extraction timestamp
  - "View Original" button
- [ ] Implement full-screen image viewer (zoom/pan)
- [ ] Add navigation from feed card to detail

---

## Phase 3: Search + Topics + Solved

### 3.1 FTS5 Search Index
- [ ] Create `search_index` FTS5 virtual table
- [ ] Define columns: title, summary, entities, topics, ocrText, domain, type
- [ ] Create `SearchIndexDao` with search query
- [ ] Create triggers or manual update logic after essence save
- [ ] Write tests for FTS queries

### 3.2 Search Screen
- [ ] Create `SearchViewModel` with query state
- [ ] Implement debounced search input
- [ ] Query FTS table and map to domain models
- [ ] Create `SearchScreen` composable:
  - Search input field
  - Results list (reuse ScreenshotCard)
  - Empty state / no results state
- [ ] Add date filter (optional for MVP)

### 3.3 Topics Aggregation
- [ ] Create DAO query to extract distinct topics with counts
- [ ] Parse topicsJson and aggregate across all essences
- [ ] Create `TopicsViewModel`

### 3.4 Topics Screen
- [ ] Create `TopicsScreen` composable:
  - List of topic chips/cards with counts
  - Tap topic → filtered feed
- [ ] Create `TopicDetailScreen` or filter feed by topic
- [ ] Handle topics with special characters

### 3.5 Solved Toggle
- [ ] Add "Mark Solved" action in detail screen
- [ ] Add "Mark Solved" swipe action or button in feed card
- [ ] Update repository with toggleSolved method
- [ ] Update feed query to hide solved by default
- [ ] Add "Show Solved" toggle in feed (toolbar or filter)
- [ ] Add solved filter to search

### 3.6 Update Paging Queries
- [ ] Modify feed paging source to respect hideSolvedByDefault
- [ ] Ensure solved items can be retrieved when filter toggled
- [ ] Test paging with large datasets

---

## Phase 4: Cloud Adapter (Post-MVP)

### 4.1 Network Layer
- [ ] Add Retrofit/OkHttp dependencies
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
- [ ] Show failed items in feed with error indicator
- [ ] Add "Retry" button in detail screen for FAILED items
- [ ] Create use case to retry single item extraction
- [ ] Handle file-not-found errors (mark as FAILED, show message)

### 5.2 Permission Handling
- [ ] Detect revoked folder permission
- [ ] Show "Re-select folder" prompt
- [ ] Handle permission denial gracefully

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
- [ ] Ensure stable keys for LazyColumn
- [ ] Index database columns used in WHERE clauses

### 5.5 Settings Enhancements
- [ ] Add "Clear database" option (keeps screenshots)
- [ ] Add "Rebuild index" option
- [ ] Add "Export metadata" option (JSON)
- [ ] Show storage usage stats

### 5.6 UI Polish
- [ ] Implement Material 3 theming
- [ ] Add dark mode support
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

- [ ] User can select folder; persists across restarts
- [ ] App scans and shows screenshots in timeline
- [ ] Each item gets essence (OCR-only minimum)
- [ ] Search works across essence fields
- [ ] Topic browsing works
- [ ] Solved items hidden by default but recoverable
- [ ] Original screenshot viewable from detail screen
- [ ] App handles failures gracefully

---

## Open Decisions (Document Here)

| Question | Decision | Rationale |
|----------|----------|-----------|
| Topics: free-form vs normalized? | TBD | |
| Dedup: SHA-256 vs perceptual hash? | SHA-256 | Simpler, exact dedup sufficient for MVP |
| Store OCR text always? | Yes | Needed for search; privacy acceptable for local-only |
| Cloud mode: API key vs backend? | Backend proxy | Security best practice per spec |

---

## Notes

- Follow CLAUDE.md Section 2.1 quality bar for all code
- Keep the app usable without cloud mode (OCR-only fallback)
- No API keys in the app
- Use SAF, not broad storage permissions
- Side effects only in UseCases/Workers/Repos
