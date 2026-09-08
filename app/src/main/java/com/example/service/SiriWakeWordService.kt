package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class SiriWakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "siri_wake_word_channel"
        const val NOTIFICATION_ID = 7001
        const val ACTION_START = "com.example.service.START"
        const val ACTION_STOP = "com.example.service.STOP"
        const val ACTION_WAKE_WORD_DETECTED = "com.example.service.WAKE_WORD_DETECTED"
        const val EXTRA_WAKE_WORD_TRIGGERED = "extra_wake_word_triggered"

        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, SiriWakeWordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SiriWakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var audioRecord: AudioRecord? = null
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireCpuWakeLock()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true

        startWakeWordListeningLoop()

        return START_STICKY
    }

    private fun startWakeWordListeningLoop() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)

                var consecutiveVoiceFrames = 0
                var cooldownFrames = 0

                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Calculate Root Mean Square energy (audio loudness)
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)

                        if (cooldownFrames > 0) {
                            cooldownFrames--
                            continue
                        }

                        // Speech energy threshold for wake sound detection
                        if (rms > 2500) {
                            consecutiveVoiceFrames++
                            // When sustained voice pattern matches wake word length
                            if (consecutiveVoiceFrames >= 4) {
                                consecutiveVoiceFrames = 0
                                cooldownFrames = 25 // 2.5 second cooldown to avoid retriggering
                                onWakeWordDetected()
                            }
                        } else {
                            if (consecutiveVoiceFrames > 0) {
                                consecutiveVoiceFrames--
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun onWakeWordDetected() {
        // 1. Play haptic vibration
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 60, 70), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 70, 60, 70), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Play Siri double tone
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 120)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Wake up screen
        wakeUpScreen()

        // 4. Launch or Bring MainActivity to front
        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_WAKE_WORD_TRIGGERED, true)
            }
            startActivity(launchIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Send local broadcast
        val broadcast = Intent(ACTION_WAKE_WORD_DETECTED).apply {
            setPackage(packageName)
        }
        sendBroadcast(broadcast)
    }

    private fun wakeUpScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "SiriAssistant:ScreenWakeLock"
            )
            screenWakeLock?.acquire(3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireCpuWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SiriAssistant:BackgroundWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseCpuWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SiriWakeWordService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Siri Voice Assistant")
            .setContentText("Listening for \"Hey Siri\" command (Offline & Screen-off active)…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Siri", pendingStop)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Siri Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps voice detection running when phone screen is locked"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        releaseCpuWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
