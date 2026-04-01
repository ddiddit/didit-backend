package com.didit.application.inquiry

import com.didit.application.inquiry.exception.InquiryNotFoundException
import com.didit.application.inquiry.provided.InquiryModifier
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.domain.inquiry.Inquiry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class InquiryModifierService(
    private val inquiryRepository: InquiryRepository,
) : InquiryModifier {
    @Transactional
    override fun answer(
        inquiryId: UUID,
        adminId: UUID,
        answer: String,
    ): Inquiry {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId) ?: throw InquiryNotFoundException()

        inquiry.answer(adminId, answer)
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
        return inquiryRepository.save(inquiry)
    }

    @Transactional
    override fun delete(
        inquiryId: UUID,
        userId: UUID,
    ) {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId) ?: throw InquiryNotFoundException()

        inquiry.delete(userId)
    }
}
