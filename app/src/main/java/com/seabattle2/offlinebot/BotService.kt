package com.seabattle2.offlinebot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class BotService : Service() {

    companion object {
        const val ACTION_START = "com.seabattle2.offlinebot.action.START"
        const val ACTION_STOP  = "com.seabattle2.offlinebot.action.STOP"

        private const val NOTIF_CHANNEL_ID = "offlinebot_channel"
        private const val NOTIF_ID = 1001
    }

    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    private lateinit var store: CalibrationStore

    // ===== НАСТРОЙКИ "МЕНЮ" =====
    // Нажать по каждой кнопке 3 раза, пауза 10 секунд между нажатиями
    private val menuTapCount = 3
    private val menuTapDelayMs = 10_000L

    // ===== НАСТРОЙКИ "ВЫСТРЕЛА" (по клетке) =====
    // Сколько раз нажимать по клетке, чтобы не промазать по лагам/анимации
    private val shotTapCount = 2              // поменяешь под себя
    private val shotTapGapMs = 450L           // пауза между нажатиями по клетке
    private val shotAfterMs  = 900L           // пауза после серии нажатий по клетке

    // ===== ПРОВЕРКА "МОЙ ХОД" =====
    private val turnPollMs = 300L
    private val turnWaitMaxMs = 25_000L       // максимум ждать "мой ход" перед тем как сдаться и пойти дальше
    private val turnColorTolerance = 22       // допуск по RGB

    // ===== СЕТКА ВЫСТРЕЛОВ =====
    private val gridSize = 10

    // Ключи из калибровки (как у тебя в OverlayService steps)
    private val kOpenMode = "btn_open_mode"
    private val kStartGame = "btn_start_game"
    private val kRandomLayout = "btn_random_layout"
    private val kStartSearch = "btn_start_search"
    private val kCloseResult = "btn_close_result"

    private val kRightTL = "field_right_tl"
    private val kRightBR = "field_right_br"

    private val kTurnPixel = "turn_pixel" // ты добавишь эту зону в OverlayService (маркер TURN)

    override fun onCreate() {
        super.onCreate()
        store = CalibrationStore(this)
        ensureForeground()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopNow()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startNow()
            ACTION_STOP -> stopNow()
        }
        return START_STICKY
    }

    private fun startNow() {
        if (running.get()) return
        running.set(true)

        worker = Thread {
            try {
                mainLoop()
            } catch (_: Throwable) {
                // если что-то упало — просто остановим
            } finally {
                running.set(false)
            }
        }.apply { start() }
    }

    private fun stopNow() {
        running.set(false)
        worker?.interrupt()
        worker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "OfflineBot",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(ch)
        }

        val notif: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle("OfflineBot")
                .setContentText("Бот запущен")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("OfflineBot")
                .setContentText("Бот запущен")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build()
        }

        startForeground(NOTIF_ID, notif)
    }

    // ==========================
    // Главный цикл
    // ==========================
    private fun mainLoop() {
        // 1) Меню-автоподготовка (кнопки по 3 раза с 10 сек паузой)
        //    Если игра уже в бою — эти точки могут быть неактуальны, но клики будут "мимо" и ничего страшного.
        menuPreparation()

        // 2) Бой: ходим по клеткам правого поля (enemy) по порядку
        //    Если ты хочешь другой алгоритм — скажешь, но это уже полноценная логика выбора.
        battleLoop()
    }

    // ==========================
    // МЕНЮ: по 3 раза с 10 сек
    // ==========================
    private fun menuPreparation() {
        // Сценарий:
        // 1) открыть режим/играть
        // 2) начать игру
        // 3) случайная расстановка
        // 4) старт боя/поиск/играть
        //
        // Закрыть результат (если внезапно висит окно конца) — тоже дергаем в конце.
        val sequence = listOf(kOpenMode, kStartGame, kRandomLayout, kStartSearch)

        for (key in sequence) {
            if (!running.get()) return
            tapMenuButton3x(key)
        }

        // Иногда после входа/выхода может висеть "Далее" или окно — на всякий случай
        if (!running.get()) return
        tapMenuButton3x(kCloseResult)
    }

    private fun tapMenuButton3x(key: String) {
        val p = store.getPoint(key) ?: return
        repeat(menuTapCount) { i ->
            if (!running.get()) return
            RootShell.tap(p.first, p.second)
            // Между нажатиями именно 10 секунд
            if (i < menuTapCount - 1) sleepMs(menuTapDelayMs)
        }
    }

    // ==========================
    // БОЙ
    // ==========================
    private fun battleLoop() {
        val tl = store.getPoint(kRightTL)
        val br = store.getPoint(kRightBR)

        // Если сетка не откалибрована — просто ничего не делаем (чтобы не тыкать в экран хаотично)
        if (tl == null || br == null) return

        // Предварительно вычислим точки центров клеток 10x10
        val cells = buildGridCells(
            x1 = tl.first,
            y1 = tl.second,
            x2 = br.first,
            y2 = br.second,
            n = gridSize
        )

        var idx = 0
        while (running.get()) {
            // Каждый "ход" — очередная клетка
            val (x, y) = cells[idx]

            // Подождать, пока реально можно стрелять (если turn_pixel + turn_color есть)
            waitMyTurnOrTimeout()

            // Нажать по клетке N раз с паузой
            tapBurst(x, y, shotTapCount, shotTapGapMs)

            // Пауза после попытки выстрела (даём игре анимацию/переход хода)
            sleepMs(shotAfterMs)

            idx++
            if (idx >= cells.size) idx = 0

            // Иногда по кругу полезно "закрыть результат", если бой закончился и висит окно
            // (в бою это обычно безопасно — если окна нет, клик уйдёт мимо)
            maybeCloseResult()
        }
    }

    private fun maybeCloseResult() {
        val p = store.getPoint(kCloseResult) ?: return
        // Одного клика обычно достаточно
        RootShell.tap(p.first, p.second)
        sleepMs(250)
    }

    // ==========================
    // Ожидание "мой ход"
    // ==========================
    private fun waitMyTurnOrTimeout(): Boolean {
        val p = store.getPoint(kTurnPixel) ?: return true
        val expected = store.getInt("turn_color", 0)
        if (expected == 0) return true

        val start = SystemClock.uptimeMillis()

        while (running.get()) {
            val c = ScreenSampler.getPixelColor(p.first, p.second)
            if (c != null && TurnDetector.isSimilarColor(c, expected, tol = turnColorTolerance)) {
                return true
            }

            val elapsed = SystemClock.uptimeMillis() - start
            if (elapsed >= turnWaitMaxMs) return false

            sleepMs(turnPollMs)
        }
        return false
    }

    // ==========================
    // Вспомогательные
    // ==========================
    private fun tapBurst(x: Int, y: Int, count: Int, gapMs: Long) {
        if (count <= 0) return
        var i = 0
        while (i < count && running.get()) {
            RootShell.tap(x, y)
            if (i < count - 1) sleepMs(gapMs)
            i++
        }
    }

    private fun buildGridCells(x1: Int, y1: Int, x2: Int, y2: Int, n: Int): List<Pair<Int, Int>> {
        val left = minOf(x1, x2)
        val right = maxOf(x1, x2)
        val top = minOf(y1, y2)
        val bottom = maxOf(y1, y2)

        val w = (right - left).coerceAtLeast(1)
        val h = (bottom - top).coerceAtLeast(1)

        val cellW = w.toFloat() / n.toFloat()
        val cellH = h.toFloat() / n.toFloat()

        val out = ArrayList<Pair<Int, Int>>(n * n)

        // Центры клеток: (col + 0.5) * cellW
        for (row in 0 until n) {
            for (col in 0 until n) {
                val cx = left + ((col + 0.5f) * cellW).roundToInt()
                val cy = top + ((row + 0.5f) * cellH).roundToInt()
                out.add(cx to cy)
            }
        }
        return out
    }

    private fun sleepMs(ms: Long) {
        if (ms <= 0) return
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
        }
    }
}
