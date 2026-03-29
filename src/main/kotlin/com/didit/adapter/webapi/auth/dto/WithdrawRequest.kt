package com.didit.adapter.webapi.auth.dto

import com.didit.domain.auth.WithdrawalReason

data class WithdrawRequest(
    val reason: WithdrawalReason,
    val reasonDetail: String? = null,
)
