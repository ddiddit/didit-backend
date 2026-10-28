package com.didit.application.retrospect

import com.didit.application.retrospect.required.GeneratedRetrospectiveResultV2
import com.didit.domain.retrospect.RetrospectiveResultDetail
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RetrospectiveResultV2TonePolicyTest {
    private val policy = RetrospectiveResultV2TonePolicy()

    @Test
    fun `서술형 결과만 해요체로 보정하고 분석 항목의 명사형은 유지한다`() {
        val normalized =
            policy.normalize(
                GeneratedRetrospectiveResultV2(
                    title = "배포 회고",
                    summary = "개발 서버에서 테스트했다.",
                    strengths = listOf("팀원과 문제를 해결한 점", "직접 적용한 경험"),
                    improvements = listOf("사전 확인 부족"),
                    processes = listOf("테스트 범위 점검"),
                    learnings = listOf("협업 시 확인의 필요성"),
                    insight = RetrospectiveResultDetail("협업", "함께 확인하여 마무리했다."),
                    nextActions = listOf(RetrospectiveResultDetail("사전 점검", "다음에는 테스트 범위를 줄인다.")),
                    inputTokens = 10,
                    outputTokens = 5,
                ),
            )

        assertThat(normalized.title).isEqualTo("배포 회고")
        assertThat(normalized.summary).isEqualTo("개발 서버에서 테스트했어요.")
        assertThat(normalized.strengths)
            .containsExactly("팀원과 문제를 해결한 점", "직접 적용한 경험")
        assertThat(normalized.improvements).containsExactly("사전 확인 부족")
        assertThat(normalized.processes).containsExactly("테스트 범위 점검")
        assertThat(normalized.learnings).containsExactly("협업 시 확인의 필요성")
        assertThat(normalized.insight).isEqualTo(RetrospectiveResultDetail("협업", "함께 확인하여 마무리했어요."))
        assertThat(normalized.nextActions)
            .containsExactly(RetrospectiveResultDetail("사전 점검", "다음에는 테스트 범위를 줄여요."))
        assertThat(normalized.inputTokens).isEqualTo(10)
        assertThat(normalized.outputTokens).isEqualTo(5)
    }

    @Test
    fun `보정 뒤에도 보고서체가 남으면 재생성이 필요하다`() {
        val normalized =
            policy.normalize(
                result(summary = "좋다."),
            )

        assertThat(policy.requiresRegeneration(normalized)).isTrue()
        assertThat(policy.requiresRegeneration(result(summary = "직접 적용해봤어요."))).isFalse()
    }

    @Test
    fun `과거 명사형 종결을 해요체로 보정한다`() {
        assertThat(policy.normalize(result(summary = "배포가 완료되었음.")).summary)
            .isEqualTo("배포가 완료되었어요.")
        assertThat(policy.normalize(result(summary = "협업 경험이 좋았음.")).summary)
            .isEqualTo("협업 경험이 좋았어요.")
        assertThat(policy.normalize(result(summary = "범위를 확인했었음.")).summary)
            .isEqualTo("범위를 확인했었어요.")
    }

    @Test
    fun `보정하지 못한 미음 명사형 종결은 재생성이 필요하다`() {
        val normalized = policy.normalize(result(summary = "배포 준비가 완료됨."))

        assertThat(normalized.summary).isEqualTo("배포 준비가 완료됨.")
        assertThat(policy.requiresRegeneration(normalized)).isTrue()
    }

    @Test
    fun `분석 항목의 명사형 종결은 재생성을 요구하지 않는다`() {
        val normalized =
            policy.normalize(
                GeneratedRetrospectiveResultV2(
                    title = "온보딩 회고",
                    summary = "온보딩 흐름을 개선했어요.",
                    strengths = listOf("사용자 경험 중심 문제 재정의"),
                    improvements = listOf("정보 우선순위 기준 설정의 어려움"),
                    processes = listOf("핵심 기능 우선 경험 후 정보 보충 방식"),
                    learnings = listOf("점진적 정보 제공의 중요성"),
                    insight = RetrospectiveResultDetail("경험 중심 설계", "사용자 경험을 먼저 고려하는 것이 중요해요."),
                    nextActions = listOf(RetrospectiveResultDetail("온보딩 개선", "개선 효과를 데이터로 검증해요.")),
                    inputTokens = 10,
                    outputTokens = 5,
                ),
            )

        assertThat(normalized.strengths).containsExactly("사용자 경험 중심 문제 재정의")
        assertThat(normalized.improvements).containsExactly("정보 우선순위 기준 설정의 어려움")
        assertThat(normalized.processes).containsExactly("핵심 기능 우선 경험 후 정보 보충 방식")
        assertThat(normalized.learnings).containsExactly("점진적 정보 제공의 중요성")
        assertThat(policy.requiresRegeneration(normalized)).isFalse()
    }

    @Test
    fun `분석 항목이 해요체나 보고서 문장으로 끝나면 재생성이 필요하다`() {
        val politeListResult =
            result(summary = "온보딩 흐름을 개선했어요.").copy(
                strengths = listOf("사용자 경험을 중심으로 문제를 해결했어요."),
            )
        val reportListResult =
            result(summary = "온보딩 흐름을 개선했어요.").copy(
                improvements = listOf("정보 우선순위를 정하기 어려웠다."),
            )

        assertThat(policy.requiresRegeneration(politeListResult)).isTrue()
        assertThat(policy.requiresRegeneration(reportListResult)).isTrue()
    }

    @Test
    fun `분석 항목 중간 단어의 종결 어미는 항목 끝으로 오인하지 않는다`() {
        val nominalResult =
            result(summary = "온보딩 흐름을 개선했어요.").copy(
                strengths = listOf("이전보다 빠른 문제 해결"),
                processes = listOf("다 함께 협업하는 방식"),
            )

        assertThat(policy.requiresRegeneration(nominalResult)).isFalse()
    }

    private fun result(summary: String) =
        GeneratedRetrospectiveResultV2(
            title = "배포 회고",
            summary = summary,
            strengths = null,
            improvements = null,
            processes = null,
            learnings = null,
            insight = null,
            nextActions = null,
            inputTokens = 10,
            outputTokens = 5,
        )
}
