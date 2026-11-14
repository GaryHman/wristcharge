package com.meneer.wristcharge

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Date

/** Tiny status screen. Opening it once also starts the background reporting. */
class MainActivity : Activity() {
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info = TextView(this).apply { gravity = Gravity.CENTER; textSize = 16f }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val p = (resources.displayMetrics.widthPixels * 0.12).toInt()
            setPadding(p, p, p, p)
            addView(info)
            addView(Button(context).apply {
                text = "Send now"
                setOnClickListener { send() }
            })
        }
        setContentView(col)
        send()
    }

    private fun send() {
        info.text = "Sending…"
        Thread {
            val b = Battery.sendNow(applicationContext)
            val last = getSharedPreferences("wear", Context.MODE_PRIVATE).getLong("lastSent", 0L)
            val sent = if (last > 0) "Sent to phone " + DateFormat.getTimeFormat(this).format(Date(last)) else "Phone not reachable"
            val state = when { b.charging && (b.full || b.level >= 100) -> "Fully charged"; b.charging -> "Charging"; else -> "On battery" }
            runOnUiThread { info.text = "WristCharge\n${b.level}% · $state\n$sent" }
        }.start()
    }
}
