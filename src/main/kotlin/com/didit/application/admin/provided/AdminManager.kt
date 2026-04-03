package com.didit.application.admin.provided

import java.util.UUID

interface AdminManager {
    fun approve(adminId: UUID)

    fun reject(adminId: UUID)

    fun delete(adminId: UUID)
}
