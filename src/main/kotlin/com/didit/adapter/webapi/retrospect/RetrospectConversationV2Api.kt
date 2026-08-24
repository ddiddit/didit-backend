package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.ConversationV2Response
import com.didit.adapter.webapi.retrospect.dto.FinishConversationV2Response
import com.didit.adapter.webapi.retrospect.dto.StartConversationV2Response
import com.didit.adapter.webapi.retrospect.dto.SubmitConversationMessageV2Request
import com.didit.adapter.webapi.retrospect.dto.SubmitConversationMessageV2Response
import com.didit.application.audit.Audit
import com.didit.application.audit.AuditAction
import com.didit.application.retrospect.provided.RetrospectiveConversationV2
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RetrospectConversationV2Api(
    private val conversation: RetrospectiveConversationV2,
) {
    @Audit(AuditAction.RETROSPECTIVE_STARTED)
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v2/retrospectives")
    fun start(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<StartConversationV2Response> = SuccessResponse.of(StartConversationV2Response.from(conversation.start(userId)))

    @RequireAuth
    @PostMapping("/api/v2/retrospectives/{retrospectiveId}/messages")
    fun submitMessage(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @Valid @RequestBody request: SubmitConversationMessageV2Request,
    ): SuccessResponse<SubmitConversationMessageV2Response> =
        SuccessResponse.of(
            SubmitConversationMessageV2Response.from(
                conversation.submitMessage(retrospectiveId, userId, request.clientMessageId, request.content, request.inputType),
            ),
        )

    @RequireAuth
    @GetMapping("/api/v2/retrospectives/{retrospectiveId}/conversation")
    fun getConversation(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<ConversationV2Response> =
        SuccessResponse.of(ConversationV2Response.from(conversation.getConversation(retrospectiveId, userId)))

    @RequireAuth
    @PostMapping("/api/v2/retrospectives/{retrospectiveId}/finish")
    fun finish(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<FinishConversationV2Response> =
        SuccessResponse.of(FinishConversationV2Response.from(conversation.finish(retrospectiveId, userId)))
}
