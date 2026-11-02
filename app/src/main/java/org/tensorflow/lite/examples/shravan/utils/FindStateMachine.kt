package org.tensorflow.lite.examples.shravan.utils

enum class FindState { SEARCHING, FOUND, TRACKING, CENTERED, LOST }
enum class FindEvent { NONE, FOUND, DIRECTION_CHANGED, CENTERED, LOST }
enum class FindDirection { LEFT, CENTER, RIGHT }

data class FindObservation(
    val classId: Int,
    val confidence: Float,
    val centerX: Float,
    val areaFraction: Float
)

data class FindFrameResult(
    val state: FindState,
    val event: FindEvent = FindEvent.NONE,
    val direction: FindDirection? = null,
    val observation: FindObservation? = null,
    val hapticIntervalMs: Long? = null
)

class FindStateMachine {
    var state: FindState = FindState.SEARCHING
        private set

    private var candidateFrames = 0
    private var lastSeenAtMs: Long? = null
    private var centeredSinceMs: Long? = null
    private var lastAnnouncedDirection: FindDirection? = null
    private var lastDirectionPromptAtMs = 0L

    fun reset() {
        state = FindState.SEARCHING
        candidateFrames = 0
        lastSeenAtMs = null
        centeredSinceMs = null
        lastAnnouncedDirection = null
        lastDirectionPromptAtMs = 0L
    }

    fun update(
        observations: List<FindObservation>,
        targetClassId: Int,
        nowMs: Long
    ): FindFrameResult {
        val observation = observations
            .asSequence()
            .filter {
                it.classId == targetClassId &&
                    it.confidence >= FeedbackConfig.FIND_MIN_CONFIDENCE
            }
            .maxByOrNull { it.confidence }

        if (observation != null &&
            (state == FindState.FOUND ||
                state == FindState.TRACKING ||
                state == FindState.CENTERED)
        ) {
            val lastSeen = lastSeenAtMs
            if (lastSeen != null && nowMs - lastSeen > FeedbackConfig.FIND_LOST_MS) {
                state = FindState.LOST
                candidateFrames = 0
                lastSeenAtMs = null
                centeredSinceMs = null
                return FindFrameResult(state, FindEvent.LOST)
            }
        }

        if (observation == null) {
            candidateFrames = 0
            centeredSinceMs = null

            if (state == FindState.FOUND ||
                state == FindState.TRACKING ||
                state == FindState.CENTERED
            ) {
                val lastSeen = lastSeenAtMs
                if (lastSeen != null && nowMs - lastSeen > FeedbackConfig.FIND_LOST_MS) {
                    state = FindState.LOST
                    lastSeenAtMs = null
                    return FindFrameResult(state, FindEvent.LOST)
                }
            } else if (state == FindState.LOST) {
                state = FindState.SEARCHING
            }

            return FindFrameResult(state)
        }

        if (state == FindState.LOST) {
            state = FindState.SEARCHING
            candidateFrames = 0
        }

        if (state == FindState.SEARCHING) {
            candidateFrames++
            if (candidateFrames < FeedbackConfig.FIND_CONFIRM_FRAMES) {
                return FindFrameResult(state)
            }
        }

        lastSeenAtMs = nowMs
        val direction = when {
            observation.centerX < FeedbackConfig.FIND_LEFT_MAX_X -> FindDirection.LEFT
            observation.centerX > FeedbackConfig.FIND_RIGHT_MIN_X -> FindDirection.RIGHT
            else -> FindDirection.CENTER
        }
        val interval = (
            FeedbackConfig.HAPTIC_INTERVAL_FAR_MS -
                FeedbackConfig.FIND_HAPTIC_AREA_SCALE_MS * observation.areaFraction.coerceIn(0f, 1f)
            ).toLong().coerceIn(
                FeedbackConfig.HAPTIC_INTERVAL_NEAR_MS,
                FeedbackConfig.HAPTIC_INTERVAL_FAR_MS
            )

        if (state == FindState.SEARCHING) {
            state = FindState.FOUND
            centeredSinceMs = if (direction == FindDirection.CENTER) nowMs else null
            lastAnnouncedDirection = direction
            lastDirectionPromptAtMs = nowMs
            return FindFrameResult(state, FindEvent.FOUND, direction, observation, interval)
        }

        if (direction == FindDirection.CENTER) {
            if (centeredSinceMs == null) centeredSinceMs = nowMs
            if (nowMs - centeredSinceMs!! >= FeedbackConfig.FIND_CENTERED_MS) {
                if (state != FindState.CENTERED) {
                    state = FindState.CENTERED
                    lastAnnouncedDirection = direction
                    lastDirectionPromptAtMs = nowMs
                    return FindFrameResult(state, FindEvent.CENTERED, direction, observation, interval)
                }
                state = FindState.CENTERED
            } else {
                state = FindState.TRACKING
            }
        } else {
            centeredSinceMs = null
            state = FindState.TRACKING
        }

        val directionChanged = direction != lastAnnouncedDirection
        val cooldownElapsed =
            nowMs - lastDirectionPromptAtMs >= FeedbackConfig.FIND_DIRECTION_COOLDOWN_MS

        if (directionChanged && cooldownElapsed) {
            lastAnnouncedDirection = direction
            lastDirectionPromptAtMs = nowMs
            return FindFrameResult(
                state, FindEvent.DIRECTION_CHANGED, direction, observation, interval
            )
        }

        return FindFrameResult(state, observation = observation, hapticIntervalMs = interval)
    }
}