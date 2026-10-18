package com.bmwelectric.snake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreHistoryTest {
    private fun entry(
        score: Int,
        speedProfile: SpeedProfile = SpeedProfile.Comfort,
        completedAtMillis: Long = 0L,
        sequence: Long,
    ) = ScoreEntry(
        score = score,
        speedProfile = speedProfile,
        completedAtMillis = completedAtMillis,
        sequence = sequence,
    )

    @Test
    fun decodeBlankHistoryYieldsEmptyBoard() {
        assertEquals(emptyList<ScoreEntry>(), decodeScoreHistory(null))
        assertEquals(emptyList<ScoreEntry>(), decodeScoreHistory(""))
        assertEquals(emptyList<ScoreEntry>(), decodeScoreHistory("\n  \n"))
    }

    @Test
    fun decodeSkipsMalformedRowsButKeepsValidRows() {
        val rawHistory = listOf(
            "20|Eco|100|1",
            "broken",
            "40|Dynamic|90|2",
            "-1|Comfort|120|3",
            "40|Comfort|110|4",
            "10|Missing|130|5",
            "50|Comfort|not-a-time|6",
            "50|Comfort|140",
            "60|Comfort|150|0",
        ).joinToString("\n")

        val history = decodeScoreHistory(rawHistory)

        assertEquals(listOf(4L, 2L, 1L), history.map { it.sequence })
        assertEquals(40, history[0].score)
        assertEquals(SpeedProfile.Comfort, history[0].speedProfile)
        assertEquals(110L, history[0].completedAtMillis)
        assertEquals(40, history[1].score)
        assertEquals(SpeedProfile.Dynamic, history[1].speedProfile)
        assertEquals(20, history[2].score)
        assertEquals(SpeedProfile.Eco, history[2].speedProfile)
    }

    @Test
    fun rankOrdersByScoreDescThenRecencyForTies() {
        val history = rankScoreHistory(
            listOf(
                entry(score = 20, completedAtMillis = 100L, sequence = 1L),
                entry(score = 40, completedAtMillis = 90L, sequence = 2L),
                entry(score = 40, completedAtMillis = 110L, sequence = 3L),
                entry(score = 30, completedAtMillis = 120L, sequence = 4L),
            ),
        )

        assertEquals(listOf(3L, 2L, 4L, 1L), history.map { it.sequence })
    }

    @Test
    fun rankTieWithSameTimestampUsesSequenceDesc() {
        val history = rankScoreHistory(
            listOf(
                entry(score = 40, completedAtMillis = 100L, sequence = 7L),
                entry(score = 40, completedAtMillis = 100L, sequence = 9L),
            ),
        )

        assertEquals(listOf(9L, 7L), history.map { it.sequence })
    }

    @Test
    fun rankIsDeterministicAcrossRepeatedCalls() {
        val input = listOf(
            entry(score = 30, completedAtMillis = 50L, sequence = 5L),
            entry(score = 30, completedAtMillis = 80L, sequence = 2L),
            entry(score = 90, completedAtMillis = 10L, sequence = 9L),
        )

        val first = rankScoreHistory(input)
        val second = rankScoreHistory(input.reversed())

        assertEquals(first, second)
        assertEquals(listOf(9L, 2L, 5L), first.map { it.sequence })
    }

    @Test
    fun rankDropsInvalidEntriesAndDuplicateSequencesAndCapsLimit() {
        val input = buildList {
            add(entry(score = -5, sequence = 1L))
            add(entry(score = 100, completedAtMillis = -1L, sequence = 2L))
            add(entry(score = 70, sequence = 3L))
            add(entry(score = 70, completedAtMillis = 5L, sequence = 3L)) // duplicate sequence
            for (i in 4..14) {
                add(entry(score = i, completedAtMillis = i.toLong(), sequence = i.toLong()))
            }
        }

        val history = rankScoreHistory(input)

        assertEquals(SCORE_HISTORY_LIMIT, history.size)
        assertEquals(70, history.first().score)
        assertEquals(1, history.count { it.sequence == 3L })
        // 11 valid rows ranked 70,14..4; cap keeps the ten highest, ending at score 6.
        assertEquals(6, history.last().score)
    }

    @Test
    fun encodeDecodeRoundTripPreservesRankedBoard() {
        val ranked = rankScoreHistory(
            listOf(
                entry(score = 20, speedProfile = SpeedProfile.Eco, completedAtMillis = 100L, sequence = 1L),
                entry(score = 60, speedProfile = SpeedProfile.Dynamic, completedAtMillis = 200L, sequence = 2L),
            ),
        )

        val encoded = encodeScoreHistory(ranked)
        val decoded = decodeScoreHistory(encoded)

        assertEquals(ranked, decoded)
        assertEquals(encoded, encodeScoreHistory(decoded))
    }

    @Test
    fun nextSequenceIsMonotonicEvenAfterTruncationOrGaps() {
        assertEquals(1L, nextScoreSequence(emptyList()))
        assertEquals(8L, nextScoreSequence(listOf(entry(score = 1, sequence = 7L))))
    }

    @Test
    fun leaderboardRankIsOneBasedAndZeroWhenAbsent() {
        val history = rankScoreHistory(
            listOf(
                entry(score = 10, sequence = 1L),
                entry(score = 30, sequence = 2L),
            ),
        )

        assertEquals(1, leaderboardRank(history, 2L))
        assertEquals(2, leaderboardRank(history, 1L))
        assertEquals(0, leaderboardRank(history, 99L))
    }

    @Test
    fun encodeOfExcessiveHistoryStaysBoundedToLimit() {
        val oversized = (1L..20L).map { entry(score = it.toInt(), sequence = it) }

        val encoded = encodeScoreHistory(oversized)

        assertEquals(SCORE_HISTORY_LIMIT, encoded.lines().size)
        assertEquals(20, encoded.lines().first().substringBefore('|').toInt())
    }

    @Test
    fun emptyBoardHasNoBestScoreAndNullRank() {
        assertNull(emptyList<ScoreEntry>().maxOfOrNull { it.score })
        assertEquals(0, leaderboardRank(emptyList(), 1L))
    }
}
