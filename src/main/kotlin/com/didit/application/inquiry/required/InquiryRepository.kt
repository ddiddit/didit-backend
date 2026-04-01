package com.didit.application.inquiry.required

import com.didit.domain.inquiry.Inquiry
import org.springframework.data.repository.Repository
import java.util.UUID

interface InquiryRepository : Repository<Inquiry, UUID> {
    fun save(inquiry: Inquiry): Inquiry

    fun findAllByUserIdAndDeletedAtIsNull(userId: UUID): List<Inquiry>

    fun findByIdAndDeletedAtIsNull(inquiryId: UUID): Inquiry?

    fun findAllByDeletedAtIsNullOrderByCreatedAtDesc(): List<Inquiry>
}
