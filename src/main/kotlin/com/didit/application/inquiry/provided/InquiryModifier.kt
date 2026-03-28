package com.didit.application.inquiry.provided

import java.util.UUID

interface InquiryModifier {
    fun delete(
        inquiryId: UUID,
        userId: UUID,
    )
}
