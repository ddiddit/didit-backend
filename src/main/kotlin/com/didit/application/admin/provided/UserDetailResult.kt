package com.didit.application.admin.provided

import com.didit.application.audit.AuditEntry

data class UserDetailResult(
    val profile: UserSummary,
    val timeline: List<AuditEntry>,
)
