package com.didit.application.inquiry.provided

import java.util.UUID

interface InquiryInfoFinder {
    fun findEmail(userId: UUID): String?
}
