package com.bmwelectric.snake.game

const val SCORE_HISTORY_KEY = "bmwSnakeScoreHistory"

const val SCORE_HISTORY_LIMIT = 10

private const val ROW_SEPARATOR = "\n"
private const val FIELD_SEPARATOR = "|"

/**
 * One finished (collision-ended) round. [sequence] is a per-install monotonically increasing
 * identifier assigned when the round is recorded, so equal scores can be ordered deterministically.
 */
data class ScoreEntry(
    val score: Int,
    val speedProfile: SpeedProfile,
    val completedAtMillis: Long,
    val sequence: Long,
)

/** Next monotonic sequence for a newly finished round. */
fun nextScoreSequence(history: List<ScoreEntry>): Long =
    (history.maxOfOrNull { it.sequence } ?: 0L) + 1L

/**
 * Leaderboard order: higher score first; for ties the more recently finished round ranks first
 * (newest [completedAtMillis], then newest [sequence]). Invalid entries are dropped, duplicated
 * sequences collapse to one row, and only [SCORE_HISTORY_LIMIT] rows are retained. The result is
 * stable and deterministic for identical persisted input.
 */
fun rankScoreHistory(history: List<ScoreEntry>): List<ScoreEntry> = history
    .asSequence()
    .filter { it.score >= 0 && it.completedAtMillis >= 0L && it.sequence > 0L }
    .distinctBy { it.sequence }
    .sortedWith(
        compareByDescending<ScoreEntry> { it.score }
            .thenByDescending { it.completedAtMillis }
            .thenByDescending { it.sequence },
    )
    .take(SCORE_HISTORY_LIMIT)
    .toList()

/** One-based leaderboard rank of the entry with [sequence], or 0 when it is not on the board. */
fun leaderboardRank(history: List<ScoreEntry>, sequence: Long): Int =
    history.indexOfFirst { it.sequence == sequence } + 1

/** Persists at most [SCORE_HISTORY_LIMIT] already-ranked rows as newline separated text. */
fun encodeScoreHistory(history: List<ScoreEntry>): String =
    rankScoreHistory(history).joinToString(ROW_SEPARATOR) { entry ->
        listOf(
            entry.score.toString(),
            entry.speedProfile.name,
            entry.completedAtMillis.toString(),
            entry.sequence.toString(),
        ).joinToString(FIELD_SEPARATOR)
    }

/**
 * Reads [encodeScoreHistory] output. Blank input yields an empty board; malformed rows (wrong field
 * count, unknown speed profile, non-numeric or negative values) are skipped instead of failing, so
 * a partially corrupted preference still yields a usable ranked board.
 */
fun decodeScoreHistory(rawHistory: String?): List<ScoreEntry> {
    if (rawHistory.isNullOrBlank()) return emptyList()

    val entries = rawHistory
        .lineSequence()
        .mapNotNull { line -> parseScoreEntryRow(line) }
        .toList()
    return rankScoreHistory(entries)
}

private fun parseScoreEntryRow(line: String): ScoreEntry? {
    if (line.isBlank()) return null
    val parts = line.split(FIELD_SEPARATOR)
    if (parts.size != 4) return null

    val score = parts[0].trim().toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val speedProfile = runCatching { SpeedProfile.valueOf(parts[1].trim()) }.getOrNull() ?: return null
    val completedAtMillis = parts[2].trim().toLongOrNull()?.takeIf { it >= 0L } ?: return null
    val sequence = parts[3].trim().toLongOrNull()?.takeIf { it > 0L } ?: return null

    return ScoreEntry(
        score = score,
        speedProfile = speedProfile,
        completedAtMillis = completedAtMillis,
        sequence = sequence,
    )
}
