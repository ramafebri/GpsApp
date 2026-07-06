package com.rama.gpsapp.actions

interface GestureAction {
    suspend fun execute(): ActionResult
}
