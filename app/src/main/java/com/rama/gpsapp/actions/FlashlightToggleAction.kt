package com.rama.gpsapp.actions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class FlashlightToggleAction(context: Context) : GestureAction {
    private val cameraManager = context.applicationContext.getSystemService(
        Context.CAMERA_SERVICE
    ) as CameraManager
    private val torchStates = mutableMapOf<String, Boolean>()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchStates[cameraId] = enabled
        }
    }

    init {
        cameraManager.registerTorchCallback(torchCallback, null)
    }

    override suspend fun execute(): ActionResult {
        val cameraId = findFlashCameraId()
            ?: return ActionResult.Ignored("No flash-capable camera")

        return runCatching {
            val next = !(torchStates[cameraId] ?: false)
            cameraManager.setTorchMode(cameraId, next)
            ActionResult.Success
        }.getOrElse { throwable ->
            ActionResult.Failure(throwable.message ?: "Failed to toggle flashlight")
        }
    }

    private fun findFlashCameraId(): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
}
