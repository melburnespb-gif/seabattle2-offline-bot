package com.seabattle2.offlinebot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object ScreenIO {
    fun screenshot(ctx: Context): Bitmap? {
        val f = File(ctx.getExternalFilesDir(null), "screen.png")
        val ok = RootShell.screencap(f.absolutePath)
        return if (ok && f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }
}
