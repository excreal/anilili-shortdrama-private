# Graph Report - AnililiShortDrama  (2026-11-11)

## Corpus Check
- 318 files · ~283,931 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 38 file(s) not represented in the graph (top: .xml 22, (none) 3, .ttf 3)

## Summary
- 4796 nodes · 11791 edges · 239 communities (162 shown, 77 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 721 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5fc04a57`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DramaPlayerSurface.kt
- AnililiError
- developer_notices.py
- ExternalPlaybackReturns.kt
- SettingsStore
- InPlayerEpisodeList.kt
- FlixcloudPlaylistDataSource.kt
- ui/components/WaterFillLogoIndicator.kt
- PlayerControlsScaffold
- AppDeviceProfile.kt
- .all
- AniListModels.kt
- PlayerSurface.kt
- TvPlayerControls.kt
- MidnightSettings.kt
- ExistingOutput.kt
- AnimeRepository.kt
- PlayerSleepTimer.kt
- .manager
- SettingsStore.kt
- PlayerSettingsSheet.kt
- PlayerSurface
- SiteDownloadSeries
- CastRoutes.kt
- .operation
- SearchScreenTest.kt
- dispatchers
- SafeTvEditText
- OpenSubtitlesPanel.kt
- NartoMetadataApi
- HlsExportTest.kt
- .editCaptionStyle
- PipeModels.kt
- NostrUpdateSource.kt
- FfmpegRemux.kt
- TelevisionSignals.kt
- DownloaderViewModel.kt
- ThemeAccent
- SleepTimer.kt
- AppBackdrop.kt
- LibraryTools.kt
- Review implementation status
- SourceFilterLanguage
- file
- Test
- OpenSubtitlesClient.kt
- ThemePreset
- AnililiFailure
- ExportService.kt
- LibraryModels.kt
- SubtitleCueLayout.kt
- SubtitleTrackMerge.kt
- SiteDownloadSources.kt
- SiteMetadataApi
- Composable
- DownloadStore
- MetadataResponse
- OpenSubtitlesPanel.kt
- AppSymbol
- FloatingNavigationBar.kt
- AppColorScheme.kt
- Listener
- AudioOutputFormat.kt
- TvFocusTarget.kt
- SecureTokenStore.kt
- SheetSections
- tohttpurl
- SiteDownloadRepository
- MegaPlayCdnHealth.kt
- CastButton
- SiteMetadataApi.kt
- .verifyExport
- EpisodeControlPlayer
- test_narto_playback.py
- BoundedUrlTokenRegistry
- AnnouncementManager.kt
- dispatchers
- AnimepaheDL resume state
- CatalogPage
- ExternalSubtitleFiles.kt
- AnnouncementSource.kt
- ProviderSubtitles.kt
- AppDns
- PreferredAudioLanguage
- DiagnosticsLog
- AppIcon
- PreferredSubtitleLanguage
- PlaybackService.kt
- LocalStreamProxy.kt
- ReferencePlaybackRegistry
- DramatipMetadataApi.kt
- LocalStreamProxy
- PopupGlass.kt
- Anilili UI reference audit
- SiteDownloadProvider
- SubtitleSearchLanguage
- .switchPlaybackPlayer
- MediaCache.kt
- Listener
- BackdropBlur.kt
- SecureTokenStore
- dramatipPublicPosterUrl
- Kiss18MetadataApi.kt
- ThreadTraceListener.java
- AnnouncementInputBoundsTest
- Test
- SubtitleDelay.kt
- PlayerGestureControls
- CatalogScreen
- SiteDownloadViewModel.kt
- PlayerFillerSkip.kt
- TimeOfDay.kt
- SiteDownloadScreen
- CatalogSessionCookies
- PlaybackBuffer.kt
- ExternalPlayer.kt
- .configureRequestHeaders
- decodeRelayFrame
- SiteDownloadPreparation
- ErrorDomain
- Modifier
- SiteDownloadRepositoryTest
- SiteDownloadSourcesTest
- WebSocketListener
- MidnightHelper
- LocalStreamProxyService
- siteDownloadCookieHeader
- ForwardingPlayer
- UI and usability findings
- SkipTimingStatus
- TvPlayerControl
- AnimepaheDL codebase review
- PreparationResponsivenessTest.kt
- OpenSubtitlesAuthManager.kt
- CaptionAppearanceEditor
- Performance and maintainability
- Thumbnail
- SubtitleCueTimes.kt
- DpadLongPress.kt
- GestureDragAxis
- tohttpurlornull
- MediaListEntry
- PosterElement
- .write
- LocalStreamProxyHeaderPolicyTest
- StreamRefusalLoadErrorPolicy.kt
- AppSymbol
- Real-device verification
- ErrorDetailsDialog
- ErrorAction
- HlsPlaylistRewrite.kt
- MidnightSettingSwitchRow
- NextEpisodeTrigger
- .check
- CatalogPosterCard
- Browse, play and download
- EpisodeSource
- .onCreate
- .capture
- TvPlayerBackTarget
- FailureReason
- ProviderBenchmark
- ThemeSettings.kt
- DownloadQuality
- PlatformErrorCodes.kt
- MetadataBrowseQuery
- isExportDurationComplete
- SiteShowMetadata
- OrderedWrites
- SiteDownloadEpisode
- Anilili feature audit checklist
- WatchedFilter
- MenuLanguage
- .transform
- .init
- DownloadReferenceEpisodeRow
- backdropBlur
- ReferenceChromeTokens.kt
- ExampleInstrumentedTest.kt
- Viewer
- UiScale
- selectCachedHlsVariant
- normalizeOutputFolder
- TvNativeTextField
- verificationPage
- Kind
- DefaultQuality
- ThemeStyle
- DiagnosticsLog
- DesignSystem.kt
- NativeTvTextWatcher
- EroticMV metadata API
- Theme.kt
- findActivity
- ContinueWatchingRow
- ExampleUnitTest.kt
- AppBackdropDim
- Device UI verification
- Developer announcements
- AGENTS.md
- components/README.md
- docs/KISSKH.md
- THIRD_PARTY_NOTICES.md
- make-export-benchmark.sh
- ThemeBackground
- ThemeContrast
- Callback
- UiState.kt
- Reliability and correctness findings
- Callback
- TvTextInputType
- RelatedLayout
- Security and privacy findings
- ViewerOptionsData
- SafeDeletionTest.kt

## God Nodes (most connected - your core abstractions)
1. `SettingsStore` - 156 edges
2. `PlayerSurface()` - 135 edges
3. `AnililiError` - 114 edges
4. `PlaybackService` - 45 edges
5. `rememberFocusFill()` - 44 edges
6. `SiteDownloadSeries` - 37 edges
7. `NartoMetadataApi` - 37 edges
8. `SourceFilterLanguage` - 33 edges
9. `DiagnosticsLog` - 30 edges
10. `LocalStreamProxy` - 30 edges

## Surprising Connections (you probably didn't know these)
- `Browse and filtering` --references--> `MetadataHttp`  [INFERRED]
  docs/api/KISS18.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `API methods and routing` --references--> `MetadataPage`  [INFERRED]
  docs/api/NARTO.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `Download playback-state correction (current source)` --references--> `WatchProgressBar()`  [INFERRED]
  docs/UI_REFERENCE_CHECKLIST.md → app/src/main/java/com/anilili/ui/components/Common.kt
- `Verification plan` --references--> `LibraryNavigationTest`  [INFERRED]
  docs/ui-consistency-and-wrapping-audit.md → app/src/androidTest/java/com/example/animepahedl/LibraryNavigationTest.kt
- `Device verification` --references--> `LibraryNavigationTest`  [INFERRED]
  README.md → app/src/androidTest/java/com/example/animepahedl/LibraryNavigationTest.kt

## Import Cycles
- None detected.

## Communities (239 total, 77 thin omitted)

### Community 0 - "DramaPlayerSurface.kt"
Cohesion: 0.06
Nodes (10): catalogArtworkRequest(), close(), DramaPlayerEpisode, persist(), ReferenceSettingsIndex(), SettingsSectionCard(), SettingsSectionTile, onPause() (+2 more)

### Community 1 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 2 - "developer_notices.py"
Cohesion: 0.22
Nodes (16): broadcast(), cmd_announce(), cmd_announce_clear(), cmd_announce_fetch(), cmd_fetch(), on_event(), cmd_keygen(), cmd_publish() (+8 more)

### Community 3 - "ExternalPlaybackReturns.kt"
Cohesion: 0.15
Nodes (7): LibraryStore, ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, shouldSyncAniListProgress(), withExportProgressDeadline()

### Community 5 - "InPlayerEpisodeList.kt"
Cohesion: 0.08
Nodes (35): EpisodeItem, EpisodeLayout, GRID, LIST, rememberFocusFill(), EpisodeFillerBadge(), episodeWatchFraction(), ErrorBox() (+27 more)

### Community 6 - "FlixcloudPlaylistDataSource.kt"
Cohesion: 0.05
Nodes (17): decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace(), hlsIv(), KwikPlaylistDataSource, kwikProgressiveMp4() (+9 more)

### Community 7 - "ui/components/WaterFillLogoIndicator.kt"
Cohesion: 0.07
Nodes (21): silhouetteFrame(), silhouetteMask(), BubbleSpec, cappedFrameSteps(), DetailedWaterFillLogo(), liquidLevel(), LoopHandover, PlainWaterFillLogo() (+13 more)

### Community 8 - "PlayerControlsScaffold"
Cohesion: 0.06
Nodes (28): FocusFillState, SeriesLogo(), BufferedSeekTrack(), controlsSeekAnchorFor(), formatPlayerTime(), PlayerActionPill(), PlayerActionRow(), playerActionRowScale() (+20 more)

### Community 9 - "AppDeviceProfile.kt"
Cohesion: 0.18
Nodes (3): focusHalo(), focusHighlight(), phoneRailPosterWidth()

### Community 10 - ".all"
Cohesion: 0.10
Nodes (12): DeletionRecoveryTest, LibraryNavigationTest, AppSettings, AndroidDownloadRepository, DownloadIdentity, adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos() (+4 more)

### Community 11 - "AniListModels.kt"
Cohesion: 0.10
Nodes (52): AiringCountEntry, AiringCountMedia, AiringCountPage, AiringDayCount, AiringSchedule, CoverImage, DiscoverOptions, DiscoverOptionsData (+44 more)

### Community 12 - "PlayerSurface.kt"
Cohesion: 0.04
Nodes (34): adaptiveVideoHeightCeiling(), applyAudioTrack(), applyCaptionEdgeInset(), applyCaptionStyle(), applyCategoryAudioPreference(), applyContentAspectRatio(), applyTextTrack(), applyVideoHeight() (+26 more)

### Community 13 - "TvPlayerControls.kt"
Cohesion: 0.05
Nodes (13): AppUiScale(), UnscaledUi(), captionOffset(), bufferedFraction(), playerTypography(), formatTvPlayerTime(), TvControlButton(), TvPlayerBackArrow() (+5 more)

### Community 14 - "MidnightSettings.kt"
Cohesion: 0.06
Nodes (5): EpisodeArtwork(), EqualizerWaveIndicator(), MidnightSettingSpotlight, Midnight, PlayerAccentScope()

### Community 15 - "ExistingOutput.kt"
Cohesion: 0.10
Nodes (18): DownloadLocation, downloadLocation(), episodeTitle(), outputLocation(), safeFileName(), existingOutput(), ExistingOutputIndex, ExistingVideo (+10 more)

### Community 16 - "AnimeRepository.kt"
Cohesion: 0.09
Nodes (19): AnimeRepository, Anime, AnimeDetails, AnimeNetwork, AnimeRepository, Episode, findMediaUrl(), HttpFailure (+11 more)

### Community 17 - "PlayerSleepTimer.kt"
Cohesion: 0.10
Nodes (6): AutoSkipButtonBounds(), autoSkipSweep(), playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute()

### Community 18 - ".manager"
Cohesion: 0.09
Nodes (5): DownloadRetryService, MediaVerification, OfflineDownloads, Listener, OfflineDownloadService

### Community 19 - "SettingsStore.kt"
Cohesion: 0.05
Nodes (19): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), defaultQualityForDevice(), deviceDefaultQuality(), DiscordStatus (+11 more)

### Community 20 - "PlayerSettingsSheet.kt"
Cohesion: 0.04
Nodes (4): tvFocusHandoff(), tvFocusTarget(), rememberTimedSkipPopup(), PageSearchField()

### Community 21 - "PlayerSurface"
Cohesion: 0.04
Nodes (48): PlayerContentScale, CROP, FILL, FIT, signsMergeDialogueLabel(), rememberScreenReaderActive(), CompiledSeparately(), Routes (+40 more)

### Community 22 - "SiteDownloadSeries"
Cohesion: 0.10
Nodes (9): OnlineDramaHistoryTest, PlaybackHistory, SavedShowsStore, appendSitePreparation(), SiteDownloadPreparation, SiteDownloadPreparationBatch, mergeDiscoveredQualities(), SiteDownloadEpisode (+1 more)

### Community 23 - "CastRoutes.kt"
Cohesion: 0.06
Nodes (7): CastRoute, CastRoutes, GoogleCastRoute, SessionAvailabilityListener, mediaClientOrNull(), shouldGiveUpOnCastLoad(), MiruroCastMediaItemConverter

### Community 24 - ".operation"
Cohesion: 0.19
Nodes (3): reconcileDirectEnqueues(), PreparationJobs, PreparationWorker

### Community 25 - "SearchScreenTest.kt"
Cohesion: 0.05
Nodes (28): DownloadSelectionTest, SearchScreenTest, 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed (+20 more)

### Community 26 - "dispatchers"
Cohesion: 0.13
Nodes (6): mediaRequestHeaders(), MediaRequests, RemoteDramaPlayer(), appSiteDownloadRepository(), EditionProviderBoundaryTest, SiteDownloadDownloadTest

### Community 27 - "SafeTvEditText"
Cohesion: 0.21
Nodes (4): beginNativeTvEditing(), hideNativeTvKeyboard(), isImeVisible(), SafeTvEditText

### Community 28 - "OpenSubtitlesPanel.kt"
Cohesion: 0.10
Nodes (4): NextEpisodeAction(), NextEpisodeCard(), PlayerPauseOverlay(), ScrollHintColumn()

### Community 29 - "NartoMetadataApi"
Cohesion: 0.13
Nodes (10): Document, details(), metadataDurationSeconds(), metadataFromDocument(), metadataFromHtml(), classifyNartoLinkedTerms(), NartoMetadataApi, NartoTermGroups (+2 more)

### Community 30 - "HlsExportTest.kt"
Cohesion: 0.05
Nodes (4): ProgressiveExportTest, EpisodeDownloads, DebugTrace, copyCachedProgressive()

### Community 31 - ".editCaptionStyle"
Cohesion: 0.05
Nodes (23): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+15 more)

### Community 32 - "PipeModels.kt"
Cohesion: 0.09
Nodes (19): Category, DUB, SUB, EpisodesResult, hasUsableWindow(), playsInWebView(), ProviderData, SkipTimes (+11 more)

### Community 33 - "NostrUpdateSource.kt"
Cohesion: 0.17
Nodes (7): hexToBytes(), NostrEvent, NostrUpdateSource, RelayFrameGate, RelayScanResult, toHexString(), toNostrEvent()

### Community 34 - "FfmpegRemux.kt"
Cohesion: 0.10
Nodes (9): CacheServerTest, copyCancellable(), consumeCancellable(), CachedHlsServer, InputStream, copyExactly(), ffmpegRemuxCachedHls(), readBytesBounded() (+1 more)

### Community 35 - "TelevisionSignals.kt"
Cohesion: 0.06
Nodes (33): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+25 more)

### Community 36 - "DownloaderViewModel.kt"
Cohesion: 0.07
Nodes (7): loadBookmarks(), toggleBookmark(), DownloadRepository, deleteOutputCompanions(), deleteOwnedMedia(), isOutputSubtitleSidecar(), ItemResult

### Community 37 - "ThemeAccent"
Cohesion: 0.13
Nodes (13): ThemeAccent, BLOOD_RED, CUSTOM, CYAN, EMERALD, GOLD, MONOCHROME, NEON_GREEN (+5 more)

### Community 38 - "SleepTimer.kt"
Cohesion: 0.07
Nodes (13): NetworkIdentity, Counting, EndOfEpisode, formatSleepRemaining(), Off, sleepFadeLevel(), SleepTimer, State (+5 more)

### Community 39 - "AppBackdrop.kt"
Cohesion: 0.10
Nodes (10): AppBackdrop, appCanvas(), appCanvasFade(), canvasColor(), centreCrop(), drawAppCanvas(), drawBackdropSlice(), hasAppBackdrop() (+2 more)

### Community 40 - "LibraryTools.kt"
Cohesion: 0.20
Nodes (7): classifyFailure(), diagnostics(), inspectStorage(), QueueOrder, StorageReport, verifyCopy(), OutputStream

### Community 41 - "Review implementation status"
Cohesion: 0.29
Nodes (5): Latest user corrections, Reliability and security, Review implementation status, UI, performance, and features, Verification

### Community 42 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 44 - "Test"
Cohesion: 0.10
Nodes (12): parseByteRange(), Details, Download, OperationFailure, OperationId, Preparation, presented(), Search (+4 more)

### Community 45 - "OpenSubtitlesClient.kt"
Cohesion: 0.13
Nodes (7): isJwtExpired(), jwtLongClaim(), normalizeSubtitleLanguages(), OpenSubtitleDownload, OpenSubtitlesClient, OpenSubtitleSearchResult, parseSearchResults()

### Community 46 - "ThemePreset"
Cohesion: 0.17
Nodes (12): ThemePreset, CRIMSON_NIGHT, CYBER, DEEP_SEA, EMBER, FOREST, GILDED, HIGH_CONTRAST (+4 more)

### Community 47 - "AnililiFailure"
Cohesion: 0.15
Nodes (9): AnililiException, AnililiFailure, anililiFailure(), asFailure(), classifyTransport(), isCancellation(), raise(), report() (+1 more)

### Community 48 - "ExportService.kt"
Cohesion: 0.07
Nodes (11): ExistingOutputTest, IntegrityTest, downloadsPendingIntent(), DownloadCompleteReceiver, ExportProgress, onBind(), onCreate(), savingNotification() (+3 more)

### Community 49 - "LibraryModels.kt"
Cohesion: 0.15
Nodes (23): capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled(), historyAfterEpisodeWatched(), historyAfterRemoteProgress() (+15 more)

### Community 50 - "SubtitleCueLayout.kt"
Cohesion: 0.14
Nodes (18): CaptionPosition, BOTTOM, TOP, autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues() (+10 more)

### Community 51 - "SubtitleTrackMerge.kt"
Cohesion: 0.13
Nodes (33): assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), buildSignsMerge(), decodeEntities() (+25 more)

### Community 52 - "SiteDownloadSources.kt"
Cohesion: 0.20
Nodes (21): chooseDramaVariant(), chooseNartoVariant(), decryptDramaSource(), dramaAbsolute(), dramaArray(), dramaHeight(), dramaInt(), dramaJsonAfter() (+13 more)

### Community 53 - "SiteMetadataApi"
Cohesion: 0.08
Nodes (19): MetadataSite, DRAMATIP, KISS18, NARTO, SiteMetadataApi, SiteMetadataApis, KissKH metadata API, Query/API use (+11 more)

### Community 54 - "Composable"
Cohesion: 0.16
Nodes (21): CoverScreenWithDialogWindow(), MidnightBodyTopStop(), MidnightConfirmDialog(), MidnightConnectionStatus(), MidnightDialogFooter(), MidnightDialogFrame(), MidnightDialogHost(), MidnightFilledAction() (+13 more)

### Community 55 - "DownloadStore"
Cohesion: 0.14
Nodes (5): DownloadDao, DownloadDatabase, DownloadStore, PendingOperation, StoredDownload

### Community 56 - "MetadataResponse"
Cohesion: 0.18
Nodes (6): MetadataHttp, MetadataResponse, Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 57 - "OpenSubtitlesPanel.kt"
Cohesion: 0.12
Nodes (7): focusFill(), defaultOpenSubtitlesLanguages(), openSubtitlesEpisodeField(), OpenSubtitlesPanel(), SubtitleResultRow(), PlayerLockButton(), PlayerLockOverlay()

### Community 58 - "AppSymbol"
Cohesion: 0.17
Nodes (10): DownloadReferenceEpisode, DownloadReferenceSeries, AboutScreen(), CatalogSourceSettingRow(), DownloaderSettingsSection(), DownloadFilterPill(), DownloadsScreen(), friendlyDownloadStatus() (+2 more)

### Community 59 - "FloatingNavigationBar.kt"
Cohesion: 0.10
Nodes (9): chromeFade(), DownloaderNavigation(), barBlur(), barGlass(), barRim(), FloatingNavDisc(), FloatingNavIcon(), FloatingNavigationBar() (+1 more)

### Community 61 - "Listener"
Cohesion: 0.08
Nodes (13): playbackFailureEntry(), httpFailureSummary(), shouldRetryDecoderAtLowerVideoResolution(), numberKeyDigit(), shouldRecoverSeekError(), tvDpadSeekPresses(), tvNumberSeekTargetMs(), tvSeekTargetMs() (+5 more)

### Community 62 - "AudioOutputFormat.kt"
Cohesion: 0.15
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 63 - "TvFocusTarget.kt"
Cohesion: 0.15
Nodes (4): rememberTvFocusTarget(), TvFocusTarget, TvFocusTargetElement, TvFocusTargetNode

### Community 65 - "SheetSections"
Cohesion: 0.16
Nodes (22): PlayerPanelTheme(), CaptionSections(), formatSubtitleDelay(), midnightSliderColors(), PanelHeader(), PlayerAdjustButton(), PlayerAdjustRow(), PlayerCaptionsSheet() (+14 more)

### Community 67 - "SiteDownloadRepository"
Cohesion: 0.29
Nodes (8): Page, SiteDownloadRepository, dramaHost(), dramaRoute(), isDramaFile(), isDramaHls(), siteDownloadInput(), siteDownloadProvider()

### Community 68 - "MegaPlayCdnHealth.kt"
Cohesion: 0.16
Nodes (6): healthHost(), isMegaPlayMedia(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState

### Community 69 - "CastButton"
Cohesion: 0.11
Nodes (7): applyCastTextTrack(), CastButton(), Callback, openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 70 - "SiteMetadataApi.kt"
Cohesion: 0.33
Nodes (16): MetadataDetails, MetadataEpisode, metadataEpisode(), MetadataImage, MetadataLink, MetadataPage, MetadataPayload, MetadataPerson (+8 more)

### Community 71 - ".verifyExport"
Cohesion: 0.16
Nodes (5): HlsExportTest, TimerTask, TimerTask, Device verification, FFmpeg

### Community 72 - "EpisodeControlPlayer"
Cohesion: 0.14
Nodes (4): applyPreferredAudioLanguage(), applyPreferredSubtitleLanguage(), EpisodeControlPlayer, preferredLanguageCodes()

### Community 73 - "test_narto_playback.py"
Cohesion: 0.19
Nodes (8): candidates_for(), episode_entry(), fetch_page(), is_hls(), main(), parse_args(), probe_candidate(), safe_stderr()

### Community 74 - "BoundedUrlTokenRegistry"
Cohesion: 0.13
Nodes (5): BoundedUrlTokenRegistry, ProxyActiveResources, stopProxyResources(), TrackedProxyResources, UrlRegistryCapacityException

### Community 75 - "AnnouncementManager.kt"
Cohesion: 0.10
Nodes (3): AnnouncementWorker, build.sh script, usage()

### Community 77 - "AnimepaheDL resume state"
Cohesion: 0.09
Nodes (23): ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task, Current follow-up release, Custom downloader only, Dedicated details and download-range dialog, Environment and hosting, Existing work and reference (+15 more)

### Community 78 - "CatalogPage"
Cohesion: 0.05
Nodes (31): Cached, catalogCacheJson(), CatalogFailureState, catalogFailureState(), catalogPageForSource(), CatalogSource, DRAMATIP, KISS18 (+23 more)

### Community 79 - "ExternalSubtitleFiles.kt"
Cohesion: 0.13
Nodes (20): SubtitleItem, ExportedSubtitle, AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName(), downloadOpenSubtitle(), exportedSubtitleMimeType() (+12 more)

### Community 80 - "AnnouncementSource.kt"
Cohesion: 0.31
Nodes (7): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), sanitizeAnnouncementUrl()

### Community 81 - "ProviderSubtitles.kt"
Cohesion: 0.11
Nodes (9): fetchProviderSubtitleBytes(), MediaSubtitle, normalizeProviderSubtitle(), providerSubtitleRequest(), ProviderSubtitles, unreferencedProviderSubtitleFile(), ReferenceResolved, CancellableHttpTest (+1 more)

### Community 82 - "AppDns"
Cohesion: 0.05
Nodes (14): DeletionTestProvider, AppDns, DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY (+6 more)

### Community 83 - "PreferredAudioLanguage"
Cohesion: 0.10
Nodes (18): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+10 more)

### Community 84 - "DiagnosticsLog"
Cohesion: 0.06
Nodes (6): LegacyTrustAnchors, CrashReporter, DiagnosticsLog, ArtworkBitmapLoader, DetailListener, PlaybackDiagnostics

### Community 85 - "AppIcon"
Cohesion: 0.10
Nodes (21): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+13 more)

### Community 86 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 88 - "LocalStreamProxy.kt"
Cohesion: 0.13
Nodes (10): BodyHead, dispatchAcceptedSocket(), namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readBody(), readHead(), readLine() (+2 more)

### Community 90 - "DramatipMetadataApi.kt"
Cohesion: 0.13
Nodes (14): dramatipCards(), dramatipFlightObjects(), dramatipImages(), dramatipIsNextLabel(), dramatipLanguages(), DramatipMetadataApi, dramatipMetadataUrl(), dramatipRequireNotFoundAbsent() (+6 more)

### Community 91 - "LocalStreamProxy"
Cohesion: 0.26
Nodes (5): Entry, LocalStreamProxy, requireNoHttpUserInfo(), RootHeaderContext, ServerEndpoint

### Community 92 - "PopupGlass.kt"
Cohesion: 0.15
Nodes (5): blurLine(), boxBlur(), PopupBackdrop, popupGlass(), rememberPopupBackdrop()

### Community 93 - "Anilili UI reference audit"
Cohesion: 0.09
Nodes (20): File inventory, Original Anilili player port, Player-port release verification (2026-11-08), Review checklist, Anilili UI reference audit, Automated source comparison, Build/package verification, Complete active shared-component inventory (+12 more)

### Community 94 - "SiteDownloadProvider"
Cohesion: 0.14
Nodes (13): ReferencePlayerHostPolicy, cleanDramaTitle(), matchesInspection(), MediaLinkRefreshGuard, mergeInspectionMetadata(), parseDramaSeries(), SiteDownloadProvider, DIRECT (+5 more)

### Community 95 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 97 - "MediaCache.kt"
Cohesion: 0.08
Nodes (12): VideoCacheSize, AUTOMATIC, GB_1, GB_2, MB_256, MB_512, OFF, ExoDatabase (+4 more)

### Community 99 - "BackdropBlur.kt"
Cohesion: 0.16
Nodes (9): GlassSection, BROWSE, DETAILS, HOME, NAVIGATION, POPUPS, REFRESH, backdropBlur() (+1 more)

### Community 101 - "dramatipPublicPosterUrl"
Cohesion: 0.21
Nodes (3): dramatipPublicPosterUrl(), DramatipSessionCookies, freshDramatipPoster()

### Community 106 - "SubtitleDelay.kt"
Cohesion: 0.13
Nodes (5): SubtitleDelay, SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 107 - "PlayerGestureControls"
Cohesion: 0.07
Nodes (36): applyBrightness(), applyVolume(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureIndicatorSide, gestureIndicatorSide(), Left (+28 more)

### Community 108 - "CatalogScreen"
Cohesion: 0.14
Nodes (3): CatalogScreen(), CatalogHero(), ReferenceHomeChrome()

### Community 109 - "SiteDownloadViewModel.kt"
Cohesion: 0.08
Nodes (16): ShortDramaPlayer(), downloadGroupArtwork(), DownloadLibrarySnapshot, downloadLibrarySnapshot(), DownloadRow, PreparationStatus, PreparationStatusRow(), isPreparedQueueRow() (+8 more)

### Community 111 - "TimeOfDay.kt"
Cohesion: 0.09
Nodes (13): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, rememberSystemUses24Hour(), ContentObserver, timeOfDayFormatter(), PlayerBars (+5 more)

### Community 112 - "SiteDownloadScreen"
Cohesion: 0.38
Nodes (7): DownloadSectionLabel(), MetadataTermChips(), RemoteDetailHero(), RemoteDownloadSheet(), SiteDownloadCookiesDialog(), SiteDownloadScreen(), SiteMetadataHighlights()

### Community 114 - "PlaybackBuffer.kt"
Cohesion: 0.10
Nodes (11): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE, AdjustableLoadControl, playbackBufferBytes() (+3 more)

### Community 115 - "ExternalPlayer.kt"
Cohesion: 0.15
Nodes (14): StreamItem, ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs(), externalPlayerIntent(), externalPlayerMimeType(), externalPlayerResultCompleted(), externalPlayerResultDurationMs() (+6 more)

### Community 116 - ".configureRequestHeaders"
Cohesion: 0.17
Nodes (3): PlaybackCacheBypassGate, PlaybackStatus, onStart()

### Community 117 - "decodeRelayFrame"
Cohesion: 0.38
Nodes (5): decodeRelayFrame(), EndOfStoredEvents, Event, RelayFrame, RelayFrameDecision

### Community 119 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 120 - "Modifier"
Cohesion: 0.26
Nodes (9): MidnightChoiceRow(), midnightFocus(), midnightInitialFocus(), MidnightRowScaffold(), midnightSelectable(), MidnightSelectionRow(), MidnightStatusRow(), MidnightStepperRow() (+1 more)

### Community 122 - "SiteDownloadSourcesTest"
Cohesion: 0.17
Nodes (5): SiteDownloadSourcesTest, Corrections made in this pass, Screen and component review, UI parity and usability checklist, Verification still needed

### Community 124 - "MidnightHelper"
Cohesion: 0.20
Nodes (12): MidnightInlineNotice(), MidnightNoticeTone, ERROR, INFO, SUCCESS, WARNING, MidnightHelper(), MidnightSectionLabel() (+4 more)

### Community 126 - "siteDownloadCookieHeader"
Cohesion: 0.15
Nodes (7): NativeHeaderContext, normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), validCookiePair(), validSiteDownloadCookieInput(), NativeRequestHeaderPolicyTest, SiteDownloadCookieTest

### Community 128 - "UI and usability findings"
Cohesion: 0.29
Nodes (7): U1 — Failed episodes are labeled “Queued” in details (P2), U2 — Search results can belong to an earlier query (P2), U3 — Documented year/type filters are absent (P3), U4 — Library grouping parses presentation text (P2), U5 — Settings failures use success-like styling (P3), U6 — Accessibility/localization coverage needs expansion (P3, verification gap), UI and usability findings

### Community 129 - "SkipTimingStatus"
Cohesion: 0.15
Nodes (12): SkipReportState, FAILED, IDLE, SENDING, SENT, SkipTimingStatus, ANISKIP, CHECKING (+4 more)

### Community 130 - "TvPlayerControl"
Cohesion: 0.10
Nodes (16): TvPlayerControl, CAPTIONS, EPISODES, FORWARD, FULLSCREEN, MUTE, MY_LIST, NEXT (+8 more)

### Community 131 - "AnimepaheDL codebase review"
Cohesion: 0.33
Nodes (6): AnimepaheDL codebase review, Architecture and strengths, Recommended features grounded in existing functionality, Scope and confidence, Suggested implementation order and acceptance gates, Verification record

### Community 132 - "PreparationResponsivenessTest.kt"
Cohesion: 0.14
Nodes (5): PreparationResponsivenessTest, SearchRecoveryTest, AnimeRepository, AnimeRepository, AnimeApplication

### Community 134 - "CaptionAppearanceEditor"
Cohesion: 0.31
Nodes (8): CaptionAppearanceDialog(), CaptionAppearanceEditor(), CaptionChoiceRow(), CaptionPlacementPad(), CaptionSample(), horizontalAnchorLabel(), marginLabel(), typeface()

### Community 135 - "Performance and maintainability"
Cohesion: 0.33
Nodes (6): P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3), P5 — Persistence schema and orchestration are difficult to evolve (P3), Performance and maintainability

### Community 137 - "SubtitleCueTimes.kt"
Cohesion: 0.15
Nodes (16): AppGraph, Profiles, loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes() (+8 more)

### Community 138 - "DpadLongPress.kt"
Cohesion: 0.26
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 139 - "GestureDragAxis"
Cohesion: 0.67
Nodes (3): GestureDragAxis, Horizontal, Vertical

### Community 142 - "MediaListEntry"
Cohesion: 0.18
Nodes (8): remoteListScores(), remoteListStatuses(), CustomListNamesSerializer, GqlMediaListResponse, MediaListCollection, MediaListCollectionData, MediaListEntry, MediaListGroup

### Community 143 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 145 - "LocalStreamProxyHeaderPolicyTest"
Cohesion: 0.20
Nodes (3): HttpOrigin, RootHeaderPolicyInterceptor, LocalStreamProxyHeaderPolicyTest

### Community 147 - "StreamRefusalLoadErrorPolicy.kt"
Cohesion: 0.33
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 148 - "AppSymbol"
Cohesion: 0.22
Nodes (3): AppSymbol(), DestinationSummary(), SettingsRow()

### Community 150 - "Real-device verification"
Cohesion: 0.18
Nodes (10): Current pending release: player controls and download playback state, Final saved-shows verification (cheaper subagent), Found and fixed, Library and Settings/announcements release, Notice failure follow-up and Korean-only KissKH, Offline-preferred playback (pending device verification), Player-port release verification (2026-11-08), Real-device verification (+2 more)

### Community 152 - "ErrorDetailsDialog"
Cohesion: 0.31
Nodes (5): DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice()

### Community 153 - "ErrorAction"
Cohesion: 0.20
Nodes (9): ErrorAction, CHANGE_SERVER, CHOOSE_FILE, FREE_SPACE, OPEN_SETTINGS, RETRY, RETRY_EXPORT, SEND_DIAGNOSTICS (+1 more)

### Community 154 - "HlsPlaylistRewrite.kt"
Cohesion: 0.42
Nodes (7): isDeclaredHlsPlaylist(), looksLikeHlsPlaylist(), replaceUri(), resolveAndRegister(), rewriteHlsPlaylist(), rewriteLine(), rewriteUriAttribute()

### Community 155 - "MidnightSettingSwitchRow"
Cohesion: 0.38
Nodes (6): MidnightSettingRow(), MidnightSettingRowScaffold(), MidnightSettingSpotlightMark(), MidnightSettingsSection(), MidnightSettingSwitchRow(), MidnightSwitch()

### Community 156 - "NextEpisodeTrigger"
Cohesion: 0.20
Nodes (10): EmbedAutoAdvanceStep, embedAutoAdvanceStep(), ADVANCE, ARM, WAIT, NextEpisodeTrigger, nextEpisodeTrigger(), AFTER_CREDITS (+2 more)

### Community 157 - ".check"
Cohesion: 0.12
Nodes (4): AnnouncementManager, AnnouncementScheduler, ForegroundCheck, NotificationPreferences()

### Community 158 - "CatalogPosterCard"
Cohesion: 0.23
Nodes (3): ArtworkRecovery, catalogHomeSections(), CatalogPosterCard()

### Community 159 - "Browse, play and download"
Cohesion: 0.20
Nodes (9): Anilili Short Dramas, Browse, play and download, Build, Downloads, Library and recovery tools, Release checks, Sessions, Supported sources (+1 more)

### Community 161 - "EpisodeSource"
Cohesion: 0.30
Nodes (6): audioLabel(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 163 - ".capture"
Cohesion: 0.22
Nodes (4): HostPlaybackSnapshot, onStop(), close(), select()

### Community 164 - "TvPlayerBackTarget"
Cohesion: 0.22
Nodes (9): TvPlayerBackTarget, CAPTION_APPEARANCE, CAPTIONS, tvPlayerBackTarget(), CONTROLS, EPISODE_DRAWER, NONE, OPEN_SUBTITLES (+1 more)

### Community 165 - "FailureReason"
Cohesion: 0.06
Nodes (38): DownloadGroupProgress, downloadGroupProgress(), Availability, SAVED, STAGING, DownloadAction, DELETE, PAUSE (+30 more)

### Community 167 - "ThemeSettings.kt"
Cohesion: 0.22
Nodes (7): accentSeedOf(), AppBackdropSource, ANIME, IMAGE, NONE, backdropUrlOf(), ProfileLook

### Community 168 - "DownloadQuality"
Cohesion: 0.25
Nodes (6): DownloadQuality, BEST, P1080, P360, P480, P720

### Community 170 - "PlatformErrorCodes.kt"
Cohesion: 0.27
Nodes (5): causeChain(), platformErrorCode(), platformErrorName(), technicalDetail(), isTlsIntercepted()

### Community 174 - "SiteShowMetadata"
Cohesion: 0.43
Nodes (7): toAppMetadata(), SiteEpisodeMetadata, SiteShowImage, SiteShowMetadata, SiteShowPerson, SiteShowRating, SiteShowTerm

### Community 178 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 179 - "WatchedFilter"
Cohesion: 0.29
Nodes (5): DiscoverFilters, WatchedFilter, COMPLETED, OFF, STARTED

### Community 180 - "MenuLanguage"
Cohesion: 0.29
Nodes (4): MenuLanguage, ENGLISH, SPANISH, SYSTEM

### Community 185 - "DownloadReferenceEpisodeRow"
Cohesion: 0.57
Nodes (4): DownloadReferenceEpisodeRow(), DownloadReferenceSeriesCard(), ReferenceCornerBadge(), WatchProgressBar()

### Community 187 - "ReferenceChromeTokens.kt"
Cohesion: 0.43
Nodes (3): appCanvasFade(), drawCanvasFade(), Midnight

### Community 192 - "Viewer"
Cohesion: 0.33
Nodes (6): AnimeStat, GqlViewerResponse, UserAvatar, Viewer, ViewerData, ViewerStatistics

### Community 193 - "UiScale"
Cohesion: 0.33
Nodes (4): UiScale, LARGE, MEDIUM, SMALL

### Community 199 - "TvNativeTextField"
Cohesion: 0.25
Nodes (3): disableSelectionActionMode(), moveNativeFocusSafely(), TvNativeTextField()

### Community 200 - "verificationPage"
Cohesion: 0.27
Nodes (4): verificationPage(), SessionBrowser(), verificationOriginMatches(), VerificationPageTest

### Community 201 - "Kind"
Cohesion: 0.40
Nodes (5): AppNotification, Kind, AIRING, MEDIA, SOCIAL

### Community 202 - "DefaultQuality"
Cohesion: 0.18
Nodes (8): DefaultQuality, AUTO, HIGHEST, P1080, P360, P480, P720, dataSaverFallbackMessage()

### Community 203 - "ThemeStyle"
Cohesion: 0.29
Nodes (5): ThemeStyle, NEUTRAL, TONAL, VIBRANT, VIVID

### Community 208 - "EroticMV metadata API"
Cohesion: 0.40
Nodes (4): Endpoints and operations, EroticMV metadata API, Fields retained, Live evidence and limitations

### Community 209 - "Theme.kt"
Cohesion: 0.52
Nodes (4): appColors(), ThemeSpec, wallpaperSeed(), AnimepaheDLTheme()

### Community 212 - "ContinueWatchingRow"
Cohesion: 0.22
Nodes (4): ContinueWatchingRow(), LibraryScreen(), rememberSeriesPlaybackSummary(), SeriesPlaybackSummary

### Community 215 - "AppBackdropDim"
Cohesion: 0.33
Nodes (4): AppBackdropDim, LIGHT, MEDIUM, STRONG

### Community 223 - "ThemeBackground"
Cohesion: 0.33
Nodes (3): ThemeBackground, AMOLED, DARK

### Community 224 - "ThemeContrast"
Cohesion: 0.33
Nodes (4): ThemeContrast, HIGH, MEDIUM, STANDARD

### Community 226 - "UiState.kt"
Cohesion: 0.29
Nodes (5): Error, Loading, rethrowIfCancellation(), Success, UiState

### Community 227 - "Reliability and correctness findings"
Cohesion: 0.18
Nodes (11): R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction), R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence), R2 — Cold-start direct completion can be missed (P1, high confidence), R3 — Direct enqueue has a process-death orphan window (P1, high confidence), R4 — HLS validation does not require expected audio (P1, high confidence), R5 — Final destination validation is weaker than staging validation (P1, high confidence), R6 — Unhandled action failures can crash the app (P2, high confidence), R7 — Retry can discard the only reference to unfinished output (P2, high confidence) (+3 more)

### Community 239 - "TvTextInputType"
Cohesion: 0.40
Nodes (5): TvTextInputType, EMAIL, NUMBER, PASSWORD, TEXT

### Community 253 - "RelatedLayout"
Cohesion: 0.33
Nodes (3): RelatedLayout, LIST, TITLES

### Community 255 - "Security and privacy findings"
Cohesion: 0.33
Nodes (6): S1 — Cache server reads unbounded lines before checking their length (P2, high confidence), S2 — Remote response bodies and pagination lack resource bounds (P2, high confidence), S3 — Verification completion accepts unrelated HTTPS pages (P2, high confidence), S4 — Session reset can race background network users (P2, high confidence), S5 — Native artifact verification checks size, not identity (P2, high confidence), Security and privacy findings

### Community 257 - "ViewerOptionsData"
Cohesion: 0.50
Nodes (4): GqlViewerOptionsResponse, ViewerOptions, ViewerOptionsData, ViewerWithOptions

## Knowledge Gaps
- **476 isolated node(s):** `NotReady`, `Settled`, `Preparation`, `Search`, `Sources` (+471 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1245 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **77 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SettingsStore` connect `SettingsStore` to `DramaPlayerSurface.kt`, `OpenSubtitlesAuthManager.kt`, `CaptionAppearanceEditor`, `InPlayerEpisodeList.kt`, `AppDeviceProfile.kt`, `PlayerSurface.kt`, `TvPlayerControls.kt`, `PosterElement`, `SettingsStore.kt`, `PlayerSettingsSheet.kt`, `PlayerSurface`, `.editCaptionStyle`, `PipeModels.kt`, `TelevisionSignals.kt`, `ThemeAccent`, `ThemeSettings.kt`, `DownloadQuality`, `AppBackdrop.kt`, `SourceFilterLanguage`, `OpenSubtitlesClient.kt`, `SubtitleCueLayout.kt`, `WatchedFilter`, `MenuLanguage`, `.init`, `OpenSubtitlesPanel.kt`, `AppSymbol`, `UiScale`, `DefaultQuality`, `ThemeStyle`, `Theme.kt`, `AppDns`, `PreferredAudioLanguage`, `PreferredSubtitleLanguage`, `AppBackdropDim`, `PlaybackService.kt`, `ThemeBackground`, `ThemeContrast`, `MediaCache.kt`, `BackdropBlur.kt`, `PlayerGestureControls`, `TimeOfDay.kt`, `PlaybackBuffer.kt`, `RelatedLayout`?**
  _High betweenness centrality (0.096) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `AutoSkipButtonBounds()`) actually correct?**
  _`PlayerSurface()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **What connects `NotReady`, `Settled`, `Preparation` to the rest of the system?**
  _476 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DramaPlayerSurface.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05996573386636208 - nodes in this community are weakly interconnected._
- **Why does `DiagnosticsLog` connect `DiagnosticsLog` to `ExternalPlaybackReturns.kt`, `OpenSubtitlesAuthManager.kt`, `FlixcloudPlaylistDataSource.kt`, `PlayerSurface.kt`, `SettingsStore.kt`, `PlayerSettingsSheet.kt`, `CastRoutes.kt`, `SleepTimer.kt`, `AppBackdrop.kt`, `AnililiFailure`, `OpenSubtitlesPanel.kt`, `MegaPlayCdnHealth.kt`, `AnnouncementManager.kt`, `AppDns`, `PlaybackService.kt`, `LocalStreamProxy.kt`, `MediaCache.kt`, `PlaybackBuffer.kt`, `ExternalPlayer.kt`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `PlayerSurface()` connect `PlayerSurface` to `SkipTimingStatus`, `TvPlayerControl`, `ExternalPlaybackReturns.kt`, `SettingsStore`, `InPlayerEpisodeList.kt`, `CaptionAppearanceEditor`, `ui/components/WaterFillLogoIndicator.kt`, `PlayerControlsScaffold`, `SubtitleCueTimes.kt`, `PlayerSurface.kt`, `TvPlayerControls.kt`, `PlayerSleepTimer.kt`, `SettingsStore.kt`, `PlayerSettingsSheet.kt`, `OpenSubtitlesPanel.kt`, `NextEpisodeTrigger`, `HlsExportTest.kt`, `.check`, `PipeModels.kt`, `TvPlayerBackTarget`, `SleepTimer.kt`, `LibraryModels.kt`, `SubtitleTrackMerge.kt`, `OpenSubtitlesPanel.kt`, `Listener`, `SheetSections`, `CastButton`, `EpisodeControlPlayer`, `DefaultQuality`, `ExternalSubtitleFiles.kt`, `PlaybackService.kt`, `SubtitleDelay.kt`, `PlayerGestureControls`, `PlayerFillerSkip.kt`, `ExternalPlayer.kt`, `.configureRequestHeaders`, `MidnightHelper`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._