package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminBadgeCategoryItem
import com.didit.application.admin.provided.AdminBadgeConditionTypeItem
import com.didit.application.admin.provided.AdminBadgeFinder
import com.didit.application.admin.provided.AdminBadgeHolder
import com.didit.application.admin.provided.AdminBadgeMetaResult
import com.didit.application.admin.provided.AdminBadgeParamSpec
import com.didit.application.admin.provided.AdminBadgeRegister
import com.didit.application.admin.provided.AdminBadgeResult
import com.didit.application.audit.AuditLogger
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class AdminBadgeApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminBadgeFinder: AdminBadgeFinder = mock(AdminBadgeFinder::class.java)
    private val adminBadgeRegister: AdminBadgeRegister = mock(AdminBadgeRegister::class.java)
    private val auditLogger: AuditLogger = mock(AuditLogger::class.java)

    override fun initController() = AdminBadgeApi(adminBadgeFinder, adminBadgeRegister, auditLogger)

    private fun badgeResult(
        id: UUID = UUID.randomUUID(),
        active: Boolean = true,
    ) = AdminBadgeResult(
        id = id,
        name = "첫 기록",
        description = "첫 회고를 작성했어요",
        category = "CONSISTENCY",
        conditionType = "CUMULATIVE_RETRO",
        threshold = 1,
        params = null,
        iconUrl = "https://cdn.didit.ai.kr/badges/first.png",
        congratsTitle = "첫 기록 달성!",
        congratsMessage = "회고의 첫 걸음을 내디뎠어요",
        active = active,
        acquiredCount = 150,
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    private fun badgeResultFields(prefix: String) =
        listOf(
            fieldWithPath("$prefix.id").type(JsonFieldType.STRING).description("배지 ID"),
            fieldWithPath("$prefix.name").type(JsonFieldType.STRING).description("배지명"),
            fieldWithPath("$prefix.description").type(JsonFieldType.STRING).description("배지 설명"),
            fieldWithPath("$prefix.category").type(JsonFieldType.STRING).description("카테고리"),
            fieldWithPath("$prefix.conditionType").type(JsonFieldType.STRING).description("획득 조건 유형"),
            fieldWithPath("$prefix.threshold").type(JsonFieldType.NUMBER).description("획득 임계값"),
            fieldWithPath("$prefix.params").type(JsonFieldType.OBJECT).optional().description("조건 파라미터(JSON)"),
            fieldWithPath("$prefix.iconUrl").type(JsonFieldType.STRING).optional().description("아이콘 URL"),
            fieldWithPath("$prefix.congratsTitle").type(JsonFieldType.STRING).optional().description("획득 축하 타이틀"),
            fieldWithPath("$prefix.congratsMessage").type(JsonFieldType.STRING).optional().description("획득 축하 메시지"),
            fieldWithPath("$prefix.active").type(JsonFieldType.BOOLEAN).description("활성 여부"),
            fieldWithPath("$prefix.acquiredCount").type(JsonFieldType.NUMBER).description("획득 유저 수"),
            fieldWithPath("$prefix.createdAt").type(JsonFieldType.STRING).optional().description("생성 일시"),
        )

    @Test
    fun `배지 목록 조회`() {
        whenever(adminBadgeFinder.findAll()).thenReturn(listOf(badgeResult()))

        mockMvc
            .perform(get("/api/v1/admin/badges").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(badgeResultFields("data[]")),
                ),
            )
    }

    @Test
    fun `배지 상세 조회`() {
        val badgeId = UUID.randomUUID()
        whenever(adminBadgeFinder.findById(any())).thenReturn(badgeResult(id = badgeId))

        mockMvc
            .perform(get("/api/v1/admin/badges/{badgeId}", badgeId).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/detail",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("badgeId").description("배지 ID")),
                    responseFields(badgeResultFields("data")),
                ),
            )
    }

    @Test
    fun `배지 조건 타입 메타 조회`() {
        val meta =
            AdminBadgeMetaResult(
                conditionTypes =
                    listOf(
                        AdminBadgeConditionTypeItem(
                            conditionType = "WEEKLY_STREAK",
                            label = "연속 주차",
                            description = "매주 weeklyMinCount회 이상을 threshold주 연속 달성하면 부여",
                            params =
                                listOf(
                                    AdminBadgeParamSpec(
                                        key = "weeklyMinCount",
                                        label = "주당 최소 회고 수",
                                        type = "INT",
                                        defaultValue = 1,
                                        required = false,
                                    ),
                                ),
                        ),
                    ),
                categories = listOf(AdminBadgeCategoryItem(category = "CONSISTENCY", label = "꾸준함")),
            )
        whenever(adminBadgeFinder.findMeta()).thenReturn(meta)

        mockMvc
            .perform(get("/api/v1/admin/badges/condition-types").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/condition-types",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.conditionTypes[].conditionType").type(JsonFieldType.STRING).description("조건 타입"),
                        fieldWithPath("data.conditionTypes[].label").type(JsonFieldType.STRING).description("조건 타입 라벨"),
                        fieldWithPath("data.conditionTypes[].description").type(JsonFieldType.STRING).description("설명"),
                        fieldWithPath("data.conditionTypes[].params[].key").type(JsonFieldType.STRING).description("파라미터 키"),
                        fieldWithPath("data.conditionTypes[].params[].label").type(JsonFieldType.STRING).description("파라미터 라벨"),
                        fieldWithPath("data.conditionTypes[].params[].type").type(JsonFieldType.STRING).description("파라미터 타입"),
                        fieldWithPath("data.conditionTypes[].params[].defaultValue")
                            .type(JsonFieldType.NUMBER)
                            .optional()
                            .description("기본값"),
                        fieldWithPath("data.conditionTypes[].params[].required").type(JsonFieldType.BOOLEAN).description("필수 여부"),
                        fieldWithPath("data.categories[].category").type(JsonFieldType.STRING).description("카테고리"),
                        fieldWithPath("data.categories[].label").type(JsonFieldType.STRING).description("카테고리 라벨"),
                    ),
                ),
            )
    }

    @Test
    fun `배지 보유 유저 목록 조회`() {
        val badgeId = UUID.randomUUID()
        val result =
            listOf(
                AdminBadgeHolder(
                    userId = UUID.randomUUID(),
                    email = "user@example.com",
                    nickname = "디딧유저",
                    acquiredAt = LocalDateTime.of(2026, 6, 1, 12, 0),
                ),
            )
        whenever(adminBadgeFinder.findHolders(any())).thenReturn(result)

        mockMvc
            .perform(
                get("/api/v1/admin/badges/{badgeId}/holders", badgeId)
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/holders",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("badgeId").description("배지 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data[].userId").type(JsonFieldType.STRING).description("유저 ID"),
                        fieldWithPath("data[].email").type(JsonFieldType.STRING).optional().description("이메일"),
                        fieldWithPath("data[].nickname").type(JsonFieldType.STRING).optional().description("닉네임"),
                        fieldWithPath("data[].acquiredAt").type(JsonFieldType.STRING).description("획득 일시"),
                    ),
                ),
            )
    }

    @Test
    fun `배지 생성`() {
        whenever(adminBadgeRegister.create(any())).thenReturn(badgeResult())

        val body =
            """
            {
              "name": "첫 기록",
              "description": "첫 회고를 작성했어요",
              "category": "CONSISTENCY",
              "conditionType": "CUMULATIVE_RETRO",
              "threshold": 1,
              "iconUrl": "https://cdn.didit.ai.kr/badges/first.png",
              "congratsTitle": "첫 기록 달성!",
              "congratsMessage": "회고의 첫 걸음을 내디뎠어요"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/api/v1/admin/badges")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/create",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("name").type(JsonFieldType.STRING).description("배지명"),
                        fieldWithPath("description").type(JsonFieldType.STRING).description("배지 설명"),
                        fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                        fieldWithPath("conditionType").type(JsonFieldType.STRING).description("획득 조건 유형"),
                        fieldWithPath("threshold").type(JsonFieldType.NUMBER).description("획득 임계값(1 이상)"),
                        fieldWithPath("params").type(JsonFieldType.OBJECT).optional().description("조건 파라미터(JSON)"),
                        fieldWithPath("iconUrl").type(JsonFieldType.STRING).optional().description("아이콘 URL"),
                        fieldWithPath("congratsTitle").type(JsonFieldType.STRING).optional().description("획득 축하 타이틀"),
                        fieldWithPath("congratsMessage").type(JsonFieldType.STRING).optional().description("획득 축하 메시지"),
                    ),
                    responseFields(badgeResultFields("data")),
                ),
            )
    }

    @Test
    fun `배지 수정`() {
        val badgeId = UUID.randomUUID()
        whenever(adminBadgeRegister.update(any(), any())).thenReturn(badgeResult(id = badgeId))

        val body =
            """
            {
              "name": "첫 기록",
              "description": "수정된 설명",
              "category": "CONSISTENCY",
              "conditionType": "CUMULATIVE_RETRO",
              "threshold": 1
            }
            """.trimIndent()

        mockMvc
            .perform(
                put("/api/v1/admin/badges/{badgeId}", badgeId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("badgeId").description("배지 ID")),
                    requestFields(
                        fieldWithPath("name").type(JsonFieldType.STRING).description("배지명"),
                        fieldWithPath("description").type(JsonFieldType.STRING).description("배지 설명"),
                        fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                        fieldWithPath("conditionType").type(JsonFieldType.STRING).description("획득 조건 유형"),
                        fieldWithPath("threshold").type(JsonFieldType.NUMBER).description("획득 임계값(1 이상)"),
                        fieldWithPath("params").type(JsonFieldType.OBJECT).optional().description("조건 파라미터(JSON)"),
                        fieldWithPath("iconUrl").type(JsonFieldType.STRING).optional().description("아이콘 URL"),
                        fieldWithPath("congratsTitle").type(JsonFieldType.STRING).optional().description("획득 축하 타이틀"),
                        fieldWithPath("congratsMessage").type(JsonFieldType.STRING).optional().description("획득 축하 메시지"),
                    ),
                    responseFields(badgeResultFields("data")),
                ),
            )
    }

    @Test
    fun `배지 활성 상태 변경`() {
        val badgeId = UUID.randomUUID()
        whenever(adminBadgeRegister.changeActive(any(), eq(false))).thenReturn(badgeResult(id = badgeId, active = false))

        mockMvc
            .perform(
                patch("/api/v1/admin/badges/{badgeId}/active", badgeId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"active": false}""")
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/badges/active",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(parameterWithName("badgeId").description("배지 ID")),
                    requestFields(
                        fieldWithPath("active").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                    ),
                    responseFields(badgeResultFields("data")),
                ),
            )
    }
}
