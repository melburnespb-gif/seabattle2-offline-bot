package com.example.seabattle2offlinebot.bot

import kotlin.random.Random

object Strategy {
    const val TOTAL_HITS_TO_WIN = 20

    data class Move(val x: Int, val y: Int)

    fun nextMove(board: Array<Array<CellState>>): Move? {
        // Target-mode: если есть HIT — стреляем рядом
        for (y in 0 until 10) for (x in 0 until 10) {
            if (board[y][x] == CellState.HIT) {
                val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1).shuffled()
                for ((dx, dy) in dirs) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0..9 && ny in 0..9 && board[ny][nx] == CellState.UNKNOWN) {
                        return Move(nx, ny)
                    }
                }
            }
        }

        // Hunt-mode: шахматка
        val candidates = ArrayList<Move>()
        for (y in 0 until 10) for (x in 0 until 10) {
            if ((x + y) % 2 == 0 && board[y][x] == CellState.UNKNOWN) {
                candidates.add(Move(x, y))
            }
        }
        if (candidates.isNotEmpty()) return candidates[Random.nextInt(candidates.size)]

        // fallback: любая неизвестная
        val any = ArrayList<Move>()
        for (y in 0 until 10) for (x in 0 until 10) {
            if (board[y][x] == CellState.UNKNOWN) any.add(Move(x, y))
        }
        return if (any.isEmpty()) null else any[Random.nextInt(any.size)]
    }
}
