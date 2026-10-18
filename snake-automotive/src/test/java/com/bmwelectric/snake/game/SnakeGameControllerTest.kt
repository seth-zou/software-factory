package com.bmwelectric.snake.game

import org.junit.Assert.assertEquals
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
    fun gameOverPersistsCompletedRoundInRankedHistory() {
        var persistedHistory = emptyList<ScoreEntry>()
        val previousEntry = ScoreEntry(
            score = 30,
            speedProfile = SpeedProfile.Dynamic,
            completedAtMillis = 900L,
            sequence = 7L,
        )
        val controller = SnakeGameController(
            bestScore = 0,
            scoreHistory = listOf(previousEntry),
            persistScoreHistory = { persistedHistory = it },
            completedAtProvider = { 1000L },
            nextEnergyProvider = { _, _ -> Point(0, 0) },
        )
        controller.start()

        assertEquals(listOf(previousEntry), controller.state.scoreHistory)

        repeat(14) {
            controller.tick()
        }

        assertEquals(RoundState.GameOver, controller.state.roundState)
        assertEquals(20, controller.state.score)
        assertEquals(2, controller.state.scoreHistory.size)
        assertEquals(previousEntry, controller.state.scoreHistory.first())
        assertEquals(20, controller.state.scoreHistory[1].score)
        assertEquals(SpeedProfile.Comfort, controller.state.scoreHistory[1].speedProfile)
        assertEquals(1000L, controller.state.scoreHistory[1].completedAtMillis)
        assertEquals(controller.state.scoreHistory, persistedHistory)
    }

    @Test
    fun scoreHistoryCodecSkipsInvalidRowsAndSortsDeterministically() {
        val rawHistory = listOf(
            "20|Eco|100|1",
            "broken",
            "40|Dynamic|90|2",
            "-1|Comfort|120|3",
            "40|Comfort|110|4",
            "10|Missing|130|5",
        ).joinToString("\n")

        val history = decodeScoreHistory(rawHistory)

        assertEquals(3, history.size)
        assertEquals(40, history[0].score)
        assertEquals(SpeedProfile.Comfort, history[0].speedProfile)
        assertEquals(40, history[1].score)
        assertEquals(SpeedProfile.Dynamic, history[1].speedProfile)
        assertEquals(20, history[2].score)
        assertEquals(SpeedProfile.Eco, history[2].speedProfile)
        assertEquals(history, decodeScoreHistory(encodeScoreHistory(history)))
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
}
