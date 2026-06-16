package com.didit.application.admin.provided

data class AdminWithdrawalStatsResult(
    val total: Long,
    val breakdown: List<WithdrawalReasonCount>,
)

data class WithdrawalReasonCount(
    val reason: String,
    val count: Long,
    val percentage: Double,
)
