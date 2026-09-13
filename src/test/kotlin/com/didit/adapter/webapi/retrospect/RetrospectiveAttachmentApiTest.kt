package com.didit.adapter.webapi.retrospect

import com.didit.application.retrospect.dto.AttachmentDownloadResult
import com.didit.application.retrospect.dto.AttachmentResult
import com.didit.application.retrospect.dto.CreateAttachmentUploadResult
import com.didit.application.retrospect.provided.RetrospectiveAttachments
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.retrospect.AttachmentAnalysisStatus
import com.didit.domain.retrospect.AttachmentFileType
import com.didit.domain.retrospect.AttachmentUploadStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class RetrospectiveAttachmentApiTest : AuthenticatedRestDocsSupport() {
    private val attachments = mock(RetrospectiveAttachments::class.java)
    private val retrospectiveId = UUID.randomUUID()
    private val attachmentId = UUID.randomUUID()
    private val now = LocalDateTime.of(2026, 9, 6, 12, 0)

    override fun initController() = RetrospectiveAttachmentApi(attachments)

    @Test
    fun `첨부파일 사전 업로드 URL 생성`() {
        whenever(attachments.createUpload(userId, retrospectiveId, "work.md", "text/plain", 100L, CHECKSUM)).thenReturn(
            CreateAttachmentUploadResult(attachmentId, "https://upload.example/file", now.plusMinutes(5)),
        )

        mockMvc
            .perform(
                post("/api/v2/retrospectives/{retrospectiveId}/attachments", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"filename":"work.md","contentType":"text/plain","size":100,"checksumSha256":"$CHECKSUM"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.attachmentId").value(attachmentId.toString()))
            .andDo(
                document(
                    "retrospect-v2/create-attachment-upload",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("retrospectiveId").description("회고 ID")),
                    requestFields(
                        fieldWithPath("filename").type(JsonFieldType.STRING).description("원본 파일명"),
                        fieldWithPath("contentType").type(JsonFieldType.STRING).description("파일 MIME 타입"),
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description("파일 크기(byte), 최대 10MB"),
                        fieldWithPath("checksumSha256").type(JsonFieldType.STRING).description("파일 본문의 Base64 SHA-256 체크섬"),
                    ),
                    responseFields(
                        fieldWithPath("data.attachmentId").type(JsonFieldType.STRING).description("첨부파일 ID"),
                        fieldWithPath("data.uploadUrl").type(JsonFieldType.STRING).description("S3 PUT presigned URL"),
                        fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("업로드 URL 만료 시각"),
                    ),
                ),
            )
    }

    @Test
    fun `SHA-256 체크섬이 없으면 첨부파일 업로드 URL을 만들 수 없다`() {
        mockMvc
            .perform(
                post("/api/v2/retrospectives/{retrospectiveId}/attachments", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"filename":"work.md","contentType":"text/plain","size":100}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `첨부파일 업로드 완료`() {
        whenever(attachments.completeUpload(userId, retrospectiveId, attachmentId)).thenReturn(attachmentResult())

        mockMvc
            .perform(post("/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}/complete", retrospectiveId, attachmentId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.uploadStatus").value("UPLOADED"))
            .andDo(
                document(
                    "retrospect-v2/complete-attachment-upload",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    attachmentPathParameters(),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("첨부파일 ID"),
                        fieldWithPath("data.filename").type(JsonFieldType.STRING).description("원본 파일명"),
                        fieldWithPath("data.fileType").type(JsonFieldType.STRING).description("파일 형식"),
                        fieldWithPath("data.contentType").type(JsonFieldType.STRING).description("MIME 타입"),
                        fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("파일 크기(byte)"),
                        fieldWithPath("data.uploadStatus").type(JsonFieldType.STRING).description("업로드 상태"),
                        fieldWithPath("data.analysisStatus").type(JsonFieldType.STRING).description("분석 상태"),
                    ),
                ),
            )
    }

    @Test
    fun `전송 전 첨부파일 삭제`() {
        mockMvc
            .perform(delete("/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}", retrospectiveId, attachmentId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "retrospect-v2/delete-attachment",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    attachmentPathParameters(),
                ),
            )

        verify(attachments).delete(userId, retrospectiveId, attachmentId)
    }

    @Test
    fun `첨부파일 상세보기 URL 생성`() {
        whenever(attachments.createDownload(userId, retrospectiveId, attachmentId)).thenReturn(
            AttachmentDownloadResult("https://download.example/file", now.plusMinutes(5)),
        )

        mockMvc
            .perform(get("/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}/download-url", retrospectiveId, attachmentId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.url").value("https://download.example/file"))
            .andDo(
                document(
                    "retrospect-v2/create-attachment-download",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    attachmentPathParameters(),
                    responseFields(
                        fieldWithPath("data.url").type(JsonFieldType.STRING).description("S3 GET presigned URL"),
                        fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("다운로드 URL 만료 시각"),
                    ),
                ),
            )
    }

    @Test
    fun `첨부파일 분석 재시도`() {
        whenever(attachments.retryAnalysis(userId, retrospectiveId, attachmentId)).thenReturn(
            attachmentResult().copy(analysisStatus = AttachmentAnalysisStatus.PROCESSING),
        )

        mockMvc
            .perform(
                post(
                    "/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}/retry-analysis",
                    retrospectiveId,
                    attachmentId,
                ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.analysisStatus").value("PROCESSING"))
            .andDo(
                document(
                    "retrospect-v2/retry-attachment-analysis",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    attachmentPathParameters(),
                    responseFields(*attachmentResponseFields()),
                ),
            )
    }

    private fun attachmentResult() =
        AttachmentResult(
            attachmentId,
            "work.md",
            AttachmentFileType.MD,
            "text/plain",
            100L,
            AttachmentUploadStatus.UPLOADED,
            AttachmentAnalysisStatus.NOT_STARTED,
        )

    private fun attachmentPathParameters() =
        pathParameters(
            parameterWithName("retrospectiveId").description("회고 ID"),
            parameterWithName("attachmentId").description("첨부파일 ID"),
        )

    private fun attachmentResponseFields() =
        arrayOf(
            fieldWithPath("data.id").type(JsonFieldType.STRING).description("첨부파일 ID"),
            fieldWithPath("data.filename").type(JsonFieldType.STRING).description("원본 파일명"),
            fieldWithPath("data.fileType").type(JsonFieldType.STRING).description("파일 형식"),
            fieldWithPath("data.contentType").type(JsonFieldType.STRING).description("MIME 타입"),
            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("파일 크기(byte)"),
            fieldWithPath("data.uploadStatus").type(JsonFieldType.STRING).description("업로드 상태"),
            fieldWithPath("data.analysisStatus").type(JsonFieldType.STRING).description("분석 상태"),
        )

    companion object {
        private const val CHECKSUM = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
}
