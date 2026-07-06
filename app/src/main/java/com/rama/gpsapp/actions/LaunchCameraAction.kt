package com.rama.gpsapp.actions

import android.content.Context
import android.content.Intent
import android.provider.MediaStore

class LaunchCameraAction(context: Context) : GestureAction {
    private val appContext = context.applicationContext

    override suspend fun execute(): ActionResult {
        val intent = buildIntent() ?: return ActionResult.Ignored("No camera app available")
        return runCatching {
            appContext.startActivity(intent)
            ActionResult.Success
        }.getOrElse { throwable ->
            ActionResult.Failure(throwable.message ?: "Failed to launch camera")
        }
    }

    private fun buildIntent(): Intent? {
        val preferred = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (preferred.resolveActivity(appContext.packageManager) != null) {
            return preferred
        }
        val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return fallback.takeIf { it.resolveActivity(appContext.packageManager) != null }
    }
}
