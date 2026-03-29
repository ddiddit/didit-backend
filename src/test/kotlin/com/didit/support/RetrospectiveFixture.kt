package com.didit.support

import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import java.util.UUID

object RetrospectiveFixture {
    fun createCompleted(userId: UUID = UUID.randomUUID()): Retrospective {
        val retro =
            Retrospective.create(userId).apply {
                startProgress()
                addMessage(ChatMessage.question(this, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
                addMessage(ChatMessage.userAnswer(this, "로그인 API 연동 작업을 했습니다.", QuestionType.Q1, InputType.TEXT))
                addMessage(ChatMessage.question(this, "어떤 시도가 있었나요?", QuestionType.Q2))
                addMessage(ChatMessage.userAnswer(this, "토큰 만료 처리 로직이 복잡했습니다.", QuestionType.Q2, InputType.TEXT))
                addMessage(ChatMessage.question(this, "배운 점이 있나요?", QuestionType.Q3))
                addMessage(ChatMessage.userAnswer(this, "에러 처리를 초반에 설계해야 한다는 것을 배웠습니다.", QuestionType.Q3, InputType.TEXT))
                complete(
                    title = "로그인 API 연동 회고",
                    summary =
                        RetrospectiveSummary(
                            feedback = "오늘 작업을 잘 마무리하셨네요.",
                            insight = "문제를 작게 나누는 것이 중요합니다.",
                            doneWork = "로그인 API 연동 작업을 완료했습니다.",
                            blockedPoint = "토큰 만료 처리 로직이 복잡했습니다.",
                            solutionProcess = "공식 문서를 참고하여 해결했습니다.",
                            lessonLearned = "초반에 에러 처리를 설계해두면 편합니다.",
                        ),
                    inputTokens = 100,
                    outputTokens = 200,
                )
            }
        return retro
    }

    fun createInProgress(userId: UUID = UUID.randomUUID()): Retrospective =
        Retrospective.create(userId).apply {
            startProgress()
            addMessage(ChatMessage.question(this, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
        }
}
