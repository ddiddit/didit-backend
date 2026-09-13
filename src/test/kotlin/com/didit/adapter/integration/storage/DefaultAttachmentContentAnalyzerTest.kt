package com.didit.adapter.integration.storage

import com.didit.application.retrospect.required.ImageAttachmentAnalyzer
import com.didit.domain.retrospect.AttachmentFileType
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream

class DefaultAttachmentContentAnalyzerTest {
    private val imageAnalyzer = mock(ImageAttachmentAnalyzer::class.java)
    private val analyzer = DefaultAttachmentContentAnalyzer(imageAnalyzer, 30, 30_000)

    @Test
    fun `마크다운 파일은 UTF-8 텍스트로 추출한다`() {
        val result = analyzer.analyze(AttachmentFileType.MD, "text/plain", "# Work\n- deployed".toByteArray())

        assertThat(result).isEqualTo("# Work\n- deployed")
    }

    @Test
    fun `PDF 파일에서 텍스트를 추출한다`() {
        val result = analyzer.analyze(AttachmentFileType.PDF, "application/pdf", pdf("deployment complete"))

        assertThat(result).contains("deployment complete")
    }

    @Test
    fun `이미지는 전용 AI 분석기로 전달한다`() {
        val bytes = byteArrayOf(1, 2, 3)
        whenever(imageAnalyzer.analyze("image/png", bytes)).thenReturn("배포 대시보드 화면")

        val result = analyzer.analyze(AttachmentFileType.PNG, "image/png", bytes)

        assertThat(result).isEqualTo("배포 대시보드 화면")
        verify(imageAnalyzer).analyze("image/png", bytes)
    }

    private fun pdf(text: String): ByteArray {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)
        PDPageContentStream(document, page).use {
            it.beginText()
            it.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
            it.showText(text)
            it.endText()
        }
        return ByteArrayOutputStream().use {
            document.save(it)
            document.close()
            it.toByteArray()
        }
    }
}
