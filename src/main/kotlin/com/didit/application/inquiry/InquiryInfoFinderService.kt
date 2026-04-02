package com.didit.application.inquiry

import com.didit.application.auth.provided.UserFinder
import com.didit.application.inquiry.provided.InquiryInfoFinder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class InquiryInfoFinderService(
    private val userFinder: UserFinder,
) : InquiryInfoFinder {
    override fun findEmail(userId: UUID): String {
        val user = userFinder.findByIdOrThrow(userId)

        return requireNotNull(user.email) { "유저 이메일은 null일 수 없습니다. userId=$userId" }
    }
}
