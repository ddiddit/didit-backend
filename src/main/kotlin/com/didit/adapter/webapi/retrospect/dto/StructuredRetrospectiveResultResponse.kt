package com.didit.adapter.webapi.retrospect.dto

import com.didit.adapter.webapi.organization.dto.ProjectListResponse
import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.domain.retrospect.RetrospectiveFlowVersion
import java.time.LocalDateTime
import java.util.UUID

data class StructuredRetrospectiveResultResponse(
    val id: UUID,
    val flowVersion: RetrospectiveFlowVersion,
    val title: String?,
    val content: StructuredRetrospectiveContentResponse,
    val completedAt: LocalDateTime?,
    val project: ProjectListResponse?,
    val tags: List<TagListResponse>,
) {
    companion object {
        fun from(result: RetrospectiveDetailResult): StructuredRetrospectiveResultResponse {
            val retrospective = result.retrospective
            return StructuredRetrospectiveResultResponse(
                id = retrospective.id,
                flowVersion = retrospective.flowVersion,
                title = retrospective.title,
                content =
                    if (retrospective.isV2()) {
                        StructuredRetrospectiveContentResponse.fromV2(retrospective.resultV2)
                    } else {
                        StructuredRetrospectiveContentResponse.fromV1(retrospective.summary)
                    },
                completedAt = retrospective.completedAt,
                project = result.project?.let(ProjectListResponse::from),
                tags = result.tags.map(TagListResponse::from),
            )
        }
    }
}

data class StructuredRetrospectiveContentResponse(
    val summary: String,
    val strength: String,
    val improvement: String,
    val process: String,
    val learning: String,
    val insight: String,
    val nextActions: List<String>,
) {
    companion object {
        fun fromV2Result(result: com.didit.application.retrospect.dto.RetrospectiveResultV2Result) =
            StructuredRetrospectiveContentResponse(
                summary = result.summary ?: StructuredResultDefaults.SUMMARY,
                strength = result.strength ?: StructuredResultDefaults.STRENGTH,
                improvement = result.improvement ?: StructuredResultDefaults.IMPROVEMENT,
                process = result.process ?: StructuredResultDefaults.PROCESS,
                learning = result.learning ?: StructuredResultDefaults.LEARNING,
                insight = result.insight ?: StructuredResultDefaults.INSIGHT,
                nextActions = result.nextActions ?: listOf(StructuredResultDefaults.NEXT_ACTION),
            )

        fun fromV2(result: com.didit.domain.retrospect.RetrospectiveResultV2?) =
            StructuredRetrospectiveContentResponse(
                summary = result?.summary ?: StructuredResultDefaults.SUMMARY,
                strength = result?.strength ?: StructuredResultDefaults.STRENGTH,
                improvement = result?.improvement ?: StructuredResultDefaults.IMPROVEMENT,
                process = result?.process ?: StructuredResultDefaults.PROCESS,
                learning = result?.learning ?: StructuredResultDefaults.LEARNING,
                insight = result?.insight ?: StructuredResultDefaults.INSIGHT,
                nextActions = result?.nextActions ?: listOf(StructuredResultDefaults.NEXT_ACTION),
            )

        fun fromV1(summary: com.didit.domain.retrospect.RetrospectiveSummary?) =
            StructuredRetrospectiveContentResponse(
                summary = summary?.summary.normalized() ?: StructuredResultDefaults.SUMMARY,
                strength = StructuredResultDefaults.STRENGTH,
                improvement = summary?.blockedPoint.joinContent() ?: StructuredResultDefaults.IMPROVEMENT,
                process = summary?.solutionProcess.joinContent() ?: StructuredResultDefaults.PROCESS,
                learning = summary?.lessonLearned.joinContent() ?: StructuredResultDefaults.LEARNING,
                insight =
                    listOf(summary?.insightTitle, summary?.insightDescription).joinContent()
                        ?: StructuredResultDefaults.INSIGHT,
                nextActions =
                    listOf(summary?.nextActionTitle, summary?.nextActionDescription).joinContent()?.let(::listOf)
                        ?: listOf(StructuredResultDefaults.NEXT_ACTION),
            )

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)

        private fun Iterable<String?>?.joinContent(): String? =
            this
                ?.mapNotNull { it.normalized() }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString("\n")
    }
}

object StructuredResultDefaults {
    const val SUMMARY = "충분한 내용이 모이지 않았어요."
    const val STRENGTH = "오늘 잘한 점은 대화에서 확인되지 않았어요."
    const val IMPROVEMENT = "아쉬웠던 지점은 대화에서 확인되지 않았어요."
    const val PROCESS = "돌아본 과정은 대화에서 확인되지 않았어요."
    const val LEARNING = "오늘의 배움은 대화에서 확인되지 않았어요."
    const val INSIGHT = "디딧의 인사이트를 만들 만큼 충분한 내용이 없었어요."
    const val NEXT_ACTION = "다음에 해볼 일은 대화에서 확인되지 않았어요."
}
