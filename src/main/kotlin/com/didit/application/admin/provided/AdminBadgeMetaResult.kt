package com.didit.application.admin.provided

data class AdminBadgeMetaResult(
    val conditionTypes: List<AdminBadgeConditionTypeItem>,
    val categories: List<AdminBadgeCategoryItem>,
)

data class AdminBadgeConditionTypeItem(
    val conditionType: String,
    val label: String,
    val description: String,
    val params: List<AdminBadgeParamSpec>,
)

data class AdminBadgeParamSpec(
    val key: String,
    val label: String,
    val type: String,
    val defaultValue: Any?,
    val required: Boolean,
)

data class AdminBadgeCategoryItem(
    val category: String,
    val label: String,
)
