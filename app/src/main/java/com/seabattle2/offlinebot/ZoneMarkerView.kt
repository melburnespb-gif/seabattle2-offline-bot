package com.seabattle2.offlinebot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class ZoneMarkerView(
    ctx: Context,
    private val label: String,
    private val onMove: (dx: Int, dy: Int) -> Unit,
    private val onClick: () -> Unit
) : View(ctx) {

    private val pBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
    }

    private var downX = 0f
    private var downY = 0f
    private var moved = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // размер “мишени”
        setMeasuredDimension(160, 160)
    }

    override fun onDraw(c: Canvas) {
        // фон-кружок
        pBg.color = 0xAA000000.toInt()
        c.drawCircle(width / 2f, height / 2f, 70f, pBg)

        // маленькая точка в центре (прицел)
        pBg.color = 0xFFFFCC00.toInt()
        c.drawCircle(width / 2f, height / 2f, 10f, pBg)

        // подпись
        pText.color = 0xFFFFFFFF.toInt()
        val t = label.take(6) // коротко
        val tw = pText.measureText(t)
        c.drawText(t, width / 2f - tw / 2f, height / 2f - 85f, pText)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                moved = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (e.rawX - downX).toInt()
                val dy = (e.rawY - downY).toInt()
                if (abs(dx) > 6 || abs(dy) > 6) moved = true
                if (dx != 0 || dy != 0) {
                    onMove(dx, dy)
                    downX = e.rawX
                    downY = e.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!moved) onClick()
                return true
            }
        }
        return false
    }
}
