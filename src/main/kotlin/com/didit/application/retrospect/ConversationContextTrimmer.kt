package com.didit.application.retrospect

import com.didit.application.retrospect.required.ConversationContextMessage

object ConversationContextTrimmer {
    fun trim(
        messages: List<ConversationContextMessage>,
        maxCharacters: Int,
    ): List<ConversationContextMessage> {
        var remaining = maxCharacters
        val selected = mutableListOf<ConversationContextMessage>()
        for (message in messages.asReversed()) {
            if (remaining <= 0) break
            if (message.content.length > remaining && selected.isNotEmpty()) break
            val content = message.content.take(remaining)
            remaining -= content.length
            val attachments =
                message.attachments.mapNotNull { attachment ->
                    if (remaining <= 0) return@mapNotNull null
                    val extractedContent = attachment.extractedContent.take(remaining)
                    remaining -= extractedContent.length
                    attachment.copy(extractedContent = extractedContent).takeIf { extractedContent.isNotEmpty() }
                }
            if (content.isNotEmpty() || attachments.isNotEmpty()) selected += message.copy(content = content, attachments = attachments)
        }
        return selected.asReversed()
    }
}
