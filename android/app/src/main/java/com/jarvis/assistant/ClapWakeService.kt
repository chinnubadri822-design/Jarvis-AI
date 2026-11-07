package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import android.content.pm.ServiceInfo
import kotlin.concurrent.thread
import kotlin.math.abs

class ClapWakeService : Service() {
    @Volatile private var running = false
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private var lastClapAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) {
            createChannel()
            val notification = buildNotification("Microphone is active for clap detection")
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            startCapture()
        }
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "JARVIS microphone", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(message: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, ClapWakeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("JARVIS is listening for a clap")
            .setContentText(message)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", stopIntent)
        return builder.build()
    }

    private fun startCapture() {
        val sampleRate = 16000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            stopSelf()
            return
        }
        val audio = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, 4096)
        )
        if (audio.state != AudioRecord.STATE_INITIALIZED) {
            audio.release()
            stopSelf()
            return
        }
        recorder = audio
        running = true
        audio.startRecording()
        worker = thread(name = "JarvisClapDetector") {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val buffer = ShortArray(1024)
            while (running) {
                val count = audio.read(buffer, 0, buffer.size)
                if (count > 0) {
                    var peak = 0
                    var sum = 0.0
                    for (i in 0 until count) {
                        val value = abs(buffer[i].toInt())
                        if (value > peak) peak = value
                        sum += value.toDouble() * value.toDouble()
                    }
                    val rms = kotlin.math.sqrt(sum / count).toInt()
                    val now = SystemClock.elapsedRealtime()
                    // Conservative transient threshold; other sharp sounds can also trigger it.
                    if (peak > 12000 && rms > 1800 && now - lastClapAt > 1800) {
                        lastClapAt = now
                        val manager = getSystemService(NotificationManager::class.java)
                        manager.notify(NOTIFICATION_ID, buildNotification("Clap detected. Tap to open JARVIS."))
                    }
                }
            }
        }
    }

    private fun stopCapture() {
        running = false
        try { recorder?.stop() } catch (_: Exception) {}
        worker?.join(500)
        recorder?.release()
        recorder = null
        worker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.jarvis.assistant.START_CLAP"
        const val ACTION_STOP = "com.jarvis.assistant.STOP_CLAP"
        private const val CHANNEL_ID = "jarvis_mic"
        private const val NOTIFICATION_ID = 812
    }
}
