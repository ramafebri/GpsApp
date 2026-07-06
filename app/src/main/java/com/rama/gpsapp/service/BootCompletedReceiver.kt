package com.rama.gpsapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rama.gpsapp.data.GestureSettingsRepository
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
                val settings = repository.settings.first()
                if (settings.serviceEnabled) {
                    GestureShortcutService.start(context.applicationContext)
                }
            }
            pendingResult.finish()
        }
    }
}
