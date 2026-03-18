package com.didit.docs

import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

object CommonDocumentation {
    fun successResponseFields(vararg dataFields: FieldDescriptor): Array<FieldDescriptor> {
        val baseFields =
            arrayOf(
                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터").optional(),
                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
            )
        return baseFields + dataFields
    }

    fun successMessageResponseFields(): Array<FieldDescriptor> =
        arrayOf(
            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
        )

    fun errorResponseFields(): Array<FieldDescriptor> =
        arrayOf(
            fieldWithPath("type").type(JsonFieldType.STRING).description("에러 타입"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 메시지"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("에러 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("요청 URI"),
            fieldWithPath("properties").type(JsonFieldType.OBJECT).description("추가 정보"),
            fieldWithPath("properties.timestamp").type(JsonFieldType.STRING).description("에러 발생 시간"),
            fieldWithPath("properties.code").type(JsonFieldType.STRING).description("에러 코드"),
        )
}
