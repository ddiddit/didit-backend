package com.didit.adapter.webapi.app.dto

import com.didit.domain.app.AppConfigUpdateRequest

data class AppConfigUpdateRequest(
    val maintenanceMode: Boolean,
    val maintenanceMessage: String?,
    val minimumVersion: String,
) {
    fun toDomain() =
        AppConfigUpdateRequest(
            maintenanceMode = maintenanceMode,
            maintenanceMessage = maintenanceMessage,
            minimumVersion = minimumVersion,
        )
}
