package com.meneer.wristcharge

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives battery reports from the watch app via the Wearable Data Layer. */
class WatchDataService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val readings = events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == Protocol.PATH_BATTERY }
            .map {
                val m = DataMapItem.fromDataItem(it.dataItem).dataMap
                Reading(
                    level = m.getInt(Protocol.KEY_LEVEL),
                    charging = m.getBoolean(Protocol.KEY_CHARGING),
                    full = m.getBoolean(Protocol.KEY_FULL),
                    timeMillis = m.getLong(Protocol.KEY_TIME),
                )
            }
        readings.sortedBy { it.timeMillis }.forEach { handle(it) }
    }

    private fun handle(r: Reading) = synchronized(LOCK) {
        val store = Store(this)
        val result = AlertEngine.evaluate(store.state, r, store.settings)
        if (result.state == store.state) return
        store.state = result.state
        store.reading = r
        Notifier.clearStale(this, r)
        result.alerts.forEach { Notifier.postAlert(this, it) }
        Notifier.updateStatus(this, store)
    }

    companion object { private val LOCK = Any() }
}
