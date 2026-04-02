package com.didit.adapter.webapi.inquiry

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.inquiry.dto.InquiryListResponse
import com.didit.adapter.webapi.inquiry.dto.InquiryRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.audit.Audit
import com.didit.application.audit.AuditAction
import com.didit.application.inquiry.provided.InquiryFinder
import com.didit.application.inquiry.provided.InquiryInfoFinder
import com.didit.application.inquiry.provided.InquiryModifier
import com.didit.application.inquiry.provided.InquiryRegister
import com.didit.domain.inquiry.InquiryRegisterRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inquiries")
class InquiryUserApi(
    private val inquiryInfoFinder: InquiryInfoFinder,
    private val inquiryRegister: InquiryRegister,
    private val inquiryFinder: InquiryFinder,
    private val inquiryModifier: InquiryModifier,
) {
    @RequireAuth
    @GetMapping("/me")
    fun info(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<String?> = SuccessResponse.of(inquiryInfoFinder.findEmail(userId))

    @Audit(AuditAction.INQUIRY_REGISTERED)
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    fun register(
        @CurrentUserId userId: UUID,
        @RequestBody request: InquiryRequest,
    ) {
        inquiryRegister.register(
            InquiryRegisterRequest(
                userId = userId,
                email = "",
                type = request.type,
                typeEtc = request.typeEtc,
                content = request.content,
                isAgreed = request.isAgreed,
            ),
            userId,
        )
    }

    @RequireAuth
    @GetMapping
    fun findAll(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<InquiryListResponse>> {
        val inquiries = inquiryFinder.findAll(userId)

        return SuccessResponse.of(inquiries.map { InquiryListResponse.from(it) })
    }

    @Audit(AuditAction.INQUIRY_DELETED, targetType = "INQUIRY")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{inquiryId}")
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable inquiryId: UUID,
    ) {
        inquiryModifier.delete(inquiryId, userId)
    }
}
