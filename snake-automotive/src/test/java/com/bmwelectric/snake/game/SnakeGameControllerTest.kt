package com.bmwelectric.snake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeGameControllerTest {
    @Test
    fun startsInParkedReadyWithComfortSpeed() {
        val controller = SnakeGameController(bestScore = 12)

        assertEquals(VehicleState.Parked, controller.state.vehicleState)
        assertEquals(RoundState.Ready, controller.state.roundState)
        assertEquals(SpeedProfile.Comfort, controller.state.speedProfile)
        assertEquals(12, controller.state.bestScore)
        assertEquals(4, controller.state.snake.size)
    }

    @Test
    fun drivingLocksImmediatelyAndParkingReturnsReadyWithoutResume() {
        val controller = SnakeGameController(bestScore = 40)
        controller.start()
        controller.inputDirection(Direction.Down)
        controller.tick()

        controller.setVehicleState(VehicleState.Driving)

        assertEquals(RoundState.DrivingLocked, controller.state.roundState)
        assertEquals(VehicleState.Driving, controller.state.vehicleState)
        assertEquals(40, controller.state.bestScore)

        controller.setVehicleState(VehicleState.Parked)

        assertEquals(RoundState.Ready, controller.state.roundState)
        assertEquals(VehicleState.Parked, controller.state.vehicleState)
        assertEquals(0, controller.state.score)
        assertEquals(Direction.Right, controller.state.direction)
        assertEquals(40, controller.state.bestScore)
    }

    @Test
    fun directReverseInputIsIgnored() {
        val controller = SnakeGameController()
        controller.start()

        controller.inputDirection(Direction.Left)

        assertEquals(Direction.Right, controller.state.nextDirection)
        controller.tick()
        assertEquals(Point(7, 10), controller.state.snake.first())
    }

    @Test
    fun rapidInputKeepsLatestLegalDirectionAndIgnoresReverseOfCurrentMotion() {
        val controller = SnakeGameController()
        controller.start()

        controller.inputDirection(Direction.Up)
        controller.inputDirection(Direction.Left)
        controller.inputDirection(Direction.Down)

        assertEquals(Direction.Down, controller.state.nextDirection)
        controller.tick()
        assertEquals(Direction.Down, controller.state.direction)
        assertEquals(Point(6, 11), controller.state.snake.first())
    }

    @Test
    fun pauseAndContinueFreezeAndResumeTicks() {
        val controller = SnakeGameController()
        controller.start()
        controller.pauseOrContinue()
        val pausedHead = controller.state.snake.first()

        controller.tick()

        assertEquals(RoundState.Paused, controller.state.roundState)
        assertEquals(pausedHead, controller.state.snake.first())

        controller.pauseOrContinue()
        controller.tick()

        assertEquals(RoundState.Playing, controller.state.roundState)
        assertEquals(Point(7, 10), controller.state.snake.first())
    }

    @Test
    fun wallCollisionEndsRound() {
        val controller = SnakeGameController()
        controller.start()

        repeat(14) {
            controller.tick()
        }

        assertEquals(RoundState.GameOver, controller.state.roundState)
        assertTrue(controller.state.statusAnnouncement.contains("Collision"))
    }

    @Test
    fun eatingEnergyUpdatesScoreBestAndPersistence() {
        var persistedBest = 0
        val controller = SnakeGameController(
            bestScore = 0,
            persistBestScore = { persistedBest = it },
            nextEnergyProvider = { _, _ -> Point(0, 0) },
        )
        controller.start()

        repeat(8) {
            controller.tick()
        }

        assertEquals(20, controller.state.score)
        assertEquals(20, controller.state.bestScore)
        assertEquals(20, persistedBest)
        assertEquals(5, controller.state.snake.size)
    }

    @Test
    fun drivingStateIgnoresGameActions() {
        val controller = SnakeGameController()
        controller.setVehicleState(VehicleState.Driving)

        controller.start()
        controller.inputDirection(Direction.Down)
        controller.tick()

        assertEquals(RoundState.DrivingLocked, controller.state.roundState)
        assertEquals(0, controller.state.score)
        assertEquals(Direction.Right, controller.state.nextDirection)
        assertEquals(Point(6, 10), controller.state.snake.first())
    }

    @Test
    fun initialBestScoreIsDerivedFromHistoryAndNegativeInputClamped() {
        val history = listOf(
            ScoreEntry(score = 60, speedProfile = SpeedProfile.Dynamic, completedAtMillis = 10L, sequence = 1L),
            ScoreEntry(score = 20, speedProfile = SpeedProfile.Eco, completedAtMillis = 20L, sequence = 2L),
        )

        val controller = SnakeGameController(bestScore = -5, scoreHistory = history)

        assertEquals(60, controller.state.bestScore)
        assertEquals(listOf(1L, 2L), controller.state.scoreHistory.map { it.sequence })
    }

    @Test
    fun collisionRecordsOneRankedEntryAndPersistsExactlyOnce() {
        var persistCount = 0
        var persistedHistory: List<ScoreEntry>? = null
        val controller = SnakeGameController(
            persistScoreHistory = {
                persistCount += 1
                persistedHistory = it
            },
            completedAtProvider = { 5_000L },
        )
        controller.start()

        repeat(14) {
            controller.tick()
        }

        // Straight run eats the (14,10) energy at tick 8 (+20 Comfort) and hits the wall at tick 14.
        assertEquals(RoundState.GameOver, controller.state.roundState)
        assertEquals(20, controller.state.score)
        assertEquals(1, controller.state.scoreHistory.size)
        val entry = controller.state.scoreHistory.single()
        assertEquals(20, entry.score)
        assertEquals(SpeedProfile.Comfort, entry.speedProfile)
        assertEquals(5_000L, entry.completedAtMillis)
        assertEquals(1L, entry.sequence)
        assertEquals(1L, controller.state.lastRecordedSequence)
        assertEquals(1, persistCount)
        assertEquals(controller.state.scoreHistory, persistedHistory)
        assertTrue(controller.state.statusAnnouncement.contains("Rank 1 of 1"))

        // Further ticks after GameOver must not duplicate the recorded round.
        controller.tick()
        controller.tick()
        assertEquals(1, controller.state.scoreHistory.size)
        assertEquals(1, persistCount)
    }

    @Test
    fun zeroScoreRoundIsStillRecordedOnLeaderboard() {
        val controller = SnakeGameController(completedAtProvider = { 1_000L })
        controller.start()
        // Turn up immediately so the snake crashes into the top wall before reaching energy.
        controller.inputDirection(Direction.Up)
        repeat(12) {
            controller.tick()
        }

        assertEquals(RoundState.GameOver, controller.state.roundState)
        assertEquals(0, controller.state.score)
        assertEquals(1, controller.state.scoreHistory.size)
        assertEquals(0, controller.state.scoreHistory.single().score)
    }

    @Test
    fun repeatedRoundsAppendEntriesAndTiesRankNewestFirst() {
        var now = 1_000L
        val controller = SnakeGameController(completedAtProvider = { now })
        controller.start()
        repeat(14) { controller.tick() }
        assertEquals(RoundState.GameOver, controller.state.roundState)

        now = 2_000L
        controller.restart()
        repeat(14) { controller.tick() }

        assertEquals(RoundState.GameOver, controller.state.roundState)
        assertEquals(2, controller.state.scoreHistory.size)
        // Equal 20-point Comfort rounds: newer completion ranks first.
        assertEquals(listOf(2L, 1L), controller.state.scoreHistory.map { it.sequence })
        assertEquals(2L, controller.state.lastRecordedSequence)
        assertTrue(controller.state.statusAnnouncement.contains("Rank 1 of 2"))
    }

    @Test
    fun differentSpeedScoresRankByPointsAndSurviveReadyTransitions() {
        var now = 1_000L
        val controller = SnakeGameController(completedAtProvider = { now })

        // Eco round: straight run scores 10.
        controller.setSpeedProfile(SpeedProfile.Eco)
        controller.start()
        repeat(14) { controller.tick() }
        assertEquals(10, controller.state.score)

        // Exit to Ready (history retained), switch to Dynamic, score 30 in the next round.
        controller.exit()
        now = 2_000L
        controller.setSpeedProfile(SpeedProfile.Dynamic)
        controller.start()
        repeat(14) { controller.tick() }

        assertEquals(30, controller.state.score)
        assertEquals(listOf(30, 10), controller.state.scoreHistory.map { it.score })
        assertEquals(
            listOf(SpeedProfile.Dynamic, SpeedProfile.Eco),
            controller.state.scoreHistory.map { it.speedProfile },
        )
        assertEquals(30, controller.state.bestScore)
    }

    @Test
    fun historySurvivesDrivingLockAndReturnToParked() {
        val controller = SnakeGameController(completedAtProvider = { 1_000L })
        controller.start()
        repeat(14) { controller.tick() }
        assertEquals(1, controller.state.scoreHistory.size)

        controller.setVehicleState(VehicleState.Driving)
        assertEquals(RoundState.DrivingLocked, controller.state.roundState)
        assertEquals(1, controller.state.scoreHistory.size)

        controller.setVehicleState(VehicleState.Parked)
        assertEquals(RoundState.Ready, controller.state.roundState)
        assertEquals(0, controller.state.score)
        assertEquals(1, controller.state.scoreHistory.size)
        assertEquals(20, controller.state.bestScore)
        assertNull(controller.state.lastRecordedSequence)
    }

    @Test
    fun exitingMidRoundDoesNotRecordHistory() {
        var persistCount = 0
        val controller = SnakeGameController(
            persistScoreHistory = { persistCount += 1 },
            nextEnergyProvider = { _, _ -> Point(0, 0) },
        )
        controller.start()
        repeat(3) { controller.tick() }
        assertEquals(0, controller.state.scoreHistory.size)

        controller.exit()

        assertEquals(RoundState.Ready, controller.state.roundState)
        assertEquals(0, controller.state.scoreHistory.size)
        assertEquals(0, persistCount)
    }
}
