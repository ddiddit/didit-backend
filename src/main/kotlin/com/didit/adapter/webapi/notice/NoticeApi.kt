package com.didit.adapter.webapi.notice

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.notice.dto.NoticeDetailResponse
import com.didit.adapter.webapi.notice.dto.NoticeListResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.notice.provided.NoticeFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/notices")
@RestController
class NoticeApi(
    private val noticeFinder: NoticeFinder,
) {
    @RequireAuth
    @GetMapping
    fun getNotices(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<NoticeListResponse>> {
        val notices = noticeFinder.findAll()
        return SuccessResponse.of(notices.map { NoticeListResponse.from(it) })
    }

    @GetMapping("/{noticeId}")
    fun getNoticeDetail(
        @CurrentUserId userId: UUID,
        @PathVariable noticeId: UUID,
    ): SuccessResponse<NoticeDetailResponse> {
        val notice = noticeFinder.findById(noticeId)
        return SuccessResponse.of(NoticeDetailResponse.of(notice))
    }
}
