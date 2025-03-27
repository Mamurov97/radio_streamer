package com.example.radiostreamer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.Base64

class AudioPlayerService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private val CHANNEL_ID = "RadioPlayerChannel"
    private val NOTIFICATION_ID = 1

    // API endpoint va Basic Auth ma'lumotlari
    private val apiUrl = "http://salesman.garant.uz:80/api/get-radio"
    private val username = "login"
    private val password = "password"
    private val retryIntervalMillis = 10000L

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        var audioSessionId: Int = -1
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Radio is playing")
                .setContentText("Online radio streaming").setContentIntent(pendingIntent).build()

        startForeground(NOTIFICATION_ID, notification)
        fetchAndPlayRadioUrl()
        return START_STICKY
    }

    @OptIn(UnstableApi::class)
    private fun fetchAndPlayRadioUrl() {
        serviceScope.launch {
            try {
                val radioUrl = fetchRadioUrl()
                Log.d("RadioStreamer", "Radio URL: $radioUrl")
                if (!radioUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        exoPlayer.stop()
                        exoPlayer.setMediaItem(MediaItem.fromUri(radioUrl))
                        exoPlayer.prepare()
                        exoPlayer.play()
                        // 2 soniya kechikish orqali audio sessiya ID ni olish
                        Handler(Looper.getMainLooper()).postDelayed({
                            audioSessionId = exoPlayer.audioSessionId
                            Log.d("RadioStreamer", "Audio Session ID: $audioSessionId")
                        }, 2000)
                    }
                } else {
                    scheduleRetry()
                }
            } catch (e: Exception) {
                Log.e("RadioStreamer", "Error fetching radio URL", e)
                scheduleRetry()
            }
        }
    }

    private fun fetchRadioUrl(): String? {
        val client = OkHttpClient()
        val credentials = "$username:$password"
        val encodedCredentials = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(credentials.toByteArray())
        } else {
            android.util.Base64.encodeToString(
                credentials.toByteArray(), android.util.Base64.NO_WRAP
            )
        }
        val request: Request =
            Request.Builder().url(apiUrl).addHeader("Authorization", "Basic $encodedCredentials")
                .build()
        val response: Response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            if (!responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                return json.optString("url")
            }
        }
        return null
    }

    private fun scheduleRetry() {
        handler.postDelayed({ fetchAndPlayRadioUrl() }, retryIntervalMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        exoPlayer.stop()
        exoPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Radio Player Channel", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
