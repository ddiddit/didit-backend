package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireSuperAdmin
import com.didit.adapter.webapi.admin.dto.AdminInviteRequest
import com.didit.adapter.webapi.admin.dto.AdminListResponse
import com.didit.adapter.webapi.admin.dto.AdminRegisterRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminFinder
import com.didit.application.admin.provided.AdminInviteManager
import com.didit.application.admin.provided.AdminManager
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin")
@RestController
class AdminApi(
    private val adminFinder: AdminFinder,
    private val adminManager: AdminManager,
    private val adminInviteManager: AdminInviteManager,
) {
    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/invite")
    fun invite(
        @CurrentAdminId adminId: UUID,
        @Valid @RequestBody request: AdminInviteRequest,
    ) {
        adminInviteManager.invite(
            invitedBy = adminId,
            email = request.email,
            position = request.position,
        )
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun register(
        @RequestParam token: UUID,
        @Valid @RequestBody request: AdminRegisterRequest,
    ) {
        adminInviteManager.register(
            token = token,
            email = request.email,
            password = request.password,
        )
    }

    @RequireSuperAdmin
    @GetMapping
    fun findAll(): SuccessResponse<List<AdminListResponse>> {
        val admins = adminFinder.findAll()
        return SuccessResponse.of(admins.map { AdminListResponse.from(it) })
    }

    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{adminId}/approve")
    fun approve(
        @PathVariable adminId: UUID,
    ) {
        adminManager.approve(adminId)
    }

    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{adminId}/reject")
    fun reject(
        @PathVariable adminId: UUID,
    ) {
        adminManager.reject(adminId)
    }

    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{adminId}")
    fun delete(
        @PathVariable adminId: UUID,
    ) {
        adminManager.delete(adminId)
    }
}
