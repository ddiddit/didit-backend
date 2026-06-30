package com.didit.application.achievement

import com.didit.application.achievement.provided.UserLevelFinder
import com.didit.application.achievement.required.UserLevelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class UserLevelQueryService(
    private val userLevelRepository: UserLevelRepository,
) : UserLevelFinder {
    override fun getCurrentLevel(userId: UUID): Int = userLevelRepository.findByUserId(userId)?.currentLevel ?: DEFAULT_LEVEL

    companion object {
        private const val DEFAULT_LEVEL = 1
    }
}
