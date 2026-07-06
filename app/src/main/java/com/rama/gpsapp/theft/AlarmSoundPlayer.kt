package com.rama.gpsapp.theft

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmSoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var originalAlarmVolume: Int? = null

    fun start(vibrateEnabled: Boolean): Boolean {
        if (mediaPlayer?.isPlaying == true) return true

        val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(
            appContext,
            RingtoneManager.TYPE_ALARM
        ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        return runCatching {
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(appContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }

            if (vibrateEnabled) {
                startVibration()
            }
        }.onFailure {
            stop()
        }.isSuccess
    }

    fun stop() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        runCatching {
            vibrator?.cancel()
            vibrator = null
        }

        val restoreVolume = originalAlarmVolume
        if (restoreVolume != null) {
            runCatching {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, restoreVolume, 0)
            }
        }
        originalAlarmVolume = null
    }

    private fun startVibration() {
        val resolvedVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator = resolvedVibrator
        val pattern = longArrayOf(0, 600, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            resolvedVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            resolvedVibrator.vibrate(pattern, 0)
        }
    }
}
