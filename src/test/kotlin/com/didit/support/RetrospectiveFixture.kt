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
                saveSummary(
                    RetrospectiveSummary(
                        summary = "오늘은 로그인 API 연동 작업을 마무리하면서 토큰 만료 처리 로직에서 예상치 못한 엣지 케이스를 발견하고 해결한 하루였어요.",
                        blockedPoint = "토큰 만료 처리 로직이 복잡했습니다.",
                        solutionProcess = "공식 문서를 참고하여 해결했습니다.",
                        lessonLearned = "초반에 에러 처리를 설계해두면 편합니다.",
                        insightTitle = "문제를 작게 나누는 것의 중요성",
                        insightDescription = "문제를 작게 나누어 접근하면 복잡한 이슈도 더 안정적으로 해결할 수 있다는 점을 느꼈어요.",
                        nextActionTitle = "토큰 만료 엣지 케이스 테스트 작성",
                        nextActionDescription = "토큰 만료 상황에서 발생할 수 있는 예외 흐름을 테스트 코드로 보완해볼 예정이에요.",
                    ),
                )
                complete(title = "로그인 API 연동 회고")
            }
        return retro
    }
}
