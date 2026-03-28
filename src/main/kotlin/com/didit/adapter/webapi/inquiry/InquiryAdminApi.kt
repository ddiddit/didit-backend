package com.didit.adapter.webapi.inquiry

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.inquiry.dto.InquiryAdminListResponse
import com.didit.adapter.webapi.inquiry.dto.InquiryAnswerRequest
import com.didit.adapter.webapi.inquiry.dto.InquiryResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.inquiry.provided.InquiryFinder
import com.didit.application.inquiry.provided.InquiryModifier
import org.springframework.web.bind.annotation.GetMapping
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
    private val inquiryFinder: InquiryFinder,
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

    @RequireAdmin
    @GetMapping
    fun findAll(
        @CurrentAdminId adminId: UUID,
    ): SuccessResponse<List<InquiryAdminListResponse>> {
        val inquiries = inquiryFinder.findAll()

        return SuccessResponse.of(inquiries.map { InquiryAdminListResponse.from(it) })
    }

    @RequireAdmin
    @GetMapping("/{inquiryId}")
    fun get(
        @CurrentAdminId adminId: UUID,
        @PathVariable inquiryId: UUID,
    ): SuccessResponse<InquiryResponse> {
        val inquiry = inquiryFinder.findById(inquiryId)

        return SuccessResponse.of(InquiryResponse.of(inquiry))
    }
}
