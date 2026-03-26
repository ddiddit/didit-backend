package com.didit.domain.app

data class AppConfigUpdateRequest(
    val maintenanceMode: Boolean,
    val maintenanceMessage: String?,
    val minimumVersion: String,
)
