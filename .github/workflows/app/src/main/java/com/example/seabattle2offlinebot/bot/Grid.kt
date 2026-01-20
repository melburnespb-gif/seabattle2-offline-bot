package com.example.seabattle2offlinebot.bot

data class FieldRect(val tl: Pair<Int, Int>, val br: Pair<Int, Int>) {
    fun width() = br.first - tl.first
    fun height() = br.second - tl.second
}

object Grid {
    data class Cell(val gx: Int, val gy: Int, val sx: Int, val sy: Int)

    fun build(field: FieldRect): List<Cell> {
        val cells = ArrayList<Cell>(100)
        val w = field.width().toDouble() / 10.0
        val h = field.height().toDouble() / 10.0
        for (gy in 0 until 10) for (gx in 0 until 10) {
            val sx = (field.tl.first + gx * w + w / 2.0).toInt()
            val sy = (field.tl.second + gy * h + h / 2.0).toInt()
            cells.add(Cell(gx, gy, sx, sy))
        }
        return cells
    }
}
