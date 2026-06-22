package com.didit.domain.retrospect

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListJsonConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String =
        objectMapper.writeValueAsString(
            attribute ?: emptyList<String>(),
        )

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        if (dbData.isNullOrBlank()) return emptyList()

        return runCatching { objectMapper.readValue<List<String>>(dbData) }
            .getOrElse { dbData.split("\n") }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}
