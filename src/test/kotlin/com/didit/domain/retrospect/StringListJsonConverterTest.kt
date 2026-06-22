package com.didit.domain.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringListJsonConverterTest {
    private val converter = StringListJsonConverter()

    @Test
    fun `리스트를 JSON으로 저장하고 그대로 복원한다`() {
        val original = listOf("막힌 지점", "두 번째 지점")

        val column = converter.convertToDatabaseColumn(original)

        assertThat(converter.convertToEntityAttribute(column)).isEqualTo(original)
    }

    @Test
    fun `원소 내부에 줄바꿈이 있어도 개수가 깨지지 않는다`() {
        val original = listOf("첫째 줄\n둘째 줄", "두 번째 항목")

        val column = converter.convertToDatabaseColumn(original)

        assertThat(converter.convertToEntityAttribute(column)).isEqualTo(original)
    }

    @Test
    fun `JSON이 아닌 레거시 데이터는 줄바꿈으로 복원한다`() {
        val legacy = "첫째\n둘째\n셋째"

        assertThat(converter.convertToEntityAttribute(legacy)).containsExactly("첫째", "둘째", "셋째")
    }

    @Test
    fun `null 또는 빈 값은 빈 리스트를 반환한다`() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty()
        assertThat(converter.convertToEntityAttribute("")).isEmpty()
    }
}
