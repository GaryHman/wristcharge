package com.meneer.wristcharge

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit

data class WatchBattery(val level: Int, val charging: Boolean, val full: Boolean)

object Battery {
    fun read(c: Context): WatchBattery {
        val i: Intent? = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        return WatchBattery(
            level = pct,
            charging = plugged || status == BatteryManager.BATTERY_STATUS_CHARGING,
            full = status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }

    /** Reads the battery and pushes it to the phone. Blocking: call off the main thread. */
    fun sendNow(c: Context): WatchBattery {
        val b = read(c)
        val now = System.currentTimeMillis()
        val req = PutDataMapRequest.create(Protocol.PATH_BATTERY).apply {
            dataMap.putInt(Protocol.KEY_LEVEL, b.level)
            dataMap.putBoolean(Protocol.KEY_CHARGING, b.charging)
            dataMap.putBoolean(Protocol.KEY_FULL, b.full)
            dataMap.putLong(Protocol.KEY_TIME, now)   // always changes, so every report is delivered
        }.asPutDataRequest().setUrgent()
        try {
            Tasks.await(Wearable.getDataClient(c).putDataItem(req), 30, TimeUnit.SECONDS)
            c.getSharedPreferences("wear", Context.MODE_PRIVATE).edit().putLong("lastSent", now).apply()
        } catch (_: Exception) {
            // Phone out of range: the Data Layer syncs the item once it reconnects.
        }
        Scheduler.reschedule(c, b)
        return b
    }
}

/**
 * Plain JobScheduler, no extra libraries:
 *  - every 15 min (the platform minimum) while on battery,
 *  - a job that fires as soon as the charger is connected,
 *  - every ~2 min while charging (every ~10 min once full).
 */
object Scheduler {
    private const val JOB_PERIODIC = 1
    private const val JOB_ON_CHARGER = 2
    private const val JOB_FAST = 3

    private fun js(c: Context) = c.getSystemService(JobScheduler::class.java)
    private fun job(c: Context, id: Int) = JobInfo.Builder(id, ComponentName(c, SyncJobService::class.java)).setPersisted(true)

    fun ensurePeriodic(c: Context) {
        if (js(c).getPendingJob(JOB_PERIODIC) == null) {
            js(c).schedule(job(c, JOB_PERIODIC).setPeriodic(TimeUnit.MINUTES.toMillis(15)).build())
        }
    }

    fun reschedule(c: Context, b: WatchBattery) {
        ensurePeriodic(c)
        if (b.charging) {
            js(c).cancel(JOB_ON_CHARGER)
            val delay = if (b.full || b.level >= 100) 10L else 2L
            js(c).schedule(
                job(c, JOB_FAST)
                    .setMinimumLatency(TimeUnit.MINUTES.toMillis(delay) - 30_000)
                    .setOverrideDeadline(TimeUnit.MINUTES.toMillis(delay) + 30_000)
                    .build()
            )
        } else {
            js(c).cancel(JOB_FAST)
            js(c).schedule(job(c, JOB_ON_CHARGER).setRequiresCharging(true).build())
        }
    }
}
