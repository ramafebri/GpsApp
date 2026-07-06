package com.rama.gpsapp.actions

sealed interface ActionResult {
    data object Success : ActionResult
    data class Ignored(val reason: String) : ActionResult
    data class RequiresPermission(val permission: PermissionType) : ActionResult
    data class Failure(val message: String) : ActionResult
}

enum class PermissionType {
    NotificationPolicyAccess
}
