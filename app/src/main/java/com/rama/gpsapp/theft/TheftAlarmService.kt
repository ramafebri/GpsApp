package com.rama.gpsapp.theft

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rama.gpsapp.MainActivity
import com.rama.gpsapp.data.AntiTheftSettingsRepository
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TheftAlarmService : LifecycleService() {
    private lateinit var settingsRepository: AntiTheftSettingsRepository
    private lateinit var engine: TheftGuardEngine
    private lateinit var alarmSoundPlayer: AlarmSoundPlayer

    private val isArmed = MutableStateFlow(false)
    private var startedCollectors = false
    private var unlockReceiverRegistered = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_USER_PRESENT) return
            lifecycleScope.launch {
                stopAlarmAndDisarm()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = AntiTheftSettingsRepository(applicationContext)
        engine = TheftGuardEngine(SensorHub(applicationContext))
        alarmSoundPlayer = AlarmSoundPlayer(applicationContext)
        ensureNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(STATUS_NOTIFICATION_ID, buildStatusNotification(false, TheftGuardStage.DISARMED))
        registerUnlockReceiverIfNeeded()
        if (!startedCollectors) {
            startedCollectors = true
            startCollectors()
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (unlockReceiverRegistered) {
            unregisterReceiver(unlockReceiver)
            unlockReceiverRegistered = false
        }
        alarmSoundPlayer.stop()
        TheftAlarmState.isActive.value = false
        TheftAlarmState.stage.value = TheftGuardStage.DISARMED
    }

    private fun registerUnlockReceiverIfNeeded() {
        if (unlockReceiverRegistered) return
        registerReceiver(
            unlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT)
        )
        unlockReceiverRegistered = true
    }

    private fun startCollectors() {
        lifecycleScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                isArmed.value = settings.armed
                if (!settings.armed) {
                    engine.disarm()
                    stopAlarm()
                    TheftAlarmState.stage.value = TheftGuardStage.DISARMED
                    updateStatusNotification()
                    stopSelf()
                    return@collectLatest
                }

                engine.detect(settings).collect { trigger ->
                    if (TheftAlarmState.isActive.value) return@collect
                    val started = alarmSoundPlayer.start(vibrateEnabled = settings.vibrateEnabled)
                    if (!started) return@collect
                    TheftAlarmState.isActive.value = true
                    TheftAlarmState.stage.value = TheftGuardStage.TRIGGERED
                    showAlarmNotification(trigger)
                    updateStatusNotification()
                }
            }
        }

        lifecycleScope.launch {
            engine.stage.collect { stage ->
                TheftAlarmState.stage.value = stage
                updateStatusNotification()
            }
        }
    }

    private suspend fun stopAlarmAndDisarm() {
        stopAlarm()
        settingsRepository.setArmed(false)
        engine.disarm()
        TheftAlarmState.stage.value = TheftGuardStage.DISARMED
        isArmed.value = false
        updateStatusNotification()
    }

    private fun stopAlarm() {
        alarmSoundPlayer.stop()
        TheftAlarmState.isActive.value = false
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    private fun showAlarmNotification(trigger: TheftTrigger) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALARM_NOTIFICATION_ID, buildAlarmNotification(trigger))
    }

    private fun updateStatusNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            STATUS_NOTIFICATION_ID,
            buildStatusNotification(isArmed.value, TheftAlarmState.stage.value)
        )
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val statusChannel = NotificationChannel(
            STATUS_CHANNEL_ID,
            "Anti-theft status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows anti-theft monitoring status."
        }

        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Anti-theft alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when anti-theft detection is triggered."
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(statusChannel)
        manager.createNotificationChannel(alarmChannel)
    }

    private fun buildStatusNotification(armed: Boolean, stage: TheftGuardStage): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stageText = when (stage) {
            TheftGuardStage.DISARMED -> "Disarmed"
            TheftGuardStage.ARMING -> "Arming..."
            TheftGuardStage.CALIBRATING -> "Calibrating stillness..."
            TheftGuardStage.MONITORING -> "Monitoring for movement"
            TheftGuardStage.TRIGGERED -> "Alarm triggered"
        }

        val content = if (armed) stageText else "Anti-theft is disarmed"

        return NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Anti-theft protection")
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun buildAlarmNotification(trigger: TheftTrigger): Notification {
        val openAlarmIntent = Intent(this, TheftAlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            41,
            openAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            42,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerLabel = when (trigger) {
            TheftTrigger.MOVEMENT -> "Movement detected"
            TheftTrigger.ROTATION -> "Rotation detected"
        }

        return NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Anti-theft alarm")
            .setContentText("$triggerLabel. Unlock phone to stop alarm.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .build()
    }

    companion object {
        private const val STATUS_CHANNEL_ID = "anti_theft_status_channel"
        private const val ALARM_CHANNEL_ID = "anti_theft_alarm_channel"
        private const val STATUS_NOTIFICATION_ID = 7301
        private const val ALARM_NOTIFICATION_ID = 7302

        fun start(context: Context) {
            val intent = Intent(context, TheftAlarmService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TheftAlarmService::class.java))
        }
    }
}
