# UI consistency and text-wrapping audit

Date: 2026-11-03
Scope: current working-tree Android Compose UI, based on commit `09c1cef` plus existing local changes.
Method: source inspection of Explore, Episodes, Downloads, Settings, About, verification, player, theme, and UI tests. No app code was changed. No rendered-device reproduction or screenshot comparison was performed. An Android device is connected, but these findings do not claim visual verification on it.

**Priority:** P1 = likely to obstruct a core task under constrained layouts; P2 = readability or interaction inconsistency; P3 = polish. **Evidence:** confirmed means the implementation or behavior is directly established by source; layout risk means the exact visual failure still needs reproduction. Intentional wrapping is not itself a defect: clipping, inaccessible content, ambiguous truncation, and excessive narrowing are the targets.

Source paths below are relative to `app/src/main/java/com/example/animepahedl/`. Line references describe this working-tree snapshot.

## Findings to fix first

### 1. Explore search actions consume the input's usable width — P1, layout risk

**Location:** `MainActivity.kt:249–262`.

The search TextField puts both a text Clear button and a padded Search button inside its trailing-icon slot, in addition to a leading icon. Clear appears only after typing, so the editable area becomes narrower at exactly the point the user needs it. The outer field has 32dp horizontal padding; both trailing actions compete with the label and query within the remaining width.

**Reproduce:** use a 320dp or 360dp window, increase font size, enter a long title, and compare the empty and nonempty field. Check cursor visibility, label visibility, and both action labels.

**Fix:** use a compact clear IconButton with an accessible label, and move Search into a separate full-width or adaptive action area. Retain keyboard search. Do not solve by reducing text size.

**Acceptance:** typing does not collapse the query area; all actions remain readable and reachable at 200% font scale.

### 2. Download cards leave too little space for identifying text — P1, confirmed narrow layout; clipping risk

**Location:** `LibraryScreens.kt:164–215`.

A completed row contains a 52dp thumbnail, weighted text, Play and More IconButtons, 12dp inter-item gaps, and 20dp card padding. With typical 48dp icon buttons, a 320dp window leaves approximately **84dp** for text: 320 − 32 list padding − 20 card padding − 52 image − 96 actions − 36 gaps. This is a layout estimate, not a measured screenshot. Title, episode/quality/audio, and status each stop at two lines with ellipsis.

**Impact:** identifying episode and audio details can disappear even when the card grows vertically. Selection mode changes the competing controls and must be checked separately.

**Reproduce:** completed downloads with long titles and Japanese/English audio metadata at 320dp; repeat in selection mode and with large fonts.

**Fix:** move actions onto a separate adaptive row, or keep one menu action and use the existing card tap for playback. Give episode identity its own readable line. Offer a details surface for truncated information.

**Acceptance:** episode identity remains distinguishable; full metadata is accessible without guessing from an ellipsis.

### 3. Stacked global banners can starve the current screen of height — P1, layout risk

**Location:** `MainActivity.kt:125–150, 151–215`.

Preparation, verification, and general-error banners are sequential non-scrolling children above the destination content. Episodes additionally reserves a save destination and download button above bottom navigation. Large text and wrapped banner actions increase these fixed regions while the list receives only the remainder.

**Reproduce:** a short landscape window with pending preparation, downloads needing verification, and a request error; repeat at 150–200% font size on Episodes.

**Fix:** consolidate persistent task notices into a compact summary with expandable details, or place relevant notices in destination scroll content. Preserve access to the download action and a useful content viewport.

**Acceptance:** the screen's list and recovery controls remain usable when notices coexist; no notice requires scrolling an area that cannot scroll.

### 4. Explore's header and empty state do not share a scroll container — P1, layout risk

**Location:** `MainActivity.kt:243–288, 407–415`; `ui/theme/Type.kt:9`.

The introductory heading (46sp), tagline, search field, filter FlowRow, and search failure are outside the results LazyColumn. The initial and no-results states are also ordinary non-scrolling content. At large fonts, filters wrap and the static header grows; the empty-state card can extend beyond the remaining height. The tagline has no horizontal inset or centered multiline text alignment.

**Reproduce:** initial Explore, no results, and search failure at 320dp and in landscape; show the keyboard and increase font size.

**Fix:** put the introductory content and empty/error states in a scrollable layout. Keep only essential search controls pinned if needed. Add horizontal padding and deliberate multiline alignment to the tagline.

**Acceptance:** all introductory, empty-state, and retry content can be reached with the keyboard visible and large fonts.

### 5. Verification instructions compete with the actual verification page — P1, layout risk

**Location:** `SessionBrowser.kt:114–155`.

A non-scrolling column contains a title row, a potentially long reason/error card, help row, weighted WebView, and footer. The WebView shrinks as explanatory text wraps. This can make the site's challenge practically unusable in landscape or with large text.

**Reproduce:** open verification on a short landscape window with a long error/reason string and enlarged font size.

**Fix:** condense the header, make detailed instructions collapsible or separately scrollable, and ensure the web challenge retains usable space. Avoid simply adding a minimum height that pushes the close/help controls off-screen.

**Acceptance:** challenge controls, close, reload, and help all remain reachable in constrained height.

## Further wrapping and information-loss issues

### 6. Episode rows combine fixed controls with a narrow label — P2, layout risk

**Location:** `MainActivity.kt:363–369`.

An 88dp screenshot, Checkbox, 24dp gaps, and 16dp padding precede weighted episode text. At 320dp, with a typical 48dp checkbox allocation and 32dp list padding, only about 112dp remains for the label. `Episode N · Saved/Queued` is a single text block with no hierarchy between identity and availability.

**Fix:** place Saved/Queued on a secondary line or badge; adapt screenshot size or switch layout under narrow constraints. Test long/fractional episode numbers and 200% text.

**Acceptance:** episode number remains easy to scan and availability does not force awkward fragmented wrapping.

### 7. Episode bulk-selection controls have no adaptive arrangement — P2, layout risk

**Location:** `MainActivity.kt:339–354`.

The episode count and Select/Deselect all share an unweighted Row. `SpaceBetween` distributes spare space but does not create it when children need more width. Select range and Clear selection are another fixed Row. The two equal-width text fields use relatively long labels, “From episode” and “To episode.”

**Fix:** give the count a weighted region and allow the action to move below it; use FlowRow or an adaptive Column for range actions. Consider shorter field labels (“From”, “To”) under an explicit “Episode range” heading, and stack fields when necessary.

**Acceptance:** counts and actions are fully visible, field labels remain understandable, and tap areas do not overlap at narrow widths and large fonts.

### 8. Destination paths have three incompatible overflow policies — P2, confirmed

**Location:** `MainActivity.kt:129`; `LibraryScreens.kt:101–107, 263, 326–327`.

The Episodes footer limits the path to two lines without ellipsis (default clipping); Settings uses two lines with ellipsis; Downloads wraps the full path next to Change. Long nested paths can look silently incomplete in the footer and disproportionately tall in Downloads.

**Fix:** use a shared destination summary with a deliberate line limit and ellipsis, plus an explicit way to inspect the full path. Retain full scrollable path details in the storage dialog.

**Acceptance:** every shortened path signals truncation and the complete destination is available before downloading.

### 9. Download status truncation hides information without a details view — P2, confirmed

**Location:** `LibraryScreens.kt:31–39, 179–205`.

Status, transferred bytes, total size, and percent are joined into one two-line ellipsized Text. The status formatter reduces some failure strings to “Download failed” or “Couldn’t save video”; the row menu exposes recovery actions but no failure-details entry. The global request error, by comparison, has a Details action.

**Fix:** separate status from transfer metrics and expose download-specific failure details with the applicable recovery action. Keep the concise friendly status for scanning.

**Acceptance:** long statuses cannot hide episode identity, and users can inspect why a specific item failed.

### 10. Collection summary gives count text precedence over its heading — P2, layout risk

**Location:** `LibraryScreens.kt:93–98`.

“Your offline collection” is weighted while the saved/active count is unweighted. Longer counts or enlarged fonts can compress the heading into a thin multiline column.

**Fix:** place counts beneath the heading or switch between Row and Column according to available width.

**Acceptance:** heading and totals remain readable without narrow word-by-word wrapping.

### 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk

**Location:** `LibraryScreens.kt:254–302, 308–314`; `MainActivity.kt:223–224`; `SessionBrowser.kt:159–162`.

The download-speed dialog has multiple paragraphs and two option groups inside a plain Column. Request details can contain arbitrary error text; verification help is a long paragraph. These bodies have no explicit scroll handling. Actual clipping depends on dialog constraints and must be tested, rather than assumed from AlertDialog alone.

**Fix:** use bounded scrollable text/content bodies and keep dismissal/confirmation actions reachable. Check the folder editor with the keyboard open; its fixed `Downloads/` prefix further reduces the editable region.

**Acceptance:** every option and the end of long error/help text can be reached in landscape, with large fonts, and with the keyboard where applicable.

### 12. Image-error copy does not adapt to thumbnail size — P3, layout risk

**Location:** `MainActivity.kt:393–397`; `LibraryScreens.kt:150, 172–173`.

The same “No image” text is rendered inside all thumbnails, including 36×50dp group images. Large text can wrap or clip inside that fixed image box.

**Fix:** use a decorative image-placeholder icon for small thumbnails and reserve explanatory text for larger image regions. Avoid duplicate spoken labels when adjacent text already identifies the show.

**Acceptance:** failed images preserve layout at every thumbnail size and font scale.

## UI consistency and state issues

### 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed

**Location:** `MainActivity.kt:169–172, 190–193`; `DownloaderViewModel.kt:99–110`.

View only sets `showDownloads = true`. The destination switch checks About and Settings first, and setting showDownloads does not clear either flag. Therefore pressing View while Settings/About is open can leave the visible screen unchanged. Bottom-navigation Downloads correctly clears both flags.

**Fix:** centralize navigation to Downloads and use it for both entry points; preferably represent the destination explicitly instead of independent flags.

**Acceptance:** pressing View from Explore, Episodes, Settings, or About visibly opens Downloads with the correct navigation selection.

### 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence

**Location:** `MainActivity.kt:175–185, 249–262, 274–280, 407–415`; `LibraryScreens.kt:121–138`.

Explore search is a filled pill with an embedded Search button; download filtering is outlined with only Clear. Some distinction is justified by submitted search versus immediate filtering, but shared dimensions and clear-control treatment are missing. Explore empty states use an inset surface, left alignment, headlineSmall/bodyLarge and a play icon; Downloads uses a centered standalone icon, titleLarge/bodyMedium and an optional CTA. General request failures use an error-colored card with Details/Dismiss; search failures use plain text with Retry/Verify.

**Fix:** define shared search-field, empty-state, and inline-error components with explicit variants for the different tasks. Standardize spacing, typography, multiline alignment, and details/recovery affordances.

**Acceptance:** equivalent states follow the same hierarchy while preserving useful distinctions between search and local filtering.

### 15. Completion and destructive-action terminology is inconsistent — P2, confirmed

**Location:** `LibraryScreens.kt:31–39, 65–69, 96, 112, 153, 206–211`; `MainActivity.kt:230–235`.

The UI alternates among Completed, Ready, and saved for completed items; single-item removal says Remove saved video, while bulk removal says Delete. Unfinished items use Cancel download. Group “active” totals and In progress filtering include every non-complete, non-failed row, including paused items.

**Fix:** agree on a vocabulary: for example Saved for completed files, explicit Paused/Waiting/Downloading states, and Delete saved video versus Cancel download. Either exclude paused items from “active” or rename that total to “unfinished.” Keep mixed bulk-action descriptions explicit.

**Acceptance:** a paused item is not misleadingly presented as transferring; equivalent destructive actions use consistent names and consequences.

### 16. Spacing, chevrons, and text hierarchy are locally defined — P3, confirmed design divergence

**Location:** `MainActivity.kt:288, 313, 368, 382, 408`; `LibraryScreens.kt:71, 155, 168, 242, 321, 329, 340`.

Screen content insets vary between 12, 16, and 20dp; card internals use 8, 10, 12, and 24dp. Some variation is legitimate, but there are no shared screen/card spacing roles. Dropdown and navigation indicators are text glyphs (`▾`, `▸`, `›`) rather than AppSymbol icons, so they scale and align as font characters. Episode identity relies on inherited Text styling plus a weight override instead of an explicit typography role.

**Fix:** establish named spacing roles, explicit episode title/status typography, and consistent vector chevrons. Give expandable groups an expanded/collapsed semantic state, rather than relying on a glyph alone. Preserve intentional differences such as poster and episode-image aspect ratios.

**Acceptance:** equivalent surfaces align consistently; chevrons remain stable at large fonts and expansion state is understandable with accessibility services.

## Suggested UI components to improve UX

These are proposed design changes, not additional reproduced defects. Build the layout fixes first, then introduce shared components. Names below describe suggested app-owned composables; they are not claims that these components already exist in the project.

| Suggested component | Where / findings | Recommended behavior and UX benefit |
| --- | --- | --- |
| `AnimeSearchField` | Explore and Downloads; 1, 4, 14 | Share input height, clear IconButton, label, and focus styling. Explore gets a separate Search action plus keyboard submission; Downloads filters immediately. Keep entered text and prior results during refresh. Provide a visible label rather than relying only on placeholder text. |
| `AdaptiveDownloadCard` | Downloads; 2, 9 | Give show title, episode identity, status, and transfer metrics distinct regions. Move secondary actions below text when space is constrained. Keep a clearly labeled menu for Pause/Resume, Retry, Details, and Delete/Cancel. Preserve the existing card playback action for saved items. |
| `DownloadDetailsSheet` | Download card menu; 2, 9 | Use a scrollable secondary surface for the complete title, episode, quality/audio, full destination, failure reason, and relevant recovery action. On short windows, allow a full-height presentation. Keep destructive actions separated from Retry/Play. |
| `TaskNoticeHost` | Global notices; 3 | Show a compact task summary with a count and one relevant action; expand to a scrollable list of preparation/verification/errors. Prioritize a blocking recovery request over routine activity. Keep unresolved errors persistent and preserve access to every notice. |
| `DestinationSummary` | Episodes, Downloads, Settings; 8 | Show a folder icon, human-readable destination, deliberate ellipsis, and a labeled View/Change action. Provide full path details separately. Use the same naming and overflow policy in all three contexts. |
| `EpisodeSelectionPanel` | Episodes; 6, 7 | Combine selected count, Select all, range fields, and Clear in an adaptive panel. Keep range entry collapsed until requested. Stack fields/actions on narrow layouts. Show invalid ranges next to the fields instead of only in a global error. |
| `EpisodeListItem` | Episodes; 6, 16 | Separate episode number, duration, and Saved/Queued status. Use an explicit text style, an adaptive thumbnail, and a decorative checkbox when the whole row handles selection. Announce the row's selected state once. |
| `DownloadFilterBar` | Downloads; 10, 15 | Retain wrapping FilterChips and place totals below the heading. Distinguish Paused from actively transferring states. Keep filter selection visible and announce it. Avoid squeezing all filters into a fixed single row. |
| `AppEmptyState` / `InlineErrorState` | Explore and Downloads; 4, 14 | Standardize title/body spacing, icon size, alignment, and one primary recovery action. Provide explicit variants for first use, no results, and recoverable failure. Preserve current results during a refresh failure. |
| `SettingsOptionRow` | Settings; 11, 16 | Share icon, title, supporting text, and trailing chevron placement. Use a full-row radio selection target for audio choices. Use a bounded scrollable settings surface for connection/concurrency controls. |
| `VerificationHeader` | Verification; 5 | Keep close, host, state, and reload compact; move long explanations behind a Help/Details action. Protect space for the challenge. Keep a visible status even when explanatory text is collapsed. |
| `AppSnackbarHost` | Transient feedback | Show brief confirmations such as “Folder updated” or “Downloads paused.” Keep failures needing user action in a persistent notice or detail surface. Offer Undo only for operations the app can actually reverse; do not offer it after permanent file deletion. |

Material offers suitable primitives such as text fields, chips, dialogs, and bottom sheets; compose these around the app's actual content constraints rather than replacing every custom row. See the [Android Material component guide](https://developer.android.com/develop/ui/compose/components). Snackbars are intended for brief feedback; see the [Compose snackbar guide](https://developer.android.com/develop/ui/compose/components/snackbar).

**Component acceptance:** at 320dp and 200% font scale, full identity/details remain accessible, actions remain reachable, and selection/expanded states are announced. Preserve at least 48dp interactive targets. Do not shrink fonts or touch targets to fit a row. Check TalkBack traversal and focus restoration after closing a sheet or dialog.

## Suggested animations and interaction feedback

The durations below are proposed starting values for this app, not platform requirements. Tune on a real device after the static layouts work. Motion should explain a state change, preserve spatial context, or acknowledge an action; it should not delay navigation or completion.

| Interaction | Proposed motion | UX purpose / implementation constraint |
| --- | --- | --- |
| Explore → Episodes and Back | A short 180–240ms fade with a restrained directional offset; reverse on Back | Clarifies navigation depth. Restore prior search/list scroll position. Keep top-level tab switches to a simple fade or immediate change; do not replay the detail transition on every data update. |
| Download group expand/collapse | Rotate the vector chevron over 150–180ms; optionally animate the reveal of a small group over 180–220ms | Makes expanded state clear. For large groups, retain lazy rendering and prefer a chevron-only animation to animating the height of hundreds of rows. Keep the group header anchored. |
| Enter/exit multi-select | Fade the contextual selection toolbar over 120–180ms; use built-in checkbox feedback and a subtle selected-surface color transition | Makes the mode change clear without moving every checkbox during selection. Keep a stable toolbar region and announce the selected count without repeated announcements for unrelated progress updates. |
| Show/hide a task notice | Brief fade and restrained vertical expansion, around 180–220ms | Explains the notice's arrival. Animate only insertion/removal or expansion, not every status-label update. Avoid repeatedly shifting content while the user is tapping. |
| Download progress | Retain the existing 250ms progress interpolation as a baseline | Smooths byte updates without inventing progress. Use indeterminate feedback when progress is unknown. Do not interpolate across different stages as though they shared one percentage; reset stage semantics explicitly. |
| Download reaches Saved | One 120–180ms status/color change and appearance of the available playback action | Gives a clear completion cue. Keep text and icon confirmation; avoid looping pulses, celebration effects, or a changed row order that pulls the item away from the user. |
| Poster loads | Optional single 120–180ms image fade inside the existing fixed bounds | Reduces abrupt image replacement. Keep dimensions stable. Avoid repeated fades on recomposition or scrolling cached images back into view. Use a static placeholder for failures. |
| Range panel or error details expands | A 180–220ms content reveal within the scrollable section | Connects the disclosure control to its content. Ensure the newly revealed fields can be scrolled into view and keyboard focus does not land in hidden content. |
| Filtered results change | Prefer immediate updates; optionally fade an empty-state replacement over 100–150ms | Preserves responsiveness while typing. Avoid staggered item entrances or animating every reorder. Retain stable item keys and avoid crossfading two large lists. |
| Buttons and option rows | Use standard Material pressed/focus/selection feedback | Confirms interaction consistently. Do not add custom bounce/scale effects to ordinary controls; they can distort text and distract from repeated actions. |

Compose provides `AnimatedVisibility` for entry/exit, `animateContentSize` for size changes, and state-driven animation APIs for values such as color or rotation. Apply them locally after checking layout behavior; animating an entire screen's height can amplify the wrapping issues in this report. See [Animation modifiers and composables](https://developer.android.com/develop/ui/compose/animation/composables-modifiers).

### Motion accessibility and performance requirements

- Respect the system animation-duration setting, including disabled animations. Verify behavior with the project's resolved Compose version; custom animations must also have an immediate final-state path.
- If an app-level reduced-motion option is introduced, suppress decorative translation, rotation, and image fades. Keep task status available through static text, icons, and progress semantics.
- Never rely on movement or color alone to communicate completion, selection, failure, or expansion. Update accessibility semantics with the actual state, without waiting for a decorative transition.
- Avoid indefinite shimmer and pulsing backgrounds. A labeled progress indicator is sufficient for ongoing work. Pause unnecessary animation when its content is not visible.
- Keep controls usable during transitions; prevent duplicate submission in application state. Do not make animation completion a prerequisite for a download, retry, or navigation action.
- Test interruption: rapidly expand/collapse a group, switch filters, navigate Back, rotate the device, and finish a download during selection. The final UI must match the latest state without ghost content or lost focus.
- Profile scrolling with multiple active downloads on a representative device. Do not animate each byte-count text change or recompose the whole list for a local decorative effect.

### Suggested rollout

1. **First:** shared search field, adaptive download/episode rows, destination summary, scrollable details, and compact notices. Ship the static layout fixes before motion polish.
2. **Next:** contextual selection feedback, group-chevron rotation, stable progress interpolation, and a single completion cue.
3. **Last:** optional navigation and image fades, only after checking large-font layouts, reduced motion, accessibility focus, and list performance.

Validate animations with system animations enabled and disabled. Compare intermediate and final layout states at the same widths/font scales used in the audit. No proposed transition should reintroduce clipping, hide recovery actions, or move a control under an active pointer.

## Implementation order

1. Fix search width allocation, download-card text space, and global/Explore/verification height handling (1–5).
2. Fix episode controls, path/status disclosure, summary layout, and dialog scrolling (6–11).
3. Correct the View navigation action and status vocabulary (13, 15).
4. Consolidate common state components, spacing, typography, and placeholders (12, 14, 16).

Avoid a global `maxLines = 1` patch: it would exchange wrapping for silent information loss. Use adaptive arrangement first; truncate only secondary content with a full-content disclosure path. Existing FlowRows for filters and selection actions are useful patterns to retain.

## Verification plan

Existing instrumentation tests cover navigation and visible text, including `SearchScreenTest`, `LibraryNavigationTest`, and `DownloadSelectionTest`. A source search found no explicit font-scale/window-size overrides or screenshot capture coverage in `app/src/androidTest`. Visibility assertions alone do not prove that text is untruncated or that adjacent controls do not collide.

Test the changes against:

- **Windows:** 320dp, 360dp, 411dp portrait; short landscape; tablet and split-screen.
- **Text/display settings:** 100%, 150%, 200% font scale, plus increased display size; verify light and dark themes.
- **Data:** 120-character titles, long words without spaces, long nested destination paths, large episode counts, fractional episode numbers, long error strings, absent thumbnails, and mixed download states.
- **States:** empty/loading/results/error Explore; Episodes with all/partial/no selection; completed/paused/failed download cards; bulk selection; stacked global notices; every settings dialog; verification loading/error; keyboard open.
- **Checks:** reachable actions and dialog endings, readable episode identity, deliberate ellipses with full details available, no overlapping controls, stable scroll access, appropriate semantics for selection and expansion.

Add focused Compose regression checks for navigation from Settings/About via View, constrained-width cards/search, and large-font dialog/list reachability. Use text-layout overflow assertions or screenshot inspection where actual wrapping matters. Do not treat node existence or `assertIsDisplayed()` alone as proof that the entire label is visible.

This report is a source-based fix backlog. Device verification should confirm the layout-risk entries before assigning them the status of reproduced visual defects.

## Follow-up status (2026-11-15)

- Finding 2: release 1.2.21 uses an 84dp thumbnail and hides the duplicate pause/resume shortcut on download cards narrower than 420dp; the same action remains in the row menu. Titles and statuses have bounded lines, and the Details action exposes the full text. Build and lint passed. The constrained-width and large-font visual acceptance check remains open because this environment has no connected Android device or emulator.
- Finding 15: release 1.2.22 says “Saved offline” instead of claiming every completed file is in the Downloads folder, and completed-item Details no longer shows the current folder as the file's actual location. Other action-label consistency still needs review.
