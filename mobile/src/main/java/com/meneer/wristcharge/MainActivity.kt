package com.meneer.wristcharge

import android.Manifest
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.wearable.Wearable

/** Single settings screen, built in code to stay dependency-free. */
class MainActivity : Activity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var store: Store
    private lateinit var status: TextView
    private lateinit var updated: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        val pad = dp(16)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(col) }
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            v.setPadding(pad + insets.systemWindowInsetLeft, pad + insets.systemWindowInsetTop,
                pad + insets.systemWindowInsetRight, pad + insets.systemWindowInsetBottom)
            insets
        }

        status = TextView(this).apply { textSize = 28f; setTypeface(typeface, Typeface.BOLD) }
        updated = TextView(this)
        col.addView(status); col.addView(updated)
        col.addView(Button(this).apply {
            text = "Refresh now"
            setOnClickListener { requestRefresh() }
        })

        val s = store.settings
        header(col, "Low battery")
        val lowSwitch = switch(col, "", s.lowEnabled) { v -> store.settings = store.settings.copy(lowEnabled = v) }
        slider(col, 5, 50, s.lowThreshold, { lowSwitch.text = "Alert at $it% or lower" }) {
            store.settings = store.settings.copy(lowThreshold = it)
        }

        header(col, "While charging")
        val aSwitch = switch(col, "", s.levelAEnabled) { v -> store.settings = store.settings.copy(levelAEnabled = v) }
        slider(col, 50, 99, s.levelA, { aSwitch.text = "Alert at $it%" }) { store.settings = store.settings.copy(levelA = it) }
        val bSwitch = switch(col, "", s.levelBEnabled) { v -> store.settings = store.settings.copy(levelBEnabled = v) }
        slider(col, 50, 99, s.levelB, { bSwitch.text = "Alert at $it%" }) { store.settings = store.settings.copy(levelB = it) }
        switch(col, "Alert when fully charged", s.fullEnabled) { v -> store.settings = store.settings.copy(fullEnabled = v) }

        header(col, "Status notification")
        switch(col, "Always show watch battery in notifications", store.persistentEnabled) { v ->
            store.persistentEnabled = v
            Notifier.updateStatus(this, store)
        }

        col.addView(TextView(this).apply {
            text = "\nInstall WristCharge on your watch too and open it there once. " +
                "The watch reports about every 15 minutes, and every 1–2 minutes while charging.\n\n" +
                "No ads. No tracking. No internet access."
            alpha = 0.7f
        })
        setContentView(scroll)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() {
        super.onResume()
        store.prefs.registerOnSharedPreferenceChangeListener(this)
        render()
    }

    override fun onPause() {
        store.prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) {
        if (key?.startsWith("r_") == true) runOnUiThread { render() }
    }

    private fun render() {
        val r = store.reading
        if (r == null) {
            status.text = "Watch: no data yet"
            updated.text = "Open WristCharge on your watch once."
        } else {
            status.text = "Watch ${Notifier.describe(r)}"
            val ago = DateUtils.getRelativeTimeSpanString(r.timeMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            updated.text = "Last updated ${Notifier.time(this, r.timeMillis)} ($ago)"
        }
    }

    private fun requestRefresh() {
        Wearable.getNodeClient(this).connectedNodes.addOnCompleteListener { task ->
            val nodes = if (task.isSuccessful) task.result.orEmpty() else emptyList()
            if (nodes.isEmpty()) {
                toast("No watch connected"); return@addOnCompleteListener
            }
            nodes.forEach { Wearable.getMessageClient(this).sendMessage(it.id, Protocol.PATH_REFRESH, null) }
            toast("Asked your watch for an update")
        }
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun header(col: LinearLayout, t: String) = col.addView(TextView(this).apply {
        text = t; textSize = 18f; setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(24), 0, dp(4))
    })

    private fun switch(col: LinearLayout, t: String, on: Boolean, changed: (Boolean) -> Unit): Switch =
        Switch(this).apply {
            text = t; isChecked = on; setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, v -> changed(v) }
            col.addView(this)
        }

    private fun slider(col: LinearLayout, min: Int, max: Int, value: Int, label: (Int) -> Unit, save: (Int) -> Unit) {
        label(value)
        col.addView(SeekBar(this).apply {
            this.min = min; this.max = max; progress = value
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) = label(p)
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) = save(sb.progress)
            })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
}
