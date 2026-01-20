package com.example.seabattle2offlinebot.bot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object ScreenIO {

    fun tap(x: Int, y: Int) {
        RootShell.exec("input tap $x $y")
    }

    fun screenshot(ctx: Context): Bitmap? {
        val f = File(ctx.getExternalFilesDir(null), "screen.png")
        RootShell.exec("screencap -p ${f.absolutePath}")
        return if (f.exists())
