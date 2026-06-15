package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.admin.annotation.RequireSuperAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.AdminPromptService
import com.didit.application.admin.provided.AdminPromptFinder
import com.didit.application.admin.provided.AdminPromptResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/prompts")
@RestController
class AdminPromptApi(
    private val adminPromptFinder: AdminPromptFinder,
    private val adminPromptService: AdminPromptService,
) {
    @RequireAdmin
    @GetMapping
    fun findAll(): SuccessResponse<List<AdminPromptResult>> = SuccessResponse.of(adminPromptFinder.findAll())

    @RequireAdmin
    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: UUID,
    ): SuccessResponse<AdminPromptResult> = SuccessResponse.of(adminPromptFinder.findById(id))

    @RequireSuperAdmin
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: PromptUpdateRequest,
        @CurrentAdminId adminId: UUID,
    ): SuccessResponse<AdminPromptResult> = SuccessResponse.of(adminPromptService.update(id, request.content, adminId.toString()))
}

data class PromptUpdateRequest(
    val content: String,
)
