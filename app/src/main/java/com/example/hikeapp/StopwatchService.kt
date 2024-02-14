package com.example.hikeapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class StopwatchService : Service() {
    private var isRunning = false
    private var timeElapsed = 0L
    private lateinit var timer: Timer
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        timer = Timer()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Decide action based on intent
        when (intent.action) {
            "START" -> startStopwatch()
            "STOP" -> stopStopwatch()
        }
        return START_NOT_STICKY
    }


    @SuppressLint("ForegroundServiceType")
    private fun startStopwatch() {
        Log.d("StopwatchService", "Stopwatch started")
        isRunning = true
        timer.cancel()
        timer = Timer()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(1, createNotification(timeElapsed))

        var lastNotificationUpdateTime = System.currentTimeMillis()

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isRunning) {
                    timeElapsed += 50
                    Log.d("StopwatchService", "Time elapsed: $timeElapsed")

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNotificationUpdateTime >= 1000) { // Update notification every second
                        notificationManager.notify(1, createNotification(timeElapsed))
                        lastNotificationUpdateTime = currentTime
                    }

                    val intent = Intent("StopwatchUpdate").putExtra("time", timeElapsed)
                    sendBroadcast(intent)
                }
            }
        }, 0, 50)
    }


    private fun stopStopwatch() {
        Log.d("StopwatchService", "Stopwatch stopped")
        isRunning = false
        stopSelf()
    }


    private fun createNotification(time: Long): Notification {
        val notificationChannelId = "stopwatch_channel_id"

        // Create a Notification channel if necessary
        val name = getString(R.string.channel_name)  // Define a name for your channel
        val descriptionText = getString(R.string.channel_description)  // Channel description
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(notificationChannelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val formattedTime = String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(time),
            TimeUnit.MILLISECONDS.toMinutes(time) % 60,
            TimeUnit.MILLISECONDS.toSeconds(time) % 60)


        val builder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_stat_name)  // Set the icon
            .setContentTitle("Stopwatch Running")  // Set the title of the notification
            .setContentText("Time elapsed: $formattedTime")  // Set the text of the notification
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true);

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


}