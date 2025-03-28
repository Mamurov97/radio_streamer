package com.example.radiostreamer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)

        // Servisni avtomatik ishga tushiramiz
        startAudioService()

        // Internet holatini kuzatamiz
        connectivityMonitor = ConnectivityMonitor(this) { isConnected ->
            runOnUiThread {
                if (isConnected) {
                    statusTextView.text = "Radio Aytmoqda"
                    // Internet tiklandi, servisni qayta ishga tushirish (restart qilish)
                    restartAudioService()
                } else {
                    statusTextView.text = "Internet uzilgan"
                }
            }
        }
        connectivityMonitor.register()

        // Agar boshqa polling yoki visualizer bo'lsa, ular ham shu yerda joylashadi
    }

    private fun startAudioService() {
        val intent = Intent(this, AudioPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // Servisni qayta ishga tushirish: oldingi servisni to'xtatib, yangi ishga tushiramiz
    private fun restartAudioService() {
        val intent = Intent(this, AudioPlayerService::class.java)
        stopService(intent)
        // Qisqa kechikish qo'shib qayta ishga tushiramiz
        handler.postDelayed({
            startAudioService()
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityMonitor.unregister()
    }
}
