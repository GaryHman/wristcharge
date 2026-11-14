package com.meneer.wristcharge

/** Data Layer contract. Keep identical in the mobile and wear modules. */
object Protocol {
    const val PATH_BATTERY = "/wristcharge/battery"
    const val PATH_REFRESH = "/wristcharge/refresh"
    const val KEY_LEVEL = "level"
    const val KEY_CHARGING = "charging"
    const val KEY_FULL = "full"
    const val KEY_TIME = "time"
}
