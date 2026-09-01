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
    strengths: List<String>?,
    improvements: List<String>?,
    processes: List<String>?,
    learnings: List<String>?,
    insight: RetrospectiveResultDetail?,
    nextActions: List<RetrospectiveResultDetail>?,
) {
    @Column(name = "result_schema_version")
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION

    @Column(name = "result_summary", columnDefinition = "TEXT")
    val summary: String? = summary.normalized()

    @Convert(converter = NullableStringListJsonConverter::class)
    @Column(name = "result_strength", columnDefinition = "TEXT")
    val strengths: List<String>? = strengths.normalizedItems("오늘 잘한 점")

    @Convert(converter = NullableStringListJsonConverter::class)
    @Column(name = "result_improvement", columnDefinition = "TEXT")
    val improvements: List<String>? = improvements.normalizedItems("아쉬웠던 점")

    @Convert(converter = NullableStringListJsonConverter::class)
    @Column(name = "result_process", columnDefinition = "TEXT")
    val processes: List<String>? = processes.normalizedItems("해결 과정")

    @Convert(converter = NullableStringListJsonConverter::class)
    @Column(name = "result_learning", columnDefinition = "TEXT")
    val learnings: List<String>? = learnings.normalizedItems("배운 점")

    @Convert(converter = NullableResultDetailJsonConverter::class)
    @Column(name = "result_insight", columnDefinition = "TEXT")
    val insight: RetrospectiveResultDetail? = insight?.normalized()

    @Convert(converter = NullableResultDetailListJsonConverter::class)
    @Column(name = "result_next_actions", columnDefinition = "TEXT")
    val nextActions: List<RetrospectiveResultDetail>? =
        nextActions
            ?.mapNotNull { it.normalized() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.also { require(it.size <= MAX_ITEMS) { "다음 행동은 최대 2개까지 저장할 수 있습니다." } }

    companion object {
        private const val CURRENT_SCHEMA_VERSION = 3
        private const val MAX_ITEMS = 2

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)

        private fun List<String>?.normalizedItems(name: String): List<String>? =
            this
                ?.mapNotNull { it.normalized() }
                ?.distinct()
                ?.takeIf { it.isNotEmpty() }
                ?.also { require(it.size <= MAX_ITEMS) { "${name}은 최대 2개까지 저장할 수 있습니다." } }
    }
}

data class RetrospectiveResultDetail(
    val title: String,
    val description: String,
) {
    internal fun normalized(): RetrospectiveResultDetail? {
        val normalizedTitle = title.trim()
        val normalizedDescription = description.trim()
        if (normalizedTitle.isEmpty() || normalizedDescription.isEmpty()) return null
        return RetrospectiveResultDetail(normalizedTitle, normalizedDescription)
    }
}

@Converter
class NullableStringListJsonConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String? = attribute?.let(objectMapper::writeValueAsString)

    override fun convertToEntityAttribute(dbData: String?): List<String>? =
        dbData?.takeIf(String::isNotBlank)?.let { data -> objectMapper.readValue<List<String>>(data) }
}

@Converter
class NullableResultDetailJsonConverter : AttributeConverter<RetrospectiveResultDetail, String> {
    override fun convertToDatabaseColumn(attribute: RetrospectiveResultDetail?): String? = attribute?.let(objectMapper::writeValueAsString)

    override fun convertToEntityAttribute(dbData: String?): RetrospectiveResultDetail? =
        dbData?.takeIf(String::isNotBlank)?.let { data -> objectMapper.readValue<RetrospectiveResultDetail>(data) }
}

@Converter
class NullableResultDetailListJsonConverter : AttributeConverter<List<RetrospectiveResultDetail>, String> {
    override fun convertToDatabaseColumn(attribute: List<RetrospectiveResultDetail>?): String? =
        attribute?.let(objectMapper::writeValueAsString)

    override fun convertToEntityAttribute(dbData: String?): List<RetrospectiveResultDetail>? =
        dbData?.takeIf(String::isNotBlank)?.let { data -> objectMapper.readValue<List<RetrospectiveResultDetail>>(data) }
}

private val objectMapper = jacksonObjectMapper()
