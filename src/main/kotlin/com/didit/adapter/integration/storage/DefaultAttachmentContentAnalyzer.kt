package com.didit.adapter.integration.storage

import com.didit.application.retrospect.required.AttachmentContentAnalyzer
import com.didit.application.retrospect.required.ImageAttachmentAnalyzer
import com.didit.domain.retrospect.AttachmentFileType
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

@Component
class DefaultAttachmentContentAnalyzer(
    private val imageAnalyzer: ImageAttachmentAnalyzer,
    @param:Value("\${retrospective.attachments.max-pdf-pages:30}") private val maxPdfPages: Int,
    @param:Value("\${retrospective.attachments.max-extracted-characters:30000}") private val maxCharacters: Int,
) : AttachmentContentAnalyzer {
    override fun analyze(
        fileType: AttachmentFileType,
        contentType: String,
        bytes: ByteArray,
    ): String =
        when (fileType) {
            AttachmentFileType.JPG, AttachmentFileType.PNG -> imageAnalyzer.analyze(contentType, bytes)
            AttachmentFileType.PDF -> extractPdf(bytes)
            AttachmentFileType.TXT, AttachmentFileType.MD -> decodeUtf8(bytes)
        }.trim().take(maxCharacters).also { require(it.isNotBlank()) { "파일에서 확인할 수 있는 내용이 없습니다." } }

    private fun extractPdf(bytes: ByteArray): String =
        runCatching {
            Loader.loadPDF(bytes).use { document ->
                require(document.numberOfPages in 1..maxPdfPages) { "PDF 페이지 수 제한을 초과했습니다." }
                PDFTextStripper().getText(document)
            }
        }.getOrElse { throw IllegalArgumentException("PDF 내용을 읽을 수 없습니다.", it) }

    private fun decodeUtf8(bytes: ByteArray): String =
        runCatching {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrElse { throw IllegalArgumentException("UTF-8 텍스트 파일이 아닙니다.", it) }
}
