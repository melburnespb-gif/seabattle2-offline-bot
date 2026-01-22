package com.seabattle2.offlinebot

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

enum class CellState { UNKNOWN, MISS, HIT }

object Vision {
    data class Rect(val l: Int, val t: Int, val r: Int, val b: Int)

    private fun clamp(v: Int, lo: Int, hi: Int): Int = max(lo, min(hi, v))

    fun whitenessRatio(bm: Bitmap, rect: Rect): Double {
        val l = clamp(rect.l, 0, bm.width - 1)
        val r = clamp(rect.r, 0, bm.width - 1)
        val t = clamp(rect.t, 0, bm.height - 1)
        val b = clamp(rect.b, 0, bm.height - 1)
        if (r <= l || b <= t) return 0.0

        var white = 0
        var total = 0
        val step = 6
        for (y in t until b step step) for (x in l until r step step) {
            val c = bm.getPixel(x, y)
            val rr = (c shr 16) and 0xFF
            val gg = (c shr 8) and 0xFF
            val bb = (c) and 0xFF
            if (rr > 220 && gg > 220 && bb > 220) white++
            total++
        }
        return if (total == 0) 0.0 else white.toDouble() / total.toDouble()
    }

    fun cellState(bm: Bitmap, cx: Int, cy: Int, radius: Int = 18): CellState {
        val l = clamp(cx - radius, 0, bm.width - 1)
        val r = clamp(cx + radius, 0, bm.width - 1)
        val t = clamp(cy - radius, 0, bm.height - 1)
        val b = clamp(cy + radius, 0, bm.height - 1)
        if (r <= l || b <= t) return CellState.UNKNOWN

        var red = 0
        var blue = 0
        var light = 0
        var total = 0
        val step = 2

        for (y in t until b step step) for (x in l until r step step) {
            val c = bm.getPixel(x, y)
            val rr = (c shr 16) and 0xFF
            val gg = (c shr 8) and 0xFF
            val bb = (c) and 0xFF

            // HIT (красный крест/метка)
            if (rr > 165 && gg < 140 && bb < 140) red++

            // MISS (синяя штриховка/квадрат)
            if (bb > 140 && rr < 120) blue++

            // белая клетка (не стреляли)
            if (rr > 230 && gg > 230 && bb > 230) light++

            total++
        }

        val redRatio = red.toDouble() / total
        val blueRatio = blue.toDouble() / total
        val lightRatio = light.toDouble() / total

        return when {
            redRatio > 0.06 -> CellState.HIT
            blueRatio > 0.10 && redRatio < 0.02 -> CellState.MISS
            lightRatio > 0.55 -> CellState.UNKNOWN
            else -> CellState.UNKNOWN
        }
    }
}
