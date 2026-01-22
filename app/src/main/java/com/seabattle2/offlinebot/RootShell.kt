package com.seabattle2.offlinebot

import java.io.DataOutputStream

object RootShell {
    fun exec(cmd: String): Boolean = try {
        val p = Runtime.getRuntime().exec("su")
        DataOutputStream(p.outputStream).use { os ->
            os.writeBytes("$cmd\n")
            os.writeBytes("exit\n")
            os.flush()
        }
        p.waitFor() == 0
    } catch (_: Throwable) { false }

    fun tap(x: Int, y: Int): Boolean = exec("input tap $x $y")

    fun screencap(path: String): Boolean = exec("screencap -p $path")
}
