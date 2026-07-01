package com.didit.application.admin.provided

data class AdminBadgeCreateCommand(
    val name: String,
    val description: String,
    val category: String,
    val conditionType: String,
    val threshold: Int,
    val params: Map<String, Any>? = null,
    val iconUrl: String? = null,
    val congratsTitle: String? = null,
    val congratsMessage: String? = null,
)

data class AdminBadgeUpdateCommand(
    val name: String,
    val description: String,
    val category: String,
    val conditionType: String,
    val threshold: Int,
    val params: Map<String, Any>? = null,
    val iconUrl: String? = null,
    val congratsTitle: String? = null,
    val congratsMessage: String? = null,
)
