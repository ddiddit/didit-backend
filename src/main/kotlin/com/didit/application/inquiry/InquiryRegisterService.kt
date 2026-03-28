package com.didit.application.inquiry

import com.didit.application.auth.provided.UserFinder
import com.didit.application.inquiry.provided.InquiryRegister
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryRegisterRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class InquiryRegisterService(
    private val inquiryRepository: InquiryRepository,
    private val userFinder: UserFinder,
) : InquiryRegister {
    @Transactional
    override fun register(
        request: InquiryRegisterRequest,
        userId: UUID,
    ): Inquiry {
        val user = userFinder.findByIdOrThrow(userId)
        val email = requireNotNull(user.email) { "유저 이메일은 null일 수 없습니다. userId=$userId" }

        val inquiry =
            Inquiry.register(request, userId, email)

        return inquiryRepository.save(inquiry)
    }
}
