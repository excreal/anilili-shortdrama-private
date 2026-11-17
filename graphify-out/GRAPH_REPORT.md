# Graph Report - AnililiShortDrama  (2026-11-17)

## Corpus Check
- 324 files · ~289,921 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 38 file(s) not represented in the graph (top: .xml 22, (none) 3, .ttf 3)

## Summary
- 4935 nodes · 11970 edges · 351 communities (158 shown, 193 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 783 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9d1200ab`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AppIcon
- AnililiError
- developer_notices.py
- ExternalPlayer.kt
- SettingsStore
- InPlayerEpisodeList.kt
- TvFocusTarget.kt
- FocusFill.kt
- TvPlayerControls.kt
- SiteDownloadSourcesTest
- .all
- AniListModels.kt
- PlayerSurface.kt
- PipeModels.kt
- CaptionPosition
- ExistingOutput.kt
- PopupGlass.kt
- .manager
- .editCaptionStyle
- ui/components/WaterFillLogoIndicator.kt
- ErrorNotice.kt
- SiteDownloadSeries
- CastRoutes.kt
- PreparationJobs.kt
- SearchScreenTest.kt
- ExportRecord
- SubtitleSearchLanguage
- cached
- NartoMetadataApi
- PlaybackService.kt
- .editCaptionStyle
- AnimeRepository.kt
- NostrUpdateSource.kt
- PlayerGestureControls
- TelevisionSignals.kt
- Theme.kt
- ThemeAccent
- SleepTimer
- AppBackdrop.kt
- SiteDownloadRepositoryTest.kt
- SourceFilterLanguage
- ThreadTraceListener.java
- Test
- LibraryModels.kt
- SavedFilesWatcher.kt
- AnililiFailure
- SubtitleDelay.kt
- MegaPlayCdnHealth.kt
- AppDeviceProfile.kt
- SubtitleTrackMerge.kt
- SiteDownloadSources.kt
- SiteDownloadRepository
- Composable
- DownloadStore
- AudioOutputFormat.kt
- Composable
- SessionBrowser.kt
- ProviderSubtitles.kt
- AppColorScheme.kt
- SiteMetadataApi.kt
- ThemeSettings.kt
- ThemePreset
- KwikPlaylistDataSource.kt
- .verifyExport
- tohttpurl
- DeletionTestProvider.java
- TvFocusTarget.kt
- CastUi.kt
- AutoSkipCountdown.kt
- ArtworkBitmapLoader.kt
- ProgressiveExportTest.kt
- SubtitleCueLayout.kt
- SiteDownloadProvider
- AnnouncementManager.kt
- AutoDns
- AnimepaheDL resume state
- AppSymbol
- ExternalSubtitleFiles.kt
- ClockFormat
- AppDns
- PreferredAudioLanguage
- ForwardingPlayer
- DpadLongPress.kt
- PreferredSubtitleLanguage
- CatalogPage
- LocalStreamProxy.kt
- Modifier
- .add
- LocalStreamProxy
- TvTextField.kt
- dispatchers
- ContinueWatchingRow
- SiteDownloadEpisode
- .switchPlaybackPlayer
- SecureTokenStore.kt
- OptIn
- DramatipMetadataApi.kt
- .transform
- SiteDownloadCookieStore
- LegacyTrustAnchors.kt
- DramaPlayerGesturesTest.kt
- PlayerFillerSkip.kt
- Test
- PosterElement
- SheetSections
- PlaybackDiagnostics.kt
- CatalogPage
- FloatingNavigationBar.kt
- .page
- PlaybackBuffer.kt
- DownloadAction
- SiteDownloadPreparation
- ErrorDomain
- Dp
- SiteDownloadRepositoryTest
- ExportService.kt
- CatalogPosterCard
- ThemeStyle
- MidnightSettingSwitchRow
- Player
- FlixcloudPlaylistDataSource.kt
- DohProvider
- DiscordStatus
- SkipTimingStatus
- OpenSubtitlesAuthManager
- LibraryNavigationTest
- LocalStreamProxyService.kt
- SettingsStore.kt
- SubtitleDelay
- CacheServerTest.kt
- ThemeBackground
- Thumbnail
- Test
- NetworkIdentity.kt
- .row
- OpenSubtitlesPanel.kt
- DiagnosticsLog
- Narto Drama public metadata API
- DownloadSelectionTest.kt
- Real-device verification
- ThemeContrast
- ErrorAction
- HlsPlaylistRewrite.kt
- MetadataBrowseQuery
- instrumentationregistry
- SubtitleCueTimes.kt
- Browse, play and download
- EpisodeSource
- Reliability and correctness findings
- FfmpegRemux.kt
- Listener
- DownloadRow
- ProviderBenchmark
- GlassSection
- .init
- DownloaderViewModel.kt
- MidnightDialog.kt
- MediaCache.kt
- isExportDurationComplete
- NextEpisodeTrigger
- Anilili feature audit checklist
- PlayerContentScale
- AnimepaheDL codebase review
- StreamRefusalLoadErrorPolicy.kt
- TvPlayerBackTarget
- UiState
- TransferState
- NartoMetadataApi.kt
- Kiss18 public metadata API
- RelatedLayout
- OperationRecovery.kt
- UniqueStreamKeyDataSource
- AnimeNetwork
- ReferencePlaybackRegistry
- SiteMetadataApi
- dispatchers
- OrderedWrites
- TvEpisodeChangeFocus
- DownloadQuality
- LocalStreamProxyHeaderPolicyTest
- DiagnosticsLog
- .write
- UiScale
- EroticMV metadata API
- .add
- CatalogSource
- ExampleUnitTest.kt
- Device UI verification
- Developer announcements
- AGENTS.md
- components/README.md
- docs/KISSKH.md
- THIRD_PARTY_NOTICES.md
- make-export-benchmark.sh
- SessionBrowser
- MenuLanguage
- PlayerSurface
- ReferenceChromeTokens.kt
- AppBackdropDim
- Callback
- DesignSystem.kt
- ProxyActiveResources
- SiteDownloadViewModel.kt
- InterceptionRetry
- TransferListener
- SafeDeletion.kt
- Base64Compat
- ExportedSubtitle
- SaveState
- ExampleInstrumentedTest.kt
- PlaylistForeignSegments.kt
- GestureDragAxis

## God Nodes (most connected - your core abstractions)
1. `SettingsStore` - 148 edges
2. `PlayerSurface()` - 134 edges
3. `AnililiError` - 113 edges
4. `PlaybackService` - 44 edges
5. `rememberFocusFill()` - 41 edges
6. `SiteDownloadSeries` - 38 edges
7. `NartoMetadataApi` - 37 edges
8. `SourceFilterLanguage` - 33 edges
9. `ExportRecord` - 30 edges
10. `SiteDownloadProvider` - 30 edges

## Surprising Connections (you probably didn't know these)
- `Browse and filtering` --references--> `MetadataHttp`  [INFERRED]
  docs/api/KISS18.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `API methods and routing` --references--> `MetadataPage`  [INFERRED]
  docs/api/NARTO.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `Download playback-state correction (current source)` --references--> `WatchProgressBar()`  [INFERRED]
  docs/UI_REFERENCE_CHECKLIST.md → app/src/main/java/com/anilili/ui/components/Common.kt
- `Kiss18 public metadata API` --references--> `Kiss18Catalog`  [INFERRED]
  docs/api/KISS18.md → app/src/main/java/com/example/animepahedl/Kiss18Catalog.kt
- `Necessary host adaptations` --references--> `ProviderCatalog`  [INFERRED]
  docs/REFERENCE_PLAYER_PORT.md → app/src/main/java/com/anilili/data/ProviderCatalog.kt

## Import Cycles
- None detected.

## Communities (351 total, 193 thin omitted)

### Community 0 - "AppIcon"
Cohesion: 0.10
Nodes (21): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+13 more)

### Community 1 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 2 - "developer_notices.py"
Cohesion: 0.05
Nodes (47): AppGraph, Profiles, File inventory, Necessary host adaptations, Original Anilili player port, Player-port release verification (2026-11-08), Review checklist, Anilili UI reference audit (+39 more)

### Community 3 - "ExternalPlayer.kt"
Cohesion: 0.07
Nodes (26): LibraryStore, ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs() (+18 more)

### Community 5 - "InPlayerEpisodeList.kt"
Cohesion: 0.07
Nodes (38): EpisodeItem, EpisodeLayout, GRID, LIST, EpisodeOrder, NEWEST_FIRST, OLDEST_FIRST, rememberFocusFill() (+30 more)

### Community 6 - "TvFocusTarget.kt"
Cohesion: 0.15
Nodes (6): rememberTvFocusTarget(), tvFocusHandoff(), TvFocusTarget, tvFocusTarget(), TvFocusTargetElement, TvFocusTargetNode

### Community 8 - "TvPlayerControls.kt"
Cohesion: 0.05
Nodes (36): bufferedFraction(), BufferedSeekTrack(), controlsSeekAnchorFor(), formatPlayerTime(), PlayerActionPill(), PlayerActionRow(), playerActionRowScale(), PlayerBackArrow() (+28 more)

### Community 10 - ".all"
Cohesion: 0.11
Nodes (10): DeletionRecoveryTest, AndroidDownloadRepository, forgetSavedDownloadRecord(), hasOutputFolderAccess(), OutputFolderAccessRequired, adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos() (+2 more)

### Community 11 - "AniListModels.kt"
Cohesion: 0.06
Nodes (78): AiringCountEntry, AiringCountMedia, AiringCountPage, AiringDayCount, AiringSchedule, AnimeStat, AppNotification, CoverImage (+70 more)

### Community 12 - "PlayerSurface.kt"
Cohesion: 0.03
Nodes (17): adaptiveVideoHeightCeiling(), applyTextTrack(), applyVideoHeight(), applyVideoHeightCeiling(), CaptionDefaults, captionDefaultsAfterChoice(), categoryAudioRank(), clearVideoSelection() (+9 more)

### Community 13 - "PipeModels.kt"
Cohesion: 0.09
Nodes (18): Category, DUB, SUB, EpisodesResult, playsInWebView(), ProviderData, SkipTimes, SourcesResult (+10 more)

### Community 14 - "CaptionPosition"
Cohesion: 0.10
Nodes (11): CaptionPosition, BOTTOM, TOP, applyCaptionEdgeInset(), applyCaptionStyle(), applyContentAspectRatio(), bindUnifiedSettingsButton(), CaptionEdgeInset (+3 more)

### Community 15 - "ExistingOutput.kt"
Cohesion: 0.10
Nodes (16): DownloadLocation, downloadLocation(), episodeTitle(), outputLocation(), safeFileName(), existingOutput(), ExistingOutputIndex, ExistingVideo (+8 more)

### Community 16 - "PopupGlass.kt"
Cohesion: 0.17
Nodes (6): glassEnabled(), blurLine(), boxBlur(), PopupBackdrop, popupGlass(), rememberPopupBackdrop()

### Community 18 - ".manager"
Cohesion: 0.08
Nodes (8): DownloadSpeed, downloadsPendingIntent(), DownloadRetryService, MediaVerification, OfflineDownloads, Listener, OfflineDownloadService, DownloadSpeedTest

### Community 19 - ".editCaptionStyle"
Cohesion: 0.13
Nodes (8): CaptionAppearanceDialog(), CaptionAppearanceEditor(), CaptionChoiceRow(), CaptionPlacementPad(), CaptionSample(), horizontalAnchorLabel(), marginLabel(), typeface()

### Community 20 - "ui/components/WaterFillLogoIndicator.kt"
Cohesion: 0.07
Nodes (21): silhouetteFrame(), silhouetteMask(), BubbleSpec, cappedFrameSteps(), DetailedWaterFillLogo(), liquidLevel(), LoopHandover, PlainWaterFillLogo() (+13 more)

### Community 21 - "ErrorNotice.kt"
Cohesion: 0.31
Nodes (5): DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice()

### Community 22 - "SiteDownloadSeries"
Cohesion: 0.13
Nodes (4): OnlineDramaHistoryTest, PlaybackHistory, SavedShowsStore, SiteDownloadSeries

### Community 23 - "CastRoutes.kt"
Cohesion: 0.06
Nodes (7): CastRoute, CastRoutes, GoogleCastRoute, SessionAvailabilityListener, mediaClientOrNull(), shouldGiveUpOnCastLoad(), MiruroCastMediaItemConverter

### Community 24 - "PreparationJobs.kt"
Cohesion: 0.32
Nodes (16): active(), appendSites(), discard(), generation(), itemSlot(), itemSlots(), lock(), migrate() (+8 more)

### Community 25 - "SearchScreenTest.kt"
Cohesion: 0.05
Nodes (28): SearchScreenTest, 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed, 16. Spacing, chevrons, and text hierarchy are locally defined — P3, confirmed design divergence (+20 more)

### Community 26 - "ExportRecord"
Cohesion: 0.11
Nodes (14): downloadGroupArtwork(), DownloadIdentity, ExportRecord, isHls(), mediaRequestHeaders(), MediaRequests, ResolvedMedia, DownloadGroupArtworkTest (+6 more)

### Community 27 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 28 - "cached"
Cohesion: 0.60
Nodes (3): opensTvPlayerControls(), cached(), Key

### Community 29 - "NartoMetadataApi"
Cohesion: 0.15
Nodes (4): browse(), NartoMetadataApi, metadataPayload(), dramaJsonAfter()

### Community 30 - "PlaybackService.kt"
Cohesion: 0.05
Nodes (10): CastRouteStatus, HostPlaybackSnapshot, PlaybackCacheBypassGate, PlaybackService, Callback, Listener, PlaybackStatus, stateName() (+2 more)

### Community 31 - ".editCaptionStyle"
Cohesion: 0.06
Nodes (23): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+15 more)

### Community 32 - "AnimeRepository.kt"
Cohesion: 0.09
Nodes (20): AnimeRepository, Anime, AnimeDetails, AnimeNetwork, AnimeRepository, Episode, findMediaUrl(), HttpFailure (+12 more)

### Community 33 - "NostrUpdateSource.kt"
Cohesion: 0.07
Nodes (22): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), readBoundedAnnouncementBody(), sanitizeAnnouncementUrl() (+14 more)

### Community 34 - "PlayerGestureControls"
Cohesion: 0.07
Nodes (35): applyBrightness(), applyVolume(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureIndicatorSide, gestureIndicatorSide(), Left (+27 more)

### Community 35 - "TelevisionSignals.kt"
Cohesion: 0.09
Nodes (26): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+18 more)

### Community 36 - "Theme.kt"
Cohesion: 0.67
Nodes (4): appColors(), ThemeSpec, wallpaperSeed(), AnimepaheDLTheme()

### Community 37 - "ThemeAccent"
Cohesion: 0.13
Nodes (13): ThemeAccent, BLOOD_RED, CUSTOM, CYAN, EMERALD, GOLD, MONOCHROME, NEON_GREEN (+5 more)

### Community 38 - "SleepTimer"
Cohesion: 0.12
Nodes (12): Counting, EndOfEpisode, formatSleepRemaining(), Off, sleepFadeLevel(), SleepTimer, State, rememberSleepRemaining() (+4 more)

### Community 39 - "AppBackdrop.kt"
Cohesion: 0.08
Nodes (12): AppUiScale(), UnscaledUi(), AppBackdrop, appCanvas(), appCanvasFade(), canvasColor(), centreCrop(), drawAppCanvas() (+4 more)

### Community 42 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 44 - "Test"
Cohesion: 0.23
Nodes (3): parseByteRange(), SearchVerificationGate, RecoveryPolicyTest

### Community 45 - "LibraryModels.kt"
Cohesion: 0.06
Nodes (32): isJwtExpired(), jwtLongClaim(), capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled() (+24 more)

### Community 46 - "SavedFilesWatcher.kt"
Cohesion: 0.27
Nodes (4): missingSavedFiles(), querySavedFiles(), savedFilePresent(), SavedFilesWatcher

### Community 47 - "AnililiFailure"
Cohesion: 0.13
Nodes (10): AnililiException, AnililiFailure, anililiFailure(), asFailure(), classifyTransport(), isCancellation(), raise(), report() (+2 more)

### Community 48 - "SubtitleDelay.kt"
Cohesion: 0.22
Nodes (4): SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 49 - "MegaPlayCdnHealth.kt"
Cohesion: 0.18
Nodes (6): healthHost(), isMegaPlayMedia(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState

### Community 50 - "AppDeviceProfile.kt"
Cohesion: 0.16
Nodes (3): focusHalo(), focusHighlight(), phoneRailPosterWidth()

### Community 51 - "SubtitleTrackMerge.kt"
Cohesion: 0.12
Nodes (35): SubtitleItem, assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), buildSignsMerge() (+27 more)

### Community 52 - "SiteDownloadSources.kt"
Cohesion: 0.13
Nodes (27): toAppMetadata(), cleanDramaTitle(), decryptDramaSource(), dramaAbsolute(), dramaArray(), dramaInt(), dramaMetadata(), dramaObject() (+19 more)

### Community 53 - "SiteDownloadRepository"
Cohesion: 0.22
Nodes (11): Page, SiteDownloadRepository, chooseDramaVariant(), chooseNartoVariant(), dramaHeight(), dramaHost(), DramaVariant, isDramaFile() (+3 more)

### Community 54 - "Composable"
Cohesion: 0.17
Nodes (21): CoverScreenWithDialogWindow(), MidnightBodyTopStop(), MidnightConfirmDialog(), MidnightConnectionStatus(), MidnightDialogFooter(), MidnightDialogFrame(), MidnightDialogHost(), MidnightFilledAction() (+13 more)

### Community 55 - "DownloadStore"
Cohesion: 0.16
Nodes (5): DownloadDao, DownloadDatabase, DownloadStore, PendingOperation, StoredDownload

### Community 56 - "AudioOutputFormat.kt"
Cohesion: 0.15
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 57 - "Composable"
Cohesion: 0.20
Nodes (17): rememberScreenReaderActive(), DownloadReferenceEpisode, DownloadReferenceEpisodeRow(), DownloadReferenceSeries, DownloadReferenceSeriesCard(), AboutScreen(), DownloaderSettingsSection(), DownloadFilterPill() (+9 more)

### Community 58 - "SessionBrowser.kt"
Cohesion: 0.11
Nodes (7): DramaPlayerEpisode, ShortDramaPlayer(), DebugTrace, nartoPlaybackCandidate(), RemoteDramaPlayer(), SessionBrowser(), WebViewClient

### Community 59 - "ProviderSubtitles.kt"
Cohesion: 0.11
Nodes (8): fetchProviderSubtitleBytes(), MediaSubtitle, normalizeProviderSubtitle(), providerSubtitleRequest(), ProviderSubtitles, unreferencedProviderSubtitleFile(), CancellableHttpTest, ProviderSubtitleDeletionTest

### Community 61 - "SiteMetadataApi.kt"
Cohesion: 0.30
Nodes (18): details(), metadataFromDocument(), metadataFromHtml(), MetadataDetails, MetadataEpisode, metadataEpisode(), MetadataImage, MetadataLink (+10 more)

### Community 62 - "ThemeSettings.kt"
Cohesion: 0.24
Nodes (7): accentSeedOf(), AppBackdropSource, ANIME, IMAGE, NONE, backdropUrlOf(), ProfileLook

### Community 63 - "ThemePreset"
Cohesion: 0.15
Nodes (12): ThemePreset, CRIMSON_NIGHT, CYBER, DEEP_SEA, EMBER, FOREST, GILDED, HIGH_CONTRAST (+4 more)

### Community 64 - "KwikPlaylistDataSource.kt"
Cohesion: 0.19
Nodes (3): hlsIv(), KwikPlaylistDataSource, rewriteKwikPlaylistIvs()

### Community 65 - ".verifyExport"
Cohesion: 0.19
Nodes (3): HlsExportTest, TimerTask, TimerTask

### Community 68 - "TvFocusTarget.kt"
Cohesion: 0.09
Nodes (12): beginNativeTvEditing(), hideNativeTvKeyboard(), isImeVisible(), moveNativeFocusSafely(), NativeTvTextWatcher, SafeTvEditText, TvNativeTextField(), TvTextInputType (+4 more)

### Community 69 - "CastUi.kt"
Cohesion: 0.11
Nodes (7): applyCastTextTrack(), CastButton(), Callback, openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 70 - "AutoSkipCountdown.kt"
Cohesion: 0.23
Nodes (5): AutoSkipButtonBounds(), AutoSkipCountdown, autoSkipCountdownLabel(), autoSkipSweep(), runAutoSkipCountdown()

### Community 72 - "ProgressiveExportTest.kt"
Cohesion: 0.13
Nodes (3): PreparationRetryTest, ProgressiveExportTest, copyCachedProgressive()

### Community 73 - "SubtitleCueLayout.kt"
Cohesion: 0.17
Nodes (15): autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues(), dropVectorDrawings(), flattenAutoStackedCues(), isAutoStack() (+7 more)

### Community 74 - "SiteDownloadProvider"
Cohesion: 0.16
Nodes (10): ReferencePlayerHostPolicy, MediaLinkRefreshGuard, SiteDownloadProvider, DIRECT, DRAMATIP, EROTICMV, HUANGGUO, KISS18 (+2 more)

### Community 75 - "AnnouncementManager.kt"
Cohesion: 0.07
Nodes (5): AnnouncementManager, AnnouncementScheduler, AnnouncementWorker, ForegroundCheck, NotificationPreferences()

### Community 77 - "AnimepaheDL resume state"
Cohesion: 0.09
Nodes (23): ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task, Current follow-up release, Custom downloader only, Dedicated details and download-range dialog, Environment and hosting, Existing work and reference (+15 more)

### Community 78 - "AppSymbol"
Cohesion: 0.24
Nodes (5): AppSymbol(), DestinationSummary(), SettingsRow(), CatalogSourceSettingRow(), SettingsScreen()

### Community 79 - "ExternalSubtitleFiles.kt"
Cohesion: 0.22
Nodes (14): AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName(), downloadOpenSubtitle(), exportedSubtitleMimeType(), extensionFor(), importSubtitleFile() (+6 more)

### Community 80 - "ClockFormat"
Cohesion: 0.25
Nodes (5): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, timeOfDayFormatter()

### Community 83 - "PreferredAudioLanguage"
Cohesion: 0.10
Nodes (18): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+10 more)

### Community 85 - "DpadLongPress.kt"
Cohesion: 0.26
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 86 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 87 - "CatalogPage"
Cohesion: 0.17
Nodes (6): catalogPosterUrl(), DramatipCatalog, parseDramatipCatalog(), HuangguoCatalog, CatalogPage, SiteCatalog

### Community 88 - "LocalStreamProxy.kt"
Cohesion: 0.12
Nodes (12): BodyHead, dispatchAcceptedSocket(), HttpOrigin, namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readBody(), readHead() (+4 more)

### Community 89 - "Modifier"
Cohesion: 0.13
Nodes (22): MidnightInlineNotice(), MidnightNoticeTone, ERROR, INFO, SUCCESS, WARNING, MidnightChoiceRow(), midnightFocus() (+14 more)

### Community 90 - ".add"
Cohesion: 0.15
Nodes (13): TvPlayerControl, CAPTIONS, EPISODES, FORWARD, FULLSCREEN, MUTE, MY_LIST, NEXT (+5 more)

### Community 91 - "LocalStreamProxy"
Cohesion: 0.23
Nodes (4): Entry, LocalStreamProxy, RootHeaderContext, ServerEndpoint

### Community 92 - "TvTextField.kt"
Cohesion: 0.04
Nodes (16): rememberSystemUses24Hour(), ContentObserver, playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute(), PlayerLockButton(), PlayerLockOverlay() (+8 more)

### Community 93 - "dispatchers"
Cohesion: 0.13
Nodes (6): findActivity(), inspectStorage(), QueueOrder, StorageReport, verifyCopy(), OutputStream

### Community 94 - "ContinueWatchingRow"
Cohesion: 0.24
Nodes (4): ContinueWatchingRow(), LibraryScreen(), rememberSeriesPlaybackSummary(), SeriesPlaybackSummary

### Community 95 - "SiteDownloadEpisode"
Cohesion: 0.35
Nodes (3): parseSiteDownloadSelection(), SiteDownloadEpisode, SiteDownloadSelectionSafetyTest

### Community 99 - "DramatipMetadataApi.kt"
Cohesion: 0.22
Nodes (14): Document, dramatipCards(), dramatipFlightObjects(), dramatipImages(), dramatipIsNextLabel(), dramatipLanguages(), dramatipMetadataUrl(), dramatipRequireNotFoundAbsent() (+6 more)

### Community 102 - "LegacyTrustAnchors.kt"
Cohesion: 0.17
Nodes (3): LegacyTrustAnchors, build.sh script, usage()

### Community 103 - "DramaPlayerGesturesTest.kt"
Cohesion: 0.15
Nodes (3): DramaPlayerGesturesTest, CatalogItem, CatalogViewModelTest

### Community 105 - "Test"
Cohesion: 0.19
Nodes (3): requireNoHttpUserInfo(), LocalStreamProxyResourceLimitsTest, Callback

### Community 106 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 107 - "SheetSections"
Cohesion: 0.10
Nodes (21): MediaVolumeSlider(), PlayerPanelTheme(), midnightSliderColors(), PanelHeader(), PlayerAdjustButton(), PlayerAdjustRow(), PlayerCaptionsSheet(), PlayerModalSheet() (+13 more)

### Community 112 - "FloatingNavigationBar.kt"
Cohesion: 0.07
Nodes (15): backdropBlur(), chromeFade(), DownloaderNavigation(), ReferenceCornerBadge(), WatchProgressBar(), barBlur(), barGlass(), barRim() (+7 more)

### Community 113 - ".page"
Cohesion: 0.19
Nodes (4): catalogImage(), CatalogSessionCookies, NartoCatalog, OtherCatalogHttp

### Community 114 - "PlaybackBuffer.kt"
Cohesion: 0.10
Nodes (11): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE, AdjustableLoadControl, playbackBufferBytes() (+3 more)

### Community 115 - "DownloadAction"
Cohesion: 0.29
Nodes (7): DownloadAction, DELETE, PAUSE, RESUME, RETRY, RETRY_SAVE, VERIFY

### Community 118 - "SiteDownloadPreparation"
Cohesion: 0.16
Nodes (12): PreparationStatus, isPreparedQueueRow(), mergePreparedQueueRows(), pendingEpisodeRowId(), aggregatePreparationStatus(), inheritLegacyPreparationState(), SiteDownloadPreparation, SiteDownloadPreparationBatch (+4 more)

### Community 119 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 120 - "Dp"
Cohesion: 0.05
Nodes (8): SeriesLogo(), TvTimePill(), close(), persist(), onPause(), onResume(), onStart(), onStop()

### Community 122 - "ExportService.kt"
Cohesion: 0.25
Nodes (5): DownloadCompleteReceiver, ExportProgress, onBind(), onCreate(), savingNotification()

### Community 123 - "CatalogPosterCard"
Cohesion: 0.08
Nodes (9): disableSelectionActionMode(), Callback, ArtworkRecovery, CatalogScreen(), CatalogHero(), catalogHomeSections(), catalogArtworkRequest(), CatalogPosterCard() (+1 more)

### Community 124 - "ThemeStyle"
Cohesion: 0.29
Nodes (5): ThemeStyle, NEUTRAL, TONAL, VIBRANT, VIVID

### Community 126 - "MidnightSettingSwitchRow"
Cohesion: 0.38
Nodes (6): MidnightSettingRow(), MidnightSettingRowScaffold(), MidnightSettingSpotlightMark(), MidnightSettingsSection(), MidnightSettingSwitchRow(), MidnightSwitch()

### Community 127 - "Player"
Cohesion: 0.15
Nodes (13): applyAudioTrack(), applyCategoryAudioPreference(), applyPreferredAudioLanguage(), applyPreferredSubtitleLanguage(), CategoryAudioDecision, categoryAudioDecision(), NotReady, preferredLanguageCodes() (+5 more)

### Community 128 - "FlixcloudPlaylistDataSource.kt"
Cohesion: 0.23
Nodes (5): decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace()

### Community 130 - "DohProvider"
Cohesion: 0.17
Nodes (10): DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY, DNS_SB, GOOGLE (+2 more)

### Community 131 - "DiscordStatus"
Cohesion: 0.33
Nodes (4): DiscordStatus, DO_NOT_DISTURB, IDLE, ONLINE

### Community 132 - "SkipTimingStatus"
Cohesion: 0.15
Nodes (12): SkipReportState, FAILED, IDLE, SENDING, SENT, SkipTimingStatus, ANISKIP, CHECKING (+4 more)

### Community 133 - "OpenSubtitlesAuthManager"
Cohesion: 0.11
Nodes (4): OpenSubtitlesAuthManager, OpenSubtitlesCredentials, Kiss18MetadataApi, Snapshot

### Community 135 - "LibraryNavigationTest"
Cohesion: 0.11
Nodes (7): LibraryNavigationTest, AnimeApplication, AppSettings, normalizeOutputFolder(), OutputFolderTest, Device verification, FFmpeg

### Community 137 - "SettingsStore.kt"
Cohesion: 0.06
Nodes (20): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), DefaultQuality, AUTO, HIGHEST (+12 more)

### Community 140 - "ThemeBackground"
Cohesion: 0.33
Nodes (3): ThemeBackground, AMOLED, DARK

### Community 143 - "Test"
Cohesion: 0.15
Nodes (7): mergeSiteDownloadCookies(), normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), SiteDownloadCookieStore, validCookiePair(), validSiteDownloadCookieInput(), SiteDownloadCookieTest

### Community 145 - ".row"
Cohesion: 0.40
Nodes (3): DownloadGroupProgress, downloadGroupProgress(), DownloadGroupProgressTest

### Community 146 - "OpenSubtitlesPanel.kt"
Cohesion: 0.08
Nodes (9): NextEpisodeAction(), NextEpisodeCard(), defaultOpenSubtitlesLanguages(), openSubtitlesEpisodeField(), OpenSubtitlesPanel(), SubtitleResultRow(), PlayerPauseOverlay(), PlayerSeriesName() (+1 more)

### Community 148 - "Narto Drama public metadata API"
Cohesion: 0.33
Nodes (5): API methods and routing, Field coverage, Live audit evidence, Narto Drama public metadata API, Not supplied / not automatically fetched

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

### Community 156 - "instrumentationregistry"
Cohesion: 0.07
Nodes (7): ExistingOutputTest, IntegrityTest, PreparationResponsivenessTest, SearchRecoveryTest, AnimeRepository, AnimeRepository, DownloadGatewayTest

### Community 158 - "SubtitleCueTimes.kt"
Cohesion: 0.39
Nodes (10): loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes(), parseTimestampMs(), stripMarkup() (+2 more)

### Community 159 - "Browse, play and download"
Cohesion: 0.20
Nodes (9): Anilili Short Dramas, Browse, play and download, Build, Downloads, Library and recovery tools, Release checks, Sessions, Supported sources (+1 more)

### Community 161 - "EpisodeSource"
Cohesion: 0.30
Nodes (6): audioLabel(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 162 - "Reliability and correctness findings"
Cohesion: 0.20
Nodes (10): R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction), R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence), R3 — Direct enqueue has a process-death orphan window (P1, high confidence), R4 — HLS validation does not require expected audio (P1, high confidence), R5 — Final destination validation is weaker than staging validation (P1, high confidence), R6 — Unhandled action failures can crash the app (P2, high confidence), R7 — Retry can discard the only reference to unfinished output (P2, high confidence), R8 — Export service waits throughout transfers (P2, high confidence) (+2 more)

### Community 163 - "FfmpegRemux.kt"
Cohesion: 0.08
Nodes (15): copyCancellable(), consumeCancellable(), CachedHlsServer, InputStream, copyExactly(), ffmpegRemuxCachedHls(), readBytesBounded(), requireCachedHlsSegments() (+7 more)

### Community 164 - "Listener"
Cohesion: 0.07
Nodes (14): causeChain(), platformErrorCode(), platformErrorName(), technicalDetail(), playbackFailureEntry(), httpFailureSummary(), shouldRetryDecoderAtLowerVideoResolution(), contentAspectRatio() (+6 more)

### Community 165 - "DownloadRow"
Cohesion: 0.14
Nodes (17): DownloadLibrarySnapshot, downloadLibrarySnapshot(), Availability, SAVED, STAGING, DownloadRow, FailureReason, DELETION (+9 more)

### Community 167 - "GlassSection"
Cohesion: 0.25
Nodes (7): GlassSection, BROWSE, DETAILS, HOME, NAVIGATION, POPUPS, REFRESH

### Community 170 - "DownloaderViewModel.kt"
Cohesion: 0.07
Nodes (6): AnnouncementDialog(), reconcileDirectEnqueues(), loadBookmarks(), toggleBookmark(), DownloadRepository, classifyFailure()

### Community 171 - "MidnightDialog.kt"
Cohesion: 0.04
Nodes (7): captionOffset(), EpisodeArtwork(), EqualizerWaveIndicator(), MidnightSettingSpotlight, Midnight, PlayerAccentScope(), PageSearchField()

### Community 172 - "MediaCache.kt"
Cohesion: 0.08
Nodes (12): VideoCacheSize, AUTOMATIC, GB_1, GB_2, MB_256, MB_512, OFF, ExoDatabase (+4 more)

### Community 177 - "NextEpisodeTrigger"
Cohesion: 0.20
Nodes (10): EmbedAutoAdvanceStep, embedAutoAdvanceStep(), ADVANCE, ARM, WAIT, NextEpisodeTrigger, nextEpisodeTrigger(), AFTER_CREDITS (+2 more)

### Community 178 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 179 - "PlayerContentScale"
Cohesion: 0.40
Nodes (4): PlayerContentScale, CROP, FILL, FIT

### Community 180 - "AnimepaheDL codebase review"
Cohesion: 0.07
Nodes (27): selectCachedHlsVariant(), HlsVariantSelectionTest, AnimepaheDL codebase review, Architecture and strengths, P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3) (+19 more)

### Community 181 - "StreamRefusalLoadErrorPolicy.kt"
Cohesion: 0.33
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 182 - "TvPlayerBackTarget"
Cohesion: 0.22
Nodes (9): TvPlayerBackTarget, CAPTION_APPEARANCE, CAPTIONS, tvPlayerBackTarget(), CONTROLS, EPISODE_DRAWER, NONE, OPEN_SUBTITLES (+1 more)

### Community 183 - "UiState"
Cohesion: 0.50
Nodes (4): Error, Loading, Success, UiState

### Community 184 - "TransferState"
Cohesion: 0.25
Nodes (8): TransferState, COMPLETE, DOWNLOADING, FAILED, PAUSED, QUEUED, REMOVING, WAITING_NETWORK

### Community 185 - "NartoMetadataApi.kt"
Cohesion: 0.83
Nodes (3): classifyNartoLinkedTerms(), NartoTermGroups, toHttpUrlOrNull()

### Community 187 - "Kiss18 public metadata API"
Cohesion: 0.29
Nodes (6): Absent or conditional information, Browse and filtering, Coverage and retained data, Kiss18 public metadata API, Sources actually visited, Verification

### Community 188 - "RelatedLayout"
Cohesion: 0.33
Nodes (3): RelatedLayout, LIST, TITLES

### Community 189 - "OperationRecovery.kt"
Cohesion: 0.14
Nodes (9): Details, Download, OperationFailure, OperationId, Preparation, presented(), Search, Sources (+1 more)

### Community 195 - "ReferencePlaybackRegistry"
Cohesion: 0.14
Nodes (4): Binding, ReferencePlaybackRegistry, close(), select()

### Community 198 - "SiteMetadataApi"
Cohesion: 0.11
Nodes (15): MetadataSite, DRAMATIP, KISS18, NARTO, SiteMetadataApi, SiteMetadataApis, KissKH metadata API, Query/API use (+7 more)

### Community 199 - "dispatchers"
Cohesion: 0.11
Nodes (6): DramatipArtworkHttp, dramatipPublicPosterUrl(), DramatipSessionCookies, freshDramatipPoster(), verificationOriginMatches(), ProviderAdGate

### Community 201 - "TvEpisodeChangeFocus"
Cohesion: 0.32
Nodes (8): TvEpisodeChangeFocus, tvEpisodeChangeFocus(), CONTROLS, HOLD, SURFACE, UNCHANGED, tvFocusLostTarget(), tvOverlayDismissFocus()

### Community 202 - "DownloadQuality"
Cohesion: 0.25
Nodes (6): DownloadQuality, BEST, P1080, P360, P480, P720

### Community 207 - "UiScale"
Cohesion: 0.33
Nodes (4): UiScale, LARGE, MEDIUM, SMALL

### Community 208 - "EroticMV metadata API"
Cohesion: 0.40
Nodes (4): Endpoints and operations, EroticMV metadata API, Fields retained, Live evidence and limitations

### Community 209 - ".add"
Cohesion: 0.13
Nodes (8): DramatipMetadataApi, MetadataHttp, MetadataResponse, DramatipMetadataFixtureTest, Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 212 - "CatalogSource"
Cohesion: 0.14
Nodes (13): catalogCacheJson(), CatalogFailureState, catalogFailureState(), catalogPageForSource(), CatalogSource, DRAMATIP, HUANGGUO, KISS18 (+5 more)

### Community 259 - "MenuLanguage"
Cohesion: 0.29
Nodes (4): MenuLanguage, ENGLISH, SPANISH, SYSTEM

### Community 267 - "PlayerSurface"
Cohesion: 0.05
Nodes (41): StreamItem, kwikProgressiveMp4(), signsMergeDialogueLabel(), CompiledSeparately(), Routes, shouldShowManualSkipButton(), outroSkipTargetMs(), outroWindowIsCredits() (+33 more)

### Community 277 - "ReferenceChromeTokens.kt"
Cohesion: 0.43
Nodes (3): appCanvasFade(), drawCanvasFade(), Midnight

### Community 284 - "AppBackdropDim"
Cohesion: 0.33
Nodes (4): AppBackdropDim, LIGHT, MEDIUM, STRONG

### Community 289 - "ProxyActiveResources"
Cohesion: 0.13
Nodes (5): BoundedUrlTokenRegistry, ProxyActiveResources, stopProxyResources(), TrackedProxyResources, UrlRegistryCapacityException

### Community 296 - "SiteDownloadViewModel.kt"
Cohesion: 0.27
Nodes (6): appSiteDownloadRepository(), changePending(), isPendingRow(), pausePending(), resumePending(), EditionProviderBoundaryTest

### Community 350 - "SafeDeletion.kt"
Cohesion: 0.24
Nodes (3): deleteOwnedMedia(), isOutputSubtitleSidecar(), SafeDeletionTest

### Community 356 - "SaveState"
Cohesion: 0.29
Nodes (7): SaveState, CHECKING, COPYING, FAILED, NONE, PREPARING, WAITING

### Community 368 - "PlaylistForeignSegments.kt"
Cohesion: 0.70
Nodes (4): hostOf(), playlistForeignHosts(), playlistSegmentCensus(), segmentHosts()

### Community 370 - "GestureDragAxis"
Cohesion: 0.67
Nodes (3): GestureDragAxis, Horizontal, Vertical

## Knowledge Gaps
- **478 isolated node(s):** `NotReady`, `Settled`, `Preparation`, `Search`, `Sources` (+473 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1383 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **193 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SettingsStore` connect `SettingsStore` to `developer_notices.py`, `DiscordStatus`, `DohProvider`, `InPlayerEpisodeList.kt`, `MenuLanguage`, `TvPlayerControls.kt`, `SettingsStore.kt`, `PlayerSurface`, `AniListModels.kt`, `PlayerSurface.kt`, `CaptionPosition`, `ThemeBackground`, `PipeModels.kt`, `OpenSubtitlesPanel.kt`, `.editCaptionStyle`, `ThemeContrast`, `AppBackdropDim`, `.editCaptionStyle`, `PlayerGestureControls`, `TelevisionSignals.kt`, `Theme.kt`, `ThemeAccent`, `GlassSection`, `.init`, `AppBackdrop.kt`, `SourceFilterLanguage`, `MidnightDialog.kt`, `MediaCache.kt`, `LibraryModels.kt`, `JsonPrimitive`, `RelatedLayout`, `ThemeSettings.kt`, `ThemePreset`, `SubtitleCueLayout.kt`, `DownloadQuality`, `UiScale`, `ClockFormat`, `PreferredAudioLanguage`, `PreferredSubtitleLanguage`, `PosterElement`, `SheetSections`, `PlaybackBuffer.kt`, `Dp`, `ThemeStyle`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Are the 69 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `externalPlaybackRequest()`) actually correct?**
  _`PlayerSurface()` has 69 INFERRED edges - model-reasoned connections that need verification._
- **What connects `NotReady`, `Settled`, `Preparation` to the rest of the system?**
  _478 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AppIcon` be split into smaller, more focused modules?**
  _Cohesion score 0.09523809523809523 - nodes in this community are weakly interconnected._
- **Why does `PlayerSurface()` connect `PlayerSurface` to `ExternalPlayer.kt`, `SettingsStore`, `InPlayerEpisodeList.kt`, `SkipTimingStatus`, `TvPlayerControls.kt`, `SettingsStore.kt`, `SubtitleDelay`, `CacheServerTest.kt`, `PlayerSurface.kt`, `PipeModels.kt`, `CaptionPosition`, `OpenSubtitlesPanel.kt`, `.editCaptionStyle`, `SubtitleCueTimes.kt`, `PlaybackService.kt`, `PlayerGestureControls`, `Listener`, `SleepTimer`, `LibraryModels.kt`, `SubtitleDelay.kt`, `NextEpisodeTrigger`, `SubtitleTrackMerge.kt`, `PlayerContentScale`, `TvPlayerBackTarget`, `Composable`, `CastUi.kt`, `AutoSkipCountdown.kt`, `TvEpisodeChangeFocus`, `AnnouncementManager.kt`, `ExternalSubtitleFiles.kt`, `CatalogSource`, `Modifier`, `TvTextField.kt`, `PlayerFillerSkip.kt`, `SheetSections`, `FloatingNavigationBar.kt`, `Player`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `DiagnosticsLog` connect `DiagnosticsLog` to `FlixcloudPlaylistDataSource.kt`, `ExternalPlayer.kt`, `OpenSubtitlesAuthManager`, `TvFocusTarget.kt`, `SettingsStore.kt`, `CastRoutes.kt`, `SleepTimer`, `AnililiFailure`, `MegaPlayCdnHealth.kt`, `UniqueStreamKeyDataSource`, `KwikPlaylistDataSource.kt`, `ArtworkBitmapLoader.kt`, `AnnouncementManager.kt`, `AppDns`, `LocalStreamProxy.kt`, `TvTextField.kt`, `LegacyTrustAnchors.kt`, `PlaybackDiagnostics.kt`, `PlaybackBuffer.kt`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._