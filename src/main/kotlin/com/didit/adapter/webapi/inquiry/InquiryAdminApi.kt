package com.didit.adapter.webapi.inquiry

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.inquiry.dto.InquiryAnswerRequest
import com.didit.adapter.webapi.inquiry.dto.InquiryResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.inquiry.provided.InquiryModifier
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/inquiries")
@RestController
class InquiryAdminApi(
    private val inquiryModifier: InquiryModifier,
) {
    @RequireAdmin
    @PostMapping("/{inquiryId}/answer")
    fun answer(
        @CurrentAdminId adminId: UUID,
        @PathVariable inquiryId: UUID,
        @RequestBody request: InquiryAnswerRequest,
    ): SuccessResponse<InquiryResponse> {
        val inquiry = inquiryModifier.answer(inquiryId, adminId, request.answer)

        return SuccessResponse.of(InquiryResponse.of(inquiry))
    }

    @RequireAdmin
    @PatchMapping("/{inquiryId}/answer")
    fun updateAnswer(
        @CurrentAdminId adminId: UUID,
        @PathVariable inquiryId: UUID,
        @RequestBody request: InquiryAnswerRequest,
    ): SuccessResponse<InquiryResponse> {
        val inquiry = inquiryModifier.updateAnswer(inquiryId, adminId, request.answer)
        return SuccessResponse.of(InquiryResponse.of(inquiry))
    }
}
