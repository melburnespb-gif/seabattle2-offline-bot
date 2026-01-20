package com.example.seabattle2offlinebot.bot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.seabattle2offlinebot.storage.CalibrationStore
import com.example.seabattle2offlinebot.storage.StatsStore
import kotlin.concurrent.thread
import kotlin.random.Random

class BotService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        private const val CHANNEL_ID = "bot"
        private const val NOTIF_ID = 1
    }

    @Volatile private var running = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startBot()
            ACTION_STOP -> stopBot()
        }
        return START_STICKY
    }

    private fun startBot() {
        if (running) return
        running = true
        startForeground(NOTIF_ID, buildNotification("Running"))

        worker = thread(start = true, name = "GridAssistant") {
            val wake = acquireWakeLock()
            try {
                loop()
            } finally {
                wake?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopBot() {
        running = false
        worker?.interrupt()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun loop() {
        val calib = CalibrationStore(this)

        val required = listOf(
            "btn_action_1", "btn_action_2", "btn_action_3", "btn_action_4", "btn_action_5",
            "field_left_tl", "field_left_br", "field_right_tl", "field_right_br"
        )
        if (!calib.hasAll(required)) {
            updateNotif("Need calibration")
            running = false
            return
        }

        fun p(key: String) = calib.getPoint(key)!!

        val leftField = FieldRect(p("field_left_tl"), p("field_left_br"))
        val rightField = FieldRect(p("field_right_tl"), p("field_right_br"))
        val leftCells = Grid.build(leftField)
        val rightCells = Grid.build(rightField)

        val stats = StatsStore(this)

        while (running) {
            updateNotif("Starting…")

            // Последовательность “входа в бой” — универсальная, ты сам калибровал кнопки
            tapP(p("btn_action_1")); humanDelay()
            tapP(p("btn_action_2")); humanDelay()
            tapP(p("btn_action_3")); humanDelay()
            tapP(p("btn_action_4")); humanDelay(900, 1400)

            val board = Array(10) { Array(10) { CellState.UNKNOWN } }
            var shots = 0
            var hits = 0

            // Определяем где “цель” (враг): поле с большей “белизной”
            val bm0 = ScreenIO.screenshot(this) ?: run { humanDelay(); continue }
            val wLeft = Vision.whitenessRatio(bm0, Vision.Rect(leftField.tl.first, leftField.tl.second, leftField.br.first, leftField.br.second))
            val wRight = Vision.whitenessRatio(bm0, Vision.Rect(rightField.tl.first, rightField.tl.second, rightField.br.first, rightField.br.second))

            val targetCells = if (wLeft > wRight) leftCells else rightCells
            updateNotif("Battle (target=${if (targetCells === leftCells) "LEFT" else "RIGHT"})")

            // Бой
            while (running) {
                val move = Strategy.nextMove(board) ?: break
                val cell = targetCells.first { it.gx == move.x && it.gy == move.y }

                ScreenIO.tap(jitter(cell.sx), jitter(cell.sy))
                shots++
                humanDelay(650, 1100)

                val bm = ScreenIO.screenshot(this) ?: continue
                val state = Vision.cellState(bm, cell.sx, cell.sy)
                board[move.y][move.x] = state
                if (state == CellState.HIT) hits++

                // Победа по классике = 20 попаданий
                if (hits >= Strategy.TOTAL_HITS_TO_WIN) {
                    stats.registerGame(win = true, shots = shots)
                    updateNotif("WIN ✅ shots=$shots")
                    break
                }

                // Если появилось окно результата — закрываем и считаем как поражение (если не 20)
                if (resultLikelyShown(bm, p("btn_action_5"))) {
                    val win = hits >= Strategy.TOTAL_HITS_TO_WIN
                    stats.registerGame(win = win, shots = shots)
                    updateNotif(if (win) "WIN ✅" else "LOSS ❌")
                    break
                }
            }

            // “Далее/закрыть результат”
            tapP(p("btn_action_5"))
            humanDelay(1200, 1800)
        }
    }

    private fun tapP(pt: Pair<Int, Int>) = ScreenIO.tap(pt.first, pt.second)

    private fun resultLikelyShown(bm: android.graphics.Bitmap, btn: Pair<Int, Int>): Boolean {
        // Эвристика: когда модальное окно результата на экране,
        // область вокруг кнопки “Далее” становится не белой и более контрастной.
        val (x, y) = btn
        val r = 22
        val l = (x - r).coerceIn(0, bm.width - 1)
        val t = (y - r).coerceIn(0, bm.height - 1)
        val rr = (x + r).coerceIn(0, bm.width - 1)
        val bb = (y + r).coerceIn(0, bm.height - 1)

        var sum = 0L
        var sum2 = 0L
        var n = 0
        val step = 2

        for (yy in t until bb step step) for (xx in l until rr step step) {
            val c = bm.getPixel(xx, yy)
            val lum = (((c shr 16) and 0xFF) * 3 + ((c shr 8) and 0xFF) * 4 + (c and 0xFF)) / 8
            sum += lum
            sum2 += lum.toLong() * lum.toLong()
            n++
        }
        if (n == 0) return false
        val mean = sum.toDouble() / n
        val varr = (sum2.toDouble() / n) - mean * mean

        // Пороги можно чуть подстроить под твою тему
        return mean < 210 && varr > 200.0
    }

    private fun jitter(v: Int): Int = v + Random.nextInt(-3, 4)

    private fun humanDelay(minMs: Int = 500, maxMs: Int = 900) {
        try {
            Thread.sleep(Random.nextLong(minMs.toLong(), maxMs.toLong()))
        } catch (_: InterruptedException) {}
    }

    private fun updateNotif(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        createChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Grid Game Assistant")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Bot", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun acquireWakeLock(): PowerManager.WakeLock? {
        return try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GridAssistant::wakelock").apply {
                acquire(10 * 60 * 1000L)
            }
        } catch (_: Throwable) { null }
    }
}
