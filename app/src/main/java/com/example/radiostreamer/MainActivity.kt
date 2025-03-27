package com.example.radiostreamer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        startAudioService()

        // Har 50 ms oralig'ida radio ulanishi tekshiriladi
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (AudioPlayerService.audioSessionId != -1) {
                    statusTextView.text = "Radio Aytmoqda"
                } else {
                    statusTextView.text = "Waiting for radio..."
                    handler.postDelayed(this, 50)
                }
            }
        }, 50)
    }

    private fun startAudioService() {
        val intent = Intent(this, AudioPlayerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
