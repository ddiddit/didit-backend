package com.didit.adapter.retrospect.out.ai

import com.didit.application.retrospect.port.out.AiAnalyzeResult
import com.didit.application.retrospect.port.out.AiDeepQuestionResult
import com.didit.application.retrospect.port.out.AiSummaryResult
import com.didit.application.retrospect.port.out.RetrospectiveAiPort
import com.didit.domain.retrospect.entity.ChatMessage
import com.didit.domain.retrospect.model.RetrospectiveSummary
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

class FakeRetrospectiveAiAdapter : RetrospectiveAiPort {

    override fun analyzeAnswer(messages: List<ChatMessage>): AiAnalyzeResult {
        return AiAnalyzeResult(
            inputTokens = 10,
            outputTokens = 5
        )
    }

    override fun generateDeepQuestion(messages: List<ChatMessage>): AiDeepQuestionResult {
        return AiDeepQuestionResult(
            question = "이번 경험에서 가장 크게 배운 점이나 다음에 더 잘하고 싶은 점은 무엇인가요?",
            inputTokens = 20,
            outputTokens = 10
        )
    }

    override fun generateSummary(messages: List<ChatMessage>): AiSummaryResult {
        return AiSummaryResult(
            summary = RetrospectiveSummary(
                doneWork = "오늘 수행한 업무를 정리했습니다.",
                blockedPoint = "구현 중 일부 구조 설계에서 고민이 있었습니다.",
                solutionProcess = "기능 흐름을 단계별로 나누고 구조를 정리해 해결했습니다.",
                lessonLearned = "질문 흐름과 상태 관리를 먼저 설계하는 것이 중요하다는 점을 배웠습니다.",
                insight = "문제 해결 전에 구조를 먼저 명확히 하면 이후 구현 속도가 빨라집니다.",
                improvementDirection = "다음에는 API와 도메인 책임을 더 일찍 구분해보세요."
            ),
            inputTokens = 50,
            outputTokens = 30
        )
    }
}