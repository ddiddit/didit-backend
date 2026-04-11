package com.didit.adapter.webapi.retrospect.dto

import com.didit.adapter.webapi.organization.dto.ProjectListResponse
import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.domain.retrospect.RetroStatus
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectWithProjectAndTagResponse(
    val id: UUID,
    val title: String?,
    val status: RetroStatus,
    val content: ContentResponse?,
    val completedAt: LocalDateTime?,
    val project: ProjectListResponse?,
    val tags: List<TagListResponse>,
) {
    companion object {
        fun from(result: RetrospectiveDetailResult): RetrospectWithProjectAndTagResponse =
            RetrospectWithProjectAndTagResponse(
                id = result.retrospective.id,
                title = result.retrospective.title,
                status = result.retrospective.status,
                content =
                    result.retrospective.summary?.let {
                        ContentResponse(
                            summary = it.summary,
                            feedback = it.feedback,
                            insight = it.insight,
                            doneWork = it.doneWork,
                            blockedPoint = it.blockedPoint.split("\n"),
                            solutionProcess = it.solutionProcess.split("\n"),
                            lessonLearned = it.lessonLearned.split("\n"),
                            nextAction = it.nextAction.split("\n"),
                        )
                    },
                completedAt = result.retrospective.completedAt,
                project = result.project?.let { ProjectListResponse.from(it) },
                tags = result.tags.map { TagListResponse.from(it) },
            )
    }
}
