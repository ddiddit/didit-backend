package com.didit.application.inquiry

import com.didit.application.auth.provided.UserFinder
import com.didit.application.inquiry.provided.InquiryRegister
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryRegisterRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class InquiryRegisterService(
    private val inquiryRepository: InquiryRepository,
    private val userFinder: UserFinder,
) : InquiryRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(InquiryRegisterService::class.java)
    }

    @Transactional
    override fun register(
        request: InquiryRegisterRequest,
        userId: UUID,
    ): Inquiry {
        val user = userFinder.findByIdOrThrow(userId)

        val email = requireNotNull(user.email) { "유저 이메일은 null일 수 없습니다. userId=$userId" }

        val inquiry = Inquiry.register(request, userId, email)

        val saved = inquiryRepository.save(inquiry)

        logger.info("문의 등록 - userId: $userId, type: ${request.type}")

        return saved
    }
}
