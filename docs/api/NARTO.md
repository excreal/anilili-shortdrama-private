# Narto Drama public metadata API

Implementation: `NartoMetadataApi`, using the shared `SiteMetadataApi` contract. This is independent of displayed UI cards and does not change existing media resolution.

## Adapter recheck (2026-11-17)

Fresh HTTPS captures of the English homepage, search for “My X-Ray Vision is Too OP”,
GoroGoro provider sections, the [series detail](https://edge.narto-drama.com/detail/watch/my-x-ray-vision-is-too-op?lang=en-US),
and episode 26 were replayed through the production Kotlin parsers. The detail exposes
59 episode anchors despite embedding Turnstile markup; the episode page exposes 59
`episodeItemsRaw` entries. Optional widgets must not invalidate that public inventory.
Metadata HTTP still rejects explicit challenge responses, challenge-page titles, and
unsuccessful HTTP responses; Narto reports temporary unavailability rather than asking
for a session.

- Inventory accepts the three supported Narto hosts, rejects foreign shows/hosts and
  insecure links, and parses the document once for title, poster, description and anchors.
- Metadata hydration uses at most one additional episode request, merges by episode
  number without dropping anchors, and reuses player metadata already in the detail page.
  Missing watch URLs are reconstructed for the matching episode; redirects and declared
  URLs for a different title/episode are rejected. Cancellation remains cancellable.
- Subtitle tracks from `multi_subtitles`, `subtitle_url`, and `direct_subtitle_url` now
  reach playback/download resolution, with HTTPS validation and URL deduplication.
- Provider cards without a valid watch or provider-import route are omitted; import links remain metadata only. Homepage provider taxonomy
  includes `tab-provider` links. Existing provider pagination tests remain in the suite.
- `scripts/test_narto_playback.py --episode-url 'https://edge.narto-drama.com/detail/watch/my-x-ray-vision-is-too-op/26?lang=en-US' --episode 26 --seconds 4 --timeout 50`
  downloaded a 4.422-second primary HLS sample: H.264 540×960 video and AAC stereo
  audio. The direct fallback returned HTTP 403, so the working browser source remains
  first. This is a server-side media check, not an Android device download test.

`NartoAdapterTest` covers the failure modes with synthetic fixtures. For a fresh live
capture replay, save the five public GET responses outside the checkout as
`narto-live-home.html`, `narto-live-search.json` (HTML search response),
`narto-live-provider.json`, `narto-live-detail.html`, and `narto-live-episode.html`.
Run `NARTO_CAPTURE_DIR=/path/to/captures ./gradlew :app:testDebugUnitTest --tests '*NartoLiveCaptureTest' --rerun-tasks`.
The opt-in test is skipped when the directory variable is absent. Raw pages and signed
media URLs are intentionally not committed.

## Live audit evidence

Inspected 2026-11-08 UTC, using successful bounded HTTPS GETs to these primary sources:

- [English homepage](https://edge.narto-drama.com/?lang=en-US): HTTP 200, 456,666-byte HTML capture; five featured cards and 24 native database cards; JSON-LD site/collection/navigation/organization objects; filter links and provider explorer JavaScript. Counts and payload sizes are observations, not hardcoded limits.
- [Sample detail](https://edge.narto-drama.com/detail/watch/mr-ceo-you-re-not-supposed-to-fall-for-me?lang=en-US): HTTP 200; full visible synopsis, poster, native movie ID, Sweet/CEO title tags, 78 episode anchors, related cards, locale alternatives. A render comment reports playable 20 versus database total 78.
- [Sample episode](https://edge.narto-drama.com/detail/watch/mr-ceo-you-re-not-supposed-to-fall-for-me/1?lang=en-US): HTTP 200; complete `episodeItemsRaw` list, all 78 entries, 22 fields per entry; episode-specific snapshot, availability and streaming hints. Its JSON-LD VideoObject includes upload date and family-friendly flag.
- Seven additional episode pages, including [DramaWave multilingual subtitles](https://edge.narto-drama.com/detail/watch/sistema-podarila-mne-triokh-devushek/1?lang=en-US), [DramaBox multilingual subtitles](https://edge.narto-drama.com/detail/watch/deadliest-prison-the-female-warden-is-my-plaything/1?lang=en-US) and [NetShort subtitles](https://edge.narto-drama.com/detail/watch/villainess-princess-billionaire-queen/1?lang=en-US): actual subtitle arrays were present. Other sampled providers had empty arrays; absent captions are not invented.
- [Provider section API](https://edge.narto-drama.com/home/providers/sections?provider=dramawave&lang=en-US&target_lang=en-US): HTTP 200 JSON. Returned `ok`, 57 provider keys/labels, `active_provider`, `sections`, `search_mode`, `search_query`, `tab_pages`, `provider_filters`.
- [Search API](https://edge.narto-drama.com/search?q=CEO&limit=5&lang=en-US), requested with JSON Accept: HTTP 200 JSON with `ok`, `items`, `failed_providers`; five items. Search results retain relevance scores as source attributes, never as user ratings.

The bare main origin can present browser verification; the supported edge origin served the same public site in these requests. The API uses the edge origin by default, validates HTTPS and approved source hosts, and respects challenge responses. Raw HTML captures remain outside the repository because pages contain CSRF/session values and unrelated scripts.

## API methods and routing

| Method | Result and behavior |
| --- | --- |
| `browse()` | HTML database items, featured section, next-page URL, taxonomy and extra page data. |
| `browse(query = MetadataBrowseQuery(query = "…"))` | Public JSON search; all result fields retained. Search language selects the site frontend, not a guaranteed original audio language. |
| `browse(query = MetadataBrowseQuery(url = "…"))` | Explicit same-source tag, genre, category, featured, filtered or next-page route. URL already contains pagination/filters; conflicting additional query options are rejected. Provider section continuation URLs issue one GET and advance only the selected `only_tab`, retaining the other query state. |
| `browse(filters = mapOf("tag" to "sweet"))` | Real `/tag/sweet` route. `genre` likewise uses `/genre/{slug}`. |
| `browse(filters = mapOf("provider" to "dramawave"))` | Public provider explorer JSON, parsed into sections and full item records. `tab-provider` is an alias matching the website's frontend URL parameter. |
| `details(url)` | Detail fields plus complete episode metadata from one first-episode HTML hydration request. Never fetches every episode; at most two HTML requests. Failed hydration retains episode anchors and reports its unavailability in a payload. |
| `episode(url)` | Requested episode, original raw object, subtitles, snapshot, visible description and JSON-LD date/flags. Requires an episode URL. |
| `taxonomy()` | All advertised page genres/tags/category/type/locale links plus the current provider API list; retains source payloads. |
| `providerSections(provider, language, page, parameters)` | GET-only extension returning full provider payload including filters/tab pages. Empty provider asks the server for its default provider. Page >1 maps to the server's actual `tab_pages[tab_key]` parameters after discovering tab keys, with at most two requests. |

Supported native filter parameters are validated. Main browse accepts `lang`, `type`, `category`, `tag`, `genre`, provider and provider-tab filters. Provider section options accept `q`, `type_context`, `only_tab`, `pinedrama_category_id`, `flareflow_class_id` and explicit `tab_pages[<key>]`. Unsupported keys are rejected rather than silently ignored. Provider page numbers refer to each source section; the server does not use a single global page parameter for its explorer. Sections expose their tab key, current page, previous-page flag, fallback, both warning fields and a provisional per-section continuation URL; continuation stops for empty, clamped, non-progressing and search responses. Provider search (`q`) cannot request a later page. `MetadataPage` also exposes `tab_pages` and normalized provider filters, including PineDrama categories and FlareFlow classes, while preserving the complete sanitized response payload. Empty array values for `tab_pages` or `provider_filters` are treated as absent objects. A search response marked `login_required` remains a valid empty metadata page with its payload retained.

## Field coverage

| Metadata | Where it is retained |
| --- | --- |
| Native movie/database ID, book ID, episode DB ID, route number and displayed number | Structured record/episode identity and original payload. |
| Title, original episode title, complete detail synopsis, short list/featured synopsis | Structured fields; visible detail synopsis takes precedence over truncated SEO description. |
| Posters, featured artwork, responsive dimensions, captions/alt text | `MetadataImage` entries and schema/meta payloads. Featured artwork is labeled a featured poster; the audit did not find a separate landscape banner asset. |
| Real episode thumbnail/snapshot | `thumb_url` becomes an `episode-snapshot` image; it is not replaced by the series poster. |
| Per-title tags, source-provided category/provider label, language code, adult flag, matched tokens, relevance score | Structured terms/languages where appropriate, primitive attributes and complete original item JSON. |
| Taxonomy genres, tags, categories, media types, locales and provider keys/labels | `MetadataTaxonomy.groups`, advertised URLs and provider payload. Taxonomy entries are global options and are not falsely assigned to every title. |
| Declared episode total versus playable count versus DB count | Separate `declaredEpisodeCount`, `playableEpisodeCountAtDetailRender`, `databaseEpisodeCount`, and hydrated `playableEpisodeCount` attributes. |
| Related/You May Like items | `MetadataDetails.related`, with own poster/title/link. |
| Upload date, language, family-friendly flag, series relation, canonical/embed/page links | Structured date/language/link when shared HTML parser recognizes them and complete JSON-LD payload in all cases. |
| Public footer global totals, category/tag count, latest update, listing pager text and explorer endpoint configuration | `pageFacts` payload; global platform totals are not mistaken for the current listing total. |
| Subtitle URL, language code, label, default flag, selected subtitle language, direct subtitle/local flags | Structured `MetadataSubtitle` per episode and original raw episode. Relative subtitle URLs are resolved against the source page. |
| Multiple resolutions, separate audio URL, stream/direct URLs, HLS/local/CORS/prefetch/playability flags | Complete public episode JSON and primitive attributes. Availability metadata is not a guarantee that an expiring link stays usable. |
| Supported subtitle-build language objects, allowed language list and public build/discovery endpoint | Whitelisted `publicPlayerConfiguration` JSON. Merely describes the web feature; no AI job is started. |
| Unknown/new fields | Original public objects and source schema/meta payloads, sanitized by the shared payload writer. |

Observed episode object fields:

`id`, `route_episode_number`, `number`, `title`, `play_url`, `play_url_cors`, `subtitle_url`, `thumb_url`, `schema_content_url`, `browser_prefetch_mode`, `is_playable`, `multi_subtitles`, `selected_subtitle_language`, `multi_resolutions`, `watch_url`, `direct_play_url`, `direct_audio_url`, `direct_subtitle_url`, `direct_play_is_local`, `direct_play_is_hls`, `direct_subtitle_is_local`.

Actual subtitle descriptors expose `language_code`, `label`, `subtitle_url`, `is_default`. Provider section cards expose `book_id`, `title`, `description`, `poster_url`, `watch_url`, `source_type`, `category_name`, `tag_names`, `is_adult`, with other provider fields retained unchanged. JSON search cards additionally expose database `id`, `tags`, `matched_tokens`, `language_code` and `_score`.

## Not supplied / not automatically fetched

- The sampled short-drama records do not report a numeric user rating/vote count, cast/crew, production country, original release year or a separate banner. Corresponding fields remain absent rather than inferred from synopsis, language or third-party providers.
- Subtitle availability differs by episode/provider. Empty descriptors are distinct from “subtitle support does not exist”. No burned-in caption track is fabricated when arrays are empty.
- Account history, subtitle preference sync and login endpoints are user state, not public title metadata. CSRF tokens, account/session seed objects, ad scripts and cookies are excluded.
- GET browse/detail APIs do not invoke account mutation, `search/import` links, subtitle generation, ad unlock, error reporting or other side effects. Provider import links are retained for a caller to act on explicitly.
- Media/subtitle proxy URLs are source-advertised expiring locators. They are retained for future callers but should be refreshed before playback. API payloads must not be treated as permanent media manifests.
- This audit inspected the public web responses and client behavior, not every one of the hundreds of thousands of titles. Field completeness applies to exposed schemas discovered here; provider-specific unknown fields remain available as raw JSON.

The original 2026-11-08 audit did not include a build or device test. See the 2026-11-17 recheck above and `docs/implementation-progress.md` for current integrated verification.
