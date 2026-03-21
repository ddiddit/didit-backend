package com.didit.adapter.retrospect.out.ai.prompt

import com.didit.domain.retrospect.entity.ChatMessage
import org.springframework.stereotype.Component

@Component
class RetrospectiveAiPromptFactory {

    fun buildAnalyzeMessages(messages: List<ChatMessage>): List<Pair<String, String>> {
        return listOf(
            "system" to """
                너는 업무 회고를 도와주는 AI다.
                사용자의 현재까지 답변을 읽고 내부 분석만 수행한다.
                답변은 반드시 한 줄로 "분석 완료"만 출력한다.
            """.trimIndent(),
            "user" to buildConversationText(messages)
        )
    }

    fun buildDeepQuestionMessages(messages: List<ChatMessage>): List<Pair<String, String>> {
        return listOf(
            "system" to """
                너는 업무 회고 챗봇이다.
                사용자의 Q1~Q3 답변을 바탕으로 더 깊이 회고할 수 있는 맞춤 질문 1개를 생성하라.
                조건:
                - 질문은 한 문장만 출력
                - 너무 추상적이지 않게
                - 행동, 판단, 배움, 개선점 중 하나를 더 깊게 묻기
                - 불필요한 설명 없이 질문만 출력
            """.trimIndent(),
            "user" to buildConversationText(messages)
        )
    }

    fun buildSummaryMessages(messages: List<ChatMessage>): List<Pair<String, String>> {
        return listOf(
            "system" to """
                너는 업무 회고 내용을 정리하는 AI다.
                주어진 대화를 바탕으로 아래 JSON만 출력하라.
                다른 설명, 마크다운, 코드블록 없이 JSON 객체만 반환하라.

                {
                  "doneWork": "...",
                  "blockedPoint": "...",
                  "solutionProcess": "...",
                  "lessonLearned": "...",
                  "insight": "...",
                  "improvementDirection": "..."
                }

                각 필드는 한국어로 자연스럽게 1~3문장 내로 작성하라.
                정보가 부족해도 최대한 문맥상 자연스럽게 요약하라.
            """.trimIndent(),
            "user" to buildConversationText(messages)
        )
    }

    private fun buildConversationText(messages: List<ChatMessage>): String {
        return buildString {
            appendLine("다음은 회고 대화 기록이다.")
            appendLine()
            messages.forEach {
                appendLine("[${it.sender.name}][${it.questionType.name}] ${it.content}")
            }
        }
    }
}