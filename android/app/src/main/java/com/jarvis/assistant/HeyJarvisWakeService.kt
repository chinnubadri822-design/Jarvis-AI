package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.openwakeword.OpenWakeWord

/**
 * Opt-in, on-device "Hey JARVIS" listener.
 * Audio is processed locally by the bundled ONNX wake-word engine.
 */
class HeyJarvisWakeService : Service() {
    private var detector: OpenWakeWord? = null
    @Volatile private var detectionShown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_ACKNOWLEDGE) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (detector == null) startListening()
        return START_NOT_STICKY
    }

    private fun startListening() {
        createChannel()
        val notification = buildNotification(
            "Microphone active. Say “Hey JARVIS”. Audio stays on this device."
        )
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val wakeDetector = OpenWakeWord.Builder(applicationContext)
                .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                .setThreshold(0.55f)
                .setDebounceMs(2500L)
                .build()
            detector = wakeDetector
            wakeDetector.start { score ->
                if (detectionShown) return@start
                detectionShown = true
                Log.i(TAG, "Hey JARVIS detected (score=$score)")
                try { wakeDetector.stop() } catch (e: Exception) { Log.w(TAG, "Could not pause detector", e) }
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(
                    NOTIFICATION_ID,
                    buildNotification("Wake phrase detected. Tap this notification to open JARVIS.")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start offline wake-word engine", e)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification("Wake-word engine failed. Open JARVIS and try again."))
            stopSelf()
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "JARVIS wake-word listener", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(message: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
            .putExtra(EXTRA_WAKE_DETECTED, detectionShown)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openPending = PendingIntent.getActivity(
            this, 71, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            this, 72, Intent(this, HeyJarvisWakeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(if (detectionShown) "JARVIS heard you" else "JARVIS is listening")
            .setContentText(message)
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", stopPending)
            .build()
    }

    override fun onDestroy() {
        try { detector?.release() } catch (e: Exception) { Log.w(TAG, "Detector release failed", e) }
        detector = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.jarvis.assistant.START_HEY_JARVIS"
        const val ACTION_STOP = "com.jarvis.assistant.STOP_HEY_JARVIS"
        const val ACTION_ACKNOWLEDGE = "com.jarvis.assistant.ACK_HEY_JARVIS"
        const val EXTRA_WAKE_DETECTED = "com.jarvis.assistant.WAKE_DETECTED"
        private const val TAG = "JarvisWakeWord"
        private const val CHANNEL_ID = "jarvis_wake_word"
        private const val NOTIFICATION_ID = 813
    }
}
