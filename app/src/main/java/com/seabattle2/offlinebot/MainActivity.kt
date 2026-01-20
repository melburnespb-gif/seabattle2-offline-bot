package com.seabattle2.offlinebot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tv = TextView(this)
        tv.text = "SeaBattle2 Offline Bot\nAPK собран успешно"
        tv.textSize = 18f
        setContentView(tv)
    }
}
