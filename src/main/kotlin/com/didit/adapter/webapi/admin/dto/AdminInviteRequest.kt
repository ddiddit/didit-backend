package com.didit.adapter.webapi.admin.dto

import com.didit.domain.admin.AdminPosition

data class AdminInviteRequest(
    val email: String,
    val position: AdminPosition,
)
