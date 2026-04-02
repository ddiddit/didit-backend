package com.didit.application.inquiry

import com.didit.application.inquiry.exception.InquiryNotFoundException
import com.didit.application.inquiry.provided.InquiryFinder
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.domain.inquiry.Inquiry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class InquiryFinderService(
    private val inquiryRepository: InquiryRepository,
) : InquiryFinder {
    override fun findAll(userId: UUID): List<Inquiry> {
        val inquiries = inquiryRepository.findAllByUserIdAndDeletedAtIsNull(userId)

        return inquiries
    }

    override fun findAll(): List<Inquiry> {
        val inquiries = inquiryRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()

        return inquiries
    }

    override fun findById(id: UUID): Inquiry {
        val inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(id) ?: throw InquiryNotFoundException()

        return inquiry
    }
}
