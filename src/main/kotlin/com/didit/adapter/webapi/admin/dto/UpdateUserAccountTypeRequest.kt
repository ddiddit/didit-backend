package com.didit.adapter.webapi.admin.dto

import com.didit.domain.auth.UserAccountType

data class UpdateUserAccountTypeRequest(
    val accountType: UserAccountType,
)
