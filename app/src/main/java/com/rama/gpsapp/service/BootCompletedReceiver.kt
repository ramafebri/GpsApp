package com.rama.gpsapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rama.gpsapp.data.AntiTheftSettingsRepository
import com.rama.gpsapp.data.GestureSettingsRepository
import com.rama.gpsapp.theft.TheftAlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val repository = GestureSettingsRepository(context.applicationContext)
                val antiTheftRepository = AntiTheftSettingsRepository(context.applicationContext)
                val settings = repository.settings.first()
                val antiTheftSettings = antiTheftRepository.settings.first()
                if (settings.serviceEnabled) {
                    GestureShortcutService.start(context.applicationContext)
                }
                if (antiTheftSettings.armed) {
                    TheftAlarmService.start(context.applicationContext)
                }
            }
            pendingResult.finish()
        }
    }
}
