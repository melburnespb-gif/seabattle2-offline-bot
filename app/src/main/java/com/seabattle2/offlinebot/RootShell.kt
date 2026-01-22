package com.seabattle2.offlinebot

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException

/**
 * Root helpers:
 * - exec("command")  -> executes via `su -c`
 * - tap(x,y)         -> input tap
 * - swipe(...)       -> optional
 *
 * Требует root (наличие `su`).
 */
object RootShell {

    private const val TAG = "RootShell"

    /**
     * Выполнить команду через root: su -c "<cmd>"
     */
    @Synchronized
    fun exec(cmd: String): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val code = p.waitFor()
            if (code != 0) {
                Log.w(TAG, "exec failed ($code): $cmd")
                // иногда полезно читать stderr, но на многих устройствах оно пустое
            }
            code == 0
        } catch (t: Throwable) {
            Log.e(TAG, "exec exception: $cmd", t)
            false
        }
    }

    /**
     * Тап по координатам (x,y) через root.
     */
    fun tap(x: Int, y: Int): Boolean {
        // input tap X Y
        return exec("input tap $x $y")
    }

    /**
     * Нажатие аппаратной/системной кнопки.
     * Примеры: KEYCODE_BACK, KEYCODE_HOME и т.д.
     */
    fun keyevent(keyCode: Int): Boolean {
        return exec("input keyevent $keyCode")
    }

    /**
     * Свайп (можно использовать для скролла/перетаскивания).
     */
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 300): Boolean {
        return exec("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }

    // ====== (опционально) режим "интерактивной сессии" через один su процесс ======
    // Если захочешь ускорить массовые команды (много тапов подряд), можно включить.
    // Сейчас твой бот и так ок, но это “на будущее”.

    private var sessionProcess: Process? = null
    private var sessionOut: DataOutputStream? = null

    /**
     * Открыть постоянную root-сессию (один su), чтобы слать много команд быстрее.
     */
    @Synchronized
    fun openSession(): Boolean {
        if (sessionProcess != null && sessionOut != null) return true
        return try {
            val p = Runtime.getRuntime().exec("su")
            val out = DataOutputStream(p.outputStream)
            sessionProcess = p
            sessionOut = out
            true
        } catch (e: IOException) {
            Log.e(TAG, "openSession failed", e)
            closeSession()
            false
        }
    }

    /**
     * Выполнить команду в постоянной сессии (если открыта), иначе fallback на exec().
     */
    @Synchronized
    fun execSession(cmd: String): Boolean {
        val out = sessionOut ?: return exec(cmd)
        return try {
            out.writeBytes(cmd)
            out.writeBytes("\n")
            out.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "execSession failed: $cmd", e)
            closeSession()
            false
        }
    }

    @Synchronized
    fun closeSession() {
        try {
            sessionOut?.writeBytes("exit\n")
            sessionOut?.flush()
        } catch (_: Throwable) {
        }
        try {
            sessionOut?.close()
        } catch (_: Throwable) {
        }
        try {
            sessionProcess?.destroy()
        } catch (_: Throwable) {
        }
        sessionOut = null
        sessionProcess = null
    }
}
