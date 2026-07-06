package com.rama.gpsapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rama.gpsapp.MainActivity
import com.rama.gpsapp.actions.FlashlightToggleAction
import com.rama.gpsapp.actions.GestureAction
import com.rama.gpsapp.actions.LaunchCameraAction
import com.rama.gpsapp.actions.MuteIncomingCallAction
import com.rama.gpsapp.call.CallState
import com.rama.gpsapp.call.CallStateMonitor
import com.rama.gpsapp.data.GestureSettingsRepository
import com.rama.gpsapp.gesture.GestureEngine
import com.rama.gpsapp.gesture.GestureType
import com.rama.gpsapp.sensor.SensorHub
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GestureShortcutService : LifecycleService() {
    private val currentCallState = MutableStateFlow(CallState.IDLE)
    private lateinit var settingsRepository: GestureSettingsRepository
    private lateinit var gestureEngine: GestureEngine
    private lateinit var callStateMonitor: CallStateMonitor
    private lateinit var actionMap: Map<GestureType, GestureAction>
    private var startedCollectors = false

    override fun onCreate() {
        super.onCreate()
        settingsRepository = GestureSettingsRepository(applicationContext)
        gestureEngine = GestureEngine(SensorHub(applicationContext))
        callStateMonitor = CallStateMonitor(applicationContext)
        actionMap = mapOf(
            GestureType.FLIP to MuteIncomingCallAction(applicationContext) { currentCallState.value },
            GestureType.SHAKE to FlashlightToggleAction(applicationContext),
            GestureType.TWIST to LaunchCameraAction(applicationContext)
        )
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!startedCollectors) {
            startedCollectors = true
            startCollectors()
        }
        return Service.START_STICKY
    }

    private fun startCollectors() {
        lifecycleScope.launch {
            callStateMonitor.callState().collect { state ->
                currentCallState.value = state
            }
        }

        lifecycleScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                if (!settings.serviceEnabled) {
                    stopSelf()
                    return@collectLatest
                }
                gestureEngine.detect(settings).collect { gesture ->
                    actionMap[gesture]?.execute()
                }
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gesture Shortcuts",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps gesture detection active in the background."
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Gesture shortcuts active")
            .setContentText("Listening for shake, flip, and twist gestures.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gesture_shortcuts_channel"
        private const val NOTIFICATION_ID = 7001

        fun start(context: Context) {
            val intent = Intent(context, GestureShortcutService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GestureShortcutService::class.java))
        }
    }
}
