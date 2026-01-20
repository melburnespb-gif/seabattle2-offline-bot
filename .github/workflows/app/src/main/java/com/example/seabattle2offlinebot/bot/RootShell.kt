package com.example.seabattle2offlinebot.bot

import java.io.BufferedReader
import java.io.InputStreamReader

object RootShell {
    fun exec(cmd: String): String {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
        val err = BufferedReader(InputStreamReader(p.errorStream)).readText()
        p.waitFor()
        return (out + "\n" + err).trim()
    }
}
