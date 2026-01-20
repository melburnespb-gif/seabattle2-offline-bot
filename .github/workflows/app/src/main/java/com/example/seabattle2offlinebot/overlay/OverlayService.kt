package com.example.seabattle2offlinebot.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import com.example.seabattle2offlinebot.bot.BotService
import com.example.seabattle2offlinebot.storage.CalibrationStore
import com.google.android.material.button.MaterialButton

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var rootView: View? = null
    private val steps = CalibrationStep.defaultSteps()
    private var stepIdx = 0

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        rootView?.let { wm.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(0xCC222222.toInt())
        }

        val title = TextView(this).apply {
            text = "Grid Game Assistant"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
        }

        val hint = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
        }

        val btnCalib = MaterialButton(this).apply { text = "CALIBRATE / NEXT" }
        val btnStart = MaterialButton(this).apply { text = "START" }
        val btnStop  = MaterialButton(this).apply { text = "STOP" }
        val btnClose = MaterialButton(this).apply { text = "CLOSE" }

        layout.addView(title)
        layout.addView(hint)
        layout.addView(btnCalib)
        layout.addView(btnStart)
        layout.addView(btnStop)
        layout.addView(btnClose)

        fun updateHint() {
            val cur = steps.getOrNull(stepIdx)
            hint.text = if (cur == null) {
                "Калибровка завершена ✅"
            } else {
                "Шаг ${stepIdx + 1}/${steps.size}: тапни по: ${cur.label}"
            }
        }
        updateHint()

        // Перетаскивание оверлея
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 150
        }

        var lastX = 0
        var lastY = 0
        var touchX = 0f
        var touchY = 0f

        layout.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = params.x
                    lastY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = lastX + (e.rawX - touchX).toInt()
                    params.y = lastY + (e.rawY - touchY).toInt()
                    wm.updateViewLayout(layout, params)
                    true
                }
                else -> false
            }
        }

        // Калибровка: после нажатия кнопки ловим один тап по экрану
        btnCalib.setOnClickListener {
            val cur = steps.getOrNull(stepIdx) ?: return@setOnClickListener
            captureOneTap { x, y ->
                CalibrationStore(this).putPoint(cur.key, x, y)
                stepIdx += 1
                updateHint()
            }
        }

        btnStart.setOnClickListener {
            startService(Intent(this, BotService::class.java).apply { action = BotService.ACTION_START })
        }
        btnStop.setOnClickListener {
            startService(Intent(this, BotService::class.java).apply { action = BotService.ACTION_STOP })
        }
        btnClose.setOnClickListener { stopSelf() }

        rootView = layout
        wm.addView(layout, params)
    }

    private fun captureOneTap(onTap: (Int, Int) -> Unit) {
        val catcher = View(this).apply { setBackgroundColor(0x00000000) }

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        catcher.setOnTouchListener { v, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                onTap(e.rawX.toInt(), e.rawY.toInt())
                wm.removeView(v)
                true
            } else false
        }

        wm.addView(catcher, p)
    }
}
