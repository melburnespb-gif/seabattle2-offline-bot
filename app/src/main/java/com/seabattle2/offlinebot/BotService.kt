package com.seabattle2.offlinebot

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

class BotService : Service() {

    companion object {
        const val ACTION_START = "com.seabattle2.offlinebot.action.START"
        const val ACTION_STOP  = "com.seabattle2.offlinebot.action.STOP"

        private const val TAG = "BotService"

        private const val NOTIF_CHANNEL_ID = "bot_channel"
        private const val NOTIF_ID = 101

        // === ТВОИ ЖЕЛАЕМЫЕ ПАРАМЕТРЫ ДЛЯ МЕНЮ ===
        private const val MENU_TAPS = 3
        private const val MENU_DELAY_MS = 10_000L

        // === ПАРАМЕТРЫ ДЛЯ ВЫСТРЕЛОВ (регулируемые) ===
        // Сколько тапов по одной клетке, чтобы наверняка сработало (например 1..3)
        private const val SHOT_TAPS = 1
        // Пауза между тапами по клетке (если SHOT_TAPS > 1)
        private const val SHOT_TAP_GAP_MS = 250L
        // Пауза после выстрела (чтобы игра успела обработать ход/анимацию)
        private const val AFTER_SHOT_PAUSE_MS = 900L

        // === ПРОВЕРКА ХОДА ПО ТРЕУГОЛЬНИКУ ===
        // Как часто проверять, если сейчас ход врага
        private const val WAIT_TURN_POLL_MS = 500L

        // Размер области (квадрат) вокруг точки треугольника для усреднения цвета
        private const val TRI_SAMPLE_RADIUS = 8 // 2*8+1 => 17x17

        // Порог “насколько цвет должен доминировать”
        private const val DOMINANCE = 45  // чем больше — тем строже
    }

    private val running = AtomicBoolean(false)
    private lateinit var store: CalibrationStore

    // файл скриншота (в директории приложения, без лишних разрешений)
    private val capFile by lazy {
        val dir = getExternalFilesDir(null) ?: filesDir
        File(dir, "cap.png")
    }

    override fun onCreate() {
        super.onCreate()
        store = CalibrationStore(this)
        ensureNotifChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startBot()
            ACTION_STOP -> stopBot()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startBot() {
        if (running.getAndSet(true)) return

        startForeground(NOTIF_ID, buildNotification("BOT: running"))

        Thread {
            try {
                mainLoop()
            } catch (t: Throwable) {
                Log.e(TAG, "Bot crashed", t)
            } finally {
                running.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun stopBot() {
        running.set(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun mainLoop() {
        Log.i(TAG, "Bot loop started")

        // 1) МЕНЮ: ты просил — “по 3 раза с задержкой 10 секунд между тыками по каждой из кнопок”
        // Если какие-то точки не откалиброваны — просто пропустим.
        runMenuMacroIfConfigured()

        // 2) БОЙ: ждать пока мой ход по треугольнику, потом делать выстрелы
        // Тут я оставляю “пример” — стрельба пока заглушка, потому что ты не дал финальную логику выбора клетки.
        // Но ПОЛНОСТЬЮ сделана проверка хода по треугольнику + защита от “прожать в ход врага”.
        while (running.get()) {

            // если нет точки треугольника — не можем проверять; чтобы не зависнуть, считаем что ход мой
            val myTurn = isMyTurnByTriangle() ?: true

            if (!myTurn) {
                SystemClock.sleep(WAIT_TURN_POLL_MS)
                continue
            }

            // === ТУТ ДОЛЖНА БЫТЬ ТВОЯ ЛОГИКА ВЫБОРА КЛЕТКИ ===
            // Сейчас: если задана точка "shot_point" — будем тыкать туда как пример.
            val shot = store.getPoint("shot_point")
            if (shot != null) {
                tapMultiple(shot.x, shot.y, SHOT_TAPS, SHOT_TAP_GAP_MS)
                SystemClock.sleep(AFTER_SHOT_PAUSE_MS)
            } else {
                // если нет клетки — просто не делаем ничего, но продолжаем проверять ход
                SystemClock.sleep(400)
            }
        }

        Log.i(TAG, "Bot loop stopped")
    }

    private fun runMenuMacroIfConfigured() {
        // Кнопки меню из твоей калибровки:
        // btn_open_mode, btn_start_game, btn_random_layout, btn_start_search, btn_close_result
        val keys = listOf(
            "btn_open_mode",
            "btn_start_game",
            "btn_random_layout",
            "btn_start_search",
            "btn_close_result",
        )

        for (key in keys) {
            if (!running.get()) return
            val p = store.getPoint(key) ?: continue

            // 3 раза с задержкой 10 секунд между нажатиями по КАЖДОЙ кнопке
            repeat(MENU_TAPS) { i ->
                if (!running.get()) return
                RootShell.tap(p.x, p.y)
                Log.i(TAG, "MENU tap $key (${i + 1}/$MENU_TAPS) at ${p.x},${p.y}")
                if (i != MENU_TAPS - 1) SystemClock.sleep(MENU_DELAY_MS)
            }

            // небольшая пауза после кнопки, чтобы интерфейс успел смениться
            SystemClock.sleep(800)
        }
    }

    /**
     * Проверка хода по треугольнику:
     * - берём скриншот root-ом
     * - вырезаем область вокруг точки "turn_triangle"
     * - считаем средний цвет
     * - если “зелёный доминирует” => мой ход
     * - если “красный доминирует” => ход врага
     *
     * @return true = мой ход, false = враг, null = не удалось определить (нет калибровки/скриншота)
     */
    private fun isMyTurnByTriangle(): Boolean? {
        val tri = store.getPoint("turn_triangle") ?: return null

        val bmp = takeScreenshot() ?: return null

        val avg = averageColor(bmp, tri.x, tri.y, TRI_SAMPLE_RADIUS)
        val r = Color.red(avg)
        val g = Color.green(avg)
        val b = Color.blue(avg)

        // Зелёный треугольник => g сильно больше r и b
        val green = (g - r) > DOMINANCE && (g - b) > DOMINANCE

        // Красный треугольник => r сильно больше g и b
        val red = (r - g) > DOMINANCE && (r - b) > DOMINANCE

        Log.d(TAG, "TURN color avg rgb=($r,$g,$b) green=$green red=$red")

        return when {
            green -> true
            red -> false
            else -> {
                // если промежуточная анимация/блеклый цвет — можно считать “не определилось”
                // и тогда бот НЕ стреляет (безопаснее)
                null
            }
        }
    }

    private fun takeScreenshot(): Bitmap? {
        try {
            // root пишет файл в директорию приложения, которую мы читаем без storage-permission
            // ВАЖНО: RootShell.exec должен выполнять "su -c ..."
            val path = capFile.absolutePath
            RootShell.exec("screencap -p \"$path\"")
            if (!capFile.exists() || capFile.length() < 1000) {
                Log.w(TAG, "Screenshot file not created: $path")
                return null
            }
            val bytes = capFile.readBytes()
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (t: Throwable) {
            Log.e(TAG, "takeScreenshot failed", t)
            return null
        }
    }

    private fun averageColor(bmp: Bitmap, cx: Int, cy: Int, radius: Int): Int {
        val x0 = max(0, cx - radius)
        val y0 = max(0, cy - radius)
        val x1 = min(bmp.width - 1, cx + radius)
        val y1 = min(bmp.height - 1, cy + radius)

        var sr = 0L
        var sg = 0L
        var sb = 0L
        var count = 0L

        for (y in y0..y1) {
            for (x in x0..x1) {
                val c = bmp.getPixel(x, y)
                sr += Color.red(c)
                sg += Color.green(c)
                sb += Color.blue(c)
                count++
            }
        }

        if (count == 0L) return Color.BLACK
        val r = (sr / count).toInt()
        val g = (sg / count).toInt()
        val b = (sb / count).toInt()
        return Color.rgb(r, g, b)
    }

    private fun tapMultiple(x: Int, y: Int, count: Int, gapMs: Long) {
        repeat(max(1, count)) { i ->
            RootShell.tap(x, y)
            if (i != count - 1) SystemClock.sleep(gapMs)
        }
    }

    private fun ensureNotifChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            NOTIF_CHANNEL_ID,
            "Seabattle Bot",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Offline Bot")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
