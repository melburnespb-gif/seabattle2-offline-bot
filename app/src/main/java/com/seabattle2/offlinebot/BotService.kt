package com.seabattle2.offlinebot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
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

        worker = thread(start = true, name = "SeaBattleBot") {
            val wake = acquireWakeLock()
            try {
                loop()
            } catch (t: Throwable) {
                updateNotif("Crash: ${t.javaClass.simpleName}")
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
        val store = CalibrationStore(this)

        fun p(key: String): Pair<Int, Int> {
            return store.loadPoint(key) ?: throw IllegalStateException("Need calibration: $key")
        }

        // кнопки
        val btnOpen = p("btn_open_mode")
        val btnStart = p("btn_start_game")
        val btnRand = p("btn_random_layout")
        val btnSearch = p("btn_start_search")
        val btnClose = p("btn_close_result")

        // поля (углы)
        val left = FieldRect(p("field_left_tl"), p("field_left_br"))
        val right = FieldRect(p("field_right_tl"), p("field_right_br"))

        val leftCells = Grid.build(left)
        val rightCells = Grid.build(right)

        while (running) {
            updateNotif("Starting…")

            // Вход в матч
            RootShell.tap(btnOpen.first, btnOpen.second); humanDelay()
            RootShell.tap(btnStart.first, btnStart.second); humanDelay()
            RootShell.tap(btnRand.first, btnRand.second); humanDelay()
            RootShell.tap(btnSearch.first, btnSearch.second); humanDelay(900, 1400)

            val board = Array(10) { Array(10) { CellState.UNKNOWN } }
            var shots = 0
            var hits = 0

            // Скриншот для определения целевого поля
            val bm0 = ScreenIO.screenshot(this)
            if (bm0 == null) {
                humanDelay()
                continue
            }

            val wLeft = Vision.whitenessRatio(
                bm0,
                Vision.Rect(left.tl.first, left.tl.second, left.br.first, left.br.second)
            )
            val wRight = Vision.whitenessRatio(
                bm0,
                Vision.Rect(right.tl.first, right.tl.second, right.br.first, right.br.second)
            )

            val targetCells = if (wLeft > wRight) leftCells else rightCells
            updateNotif("Battle target=${if (targetCells === leftCells) "LEFT" else "RIGHT"}")

            // Бой
            while (running) {
                val mv = Strategy.nextMove(board) ?: break
                val cell = targetCells.first { it.gx == mv.x && it.gy == mv.y }

                RootShell.tap(jitter(cell.sx), jitter(cell.sy))
                shots++
                humanDelay(650, 1100)

                val bm = ScreenIO.screenshot(this)
                if (bm == null) continue

                val st = Vision.cellState(bm, cell.sx, cell.sy)
                board[mv.y][mv.x] = st
                if (st == CellState.HIT) hits++

                if (hits >= Strategy.TOTAL_HITS_TO_WIN) {
                    updateNotif("WIN ✅ shots=$shots")
                    break
                }

                if (resultLikelyShown(bm, btnClose)) {
                    updateNotif("Result window (hits=$hits, shots=$shots)")
                    break
                }
            }

            // закрыть результат
            RootShell.tap(btnClose.first, btnClose.second)
            humanDelay(1200, 1800)
        }
    }

    private fun resultLikelyShown(bm: android.graphics.Bitmap, btn: Pair<Int, Int>): Boolean {
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
            .setContentTitle("SeaBattle Bot")
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
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SeaBattleBot::wakelock").apply {
                acquire(10 * 60 * 1000L)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
