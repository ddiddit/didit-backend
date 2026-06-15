package com.didit.application.admin.provided

import java.util.UUID

interface AdminPromptFinder {
    fun findAll(): List<AdminPromptResult>

    fun findById(id: UUID): AdminPromptResult
}
