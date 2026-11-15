# Graph Report - AnililiShortDrama  (2026-11-15)

## Corpus Check
- 324 files · ~288,100 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 38 file(s) not represented in the graph (top: .xml 22, (none) 3, .ttf 3)

## Summary
- 4893 nodes · 12010 edges · 300 communities (161 shown, 139 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 757 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `afe97120`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- FloatingNavigationBar.kt
- AnililiError
- developer_notices.py
- ExternalPlaybackReturns.kt
- SettingsStore
- InPlayerEpisodeList.kt
- SubtitleDelay.kt
- size
- PlayerControlsScaffold.kt
- DeletionTestProvider.java
- .all
- AniListModels.kt
- PlayerSurface.kt
- Category
- MediaCache.kt
- ExistingOutput.kt
- OpenSubtitlesClient.kt
- .manager
- SettingsStore.kt
- ui/components/WaterFillLogoIndicator.kt
- PlayerScreenLock.kt
- SiteDownloadSeries
- CastRoutes.kt
- .operation
- SearchScreenTest.kt
- ResolvedMedia
- Composable
- DramaPlayerSurface.kt
- NartoMetadataApi
- PlaybackService.kt
- .editCaptionStyle
- PipeModels.kt
- NostrUpdateSource.kt
- PlayerGestureControls.kt
- TelevisionSignals.kt
- MegaPlayCdnHealth
- ThemeAccent
- SleepTimer
- AppBackdrop.kt
- SiteDownloadRepositoryTest.kt
- SourceFilterLanguage
- instrumentationregistry
- Test
- LibraryModels.kt
- SavedFilesWatcher.kt
- AnililiFailure
- DramatipMetadataApi.kt
- SiteDownloadSourcesTest
- SubtitleCueLayout.kt
- SubtitleTrackMerge.kt
- SiteDownloadSources.kt
- CaptionAppearance.kt
- MidnightDialog.kt
- DownloadStore
- AudioOutputFormat.kt
- ThreadTraceListener.java
- .event
- PopupGlass.kt
- AppColorScheme.kt
- AnimeRepository.kt
- ThemeSettings.kt
- ThemePreset
- FlixcloudPlaylistDataSource.kt
- Listener
- tohttpurl
- SiteDownloadRepository
- TvFocusTarget.kt
- CastButton
- SiteMetadataApi.kt
- ArtworkBitmapLoader.kt
- ResultSort
- MediaListEntry
- Anilili UI reference audit
- .manager
- DefaultQuality
- AnimepaheDL resume state
- ForegroundCheck
- ExternalSubtitleFiles.kt
- TvTextField.kt
- ProviderSubtitles.kt
- AppDns
- PreferredAudioLanguage
- dramatipPublicPosterUrl
- AppIcon
- PreferredSubtitleLanguage
- .check
- LocalStreamProxy.kt
- CatalogScreen
- TvPlayerControls.kt
- LocalStreamProxy
- PlayerSystemBars.kt
- PlayerSettingsSheet.kt
- dispatchers
- SubtitleSearchLanguage
- .switchPlaybackPlayer
- OpenSubtitlesAuthManager.kt
- Kiss18MetadataApi.kt
- LegacyTrustAnchors.kt
- CatalogViewModel.kt
- SiteDownloadCookieStore
- SecureTokenStore
- EpisodeControlPlayer
- Test
- PosterElement
- PlatformErrorCodes.kt
- PlaybackDiagnostics.kt
- CatalogPage
- AppGraph.kt
- .page
- PlaybackBuffer.kt
- HlsExportTest.kt
- Reliability and correctness findings
- .init
- SiteDownloadPreparation
- ErrorDomain
- verificationPage
- SiteDownloadRepositoryTest
- Listener
- WatchedFilter
- ThemeStyle
- LocalStreamProxyService
- Theme.kt
- ForwardingPlayer
- siteDownloadCookieHeader
- TvPlayerControl
- ExampleInstrumentedTest.kt
- SkipTimingStatus
- Viewer
- LibraryNavigationTest
- OpenSubtitlesPanel
- RemoteDramaPlayer.kt
- DpadLongPress.kt
- Anime
- ThemeBackground
- PlatformErrorCodes.kt
- Test
- CatalogScreen
- .row
- OpenSubtitlesPanel.kt
- MidnightSettings.kt
- BackdropBlur.kt
- Player
- Real-device verification
- ThemeContrast
- ErrorAction
- HlsPlaylistRewrite.kt
- c
- NextEpisodeTrigger
- appSiteDownloadRepository
- SubtitleCueTimes.kt
- Browse, play and download
- EpisodeSource
- Kiss18Catalog
- MetadataBrowseQuery
- TvPlayerBackTarget
- Kind
- ProviderBenchmark
- .onMediaButtonEvent
- DownloadQuality
- ExportService.kt
- ViewerOptionsData
- TvEpisodeChangeFocus
- isExportDurationComplete
- DownloadRow
- PlayerLockButton
- AnnouncementWorker
- Anilili feature audit checklist
- Callback
- StreamRefusalLoadErrorPolicy.kt
- DownloadSpeed
- NativeTvTextWatcher
- backdropBlur
- FailureReason
- ClockFormat
- .isChallenge
- Callback
- ReferencePlaybackRegistry
- SiteMetadataApi
- DownloaderViewModel.kt
- LocalStreamProxyHeaderPolicyTest
- DiagnosticsLog
- .relay
- EroticMV metadata API
- DiagnosticsLog
- TvNativeTextField
- ExampleUnitTest.kt
- ExportedSubtitle
- Device UI verification
- Developer announcements
- AGENTS.md
- components/README.md
- docs/KISSKH.md
- THIRD_PARTY_NOTICES.md
- make-export-benchmark.sh
- CompiledSeparately
- GestureDragAxis
- subtitleReprepareState
- DiscordStatus
- Uri
- TvTextInputType
- .onCreate
- TvNativeTextField
- SiteDownloadProvider
- MenuLanguage
- .transform
- PlayerSurface
- ReferenceChromeTokens.kt
- SessionBrowser
- AppBackdropDim
- Kiss18 public metadata API
- DesignSystem.kt
- ProxyActiveResources
- cached
- UiState
- asserttrue
- DramatipMetadataApi
- findActivity
- .configureRequestHeaders

## God Nodes (most connected - your core abstractions)
1. `SettingsStore` - 155 edges
2. `PlayerSurface()` - 135 edges
3. `AnililiError` - 114 edges
4. `PlaybackService` - 45 edges
5. `rememberFocusFill()` - 43 edges
6. `SiteDownloadSeries` - 37 edges
7. `NartoMetadataApi` - 37 edges
8. `SourceFilterLanguage` - 33 edges
9. `ExportRecord` - 30 edges
10. `DiagnosticsLog` - 30 edges

## Surprising Connections (you probably didn't know these)
- `Browse and filtering` --references--> `MetadataHttp`  [INFERRED]
  docs/api/KISS18.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `API methods and routing` --references--> `MetadataPage`  [INFERRED]
  docs/api/NARTO.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `Download playback-state correction (current source)` --references--> `WatchProgressBar()`  [INFERRED]
  docs/UI_REFERENCE_CHECKLIST.md → app/src/main/java/com/anilili/ui/components/Common.kt
- `Verification plan` --references--> `SearchScreenTest`  [INFERRED]
  docs/ui-consistency-and-wrapping-audit.md → app/src/androidTest/java/com/example/animepahedl/SearchScreenTest.kt
- `Kiss18 public metadata API` --references--> `Kiss18Catalog`  [INFERRED]
  docs/api/KISS18.md → app/src/main/java/com/example/animepahedl/Kiss18Catalog.kt

## Import Cycles
- None detected.

## Communities (300 total, 139 thin omitted)

### Community 0 - "FloatingNavigationBar.kt"
Cohesion: 0.05
Nodes (15): chromeFade(), DownloaderNavigation(), barBlur(), barGlass(), barRim(), FloatingNavDisc(), FloatingNavIcon(), FloatingNavigationBar() (+7 more)

### Community 1 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 2 - "developer_notices.py"
Cohesion: 0.11
Nodes (24): broadcast(), cmd_announce(), cmd_announce_clear(), cmd_announce_fetch(), cmd_fetch(), on_event(), cmd_keygen(), cmd_publish() (+16 more)

### Community 3 - "ExternalPlaybackReturns.kt"
Cohesion: 0.08
Nodes (20): LibraryStore, ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs() (+12 more)

### Community 5 - "InPlayerEpisodeList.kt"
Cohesion: 0.05
Nodes (42): EpisodeItem, EpisodeLayout, GRID, LIST, EpisodeOrder, NEWEST_FIRST, OLDEST_FIRST, focusFill() (+34 more)

### Community 6 - "SubtitleDelay.kt"
Cohesion: 0.13
Nodes (5): SubtitleDelay, SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 8 - "PlayerControlsScaffold.kt"
Cohesion: 0.06
Nodes (28): SeriesLogo(), bufferedFraction(), BufferedSeekTrack(), controlsSeekAnchorFor(), PlayerActionPill(), PlayerActionRow(), playerActionRowScale(), PlayerBackArrow() (+20 more)

### Community 9 - "DeletionTestProvider.java"
Cohesion: 0.13
Nodes (3): DeletionTestProvider, OrderedWrites, OrderedWritesTest

### Community 10 - ".all"
Cohesion: 0.13
Nodes (9): DeletionRecoveryTest, AndroidDownloadRepository, DownloadIdentity, adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos(), ExportRecord, Exports (+1 more)

### Community 11 - "AniListModels.kt"
Cohesion: 0.10
Nodes (52): AiringCountEntry, AiringCountMedia, AiringCountPage, AiringDayCount, AiringSchedule, CoverImage, DiscoverOptions, DiscoverOptionsData (+44 more)

### Community 12 - "PlayerSurface.kt"
Cohesion: 0.05
Nodes (24): adaptiveVideoHeightCeiling(), applyCaptionEdgeInset(), applyCaptionStyle(), applyContentAspectRatio(), applyTextTrack(), applyVideoHeight(), applyVideoHeightCeiling(), availableVideoHeights() (+16 more)

### Community 13 - "Category"
Cohesion: 0.18
Nodes (12): Category, DUB, SUB, SourceVariant, DUB, GENERIC_SUB, HARD_SUB, SOFT_SUB (+4 more)

### Community 14 - "MediaCache.kt"
Cohesion: 0.06
Nodes (14): PreparationRetryTest, ProgressiveExportTest, VideoCacheSize, AUTOMATIC, GB_1, GB_2, MB_256, MB_512 (+6 more)

### Community 15 - "ExistingOutput.kt"
Cohesion: 0.11
Nodes (16): DownloadLocation, downloadLocation(), outputLocation(), safeFileName(), existingOutput(), ExistingOutputIndex, ExistingVideo, existingVideoName() (+8 more)

### Community 16 - "OpenSubtitlesClient.kt"
Cohesion: 0.12
Nodes (7): isJwtExpired(), jwtLongClaim(), normalizeSubtitleLanguages(), OpenSubtitleDownload, OpenSubtitlesClient, OpenSubtitleSearchResult, parseSearchResults()

### Community 18 - ".manager"
Cohesion: 0.11
Nodes (5): DownloadRetryService, MediaVerification, OfflineDownloads, Listener, OfflineDownloadService

### Community 19 - "SettingsStore.kt"
Cohesion: 0.06
Nodes (12): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), defaultQualityForDevice(), deviceDefaultQuality(), DownloadDestination (+4 more)

### Community 20 - "ui/components/WaterFillLogoIndicator.kt"
Cohesion: 0.07
Nodes (21): silhouetteFrame(), silhouetteMask(), BubbleSpec, cappedFrameSteps(), DetailedWaterFillLogo(), liquidLevel(), LoopHandover, PlainWaterFillLogo() (+13 more)

### Community 22 - "SiteDownloadSeries"
Cohesion: 0.09
Nodes (8): OnlineDramaHistoryTest, ContinueWatchingRow(), LibraryScreen(), PlaybackHistory, rememberSeriesPlaybackSummary(), SeriesPlaybackSummary, SiteDownloadEpisode, SiteDownloadSeries

### Community 23 - "CastRoutes.kt"
Cohesion: 0.06
Nodes (7): CastRoute, CastRoutes, GoogleCastRoute, SessionAvailabilityListener, mediaClientOrNull(), shouldGiveUpOnCastLoad(), MiruroCastMediaItemConverter

### Community 24 - ".operation"
Cohesion: 0.17
Nodes (5): reconcileDirectEnqueues(), forgetSavedDownloadRecord(), QueueOrder, PreparationJobs, PreparationWorker

### Community 25 - "SearchScreenTest.kt"
Cohesion: 0.05
Nodes (27): SearchScreenTest, 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed, 16. Spacing, chevrons, and text hierarchy are locally defined — P3, confirmed design divergence (+19 more)

### Community 26 - "ResolvedMedia"
Cohesion: 0.23
Nodes (5): isHls(), mediaRequestHeaders(), MediaRequests, ResolvedMedia, SiteDownloadDownloadTest

### Community 27 - "Composable"
Cohesion: 0.10
Nodes (22): rememberScreenReaderActive(), AppSymbol(), DestinationSummary(), DownloadReferenceEpisode, DownloadReferenceEpisodeRow(), DownloadReferenceSeries, DownloadReferenceSeriesCard(), ReferenceCornerBadge() (+14 more)

### Community 28 - "DramaPlayerSurface.kt"
Cohesion: 0.06
Nodes (5): PlayerAccentScope(), PlayerPanelTheme(), dramaPlayerGestures(), close(), persist()

### Community 29 - "NartoMetadataApi"
Cohesion: 0.15
Nodes (5): Document, classifyNartoLinkedTerms(), NartoMetadataApi, NartoTermGroups, toHttpUrlOrNull()

### Community 30 - "PlaybackService.kt"
Cohesion: 0.08
Nodes (4): CastRouteStatus, HostPlaybackSnapshot, PlaybackCacheBypassGate, PlaybackService

### Community 31 - ".editCaptionStyle"
Cohesion: 0.06
Nodes (23): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+15 more)

### Community 32 - "PipeModels.kt"
Cohesion: 0.14
Nodes (9): EpisodesResult, hasUsableWindow(), playsInWebView(), ProviderData, SkipTimes, SourcesResult, StreamItem, ProviderCatalog (+1 more)

### Community 33 - "NostrUpdateSource.kt"
Cohesion: 0.06
Nodes (23): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), readBoundedAnnouncementBody(), sanitizeAnnouncementUrl() (+15 more)

### Community 34 - "PlayerGestureControls.kt"
Cohesion: 0.06
Nodes (36): formatPlayerTime(), applyBrightness(), applyVolume(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureIndicatorSide, gestureIndicatorSide() (+28 more)

### Community 35 - "TelevisionSignals.kt"
Cohesion: 0.06
Nodes (34): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+26 more)

### Community 36 - "MegaPlayCdnHealth"
Cohesion: 0.18
Nodes (5): healthHost(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState

### Community 37 - "ThemeAccent"
Cohesion: 0.13
Nodes (13): ThemeAccent, BLOOD_RED, CUSTOM, CYAN, EMERALD, GOLD, MONOCHROME, NEON_GREEN (+5 more)

### Community 38 - "SleepTimer"
Cohesion: 0.08
Nodes (13): NetworkIdentity, Counting, EndOfEpisode, formatSleepRemaining(), Off, sleepFadeLevel(), SleepTimer, State (+5 more)

### Community 39 - "AppBackdrop.kt"
Cohesion: 0.12
Nodes (10): AppBackdrop, appCanvas(), appCanvasFade(), canvasColor(), centreCrop(), drawAppCanvas(), drawBackdropSlice(), hasAppBackdrop() (+2 more)

### Community 42 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 43 - "instrumentationregistry"
Cohesion: 0.18
Nodes (4): PreparationResponsivenessTest, SearchRecoveryTest, AnimeRepository, AnimeRepository

### Community 44 - "Test"
Cohesion: 0.06
Nodes (17): InputStream, copyExactly(), readBytesBounded(), selectCachedHlsVariant(), parseByteRange(), Details, Download, OperationFailure (+9 more)

### Community 45 - "LibraryModels.kt"
Cohesion: 0.15
Nodes (23): capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled(), historyAfterEpisodeWatched(), historyAfterRemoteProgress() (+15 more)

### Community 46 - "SavedFilesWatcher.kt"
Cohesion: 0.22
Nodes (4): missingSavedFiles(), querySavedFiles(), savedFilePresent(), SavedFilesWatcher

### Community 47 - "AnililiFailure"
Cohesion: 0.13
Nodes (10): AnililiException, AnililiFailure, anililiFailure(), asFailure(), classifyTransport(), isCancellation(), raise(), report() (+2 more)

### Community 48 - "DramatipMetadataApi.kt"
Cohesion: 0.24
Nodes (12): dramatipCards(), dramatipFlightObjects(), dramatipImages(), dramatipIsNextLabel(), dramatipLanguages(), dramatipMetadataUrl(), dramatipRequireNotFoundAbsent(), dramatipSubtitleTracks() (+4 more)

### Community 49 - "SiteDownloadSourcesTest"
Cohesion: 0.17
Nodes (5): cleanDramaTitle(), parseDramaSeries(), parseNartoVariants(), unescapeDramaFlight(), SiteDownloadSourcesTest

### Community 50 - "SubtitleCueLayout.kt"
Cohesion: 0.14
Nodes (18): CaptionPosition, BOTTOM, TOP, autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues() (+10 more)

### Community 51 - "SubtitleTrackMerge.kt"
Cohesion: 0.12
Nodes (36): SubtitleItem, assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), buildSignsMerge() (+28 more)

### Community 52 - "SiteDownloadSources.kt"
Cohesion: 0.15
Nodes (22): toAppMetadata(), decryptDramaSource(), dramaArray(), dramaInt(), dramaJsonAfter(), dramaObject(), dramaProxy(), dramaProxyBackup() (+14 more)

### Community 53 - "CaptionAppearance.kt"
Cohesion: 0.11
Nodes (12): focusHalo(), focusHighlight(), phoneRailPosterWidth(), CaptionAppearanceDialog(), CaptionAppearanceEditor(), CaptionChoiceRow(), captionOffset(), CaptionPlacementPad() (+4 more)

### Community 54 - "MidnightDialog.kt"
Cohesion: 0.07
Nodes (26): DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice(), CoverScreenWithDialogWindow(), MidnightBodyTopStop(), MidnightConfirmDialog() (+18 more)

### Community 55 - "DownloadStore"
Cohesion: 0.13
Nodes (5): DownloadDao, DownloadDatabase, DownloadStore, PendingOperation, StoredDownload

### Community 56 - "AudioOutputFormat.kt"
Cohesion: 0.15
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 58 - ".event"
Cohesion: 0.11
Nodes (10): DebugTrace, SessionBrowser(), WebViewClient, S1 — Cache server reads unbounded lines before checking their length (P2, high confidence), S2 — Remote response bodies and pagination lack resource bounds (P2, high confidence), S3 — Verification completion accepts unrelated HTTPS pages (P2, high confidence), S4 — Session reset can race background network users (P2, high confidence), S5 — Native artifact verification checks size, not identity (P2, high confidence) (+2 more)

### Community 59 - "PopupGlass.kt"
Cohesion: 0.16
Nodes (5): blurLine(), boxBlur(), PopupBackdrop, popupGlass(), rememberPopupBackdrop()

### Community 61 - "AnimeRepository.kt"
Cohesion: 0.20
Nodes (5): AnimeNetwork, HttpFailure, isMediaPath(), toHttpUrlOrNullSafe(), VerificationRequired

### Community 62 - "ThemeSettings.kt"
Cohesion: 0.22
Nodes (7): accentSeedOf(), AppBackdropSource, ANIME, IMAGE, NONE, backdropUrlOf(), ProfileLook

### Community 63 - "ThemePreset"
Cohesion: 0.15
Nodes (12): ThemePreset, CRIMSON_NIGHT, CYBER, DEEP_SEA, EMBER, FOREST, GILDED, HIGH_CONTRAST (+4 more)

### Community 64 - "FlixcloudPlaylistDataSource.kt"
Cohesion: 0.05
Nodes (17): decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace(), hlsIv(), KwikPlaylistDataSource, kwikProgressiveMp4() (+9 more)

### Community 67 - "SiteDownloadRepository"
Cohesion: 0.19
Nodes (16): Page, SiteDownloadRepository, chooseDramaVariant(), chooseNartoVariant(), dramaAbsolute(), dramaHeight(), dramaHost(), dramaMetadata() (+8 more)

### Community 68 - "TvFocusTarget.kt"
Cohesion: 0.15
Nodes (4): rememberTvFocusTarget(), TvFocusTarget, TvFocusTargetElement, TvFocusTargetNode

### Community 69 - "CastButton"
Cohesion: 0.11
Nodes (7): applyCastTextTrack(), CastButton(), Callback, openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 70 - "SiteMetadataApi.kt"
Cohesion: 0.25
Nodes (21): browse(), details(), metadataFromDocument(), metadataFromHtml(), MetadataDetails, MetadataEpisode, metadataEpisode(), MetadataImage (+13 more)

### Community 72 - "ResultSort"
Cohesion: 0.21
Nodes (8): findMediaUrl(), ResultSort, NEWEST, OLDEST, RELEVANCE, TITLE, sortResults(), AnimeRepositoryTest

### Community 73 - "MediaListEntry"
Cohesion: 0.18
Nodes (8): remoteListScores(), remoteListStatuses(), CustomListNamesSerializer, GqlMediaListResponse, MediaListCollection, MediaListCollectionData, MediaListEntry, MediaListGroup

### Community 74 - "Anilili UI reference audit"
Cohesion: 0.12
Nodes (16): Anilili UI reference audit, Automated source comparison, Build/package verification, Complete active shared-component inventory, Download playback-state correction (current source), Downloads, details and settings, Explicit backend adaptations, Home and Browse (+8 more)

### Community 75 - ".manager"
Cohesion: 0.11
Nodes (7): CacheServerTest, HlsExportTest, TimerTask, TimerTask, CachedHlsServer, ffmpegRemuxCachedHls(), requireCachedHlsSegments()

### Community 76 - "DefaultQuality"
Cohesion: 0.18
Nodes (8): DefaultQuality, AUTO, HIGHEST, P1080, P360, P480, P720, dataSaverFallbackMessage()

### Community 77 - "AnimepaheDL resume state"
Cohesion: 0.09
Nodes (23): ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task, Current follow-up release, Custom downloader only, Dedicated details and download-range dialog, Environment and hosting, Existing work and reference (+15 more)

### Community 79 - "ExternalSubtitleFiles.kt"
Cohesion: 0.22
Nodes (14): AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName(), downloadOpenSubtitle(), exportedSubtitleMimeType(), extensionFor(), importSubtitleFile() (+6 more)

### Community 80 - "TvTextField.kt"
Cohesion: 0.05
Nodes (10): tvFocusHandoff(), tvFocusTarget(), rememberSystemUses24Hour(), ContentObserver, playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute() (+2 more)

### Community 81 - "ProviderSubtitles.kt"
Cohesion: 0.12
Nodes (8): fetchProviderSubtitleBytes(), MediaSubtitle, normalizeProviderSubtitle(), providerSubtitleRequest(), ProviderSubtitles, unreferencedProviderSubtitleFile(), ReferenceResolved, ProviderSubtitleDeletionTest

### Community 82 - "AppDns"
Cohesion: 0.06
Nodes (13): AppDns, DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY, DNS_SB (+5 more)

### Community 83 - "PreferredAudioLanguage"
Cohesion: 0.10
Nodes (18): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+10 more)

### Community 84 - "dramatipPublicPosterUrl"
Cohesion: 0.27
Nodes (3): dramatipPublicPosterUrl(), DramatipSessionCookies, freshDramatipPoster()

### Community 85 - "AppIcon"
Cohesion: 0.10
Nodes (21): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+13 more)

### Community 86 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 87 - ".check"
Cohesion: 0.22
Nodes (3): AnnouncementManager, AnnouncementScheduler, NotificationPreferences()

### Community 88 - "LocalStreamProxy.kt"
Cohesion: 0.12
Nodes (11): dispatchAcceptedSocket(), HttpOrigin, namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readLine(), readRequest(), RequestHeaderBudget (+3 more)

### Community 90 - "TvPlayerControls.kt"
Cohesion: 0.06
Nodes (10): AppUiScale(), UnscaledUi(), formatTvPlayerTime(), TvControlButton(), TvPlayerBackArrow(), TvPlayerControls(), tvPointerSeekMs(), tvScrubStepMs() (+2 more)

### Community 91 - "LocalStreamProxy"
Cohesion: 0.20
Nodes (4): Entry, LocalStreamProxy, RootHeaderContext, ServerEndpoint

### Community 92 - "PlayerSystemBars.kt"
Cohesion: 0.25
Nodes (6): PlayerBars, ALL, NONE, STATUS_ONLY, playerBarsFor(), PlayerSystemBarsEffect()

### Community 93 - "PlayerSettingsSheet.kt"
Cohesion: 0.09
Nodes (36): MidnightInlineNotice(), MidnightChoiceRow(), midnightFocus(), MidnightHelper(), midnightInitialFocus(), MidnightRowScaffold(), MidnightSectionLabel(), midnightSelectable() (+28 more)

### Community 95 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 100 - "CatalogViewModel.kt"
Cohesion: 0.15
Nodes (10): catalogCacheJson(), catalogPageForSource(), CatalogSource, DRAMATIP, HUANGGUO, KISS18, NARTO, CatalogSourceOrder (+2 more)

### Community 105 - "Test"
Cohesion: 0.19
Nodes (3): requireNoHttpUserInfo(), LocalStreamProxyResourceLimitsTest, Callback

### Community 106 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 107 - "PlatformErrorCodes.kt"
Cohesion: 0.12
Nodes (10): playbackFailureEntry(), httpFailureSummary(), shouldRetryDecoderAtLowerVideoResolution(), numberKeyDigit(), shouldRecoverSeekError(), tvDpadSeekPresses(), tvNumberSeekTargetMs(), tvSeekTargetMs() (+2 more)

### Community 110 - "CatalogPage"
Cohesion: 0.18
Nodes (6): catalogPosterUrl(), DramatipCatalog, parseDramatipCatalog(), HuangguoCatalog, CatalogPage, SiteCatalog

### Community 112 - "AppGraph.kt"
Cohesion: 0.14
Nodes (7): AppGraph, Profiles, File inventory, Necessary host adaptations, Original Anilili player port, Player-port release verification (2026-11-08), Review checklist

### Community 113 - ".page"
Cohesion: 0.19
Nodes (4): catalogImage(), CatalogSessionCookies, NartoCatalog, OtherCatalogHttp

### Community 114 - "PlaybackBuffer.kt"
Cohesion: 0.10
Nodes (11): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE, AdjustableLoadControl, playbackBufferBytes() (+3 more)

### Community 115 - "HlsExportTest.kt"
Cohesion: 0.08
Nodes (3): EpisodeDownloads, copyCancellable(), copyCachedProgressive()

### Community 116 - "Reliability and correctness findings"
Cohesion: 0.20
Nodes (10): R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction), R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence), R3 — Direct enqueue has a process-death orphan window (P1, high confidence), R4 — HLS validation does not require expected audio (P1, high confidence), R5 — Final destination validation is weaker than staging validation (P1, high confidence), R6 — Unhandled action failures can crash the app (P2, high confidence), R7 — Retry can discard the only reference to unfinished output (P2, high confidence), R8 — Export service waits throughout transfers (P2, high confidence) (+2 more)

### Community 118 - "SiteDownloadPreparation"
Cohesion: 0.19
Nodes (11): onResume(), PreparationStatus, PreparationStatusRow(), isPreparedQueueRow(), mergePreparedQueueRows(), pendingEpisodeRowId(), appendSitePreparation(), SiteDownloadPreparation (+3 more)

### Community 119 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 120 - "verificationPage"
Cohesion: 0.39
Nodes (3): verificationPage(), verificationOriginMatches(), VerificationPageTest

### Community 121 - "SiteDownloadRepositoryTest"
Cohesion: 0.22
Nodes (3): parseSiteDownloadSelection(), SiteDownloadRepositoryTest, SiteDownloadSelectionSafetyTest

### Community 122 - "Listener"
Cohesion: 0.13
Nodes (5): contentAspectRatio(), diagnosticSummary(), Listener, preferredAudioTrackCodes(), stateName()

### Community 123 - "WatchedFilter"
Cohesion: 0.29
Nodes (5): DiscoverFilters, WatchedFilter, COMPLETED, OFF, STARTED

### Community 124 - "ThemeStyle"
Cohesion: 0.29
Nodes (5): ThemeStyle, NEUTRAL, TONAL, VIBRANT, VIVID

### Community 126 - "Theme.kt"
Cohesion: 0.52
Nodes (4): appColors(), ThemeSpec, wallpaperSeed(), AnimepaheDLTheme()

### Community 128 - "siteDownloadCookieHeader"
Cohesion: 0.31
Nodes (6): mergeSiteDownloadCookies(), normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), validCookiePair(), validSiteDownloadCookieInput(), SiteDownloadCookieTest

### Community 130 - "TvPlayerControl"
Cohesion: 0.15
Nodes (13): TvPlayerControl, CAPTIONS, EPISODES, FORWARD, FULLSCREEN, MUTE, MY_LIST, NEXT (+5 more)

### Community 132 - "SkipTimingStatus"
Cohesion: 0.15
Nodes (12): SkipReportState, FAILED, IDLE, SENDING, SENT, SkipTimingStatus, ANISKIP, CHECKING (+4 more)

### Community 133 - "Viewer"
Cohesion: 0.33
Nodes (6): AnimeStat, GqlViewerResponse, UserAvatar, Viewer, ViewerData, ViewerStatistics

### Community 135 - "LibraryNavigationTest"
Cohesion: 0.13
Nodes (7): DownloadSelectionTest, LibraryNavigationTest, AppSettings, downloadsPendingIntent(), Verification plan, Device verification, FFmpeg

### Community 136 - "OpenSubtitlesPanel"
Cohesion: 0.29
Nodes (4): defaultOpenSubtitlesLanguages(), openSubtitlesEpisodeField(), OpenSubtitlesPanel(), SubtitleResultRow()

### Community 137 - "RemoteDramaPlayer.kt"
Cohesion: 0.14
Nodes (5): DramaPlayerEpisode, ShortDramaPlayer(), nartoPlaybackCandidate(), RemoteDramaPlayer(), SavedShowsStore

### Community 138 - "DpadLongPress.kt"
Cohesion: 0.29
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 139 - "Anime"
Cohesion: 0.22
Nodes (8): AnimeRepository, Anime, AnimeDetails, AnimeRepository, Episode, text(), loadBookmarks(), toggleBookmark()

### Community 140 - "ThemeBackground"
Cohesion: 0.33
Nodes (3): ThemeBackground, AMOLED, DARK

### Community 142 - "PlatformErrorCodes.kt"
Cohesion: 0.25
Nodes (6): causeChain(), platformErrorCode(), platformErrorName(), technicalDetail(), build.sh script, usage()

### Community 143 - "Test"
Cohesion: 0.11
Nodes (3): DramaPlayerGesturesTest, CatalogViewModelTest, DownloadGatewayTest

### Community 144 - "CatalogScreen"
Cohesion: 0.15
Nodes (8): ArtworkRecovery, CatalogFailureState, catalogFailureState(), shouldPreserveCatalogResultsOnRefresh(), CatalogHero(), catalogArtworkRequest(), CatalogPosterCard(), CatalogItem

### Community 145 - ".row"
Cohesion: 0.19
Nodes (10): DownloadGroupProgress, downloadGroupProgress(), SaveState, CHECKING, COPYING, FAILED, NONE, PREPARING (+2 more)

### Community 146 - "OpenSubtitlesPanel.kt"
Cohesion: 0.07
Nodes (4): EpisodeArtwork(), NextEpisodeAction(), NextEpisodeCard(), ScrollHintColumn()

### Community 147 - "MidnightSettings.kt"
Cohesion: 0.08
Nodes (14): EqualizerWaveIndicator(), MidnightNoticeTone, ERROR, INFO, SUCCESS, WARNING, MidnightChoicePicker(), MidnightSettingRow() (+6 more)

### Community 148 - "BackdropBlur.kt"
Cohesion: 0.17
Nodes (9): GlassSection, BROWSE, DETAILS, HOME, NAVIGATION, POPUPS, REFRESH, backdropBlur() (+1 more)

### Community 149 - "Player"
Cohesion: 0.19
Nodes (11): applyAudioTrack(), applyCategoryAudioPreference(), applyPreferredAudioLanguage(), applyPreferredSubtitleLanguage(), CategoryAudioDecision, categoryAudioDecision(), NotReady, preferredLanguageCodes() (+3 more)

### Community 150 - "Real-device verification"
Cohesion: 0.18
Nodes (10): Current pending release: player controls and download playback state, Final saved-shows verification (cheaper subagent), Found and fixed, Library and Settings/announcements release, Notice failure follow-up and Korean-only KissKH, Offline-preferred playback (pending device verification), Player-port release verification (2026-11-08), Real-device verification (+2 more)

### Community 152 - "ThemeContrast"
Cohesion: 0.33
Nodes (4): ThemeContrast, HIGH, MEDIUM, STANDARD

### Community 153 - "ErrorAction"
Cohesion: 0.20
Nodes (9): ErrorAction, CHANGE_SERVER, CHOOSE_FILE, FREE_SPACE, OPEN_SETTINGS, RETRY, RETRY_EXPORT, SEND_DIAGNOSTICS (+1 more)

### Community 154 - "HlsPlaylistRewrite.kt"
Cohesion: 0.42
Nodes (7): isDeclaredHlsPlaylist(), looksLikeHlsPlaylist(), replaceUri(), resolveAndRegister(), rewriteHlsPlaylist(), rewriteLine(), rewriteUriAttribute()

### Community 155 - "c"
Cohesion: 0.40
Nodes (3): iso3Language(), selectedTextLanguage(), syncableSubtitle()

### Community 156 - "NextEpisodeTrigger"
Cohesion: 0.20
Nodes (10): EmbedAutoAdvanceStep, embedAutoAdvanceStep(), ADVANCE, ARM, WAIT, NextEpisodeTrigger, nextEpisodeTrigger(), AFTER_CREDITS (+2 more)

### Community 158 - "SubtitleCueTimes.kt"
Cohesion: 0.39
Nodes (10): loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes(), parseTimestampMs(), stripMarkup() (+2 more)

### Community 159 - "Browse, play and download"
Cohesion: 0.20
Nodes (9): Anilili Short Dramas, Browse, play and download, Build, Downloads, Library and recovery tools, Release checks, Sessions, Supported sources (+1 more)

### Community 161 - "EpisodeSource"
Cohesion: 0.25
Nodes (8): episodeTitle(), audioLabel(), audioTag(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 164 - "TvPlayerBackTarget"
Cohesion: 0.22
Nodes (9): TvPlayerBackTarget, CAPTION_APPEARANCE, CAPTIONS, tvPlayerBackTarget(), CONTROLS, EPISODE_DRAWER, NONE, OPEN_SUBTITLES (+1 more)

### Community 165 - "Kind"
Cohesion: 0.40
Nodes (5): AppNotification, Kind, AIRING, MEDIA, SOCIAL

### Community 168 - "DownloadQuality"
Cohesion: 0.25
Nodes (6): DownloadQuality, BEST, P1080, P360, P480, P720

### Community 170 - "ExportService.kt"
Cohesion: 0.08
Nodes (8): ExistingOutputTest, IntegrityTest, DownloadCompleteReceiver, ExportProgress, onBind(), onCreate(), savingNotification(), deleteOwnedMedia()

### Community 171 - "ViewerOptionsData"
Cohesion: 0.50
Nodes (4): GqlViewerOptionsResponse, ViewerOptions, ViewerOptionsData, ViewerWithOptions

### Community 172 - "TvEpisodeChangeFocus"
Cohesion: 0.32
Nodes (8): TvEpisodeChangeFocus, tvEpisodeChangeFocus(), CONTROLS, HOLD, SURFACE, UNCHANGED, tvFocusLostTarget(), tvOverlayDismissFocus()

### Community 174 - "DownloadRow"
Cohesion: 0.14
Nodes (13): downloadGroupArtwork(), DownloadLibrarySnapshot, downloadLibrarySnapshot(), DownloadRow, savedFileLocationStatus(), DownloadsScreen(), friendlyDownloadStatus(), DownloadGroupArtworkTest (+5 more)

### Community 178 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 181 - "StreamRefusalLoadErrorPolicy.kt"
Cohesion: 0.33
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 189 - "FailureReason"
Cohesion: 0.07
Nodes (29): Availability, SAVED, STAGING, classifyFailure(), DownloadAction, DELETE, PAUSE, RESUME (+21 more)

### Community 190 - "ClockFormat"
Cohesion: 0.25
Nodes (5): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, timeOfDayFormatter()

### Community 198 - "SiteMetadataApi"
Cohesion: 0.08
Nodes (19): MetadataSite, DRAMATIP, KISS18, NARTO, SiteMetadataApi, SiteMetadataApis, KissKH metadata API, Query/API use (+11 more)

### Community 199 - "DownloaderViewModel.kt"
Cohesion: 0.06
Nodes (8): AnimeApplication, normalizeOutputFolder(), consumeCancellable(), SiteDownloadCookieStore, isPendingRow(), pausePending(), resumePending(), OutputFolderTest

### Community 205 - ".relay"
Cohesion: 0.27
Nodes (4): BodyHead, ProgressOutputStream, readBody(), readHead()

### Community 208 - "EroticMV metadata API"
Cohesion: 0.40
Nodes (4): Endpoints and operations, EroticMV metadata API, Fields retained, Live evidence and limitations

### Community 212 - "TvNativeTextField"
Cohesion: 0.21
Nodes (4): beginNativeTvEditing(), hideNativeTvKeyboard(), isImeVisible(), SafeTvEditText

### Community 224 - "GestureDragAxis"
Cohesion: 0.67
Nodes (3): GestureDragAxis, Horizontal, Vertical

### Community 227 - "DiscordStatus"
Cohesion: 0.10
Nodes (11): DiscordStatus, DO_NOT_DISTURB, IDLE, ONLINE, RelatedLayout, LIST, TITLES, UiScale (+3 more)

### Community 230 - "Uri"
Cohesion: 0.10
Nodes (5): DownloadRepository, inspectStorage(), StorageReport, verifyCopy(), OutputStream

### Community 242 - "TvTextInputType"
Cohesion: 0.40
Nodes (5): TvTextInputType, EMAIL, NUMBER, PASSWORD, TEXT

### Community 254 - "TvNativeTextField"
Cohesion: 0.25
Nodes (3): disableSelectionActionMode(), moveNativeFocusSafely(), TvNativeTextField()

### Community 257 - "SiteDownloadProvider"
Cohesion: 0.15
Nodes (12): ReferencePlayerHostPolicy, MetadataTermChips(), SiteMetadataHighlights(), MediaLinkRefreshGuard, SiteDownloadProvider, DIRECT, DRAMATIP, EROTICMV (+4 more)

### Community 259 - "MenuLanguage"
Cohesion: 0.29
Nodes (4): MenuLanguage, ENGLISH, SPANISH, SYSTEM

### Community 267 - "PlayerSurface"
Cohesion: 0.05
Nodes (37): PlayerContentScale, CROP, FILL, FIT, Routes, AutoSkipCountdown, autoSkipCountdownLabel(), runAutoSkipCountdown() (+29 more)

### Community 277 - "ReferenceChromeTokens.kt"
Cohesion: 0.43
Nodes (3): appCanvasFade(), drawCanvasFade(), Midnight

### Community 282 - "SessionBrowser"
Cohesion: 0.07
Nodes (25): AnimepaheDL codebase review, Architecture and strengths, P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3), P5 — Persistence schema and orchestration are difficult to evolve (P3), Performance and maintainability (+17 more)

### Community 284 - "AppBackdropDim"
Cohesion: 0.33
Nodes (4): AppBackdropDim, LIGHT, MEDIUM, STRONG

### Community 285 - "Kiss18 public metadata API"
Cohesion: 0.29
Nodes (6): Absent or conditional information, Browse and filtering, Coverage and retained data, Kiss18 public metadata API, Sources actually visited, Verification

### Community 289 - "ProxyActiveResources"
Cohesion: 0.15
Nodes (3): BoundedUrlTokenRegistry, ProxyActiveResources, UrlRegistryCapacityException

### Community 290 - "cached"
Cohesion: 0.47
Nodes (3): opensTvPlayerControls(), cached(), Key

### Community 294 - "UiState"
Cohesion: 0.29
Nodes (5): Error, Loading, rethrowIfCancellation(), Success, UiState

### Community 296 - "asserttrue"
Cohesion: 0.22
Nodes (3): isOutputSubtitleSidecar(), ReferencePlayerHostPolicyTest, SafeDeletionTest

### Community 299 - "DramatipMetadataApi"
Cohesion: 0.13
Nodes (8): DramatipMetadataApi, MetadataHttp, MetadataResponse, DramatipMetadataFixtureTest, Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 319 - ".configureRequestHeaders"
Cohesion: 0.15
Nodes (3): PlaybackStatus, close(), select()

## Knowledge Gaps
- **478 isolated node(s):** `NotReady`, `Settled`, `Preparation`, `Search`, `Sources` (+473 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1316 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **139 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SettingsStore` connect `SettingsStore` to `FloatingNavigationBar.kt`, `MenuLanguage`, `InPlayerEpisodeList.kt`, `PlayerSurface`, `PlayerSurface.kt`, `ThemeBackground`, `MediaCache.kt`, `Category`, `OpenSubtitlesClient.kt`, `OpenSubtitlesPanel.kt`, `SettingsStore.kt`, `BackdropBlur.kt`, `ThemeContrast`, `AppBackdropDim`, `DramaPlayerSurface.kt`, `PlaybackService.kt`, `.editCaptionStyle`, `PlayerGestureControls.kt`, `TelevisionSignals.kt`, `ThemeAccent`, `AppBackdrop.kt`, `DownloadQuality`, `SourceFilterLanguage`, `SubtitleCueLayout.kt`, `CaptionAppearance.kt`, `ClockFormat`, `ThemeSettings.kt`, `ThemePreset`, `DefaultQuality`, `AppDns`, `PreferredAudioLanguage`, `PreferredSubtitleLanguage`, `TvPlayerControls.kt`, `PlayerSystemBars.kt`, `PlayerSettingsSheet.kt`, `DiscordStatus`, `PosterElement`, `AppGraph.kt`, `PlaybackBuffer.kt`, `.init`, `WatchedFilter`, `ThemeStyle`, `Theme.kt`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `AutoSkipButtonBounds()`) actually correct?**
  _`PlayerSurface()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **What connects `NotReady`, `Settled`, `Preparation` to the rest of the system?**
  _478 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FloatingNavigationBar.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05404551201011378 - nodes in this community are weakly interconnected._
- **Why does `DiagnosticsLog` connect `DiagnosticsLog` to `ExternalPlaybackReturns.kt`, `PlayerSurface.kt`, `MediaCache.kt`, `OpenSubtitlesPanel.kt`, `SettingsStore.kt`, `CastRoutes.kt`, `PlaybackService.kt`, `SleepTimer`, `AppBackdrop.kt`, `AnililiFailure`, `FlixcloudPlaylistDataSource.kt`, `ArtworkBitmapLoader.kt`, `AnnouncementManager.kt`, `TvTextField.kt`, `AppDns`, `LocalStreamProxy.kt`, `dispatchers`, `OpenSubtitlesAuthManager.kt`, `LegacyTrustAnchors.kt`, `PlaybackDiagnostics.kt`, `PlaybackBuffer.kt`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `AnililiError` connect `AnililiError` to `OpenSubtitlesAuthManager.kt`, `ExternalPlaybackReturns.kt`, `PlatformErrorCodes.kt`, `PlayerSurface.kt`, `AnililiFailure`, `OpenSubtitlesPanel.kt`, `ErrorDomain`, `ErrorAction`, `DramaPlayerSurface.kt`, `PlayerSettingsSheet.kt`?**
  _High betweenness centrality (0.045) - this node is a cross-community bridge._