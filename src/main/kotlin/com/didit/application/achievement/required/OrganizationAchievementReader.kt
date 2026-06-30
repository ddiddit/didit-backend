package com.didit.application.achievement.required

import java.util.UUID

interface OrganizationAchievementReader {
    fun countProjects(userId: UUID): Int

    fun countProjectAssignedRetros(userId: UUID): Int

    fun maxRetroCountInOneProject(userId: UUID): Int
}
