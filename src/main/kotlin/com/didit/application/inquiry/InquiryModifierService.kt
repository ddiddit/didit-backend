package com.didit.application.inquiry

import com.didit.application.inquiry.exception.InquiryNotFoundException
import com.didit.application.inquiry.provided.InquiryModifier
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.domain.inquiry.Inquiry
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class InquiryModifierService(
    private val inquiryRepository: InquiryRepository,
    private val notificationHistoryRegister: NotificationHistoryRegister,
) : InquiryModifier {
    companion object {
        private val logger = LoggerFactory.getLogger(InquiryModifierService::class.java)

        const val INQUIRY_ANSWERED_TITLE = "문의 답변 알림"
        const val INQUIRY_ANSWERED_BODY = "문의하신 내용에 답변이 등록되었어요. 지금 확인해 보세요."
    }

    @Transactional
    override fun answer(
        inquiryId: UUID,
        adminId: UUID,
        answer: String,
    ): Inquiry {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId) ?: throw InquiryNotFoundException()

        inquiry.answer(adminId, answer)

        logger.info("문의 답변 등록 - inquiryId: $inquiryId, adminId: $adminId")

        notificationHistoryRegister.save(
            NotificationHistoryCreateRequest(
                userId = inquiry.userId,
                type = NotificationType.INQUIRY_ANSWERED,
                title = INQUIRY_ANSWERED_TITLE,
                body = INQUIRY_ANSWERED_BODY,
            ),
        )

        return inquiryRepository.save(inquiry)
    }

    @Transactional
    override fun updateAnswer(
        inquiryId: UUID,
        adminId: UUID,
        answer: String,
    ): Inquiry {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId) ?: throw InquiryNotFoundException()

        inquiry.updateAnswer(answer, adminId)

        logger.info("문의 답변 수정 - inquiryId: $inquiryId, adminId: $adminId")

        return inquiryRepository.save(inquiry)
    }

    override fun deleteAnswer(
        inquiryId: UUID,
        adminId: UUID,
    ): Inquiry {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId) ?: throw InquiryNotFoundException()

        inquiry.deleteAnswer(adminId)

        logger.info("문의 답변 삭제 - inquiryId: $inquiryId, adminId: $adminId")

        return inquiryRepository.save(inquiry)
    }

    @Transactional
    override fun delete(
        inquiryId: UUID,
        userId: UUID,
    ) {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId) ?: throw InquiryNotFoundException()

        inquiry.delete(userId)

        logger.info("문의 삭제 - inquiryId: $inquiryId, userId: $userId")
    }
}
