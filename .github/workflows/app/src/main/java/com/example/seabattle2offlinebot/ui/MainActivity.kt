package com.example.seabattle2offlinebot.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.seabattle2offlinebot.R
import com.example.seabattle2offlinebot.bot.BotService
import com.example.seabattle2offlinebot.overlay.OverlayService
import com.example.seabattle2offlinebot.storage.StatsStore
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var statsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statsView = findViewById(R.id.stats)

        findViewById<MaterialButton>(R.id.btnOverlay).setOnClickListener {
            ensureOverlayPermission()
            startService(Intent(this, OverlayService::class.java))
        }

        findViewById<MaterialButton>(R.id.btnBot).setOnClickListener {
            ensureOverlayPermission()
            startService(Intent(this, BotService::class.java).apply {
                action = BotService.ACTION_START
            })
        }

        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener {
            startService(Intent(this, BotService::class.java).apply {
                action = BotService.ACTION_STOP
            })
        }
    }

    override fun onResume() {
        super.onResume()
        val s = StatsStore(this).load()
        statsView.text =
            "Stats\n" +
            "Games: ${s.games}\n" +
            "Wins: ${s.wins}\n" +
            "Losses: ${s.losses}\n" +
            "Winrate: ${s.winratePercent()}%\n" +
            "Avg shots: ${s.avgShots()}"
    }

    private fun ensureOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
