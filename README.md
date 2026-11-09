# Anilili Short Dramas

This edition focuses on short dramas. Browse opens Dramatip by default and offers
Kiss18 and Narto Drama. Search, homepage listings, pagination, and short-form
vertical playback use the per-site adapters. Tap a title to choose episodes and
download. Paste link remains available for these sources and direct HLS/MP4/WebM
files.

The short-drama app has its own Android application ID and can be installed
alongside Anilili Movies.

The Material theme, poster cards, animated water-fill loading mark, splash and
download display are adapted from the local Anilili application. EroticMV homepage and search were verified with browser navigation headers,
referrers and a persistent session; blocked requests show a useful error. These are internal application APIs, not a
separately hosted public REST service.

# Shared downloader engine

A Material 3 downloader using the search, episode API, source selection, and
Kwik/glot playlist-resolution flow from
[animepahe-dl](https://forgejo.excreal.duckdns.org/merlin/animepahe-dl).

## Use

1. Type a title to search automatically after a short pause. Results show posters and release years. Clear the field to return to the empty Explore screen.
2. Sort results by relevance, title, or year.
3. Open a result to load its synopsis and every episode API page.
4. Select individual episodes or **Select all**. Quality and audio choices are checked against the sources shared by every selected episode; choose from those available options and download.
5. Open **Downloads** for collapsible show groups, local episode search, and episode rows with posters, quality/audio, progress, and size. Sort the library by show title, status, or size. Use **Select** or long-press a row to select items; **Select all** includes collapsed groups in the current search. Changing the search clears selection. Delete asks once for the batch and reports partial failures; failed items remain visible and selected. Permanent deletion has no Undo. Pause/resume is offered for eligible HLS transfers; Retry save is offered for export failures. New direct downloads retain source identity for fresh-source Retry.
6. Use the compact rows in **Settings** to choose the output folder, default audio (source default, Japanese, or English), connection count, or clear browser session data. Unavailable preferred audio falls back to the source default; per-show audio remains selectable.

There are no demo results or simulated downloads.

Download metadata and pending preparation/deletion operations live in Room.
The first database open imports the old export records and direct-download index
in one transaction. The migration is repeatable, preserves IDs/destinations,
and leaves legacy preferences untouched for diagnosis. Deletion persists its
intent before stopping work and serializes against publication and Retry save.
Provider refusal or revoked access keeps the record and a retryable explanation.

## Sessions

HTTP is attempted first. Search requests debounce for 400 ms. A challenge stays
inline while typing; verification can open after 1.2 seconds without an edit,
only on Explore in the foreground. Keyboard Search permits immediate verification.
Edits cancel the previous HTTP call. Clearing or leaving Explore cancels pending
presentation. Closing the browser keeps a Verify action, and a repeated challenge
after verification stays inline instead of reopening. Background download
challenges appear on their rows and in a banner.
Complete verification; once the destination page is ready, the app saves the session and retries once automatically. The shield icon and **Settings → Open verification** also open the
browser manually. WebView stores the browser profile/cookies privately; HTTP
and image requests use those domain-scoped cookies and the same saved user
agent. No cookies need to be pasted and no session IDs need to be entered.

WebView and OkHttp have different TLS fingerprints. A site can reject HTTP
requests despite successful browser verification. The app reports failures;
verification is not guaranteed and is not automated or bypassed.

## Downloads

- **HLS:** Media3 downloads manifests, segments, and supported encryption into
  `Android/data/com.example.animepahedl/files/hls-temp`. On ARM64 devices, the
  bundled FFmpeg executable reads this cache through a token-protected loopback
  server and stream-copies video/audio into a staging MP4. The server has four
  workers, a bounded request queue, streamed binary responses, a 1 MiB playlist
  limit, and single HTTP byte-range support. The app checks tracks and timelines,
  copies to the pending destination, confirms it is readable, and publishes it
  before removing the cached source. Export failures retain the source and offer
  **Retry save**. Unsupported devices fail before queueing a new HLS export.
- **Direct MP4/WebM:** Android DownloadManager stages files in the app's
  `files/direct-temp` directory under Android/data. Completed files are copied
  to the same output folder, then the temporary download is removed.
- **Output folder:** defaults to `Downloads/Animepahe`. Settings can choose a
  folder with Android's document picker (including a writable SD-card folder),
  or use a named subfolder under Downloads. Each new download remembers its
  destination. **Retry save** uses the current Settings destination so you can
  recover from a revoked folder permission or full storage. Videos already
  saved remain in their original folders and survive app uninstall.
- **Session reset:** Settings clears WebView cookies, website storage and browser
  cache after confirmation. It does not delete downloaded videos or staging
  media. Session clearing is disabled while a site request is running.
- **Recovery:** HLS manifests, segments, and progress persist across process crashes.
  Reopen the app to restart unfinished work using the cached data. Disconnected
  downloads wait for connectivity; Android’s persistent scheduler restarts the
  service when network requirements are met. Transient network/I/O and HTTP 408/429/5xx
  failures retry after a delay through a persisted Android job, including after process death or reopening the app; verification
  and permanent HTTP failures need user intervention. HLS pause state is saved;
  use the episode's actions menu to resume. Android DownloadManager manages
  direct-file network recovery. Android force-stop and foreground-service time
  limits can require reopening the app. Interrupted MP4 assembly restarts from
  the completed cache rather than downloading the episode again.
- Download preparation (source resolution) requires the app to remain open;
  the resulting downloads and exports are handed to background services.
  Unfinished episode preparation is stored locally and offers **Resume preparation**;
  preparation runs only while the app remains open. Successfully queued episodes
  remain queued if another episode fails. Interrupted exports resume when the app
  next opens. Remux and copy stages allow up to 30 minutes each, with a two-minute
  no-progress watchdog; validation has its own two-minute deadline. Staging and final MP4
  output coexist during export, so allow space for both. Android's
  foreground-service restrictions/time limits still apply.
- Like the desktop implementation, packed player scripts are submitted to
  `https://glot.io/api/mux`. This third-party service and the site's page/API
  formats must remain compatible. Unavailable quality/audio options fail
  explicitly instead of silently choosing another language.
- No yt-dlp, Python, GTK, WebKitGTK, or ffmpeg installation is needed on Android.

Only download media you have permission to save. Live-site verification and
downloads must be tested on a device; a successful build does not prove that
the current site's protection accepts Android sessions.

## Build

Use Android SDK 37.0, NDK 28.2.13676358, and JDK 25 (the Gradle daemon toolchain):

```sh
./build.sh debug --test
./build.sh release
./build.sh all --test
```

The script defaults to `debug`, prints APK paths, and limits Gradle to two workers.
Use `BUILD_WORKERS=1 ./build.sh all` to reduce concurrency further. `--test` includes
local unit tests. It works from any directory and uses the Gradle wrapper.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Minimum Android: 15 (API 35).
Release APKs are unsigned unless all signing variables below are configured.

## Video links

Explore has separate **AnimePahe** and **Video links** tabs. Paste a
Dramatip, Kiss18, Narto Drama, EroticMV, or KissKH Korean drama link,
or a direct HTTP(S) HLS/MP4/M4V/WebM URL, then choose **Inspect**. Review the series,
quality, and selected episodes before downloading. Standalone videos have a
single **Download video** action. TikTok is excluded. Episode selection accepts
`1-5,9`; blank means all, while **Clear selection** writes `none` and selects zero.
If the exact requested quality is unavailable, the resolver chooses the highest
available quality at or below it, or the lowest available quality when all are
higher. **Best** selects the highest available profile.

**Import session cookies** accepts Netscape cookie files with domain information. Imports honor
domain, path, secure, and expiry attributes. Cookies remain in app-private storage
and are excluded from diagnostics. Use your own current session when a provider
requires it. Provider availability still depends on its current responses.

AnimePahe and video-link preparation have separate persistent background batches.
WorkManager continues preparation away from the screen, waits for connectivity,
and retries temporary failures with exponential backoff. Manage Pause, Resume,
or Discard from the compact Downloads status row. Explicit session verification
still requires the verification screen; the remaining batch then resumes. Download retries resolve the original provider
again. HLS and direct video-link files use Media3 with per-download request
context; direct files are copied from the cache without re-encoding. HLS saving
still requires an ARM64 device for the bundled FFmpeg executable.

## Device verification

`HlsExportTest` supplies a generated three-second H.264/AAC HLS fixture directly
to the download cache, exports it through the foreground service, checks that
the MP4 has audio/video and a valid duration, then checks temporary-cache cleanup.
A discontinuity fixture checks timestamp resets between segments, and an intact-audio/short-video
fixture ensures container duration cannot hide missing video.
`LibraryNavigationTest` checks the Downloads and Settings entry points.

```sh
bash ./gradlew connectedDebugAndroidTest
```

For the optional continuous 24-minute export benchmark, install both debug APKs
and run `bash scripts/make-export-benchmark.sh` (host ffmpeg and adb required).
The generated fixture stays outside the repository.

Settings → Download speed controls a shared pool of 2, 4, 8 (default), or 12 HLS
segment connections while keeping up to two episodes active. Changes resize the
pool immediately. Host/network limits still determine achievable throughput.

New exports use `Output folder/Show name/Show name ep01 720p.mp4` (or WebM
for direct WebM sources). Episode numbers are padded to at least two digits;
quality comes from the selected source. Existing files are not renamed.

### FFmpeg

HLS exports use the arm64-v8a FFmpeg 9.0 executable from
[Android-FFmpeg-Prebuilt](https://github.com/hzw1199/Android-FFmpeg-Prebuilt/tree/main/ffmpeg-9.0).
The executable is distributed under LGPL 2.1; its license is included in
`app/src/main/assets/licenses/FFmpeg-LGPL-2.1.txt`. FFmpeg source is available
at [FFmpeg n9.0](https://github.com/FFmpeg/FFmpeg/tree/n9.0). The upstream
binary SHA-256 is `9085507b0dc32643b4d6d084a7e7d3469ef17907a7ba15c22d3997ed09c932aa`.
The executable supports arm64-v8a only. HLS downloads use the existing Media3
network downloader; FFmpeg reads its cache via a loopback server and stream-copies
media into MP4. Every selected playlist's segments, keys, and initialization data
must be fully cached before remuxing. Audio and video durations are not required
to match (dubbed tracks can legitimately be shorter). FFmpeg must finish
successfully, and the final copy is integrity-checked before publication.

## Release checks

Build an optimized APK with `./build.sh release`. Debug APKs use
Android's development key and can upgrade existing debug installations. For a
personal release, create and back up a private keystore outside the repository,
then configure `ANIMEPAHE_KEYSTORE`, `ANIMEPAHE_STORE_PASSWORD`,
`ANIMEPAHE_KEY_ALIAS`, and `ANIMEPAHE_KEY_PASSWORD` in the build environment.
Run `./build.sh release`, then verify with SDK `apksigner verify --verbose`.
For local signing, `build.sh` also loads `.signing/release.env` when no
`ANIMEPAHE_KEYSTORE` is already set. This ignored file exports the same four
variables. Back up the entire `.signing/` directory securely, including the
keystore and credentials; neither belongs in Git. For direct Gradle builds,
load the environment first with `source .signing/release.env`.
Keep the same key for every upgrade; losing it prevents an in-place update.
Increment `versionCode` for every distributed APK and update `versionName`.
Never commit a keystore or passwords. Without these variables the release build
is unsigned, suitable for build validation only.

Before calling a release production-ready, run the unit and device suites and
complete live-site search/verification/source selection plus a full-length HLS
and direct-file download. Check screen-off use, interrupted transfer/export,
process reopening, storage full/revoked access, a slow provider, Android's
foreground-service time limit, large fonts, TalkBack, and keyboard/rotation.
A passing build or fixture export does not establish live-site compatibility.

## Library and recovery tools

Downloads uses compact show groups with circular progress and saving/downloading
status even when collapsed. Search and sort share a row. Filtering/grouping runs
off the UI thread and progress refresh is throttled; the library does not poll
while hidden. Confirmed external deletions are reconciled on provider changes
and on returning to the app.

- Explore offers sorting, recent searches with a Clear history action, and bookmarked shows. Open a show to bookmark it or remove its bookmark.
- **Select missing** selects episodes without a healthy job in the current quality/audio variant. Source options continue to be checked across the whole selection.
- Downloads → episode actions → **Move to front** prioritizes the item for saving and gives an eligible HLS transfer the connection pool by pausing other HLS jobs. Resume the other jobs when ready. Android manages direct-file ordering separately.
- Saved videos open in Android's system default video player via ACTION_VIEW. The app grants temporary read access to the selected file; playback controls and position history belong to that player. Files are not re-encoded, and no playback engine is bundled.
- MediaStore/Downloads and selected folders are monitored while the app is visible, with a foreground polling fallback for document providers. Confirmed deleted files are removed from the library automatically. Permission loss or an unavailable provider does not count as deletion. **Relink video** and **Remove record; keep file** remain available for other repairs.
- Settings → **Storage and recovery** reports available app-volume space, retained temporary bytes, failed saves, and inaccessible files. Export also checks estimated assembly space before starting. Document providers may have a separate capacity limit which Android does not expose.
- Settings → **Unmetered downloads** updates app-managed HLS and site-file transfers immediately. Existing Android-managed direct downloads retain the network policy they were queued with.
- Settings → **Source health** shows recent request outcomes and verification information. Browser verification alone does not prove that HTTP requests will be accepted.
- Settings → **Export diagnostics** previews a summary before saving it through the document picker. The summary omits titles, paths, cookies, and media URLs.
- HLS notifications provide Pause/Resume actions. Transfer completion and saving remain distinct; a video becomes available after final output verification.

Database metadata is published to the app only after its ordered write commits. A failed write stays queued for retry, and critical transitions cannot pass the persistence barrier until it succeeds. The Room v1 schema is exported under `app/schemas`; future schema changes must supply and test migrations. The obsolete Transformer/direct-muxer implementation and dependencies have been removed. HLS export and its fixtures use FFmpeg.

After reinstalling, use **Choose folder** in Downloads and select the original
`Download/Animepahe` folder, or the existing show folder. Android requires this
folder grant to see files from the previous installation. Matching episode,
quality and language files are reused before download; queued duplicates are
reconciled when the folder is chosen. Install subsequent APKs as updates to retain
library records and folder access. Selection controls live in the top app bar;
additional eligible actions are in its overflow menu.

Library keeps explicitly saved shows separately from Continue Watching. Use the
bookmark action in a title's details or player to save/unsave it; this does not
delete downloaded files or playback history. Saved shows persist across app
updates and restarts. Saved-video players offer the same bookmark control when
the original provider identity is available.
