package com.meneer.wristcharge

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/** Handles "Refresh now" from the phone. Runs on a background thread. */
class PhoneMessageService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == Protocol.PATH_REFRESH) Battery.sendNow(applicationContext)
    }
}
