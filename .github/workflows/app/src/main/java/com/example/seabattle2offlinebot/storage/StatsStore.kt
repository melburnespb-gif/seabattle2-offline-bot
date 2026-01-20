package com.example.seabattle2offlinebot.storage

import android.content.Context
import kotlin.math.roundToInt

data class Stats(
    val games: Int,
    val wins: Int,
    val losses: Int,
    val shotsTotal: Int
) {
    fun winratePercent(): Int =
        if (games == 0) 0 else ((wins * 100.0 / games).roundToInt())

    fun avgShots(): Double =
        if (games == 0) 0.0 else (shotsTotal.toDouble() / games.toDouble())
}

class StatsStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("stats", Context.MODE_PRIVATE)

    fun load(): Stats = Stats(
        games = prefs.getInt("games", 0),
        wins = prefs.getInt("wins", 0),
        losses = prefs.getInt("losses", 0),
        shotsTotal = prefs.getInt("shotsTotal", 0)
    )

    private fun save(s: Stats) {
        prefs.edit()
            .putInt("games", s.games)
            .putInt("wins", s.wins)
            .putInt("losses", s.losses)
            .putInt("shotsTotal", s.shotsTotal)
            .apply()
    }

    fun registerGame(win: Boolean, shots: Int) {
        val s = load()
        val ns = s.copy(
            games = s.games + 1,
            wins = s.wins + if (win) 1 else 0,
            losses = s.losses + if (win) 0 else 1,
            shotsTotal = s.shotsTotal + shots
        )
        save(ns)
    }
}
