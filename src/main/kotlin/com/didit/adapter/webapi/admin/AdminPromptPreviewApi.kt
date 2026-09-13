package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminPromptPreview
import com.didit.application.admin.provided.AdminPromptPreviewCommand
import com.didit.application.admin.provided.AdminPromptPreviewResult
import com.didit.application.admin.provided.AdminPromptPreviewState
import com.didit.application.admin.provided.AdminPromptSource
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/prompts")
@RestController
class AdminPromptPreviewApi(
    private val adminPromptPreview: AdminPromptPreview,
) {
    @RequireAdmin
    @PostMapping("/preview-v2")
    fun preview(
        @RequestBody request: AdminPromptPreviewRequest,
    ): SuccessResponse<AdminPromptPreviewResult> =
        SuccessResponse.of(
            adminPromptPreview.preview(
                AdminPromptPreviewCommand(
                    job = request.job,
                    experience = request.experience,
                    promptSource = request.promptSource,
                    draftPrompt = request.draftPrompt,
                    priorState = request.priorState,
                    userMessageId = request.userMessageId,
                    message = request.message,
                ),
            ),
        )
}

data class AdminPromptPreviewRequest(
    val job: Job,
    val experience: UserExperience,
    val promptSource: AdminPromptSource,
    val draftPrompt: String?,
    val priorState: AdminPromptPreviewState?,
    val userMessageId: UUID,
    val message: String,
)
