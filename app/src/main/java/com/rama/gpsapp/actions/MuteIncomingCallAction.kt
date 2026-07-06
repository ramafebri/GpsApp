package com.rama.gpsapp.actions

import android.content.Context
import android.media.AudioManager
import android.app.NotificationManager
import com.rama.gpsapp.call.CallState

class MuteIncomingCallAction(
    context: Context,
    private val currentCallState: () -> CallState
) : GestureAction {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun execute(): ActionResult {
        if (currentCallState() != CallState.RINGING) {
            return ActionResult.Ignored("No incoming ringing call")
        }
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return ActionResult.RequiresPermission(PermissionType.NotificationPolicyAccess)
        }
        return runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_RING,
                AudioManager.ADJUST_MUTE,
                0
            )
            ActionResult.Success
        }.getOrElse { throwable ->
            ActionResult.Failure(throwable.message ?: "Unable to mute ring stream")
        }
    }
}
