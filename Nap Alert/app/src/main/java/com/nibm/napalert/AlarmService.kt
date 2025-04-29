package com.nibm.napalert

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.PowerManager
import android.os.IBinder
import android.widget.Toast

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound).apply {
            isLooping = true
        }
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start playing the alarm sound
        mediaPlayer?.start()
        Toast.makeText(this, "Alarm triggered! Go to the app to stop it.", Toast.LENGTH_LONG).show()
        return START_STICKY
    }

    override fun onDestroy() {
        // Stop the alarm sound and release resources
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "napalert:AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
}