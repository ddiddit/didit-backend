package com.didit.domain.retrospect

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Embeddable

@Embeddable
class RetrospectiveResultV2(
    summary: String?,
    strength: String?,
    improvement: String?,
    process: String?,
    learning: String?,
    insight: String?,
    nextActions: List<String>?,
) {
    @Column(name = "result_schema_version")
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION

    @Column(name = "result_summary", columnDefinition = "TEXT")
    val summary: String? = summary.normalized()

    @Column(name = "result_strength", columnDefinition = "TEXT")
    val strength: String? = strength.normalized()

    @Column(name = "result_improvement", columnDefinition = "TEXT")
    val improvement: String? = improvement.normalized()

    @Column(name = "result_process", columnDefinition = "TEXT")
    val process: String? = process.normalized()

    @Column(name = "result_learning", columnDefinition = "TEXT")
    val learning: String? = learning.normalized()

    @Column(name = "result_insight", columnDefinition = "TEXT")
    val insight: String? = insight.normalized()

    @Convert(converter = NullableStringListJsonConverter::class)
    @Column(name = "result_next_actions", columnDefinition = "TEXT")
    val nextActions: List<String>? =
        nextActions
            ?.mapNotNull { it.normalized() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.also { require(it.size <= MAX_NEXT_ACTIONS) { "다음 행동은 최대 3개까지 저장할 수 있습니다." } }

    companion object {
        private const val CURRENT_SCHEMA_VERSION = 2
        private const val MAX_NEXT_ACTIONS = 3

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
    }
}

@Converter
class NullableStringListJsonConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String? = attribute?.let(objectMapper::writeValueAsString)

    override fun convertToEntityAttribute(dbData: String?): List<String>? =
        dbData?.takeIf(String::isNotBlank)?.let { data -> objectMapper.readValue<List<String>>(data) }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}
