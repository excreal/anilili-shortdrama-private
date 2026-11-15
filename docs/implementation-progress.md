# Review implementation status

Implementation follows `codebase-review-2026-11-03.md`. Changes include the initial batch and the completion pass.

## Current release verification (2026-11-15)

- Commit `553d9e1` contains the reviewed application fixes and initial release version bump; the unused resource cleanup is included in the following release commit.
- `./gradlew :app:testDebugUnitTest` passed (166 tests, no failures or skips); `:app:compileDebugAndroidTestKotlin` passed. Instrumentation tests were compiled but not run in this checkout because no Android device is connected.
- `:app:lintDebug` passed with warnings and no errors. The cleanup removed 28 lint-confirmed unused string/plural resources and the unreferenced legacy launcher image. A follow-up lint run reported zero `UnusedResources` issues and 127 other warnings.
- `BUILD_WORKERS=1 ./build.sh release` produced signed APK `Anilili-Short-Drama-v1.2.19.19-release.apk` (version code 19). `apksigner verify --verbose --print-certs` passed; SHA-256 signer digest `df6fcad0dd936146c3505209b93cfa10d6ab08e229e021ca705b3743f008a8b5` matches the previous release.
- The APK is published under `/srv/downloads/builds/app-release-b76eff896036.apk`; `/srv/downloads/release.apk` and the versioned nginx link point to it. Range GET requests to both links returned `206` with the requested byte. APK SHA-256 is `b76eff89603610008c5e0010a865dc70336e9649868725397d1b2e9503ff5811` both before and after publication.
- No live-site preparation/download or on-device UI/gesture run was performed for this release. The broader code review remains open.

## Reliability and security

- R1: committed-only download snapshots, ordered retry journal, persistence health surfaced in the UI, and a regression test proving failed writes cannot be bypassed. Deletion tombstones remain immediately visible and must flush before destructive work.
- R2/R3: completion receiver waits for initialization, reconciles pre-enqueue journal entries, and finishes its asynchronous receiver result; startup recovers requests matched to exact app-owned staging destinations.
- R4: sources with selected audio require an audio stream in FFmpeg and output track validation. A silent-video fixture verifies rejection.
- R5: final copies are read completely and compared by byte count and SHA-256, including providers without size metadata; publication requires a successful MediaStore update.
- R6/R7: row actions handle errors; unresolved pending output remains owned across retry.
- R8/R9: export service stops when no ready work arrives; wake lock is released per export and covers stage budgets.
- R10: validation runs interruptibly and sample scanning checks interruption; copy verification closes streams on cancellation. Native Android media APIs and remote document providers can still delay an individual blocking call, so these deadlines are not hard real-time guarantees.
- S1/S2: bounded request/header parsing and total header deadline; bounded remote bodies, pagination, resolver traversal, and HTTP call duration.
- S3/S4: verification completion is host-bound; session generation prevents stale HTTP responses from restoring cleared cookies; active work blocks reset.
- S5/S6: packaging checks native SHA-256; diagnostics remove URL paths/userinfo and redact free-text reasons/summaries.

## UI, performance, and features

- U1/U2/U3/U4: episode state uses indexed engine rows; old-query results clear; only sorting remains, as requested; library grouping uses source identity and explicit episode metadata.
- U5/U6: settings errors have error styling; selected episode cards expose selection/checkbox semantics; groups use tri-state selection. Static screen text has been extracted into translatable resources, and selected episode counts use plurals. No additional language translation is claimed.
- Progress refreshes are throttled; episode state is indexed; source checks use three workers; episode pages publish incrementally. Response bounds and pagination protections prevent runaway loading.
- Room schemas are exported. Obsolete muxer implementations and their dependencies are removed; fixtures exercise production FFmpeg.
- Storage preflight and recovery center; retained-media visibility; failed-save retry; queue priority/reordering through Move to front; repair/relink and metadata-only removal.
- Bookmarks and recent searches; Select missing by current variant; unmetered network policy.
- Internal saved-file playback, persisted position, next episode, and background pause.
- HLS notification actions and episode-specific saving notification.
- Diagnostic preview and file export; source-health history.

## Verification

- 2026-11-04: 35 local unit tests passed with no failures or skips.
- Physical TECNO KM7, Android 15/API 35: device suite completed with no failures (29 passed, two optional benchmarks skipped). After correcting shell-created fixture directory permissions, both skipped benchmarks passed separately; all 31 device cases were exercised successfully.
- The 24-minute synthetic H.264/AAC fixture remuxed in 7.821 seconds. A separate export took 7.121 seconds. Host ffprobe confirmed video duration 1440.000 seconds, audio duration 1440.055 seconds, and a 4,330,417-byte MP4. This small 160×90 fixture checks duration/track retention and orchestration, not typical full-resolution throughput.
- The latest debug APK was installed. Test media was removed and the phone's original plugged-in screen-on setting restored.

- Historical device checks below were run on an earlier revision and do not verify the current release. Current release validation is recorded above.

Live-site compatibility, OEM-specific service limits, and arbitrary cloud-provider behavior cannot be established by local fixtures alone. No claim is made that a static audit proves absence of all defects.

## Latest user corrections

- Removed year/type and download-status filters. Search and library expose sorting only; quality/audio remain download source choices, and connection counts remain settings.
- The original U3 finding about absent year/type filters is superseded by the user's explicit removal of those filters; the old review finding is historical, not an outstanding feature request.
- Removed the obsolete Transformer/direct-remux test code and dependencies, including deprecated muxer/extractor constructors. Export fixtures now use the production FFmpeg implementation.
- Updated host Protobuf Java/Java-util to 4.36.0 through dependency resolution, covering both plugin and project tool configurations. This addresses Unsafe use in standard Java messages on JDK 25 without warning suppression or a JDK downgrade.
- Updated AndroidX Test to the documented stable runner 1.7.0, JUnit extension 1.3.0, and Espresso 3.7.0.

References: [Protobuf v36.0](https://github.com/protocolbuffers/protobuf/releases/tag/v36.0), [Protobuf cross-version guarantees](https://protobuf.dev/support/cross-version-runtime-guarantee/), [AndroidX Test releases](https://developer.android.com/jetpack/androidx/releases/test), [Media3 muxer API](https://developer.android.com/reference/androidx/media3/muxer/Mp4Muxer.Builder).

The warning cleanup also updates the Gradle wrapper, AGP, Kotlin/Compose compiler, Compose BOM, AndroidX core/activity/lifecycle, OkHttp, Gson, and Jsoup to the stable versions identified by lint and official documentation. Dependencies are centralized in the version catalog. KTX edits preserve asynchronous preference semantics; queue order now uses the Room operation journal and its checked persistence barrier. Native symbol stripping uses checksum-verified NDK r28c (28.2.13676358), now pinned in the module build.

AGP also brings Tink 1.7.0 into the host build. The newer Protobuf runtime correctly reports its old generated classes as vulnerable, so the host dependency is upgraded to Tink 1.24.0, rather than setting Protobuf's warning-suppression property. See [Google's Tink setup](https://developers.google.com/tink/setup/java), [Tink 1.24.0 release](https://github.com/tink-crypto/tink-java/releases/tag/v1.24.0), and [Protobuf's advisory](https://github.com/protocolbuffers/protobuf/security/advisories/GHSA-h4h5-3hr4-j3g2).

Compose UI tests use the [v2 test rule](https://developer.android.com/develop/ui/compose/testing/migrate-v2). The notification test restores its original launch intent after asserting real PendingIntent navigation because [ActivityScenario matches lifecycle callbacks against that launch intent](https://github.com/android/android-test/blob/main/core/java/androidx/test/core/app/ActivityScenario.java).

Additional fixes include explicit loopback-domain scope in the debug network policy, the Media3 API opt-in at its usage site, configuration-aware string resources, modifier argument ordering, fixed-delay export watchdog scheduling, bounded transfer wake locks, primitive Compose state, removal of unused legacy resources, and adaptive-icon foreground padding.
