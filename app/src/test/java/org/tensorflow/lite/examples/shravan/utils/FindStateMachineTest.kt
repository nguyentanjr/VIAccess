package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FindStateMachineTest {
    private fun cup(centerX: Float = 0.5f) =
        FindObservation(classId = 41, confidence = 0.8f, centerX = centerX, areaFraction = 0.2f)

    @Test
    fun requiresThreeConsecutiveTargetFrames() {
        val machine = FindStateMachine()

        assertEquals(FindEvent.NONE, machine.update(listOf(cup()), 41, 0L).event)
        assertEquals(FindEvent.NONE, machine.update(listOf(cup()), 41, 100L).event)
        assertEquals(FindEvent.FOUND, machine.update(listOf(cup()), 41, 200L).event)
        assertEquals(FindState.FOUND, machine.state)
    }

    @Test
    fun aMissingFrameResetsTheConsecutiveConfirmationCount() {
        val machine = FindStateMachine()

        assertEquals(FindEvent.NONE, machine.update(listOf(cup()), 41, 0L).event)
        assertEquals(FindEvent.NONE, machine.update(listOf(cup()), 41, 100L).event)
        assertEquals(FindEvent.NONE, machine.update(emptyList(), 41, 200L).event)
        assertEquals(FindEvent.NONE, machine.update(listOf(cup()), 41, 300L).event)
        assertEquals(FindEvent.NONE, machine.update(listOf(cup()), 41, 400L).event)
        assertEquals(FindEvent.FOUND, machine.update(listOf(cup()), 41, 500L).event)
    }

    @Test
    fun ignoresWrongClassAndLowConfidence() {
        val machine = FindStateMachine()
        val wrongClass = cup().copy(classId = 39)
        val lowConfidence = cup().copy(confidence = 0.49f)

        repeat(3) { index ->
            val result = machine.update(listOf(wrongClass, lowConfidence), 41, index * 100L)
            assertEquals(FindState.SEARCHING, result.state)
        }
    }

    @Test
    fun reportsLostOnlyAfterConfiguredTimeout() {
        val machine = FindStateMachine()
        repeat(3) { machine.update(listOf(cup()), 41, it * 100L) }

        val lastSeen = 200L
        assertEquals(
            FindEvent.NONE,
            machine.update(emptyList(), 41, lastSeen + FeedbackConfig.FIND_LOST_MS).event
        )
        assertEquals(
            FindEvent.LOST,
            machine.update(emptyList(), 41, lastSeen + FeedbackConfig.FIND_LOST_MS + 1).event
        )
    }

    @Test
    fun reportsLostWhenTheNextDetectionArrivesAfterTimeout() {
        val machine = FindStateMachine()
        repeat(3) { machine.update(listOf(cup()), 41, it * 100L) }

        val result = machine.update(
            listOf(cup()),
            41,
            200L + FeedbackConfig.FIND_LOST_MS + 1
        )

        assertEquals(FindEvent.LOST, result.event)
        assertEquals(FindState.LOST, result.state)
    }

    @Test
    fun reportsCenteredAfterOneSecond() {
        val machine = FindStateMachine()
        repeat(3) { machine.update(listOf(cup()), 41, it * 100L) }

        val centeredAt = 200L
        val result = machine.update(
            listOf(cup()), 41, centeredAt + FeedbackConfig.FIND_CENTERED_MS
        )
        assertEquals(FindEvent.CENTERED, result.event)
    }

    @Test
    fun computesDirectionAndClampedDistanceIntervals() {
        val machine = FindStateMachine()

        repeat(3) {
            machine.update(listOf(cup(centerX = 0.2f)), 41, it * 100L)
        }
        val found = machine.update(listOf(cup(centerX = 0.2f)), 41, 300L)
        assertEquals(FindDirection.LEFT, found.direction)
        assertEquals(850L, found.hapticIntervalMs)

        val close = FindStateMachine()
        repeat(3) {
            close.update(
                listOf(cup().copy(areaFraction = 1f)),
                41,
                it * 100L
            )
        }
        assertEquals(
            FeedbackConfig.HAPTIC_INTERVAL_NEAR_MS,
            close.update(listOf(cup().copy(areaFraction = 1f)), 41, 300L).hapticIntervalMs
        )
    }

    @Test
    fun usesConfiguredBoundariesForCenterAndRightDirections() {
        val center = FindStateMachine()
        repeat(3) {
            center.update(listOf(cup(centerX = FeedbackConfig.FIND_LEFT_MAX_X)), 41, it * 100L)
        }
        assertEquals(
            FindDirection.CENTER,
            center.update(
                listOf(cup(centerX = FeedbackConfig.FIND_LEFT_MAX_X)),
                41,
                300L
            ).direction
        )

        val right = FindStateMachine()
        repeat(3) {
            right.update(
                listOf(cup(centerX = FeedbackConfig.FIND_RIGHT_MIN_X + 0.01f)),
                41,
                it * 100L
            )
        }
        assertEquals(
            FindDirection.RIGHT,
            right.update(
                listOf(cup(centerX = FeedbackConfig.FIND_RIGHT_MIN_X + 0.01f)),
                41,
                300L
            ).direction
        )
    }

    @Test
    fun doesNotRepeatCenterDirectionAfterCenteredAnnouncement() {
        val machine = FindStateMachine()
        repeat(3) {
            machine.update(listOf(cup(centerX = 0.2f)), 41, it * 100L)
        }

        machine.update(listOf(cup()), 41, 300L)
        val centered = machine.update(
            listOf(cup()),
            41,
            300L + FeedbackConfig.FIND_CENTERED_MS
        )
        assertEquals(FindEvent.CENTERED, centered.event)
        assertEquals(
            FindEvent.NONE,
            machine.update(
                listOf(cup()),
                41,
                300L + FeedbackConfig.FIND_CENTERED_MS + 1
            ).event
        )
    }
}