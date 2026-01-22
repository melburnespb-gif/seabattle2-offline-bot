package com.seabattle2.offlinebot

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var store: CalibrationStore

    private var panelView: LinearLayout? = null
    private var botBtn: TextView? = null
    private var calibBtn: TextView? = null

    private var isRunning = false
    private var isCalibrating = false

    private val zones = listOf(
        // Меню-кнопки (важны для тройного тапа)
        "btn_open_mode" to "OPEN",
        "btn_start_game" to "START",
        "btn_random_layout" to "RAND",
        "btn_start_search" to "FIGHT",
        "btn_close_result" to "NEXT",

        // Поля (если нужно для будущего)
        "field_left_tl" to "L_TL",
        "field_left_br" to "L_BR",
        "field_right_tl" to "R_TL",
        "field_right_br" to "R_BR",
    )

    private data class Marker(
        val key: String,
        val label: String,
        var x: Int,
        var y: Int,
        var view: ZoneMarkerView? = null,
        var params: WindowManager.LayoutParams? = null
    )

    private val markers = mutableListOf<Marker>()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        store = CalibrationStore(this)
        showPanel()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAll()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showPanel() {
        if (panelView != null) return

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xAA111111.toInt())
            setPadding(18, 14, 18, 14)
        }

        fun makeBtn(text: String): TextView =
            TextView(this).apply {
                this.text = text
                textSize = 16f
                setPadding(26, 18, 26, 18)
                setBackgroundColor(0xAA222222.toInt())
                setTextColor(0xFFFFFFFF.toInt())
            }

        val bot = makeBtn("BOT")
        val calib = makeBtn("CALIB")

        bot.setOnClickListener {
            toggleRun()
            bot.text = if (isRunning) "ON" else "BOT"
        }

        calib.setOnClickListener {
            toggleCalib()
            calib.text = if (isCalibrating) "SAVE" else "CALIB"
        }

        panel.addView(bot)
        panel.addView(calib)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // не забираем фокус, игра остаётся живой
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 240
        }

        // простое перетаскивание всего панеля
        panel.setOnTouchListener(DragMoveTouch(wm, panel, params))

        wm.addView(panel, params)
        panelView = panel
        botBtn = bot
        calibBtn = calib
    }

    private fun toggleRun() {
        isRunning = !isRunning
        val intent = Intent(this, BotService::class.java).apply {
            action = if (isRunning) BotService.ACTION_START else BotService.ACTION_STOP
        }
        startService(intent)
    }

    private fun toggleCalib() {
        isCalibrating = !isCalibrating
        if (isCalibrating) {
            showMarkers()
        } else {
            saveMarkers()
            hideMarkers()
        }
    }

    private fun showMarkers() {
        if (markers.isNotEmpty()) return

        // стартовые позиции: если есть сохранённые — берём их, иначе раскидаем “лесенкой”
        var startX = 80
        var startY = 420
        for ((key, label) in zones) {
            val saved = store.getPoint(key)
            val (x, y) = saved ?: (startX to startY).also {
                startY += 170
                if (startY > 1400) { startY = 420; startX += 190 }
            }

            val marker = Marker(key, label, x, y)
            addMarker(marker)
            markers.add(marker)
        }
    }

    private fun addMarker(m: Marker) {
        val v = ZoneMarkerView(
            this,
            label = m.label,
            onMove = { dx, dy ->
                val p = m.params ?: return@ZoneMarkerView
                p.x += dx
                p.y += dy
                m.x = p.x
                m.y = p.y
                wm.updateViewLayout(v, p)
            },
            onClick = {
                // быстрый тест: тап в эту точку (если хочешь проверить)
                RootShell.tap(m.x + 80, m.y + 80) // центр маркера
            }
        )

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // важно: не блокирует игру вне самого маркера
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = m.x
            y = m.y
        }

        wm.addView(v, p)
        m.view = v
        m.params = p
    }

    private fun saveMarkers() {
        // сохраняем центр каждого маркера как точку нажатия
        for (m in markers) {
            val tapX = m.x + 80
            val tapY = m.y + 80
            store.savePoint(m.key, tapX, tapY)
        }
    }

    private fun hideMarkers() {
        for (m in markers) {
            m.view?.let { wm.removeView(it) }
        }
        markers.clear()
    }

    private fun removeAll() {
        try { hideMarkers() } catch (_: Throwable) {}
        panelView?.let { wm.removeView(it) }
        panelView = null
        botBtn = null
        calibBtn = null
    }

    /**
     * Перетаскивание панели (как твой старый код, только вынесено)
     */
    private class DragMoveTouch(
        private val wm: WindowManager,
        private val view: android.view.View,
        private val params: WindowManager.LayoutParams
    ) : android.view.View.OnTouchListener {

        private var downX = 0
        private var downY = 0
        private var startX = 0
        private var startY = 0
        private var downTime = 0L
        private var moved = false

        override fun onTouch(v: android.view.View, ev: android.view.MotionEvent): Boolean {
            when (ev.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    downTime = SystemClock.uptimeMillis()
                    moved = false
                    downX = ev.rawX.toInt()
                    downY = ev.rawY.toInt()
                    startX = params.x
                    startY = params.y
                    return true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX.toInt() - downX
                    val dy = ev.rawY.toInt() - downY
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    wm.updateViewLayout(view, params)
                    return true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    // клик по панели нам не нужен
                    return true
                }
            }
            return false
        }
    }
}
