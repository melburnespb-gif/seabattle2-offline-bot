package com.seabattle2.offlinebot

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var wm: WindowManager

    private var botView: View? = null
    private var captureView: View? = null
    private var hintView: View? = null

    private var isRunning = false
    private var isCalibrating = false

    private val steps = listOf(
        "btn_open_mode" to "Кнопка: открыть выбор режима/игры (например ИГРАТЬ)",
        "btn_start_game" to "Кнопка: начать игру (в меню режима)",
        "btn_random_layout" to "Кнопка: случайная расстановка кораблей",
        "btn_start_search" to "Кнопка: начать бой/поиск бота",
        "btn_close_result" to "Кнопка: закрыть окно победы/поражения (Далее)",
        "field_left_tl" to "ЛЕВОЕ поле: верхний левый угол (край сетки)",
        "field_left_br" to "ЛЕВОЕ поле: нижний правый угол (край сетки)",
        "field_right_tl" to "ПРАВОЕ поле: верхний левый угол (край сетки)",
        "field_right_br" to "ПРАВОЕ поле: нижний правый угол (край сетки)"
    )
    private var stepIndex = 0

    private lateinit var store: CalibrationStore

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        store = CalibrationStore(this)
        showBotButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAll()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBotButton() {
        if (botView != null) return

        val button = TextView(this).apply {
            text = "BOT"
            textSize = 16f
            setPadding(26, 18, 26, 18)
            setBackgroundColor(0xAA222222.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 260
        }

        // Перетаскивание + клик/лонгклик
        var downX = 0
        var downY = 0
        var startX = 0
        var startY = 0
        var downTime = 0L
        var moved = false

        button.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = SystemClock.uptimeMillis()
                    moved = false
                    downX = ev.rawX.toInt()
                    downY = ev.rawY.toInt()
                    startX = params.x
                    startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX.toInt() - downX
                    val dy = ev.rawY.toInt() - downY
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    wm.updateViewLayout(button, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val press = SystemClock.uptimeMillis() - downTime
                    if (!moved) {
                        if (press > 600) {
                            toggleCalib()
                        } else {
                            toggleRun()
                            button.text = if (isRunning) "ON" else "BOT"
                        }
                    }
                    true
                }
                else -> false
            }
        }

        wm.addView(button, params)
        botView = button
    }

    private fun toggleRun() {
        isRunning = !isRunning
        val intent = Intent(this, BotService::class.java).apply {
            action = if (isRunning) BotService.ACTION_START else BotService.ACTION_STOP
        }
        startService(intent)
    }

    private fun toggleCalib() {
        if (!isCalibrating) {
            isCalibrating = true
            stepIndex = 0
            showHint()
            showCaptureOverlay()
        } else {
            stopCalib()
        }
    }

    private fun stopCalib() {
        isCalibrating = false
        removeCaptureOverlay()
        removeHint()
    }

    private fun showHint() {
        if (hintView != null) return

        val tv = TextView(this).apply {
            textSize = 14f
            setPadding(22, 16, 22, 16)
            setBackgroundColor(0xAA000000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 40
        }

        updateHintText(tv)
        wm.addView(tv, p)
        hintView = tv
    }

    private fun updateHintText(tv: TextView) {
        val (key, title) = steps[stepIndex]
        tv.text = "CALIB ${stepIndex + 1}/${steps.size}\n$title\n(тапни по месту на экране)\n$key"
    }

    private fun showCaptureOverlay() {
        if (captureView != null) return

        val v = View(this)
        v.setBackgroundColor(0x22000000) // слегка затемним

        v.setOnTouchListener { _, ev ->
            if (!isCalibrating) return@setOnTouchListener false
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val x = ev.rawX.toInt()
                val y = ev.rawY.toInt()

                val (key, _) = steps[stepIndex]
                store.savePoint(key, x, y)

                // ✅ прокидываем тап в игру (root)
                RootShell.tap(x, y)

                stepIndex++
                val tv = hintView as? TextView
                if (stepIndex >= steps.size) {
                    tv?.text = "CALIB DONE ✅\nДолгий тап по BOT — выйти"
                } else {
                    tv?.let { updateHintText(it) }
                }
                return@setOnTouchListener true
            }
            true
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        wm.addView(v, params)
        captureView = v
    }

    private fun removeCaptureOverlay() {
        captureView?.let { wm.removeView(it) }
        captureView = null
    }

    private fun removeHint() {
        hintView?.let { wm.removeView(it) }
        hintView = null
    }

    private fun removeAll() {
        removeCaptureOverlay()
        removeHint()
        botView?.let { wm.removeView(it) }
        botView = null
    }
}
