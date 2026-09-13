package com.didit.application.retrospect.required

import com.didit.domain.retrospect.AttachmentFileType

interface AttachmentContentAnalyzer {
    fun analyze(
        fileType: AttachmentFileType,
        contentType: String,
        bytes: ByteArray,
    ): String
}

interface ImageAttachmentAnalyzer {
    fun analyze(
        contentType: String,
        bytes: ByteArray,
    ): String
}
