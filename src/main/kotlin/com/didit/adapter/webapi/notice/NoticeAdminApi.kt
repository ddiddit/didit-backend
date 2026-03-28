package com.didit.adapter.webapi.notice

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.notice.dto.NoticeAdminDetailResponse
import com.didit.adapter.webapi.notice.dto.NoticeAdminListResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.notice.provided.NoticeFinder
import com.didit.application.notice.provided.NoticeModifier
import com.didit.application.notice.provided.NoticeRegister
import com.didit.domain.notice.NoticeRegisterRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/notices")
@RestController
class NoticeAdminApi(
    private val noticeRegister: NoticeRegister,
    private val noticeFinder: NoticeFinder,
    private val noticeModifier: NoticeModifier,
) {
    @RequireAdmin
    @GetMapping
    fun findAll(
        @CurrentAdminId adminId: UUID,
    ): SuccessResponse<List<NoticeAdminListResponse>> {
        val notices = noticeFinder.findAllForAdmin()
        return SuccessResponse.of(notices.map { NoticeAdminListResponse.from(it) })
    }

    @RequireAdmin
    @GetMapping("/{noticeId}")
    fun findById(
        @CurrentAdminId adminId: UUID,
        @PathVariable noticeId: UUID,
    ): SuccessResponse<NoticeAdminDetailResponse> {
        val notice = noticeFinder.findByIdForAdmin(noticeId)
        return SuccessResponse.of(NoticeAdminDetailResponse.of(notice))
    }

    @RequireAdmin
    @PostMapping
    fun register(
        @CurrentAdminId adminId: UUID,
        @RequestBody request: NoticeRegisterRequest,
    ): SuccessResponse<NoticeAdminDetailResponse> {
        val notice =
            noticeRegister.register(
                NoticeRegisterRequest(
                    title = request.title,
                    content = request.content,
                    status = request.status,
                    sendPush = request.sendPush,
                ),
                adminId = adminId,
            )

        return SuccessResponse.of(NoticeAdminDetailResponse.of(notice))
    }

    @RequireAdmin
    @PutMapping("/{noticeId}")
    fun update(
        @CurrentAdminId adminId: UUID,
        @PathVariable noticeId: UUID,
        @RequestBody request: NoticeRegisterRequest,
    ): SuccessResponse<NoticeAdminDetailResponse> {
        val notice =
            noticeModifier.modify(
                NoticeRegisterRequest(
                    title = request.title,
                    content = request.content,
                    status = request.status,
                    sendPush = request.sendPush,
                ),
                noticeId = noticeId,
                adminId = adminId,
            )
        return SuccessResponse.of(NoticeAdminDetailResponse.of(notice))
    }

    @RequireAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{noticeId}")
    fun delete(
        @CurrentAdminId adminId: UUID,
        @PathVariable noticeId: UUID,
    ) {
        noticeModifier.delete(noticeId, adminId)
    }
}
