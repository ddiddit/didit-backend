package com.didit.application.retrospect

import com.didit.application.retrospect.required.ConversationAttachmentContext
import com.didit.domain.retrospect.RetrospectiveAttachment

object AttachmentConversationContextMapper {
    fun from(attachment: RetrospectiveAttachment): ConversationAttachmentContext =
        ConversationAttachmentContext(
            id = attachment.id,
            filename = attachment.originalFilename,
            fileType = attachment.fileType,
            extractedContent = checkNotNull(attachment.extractedContent),
            containsSensitiveData = attachment.containsSensitiveData,
        )
}
