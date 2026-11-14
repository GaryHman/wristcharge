package com.meneer.wristcharge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import java.util.Date

object Notifier {
    private const val CH_ALERTS = "alerts"
    private const val CH_STATUS = "status"
    private const val ID_STATUS = 1
    private const val ID_LOW = 10
    private const val ID_TARGET = 11
    private const val ID_FULL = 12

    private fun nm(c: Context) = c.getSystemService(NotificationManager::class.java)

    private fun ensureChannels(c: Context) {
        nm(c).createNotificationChannels(listOf(
            NotificationChannel(CH_ALERTS, "Battery alerts", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CH_STATUS, "Watch battery status", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        ))
    }

    private fun openApp(c: Context): PendingIntent = PendingIntent.getActivity(
        c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun describe(r: Reading): String {
        val state = when {
            r.charging && (r.full || r.level >= 100) -> "Fully charged"
            r.charging -> "Charging"
            else -> "On battery"
        }
        return "${r.level}% · $state"
    }

    fun time(c: Context, millis: Long): String = DateFormat.getTimeFormat(c).format(Date(millis))

    fun postAlert(c: Context, alert: Alert) {
        ensureChannels(c)
        val (id, title, text) = when (alert) {
            is Alert.Low -> Triple(ID_LOW, "Watch battery low", "Your watch is at ${alert.level}%. Time to charge it.")
            is Alert.TargetReached -> Triple(ID_TARGET, "Watch charged to ${alert.level}%",
                "Your ${alert.target}% target is reached.")
            Alert.Full -> Triple(ID_FULL, "Watch fully charged", "Your watch is at 100%.")
        }
        val n = Notification.Builder(c, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_battery)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp(c))
            .setAutoCancel(true)
            .build()
        nm(c).notify(id, n)
    }

    /** Clears alerts that no longer apply (e.g. "low" once the watch is charging). */
    fun clearStale(c: Context, r: Reading) {
        if (r.charging) nm(c).cancel(ID_LOW) else { nm(c).cancel(ID_TARGET); nm(c).cancel(ID_FULL) }
    }

    /** Shows or removes the ongoing status notification. Not a foreground service. */
    fun updateStatus(c: Context, store: Store) {
        if (!store.persistentEnabled) { nm(c).cancel(ID_STATUS); return }
        ensureChannels(c)
        val r = store.reading
        val b = Notification.Builder(c, CH_STATUS)
            .setSmallIcon(R.drawable.ic_battery)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(openApp(c))
        if (r == null) {
            b.setContentTitle("Watch: waiting for data")
                .setContentText("Open WristCharge on your watch once.")
        } else {
            b.setContentTitle("Watch ${describe(r)}")
                .setContentText("Updated ${time(c, r.timeMillis)}")
                .setWhen(r.timeMillis).setShowWhen(true)
        }
        nm(c).notify(ID_STATUS, b.build())
    }
}
