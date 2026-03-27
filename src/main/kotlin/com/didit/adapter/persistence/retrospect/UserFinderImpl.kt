package com.didit.adapter.persistence.retrospect

import com.didit.application.retrospect.required.UserFinder
import com.didit.domain.auth.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserFinderImpl(
    private val springDataRepository: UserSpringDataRepository,
) : UserFinder {
    override fun findByIdOrThrow(userId: UUID): User =
        springDataRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
}

interface UserSpringDataRepository : JpaRepository<User, UUID>
