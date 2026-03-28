package com.didit.application.inquiry.provided

import com.didit.domain.inquiry.Inquiry
import java.util.UUID

interface InquiryFinder {
    fun findAll(userId: UUID): List<Inquiry>
}
