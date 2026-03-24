package com.didit.domain.retrospect.entity

import com.didit.domain.retrospect.enums.QuestionType
import com.didit.domain.retrospect.enums.Sender
import java.time.LocalDateTime

data class ChatMessage(
    val id: Long? = null,
    val sender: Sender,
    val content: String,
    val questionType: QuestionType,
    val isSkipped: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isUserMessage(): Boolean = sender == Sender.USER

    companion object {
        fun user(
            questionType: QuestionType,
            content: String,
        ): ChatMessage =
            ChatMessage(
                sender = Sender.USER,
                content = content,
                questionType = questionType,
            )

        fun ai(
            questionType: QuestionType,
            content: String,
        ): ChatMessage =
            ChatMessage(
                sender = Sender.AI,
                content = content,
                questionType = questionType,
            )
    }
}
