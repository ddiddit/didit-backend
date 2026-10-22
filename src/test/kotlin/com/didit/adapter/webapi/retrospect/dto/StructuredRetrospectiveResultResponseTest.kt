package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.application.retrospect.dto.RetrospectiveResultV2Result
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveResultDetail
import com.didit.domain.retrospect.RetrospectiveSummary
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class StructuredRetrospectiveResultResponseTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `V2 수집되지 않은 선택 항목은 null이고 JSON에서 생략한다`() {
        val response =
            StructuredRetrospectiveContentResponse.fromV2Result(
                RetrospectiveResultV2Result(
                    summary = "배포 오류를 발견하고 롤백했어요.",
                    strengths = null,
                    improvements = null,
                    processes = listOf("로그를 확인했어요."),
                    learnings = null,
                    insight = RetrospectiveResultDetail("원인 분리", "로그 순서가 원인을 좁히는 데 도움이 됐어요."),
                    nextActions = listOf(RetrospectiveResultDetail("체크리스트 작성하기", "배포 전 항목을 정리해요.")),
                ),
            )

        val json = jacksonObjectMapper().writeValueAsString(response)

        assertThat(response.strengths).isNull()
        assertThat(response.improvements).isNull()
        assertThat(response.processes).containsExactly("로그를 확인했어요.")
        assertThat(response.learnings).isNull()
        assertThat(json).doesNotContain("strengths", "improvements", "learnings", "확인되지 않았어요")
        assertThat(json).contains("summary", "processes", "insight", "nextActions")
    }

    @Test
    fun `기존 V1 결과를 신규 결과 항목으로 호환 변환한다`() {
        val retrospective =
            Retrospective.create(userId).apply {
                startProgress()
                saveSummary(
                    RetrospectiveSummary(
                        summary = "기존 요약",
                        blockedPoint = listOf("일정이 촉박했다."),
                        solutionProcess = listOf("범위를 줄였다."),
                        lessonLearned = listOf("우선순위 합의가 중요하다."),
                        insightTitle = "범위 조정",
                        insightDescription = "초기에 합의해야 한다.",
                        nextActionTitle = "체크인",
                        nextActionDescription = "착수일에 범위를 확인한다.",
                    ),
                )
                complete("기존 회고")
            }

        val response = StructuredRetrospectiveResultResponse.from(RetrospectiveDetailResult(retrospective, null, emptyList()))

        assertThat(response.flowVersion.name).isEqualTo("V1")
        assertThat(response.content.summary).isEqualTo("기존 요약")
        assertThat(response.content.strengths).isNull()
        assertThat(response.content.improvements).containsExactly("일정이 촉박했다.")
        assertThat(response.content.processes).containsExactly("범위를 줄였다.")
        assertThat(response.content.learnings).containsExactly("우선순위 합의가 중요하다.")
        assertThat(response.content.insight).isEqualTo(StructuredResultDetailResponse("범위 조정", "초기에 합의해야 한다."))
        assertThat(response.content.nextActions)
            .containsExactly(StructuredResultDetailResponse("체크인", "착수일에 범위를 확인한다."))
    }
}
