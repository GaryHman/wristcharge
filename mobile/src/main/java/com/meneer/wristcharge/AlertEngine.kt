package com.meneer.wristcharge

import kotlin.math.min

/** One battery report from the watch. */
data class Reading(
    val level: Int,
    val charging: Boolean,
    val full: Boolean,
    val timeMillis: Long,
)

data class AlertSettings(
    val lowEnabled: Boolean = true,
    val lowThreshold: Int = 20,
    val levelAEnabled: Boolean = true,
    val levelA: Int = 80,
    val levelBEnabled: Boolean = false,
    val levelB: Int = 90,
    val fullEnabled: Boolean = true,
) {
    /** Enabled "charged to X%" targets below 100 (100 is handled by [fullEnabled]). */
    fun chargeTargets(): Set<Int> = buildSet {
        if (levelAEnabled && levelA < 100) add(levelA)
        if (levelBEnabled && levelB < 100) add(levelB)
    }
}

/** What has already been announced, so we never alert twice for the same event. */
data class AlertState(
    val lowFired: Boolean = false,
    val firedTargets: Set<Int> = emptySet(),
    val fullFired: Boolean = false,
    val lastLevel: Int? = null,
    val lastCharging: Boolean? = null,
    val lastTimeMillis: Long = 0L,
)

sealed class Alert {
    data class Low(val level: Int) : Alert()
    data class TargetReached(val target: Int, val level: Int) : Alert()
    data object Full : Alert()
}

data class Evaluation(val alerts: List<Alert>, val state: AlertState)

/**
 * Pure decision logic.
 *
 * - Low alert fires once when the watch is on battery and at/below the threshold.
 *   It re-arms when the watch starts charging (or the level is above the threshold again).
 * - Charge targets fire once per charging session when the level crosses them.
 *   If one report crosses several targets, only one notification (the highest) is shown.
 * - When a charging session starts, targets the watch was already at or above are
 *   treated as done, so re-plugging a nearly full watch does not repeat alerts.
 * - Everything re-arms when the watch comes off the charger.
 * - Reports older than the last one seen are ignored.
 */
object AlertEngine {
    fun evaluate(state: AlertState, r: Reading, s: AlertSettings): Evaluation {
        if (r.timeMillis <= state.lastTimeMillis) return Evaluation(emptyList(), state)

        val alerts = mutableListOf<Alert>()
        var lowFired = state.lowFired
        var fired = state.firedTargets
        var fullFired = state.fullFired
        val isFull = r.full || r.level >= 100

        if (!r.charging) {
            fired = emptySet()
            fullFired = false
        } else if (state.lastCharging != true) {
            // New charging session: only alert for targets crossed from here on.
            val base = min(state.lastLevel ?: r.level, r.level)
            fired = s.chargeTargets().filter { it <= base }.toSet()
            fullFired = base >= 100
        }

        // Low battery
        if (r.charging || r.level > s.lowThreshold) {
            lowFired = false
        } else if (s.lowEnabled && !lowFired) {
            alerts += Alert.Low(r.level)
            lowFired = true
        }

        // Charging targets
        if (r.charging) {
            val crossed = s.chargeTargets().filter { r.level >= it && it !in fired }
            val fullNow = s.fullEnabled && isFull && !fullFired
            if (fullNow) {
                alerts += Alert.Full
                fullFired = true
            } else if (crossed.isNotEmpty()) {
                alerts += Alert.TargetReached(crossed.max(), r.level)
            }
            fired = fired + crossed
        }

        return Evaluation(
            alerts,
            AlertState(lowFired, fired, fullFired, r.level, r.charging, r.timeMillis),
        )
    }
}
