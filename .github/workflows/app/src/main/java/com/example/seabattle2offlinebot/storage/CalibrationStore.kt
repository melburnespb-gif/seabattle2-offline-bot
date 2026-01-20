package com.example.seabattle2offlinebot.storage

import android.content.Context

class CalibrationStore(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("calibration", Context.MODE_PRIVATE)

    fun putPoint(key: String, x: Int, y: Int) {
        prefs.edit().putString(key, "$x,$y").apply()
    }

    fun getPoint(key: String): Pair<Int, Int>? {
        val s = prefs.getString(key, null) ?: return null
        val parts = s.split(",")
        if (parts.size != 2) return null
        return parts[0].toInt() to parts[1].toInt()
    }

    fun hasAll(required: List<String>): Boolean = required.all { prefs.contains(it) }
}
