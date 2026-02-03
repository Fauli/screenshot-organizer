# Screenshot Vault

An Android app that organizes your screenshots by extracting meaningful information using OCR and AI.

## Features

- **Smart Organization** — Automatically extracts titles, summaries, topics, and entities from screenshots
- **Timeline Feed** — Browse screenshots chronologically with rich previews
- **Full-Text Search** — Search across all extracted content
- **Topic Browsing** — Explore screenshots by auto-detected topics
- **Solved Toggle** — Mark processed items as "solved" to hide them from the feed
- **Privacy-First** — Uses Android SAF for folder access; no broad storage permissions

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room + FTS5 for storage and search
- WorkManager for background processing
- ML Kit Text Recognition for OCR
- Hilt for dependency injection
- Paging 3 + Coil for efficient list rendering

## Building

```bash
./gradlew assembleDebug
```

## Usage

1. Launch the app and select a folder containing screenshots
2. The app scans and processes images in the background
3. Browse the timeline, search, or explore by topic

## Architecture

MVVM + Repository + UseCases with clean separation between UI, domain, and data layers.

## License

MIT
