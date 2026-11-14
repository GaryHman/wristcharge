package com.meneer.wristcharge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {
    private var t = 0L
    private var state = AlertState()
    private var settings = AlertSettings(levelAEnabled = true, levelA = 80, levelBEnabled = true, levelB = 90, fullEnabled = true)

    private fun feed(level: Int, charging: Boolean, full: Boolean = false): List<Alert> {
        val e = AlertEngine.evaluate(state, Reading(level, charging, full, ++t), settings)
        state = e.state
        return e.alerts
    }

    @Test fun lowFiresOnceThenStaysQuiet() {
        assertTrue(feed(40, false).isEmpty())
        assertEquals(listOf(Alert.Low(20)), feed(20, false))
        assertTrue(feed(19, false).isEmpty())
        assertTrue(feed(12, false).isEmpty())
    }

    @Test fun lowReArmsAfterCharging() {
        feed(30, false)
        assertEquals(listOf(Alert.Low(18)), feed(18, false))
        assertTrue(feed(19, true).isEmpty())            // put on charger
        assertTrue(feed(25, false).isEmpty())           // taken off above threshold
        assertEquals(listOf(Alert.Low(20)), feed(20, false))
    }

    @Test fun lowRespectsDisabledSetting() {
        settings = settings.copy(lowEnabled = false)
        feed(50, false)
        assertTrue(feed(5, false).isEmpty())
    }

    @Test fun chargeTargetsFireOncePerSession() {
        feed(30, false)
        assertTrue(feed(35, true).isEmpty())
        assertEquals(listOf(Alert.TargetReached(80, 81)), feed(81, true))
        assertTrue(feed(84, true).isEmpty())
        assertEquals(listOf(Alert.TargetReached(90, 90)), feed(90, true))
        assertTrue(feed(95, true).isEmpty())
        assertEquals(listOf(Alert.Full), feed(100, true))
        assertTrue(feed(100, true).isEmpty())
        assertTrue(feed(100, true, full = true).isEmpty())
    }

    @Test fun jumpOverSeveralTargetsGivesOneAlert() {
        feed(50, false)
        feed(52, true)
        assertEquals(listOf(Alert.TargetReached(90, 92)), feed(92, true))
        assertTrue(feed(95, true).isEmpty())
    }

    @Test fun fullBeatsTargetsInSameReport() {
        feed(50, false)
        feed(55, true)
        assertEquals(listOf(Alert.Full), feed(100, true))
        assertTrue(feed(100, true).isEmpty())
    }

    @Test fun reArmsAfterUnplugForNextSession() {
        feed(40, false); feed(45, true)
        assertEquals(1, feed(82, true).size)
        feed(70, false)                                 // unplugged, used, drained
        feed(72, true)                                  // new session
        assertEquals(listOf(Alert.TargetReached(80, 80)), feed(80, true))
    }

    @Test fun replugWhenAlreadyAboveTargetsIsSilent() {
        feed(40, false); feed(45, true)
        feed(91, true)
        feed(91, false)                                 // briefly off the charger
        assertTrue(feed(91, true).isEmpty())            // back on: no repeat of 80/90
        assertTrue(feed(93, true).isEmpty())
        assertEquals(listOf(Alert.Full), feed(100, true))
    }

    @Test fun firstEverReadingWhileChargingDoesNotAlertForPassedTargets() {
        assertTrue(feed(85, true).isEmpty())
        assertEquals(listOf(Alert.TargetReached(90, 90)), feed(90, true))
    }

    @Test fun missedSessionStartStillAlertsForCrossedTarget() {
        feed(75, false)
        assertEquals(listOf(Alert.TargetReached(80, 82)), feed(82, true))
    }

    @Test fun fullStatusFlagCountsAsFull() {
        feed(90, false); feed(91, true)
        assertEquals(listOf(Alert.Full), feed(99, true, full = true))
    }

    @Test fun disabledTargetsDoNotFire() {
        settings = settings.copy(levelAEnabled = false, levelBEnabled = false, fullEnabled = false)
        feed(10, false); feed(10, true)
        assertTrue(feed(85, true).isEmpty())
        assertTrue(feed(100, true).isEmpty())
    }

    @Test fun staleOrDuplicateReportsAreIgnored() {
        feed(30, false)
        val before = state
        val e = AlertEngine.evaluate(state, Reading(10, false, false, t), settings)  // same timestamp
        assertTrue(e.alerts.isEmpty())
        assertEquals(before, e.state)
        val old = AlertEngine.evaluate(state, Reading(10, false, false, t - 1), settings)
        assertTrue(old.alerts.isEmpty())
    }
}
