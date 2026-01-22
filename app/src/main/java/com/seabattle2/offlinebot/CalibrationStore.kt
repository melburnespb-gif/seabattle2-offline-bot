package com.seabattle2.offlinebot

import android.content.Context
import android.content.SharedPreferences

class CalibrationStore(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.getSharedPreferences("calibration", Context.MODE_PRIVATE)

    fun savePoint(key: String, x: Int, y: Int) {
        sp.edit()
            .putInt("${key}_x", x)
            .putInt("${key}_y", y)
            .apply()
    }

    fun getPoint(key: String): Pair<Int, Int>? {
        if (!sp.contains("${key}_x") || !sp.contains("${key}_y")) return null
        return sp.getInt("${key}_x", 0) to sp.getInt("${key}_y", 0)
    }

    fun hasPoint(key: String): Boolean =
        sp.contains("${key}_x") && sp.contains("${key}_y")
}
