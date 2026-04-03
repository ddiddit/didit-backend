package com.didit.application.retrospect.provided

import java.util.UUID

interface SearchHistoryRegister {
    fun register(
        userId: UUID,
        keyword: String,
    )
}
