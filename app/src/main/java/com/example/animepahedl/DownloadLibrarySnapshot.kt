package com.example.animepahedl

internal data class DownloadLibrarySnapshot(
    val source: List<DownloadRow> = emptyList(),
    val rows: List<DownloadRow> = emptyList(),
    val groups: List<Pair<String, List<DownloadRow>>> = emptyList(),
    val ready: Int = 0, val savedBytes: Long = 0
)

/** A show's poster belongs to its download group; episode thumbnails belong to its rows. */
internal fun downloadGroupArtwork(rows: List<DownloadRow>, records: Map<String, ExportRecord>): String =
    rows.firstNotNullOfOrNull { row ->
        records[row.id]?.identity?.anime?.poster?.takeIf(String::isNotBlank)
    } ?: rows.firstNotNullOfOrNull { it.thumbnail.takeIf(String::isNotBlank) }.orEmpty()

/** Filtering, sorting, and grouping run on Default rather than on Compose's UI thread. */
internal fun downloadLibrarySnapshot(rows: List<DownloadRow>, query: String, sort: String,
    previous: DownloadLibrarySnapshot): DownloadLibrarySnapshot {
    val matching = rows.filter { it.title.contains(query.trim(), ignoreCase = true) }
    val visible = when (sort) {
        "Status" -> matching.sortedBy { it.status }
        "Largest" -> matching.sortedByDescending { it.bytes }
        else -> matching.sortedBy { it.showTitle.ifBlank { it.title }.lowercase() }
    }
    val oldGroups = previous.groups.toMap()
    val groups = visible.groupBy { it.showId.ifBlank { it.showTitle.ifBlank { it.title.substringBefore(" · ") } } }
        .entries.sortedWith(compareBy<Map.Entry<String, List<DownloadRow>>> { entry -> when (sort) {
            "Status" -> entry.value.minOf { it.status }
            "Largest" -> ""
            else -> entry.value.first().showTitle.ifBlank { entry.value.first().title }.lowercase()
        } }.thenByDescending { if (sort == "Largest") it.value.sumOf { row -> row.bytes } else 0L })
        .map { entry ->
            val sorted = entry.value.sortedWith(compareBy<DownloadRow> {
                it.episodeNumber.toDoubleOrNull() ?: it.title.split(" · ").getOrNull(1)?.removePrefix("Episode ")?.toDoubleOrNull() ?: Double.MAX_VALUE
            }.thenBy { it.title })
            entry.key to (oldGroups[entry.key]?.takeIf { it == sorted } ?: sorted)
        }
    return DownloadLibrarySnapshot(rows, visible, groups, rows.count { it.complete },
        rows.filter { it.complete }.sumOf { it.bytes })
}
