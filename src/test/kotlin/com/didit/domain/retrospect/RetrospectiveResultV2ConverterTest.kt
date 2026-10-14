package com.didit.domain.retrospect

import com.fasterxml.jackson.core.JsonProcessingException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RetrospectiveResultV2ConverterTest {
    private val stringListConverter = NullableStringListJsonConverter()
    private val detailConverter = NullableResultDetailJsonConverter()
    private val detailListConverter = NullableResultDetailListJsonConverter()

    @Test
    fun `문자열 목록 converter는 스키마 2 일반 문자열을 단일 항목으로 읽는다`() {
        assertThat(stringListConverter.convertToEntityAttribute("문제를 빨리 발견했다."))
            .containsExactly("문제를 빨리 발견했다.")
    }

    @Test
    fun `문자열 목록 converter는 스키마 3 JSON 배열을 그대로 읽는다`() {
        assertThat(stringListConverter.convertToEntityAttribute("""["첫 번째", "두 번째"]"""))
            .containsExactly("첫 번째", "두 번째")
    }

    @Test
    fun `문자열 목록 converter는 손상된 배열 JSON을 일반 문자열로 숨기지 않는다`() {
        assertThrows<JsonProcessingException> {
            stringListConverter.convertToEntityAttribute("[손상된 JSON")
        }
    }

    @Test
    fun `상세 converter는 스키마 2 insight를 고정 제목과 원문 설명으로 읽는다`() {
        assertThat(detailConverter.convertToEntityAttribute("원인을 먼저 분리해 보는 게 중요하다."))
            .isEqualTo(
                RetrospectiveResultDetail(
                    title = "디딧의 인사이트",
                    description = "원인을 먼저 분리해 보는 게 중요하다.",
                ),
            )
    }

    @Test
    fun `상세 converter는 스키마 3 JSON 객체를 그대로 읽는다`() {
        assertThat(
            detailConverter.convertToEntityAttribute(
                """{"title":"재현 조건 분리","description":"원인 파악을 도왔다."}""",
            ),
        ).isEqualTo(RetrospectiveResultDetail("재현 조건 분리", "원인 파악을 도왔다."))
    }

    @Test
    fun `상세 목록 converter는 스키마 2 문자열 배열 세 개를 모두 보존한다`() {
        assertThat(
            detailListConverter.convertToEntityAttribute(
                """["테스트 추가", "체크리스트 작성", "알림 기준 점검"]""",
            ),
        ).containsExactly(
            RetrospectiveResultDetail("다음 행동", "테스트 추가"),
            RetrospectiveResultDetail("다음 행동", "체크리스트 작성"),
            RetrospectiveResultDetail("다음 행동", "알림 기준 점검"),
        )
    }

    @Test
    fun `상세 목록 converter는 스키마 3 상세 객체 배열을 그대로 읽는다`() {
        assertThat(
            detailListConverter.convertToEntityAttribute(
                """[{"title":"테스트 추가","description":"회귀 테스트를 작성한다."}]""",
            ),
        ).containsExactly(RetrospectiveResultDetail("테스트 추가", "회귀 테스트를 작성한다."))
    }

    @Test
    fun `상세 목록 converter는 문자열과 객체가 섞인 배열을 거절한다`() {
        assertThrows<JsonProcessingException> {
            detailListConverter.convertToEntityAttribute(
                """["테스트 추가", {"title":"점검","description":"알림을 확인한다."}]""",
            )
        }
    }

    @Test
    fun `converter는 null과 공백을 null로 읽는다`() {
        assertThat(stringListConverter.convertToEntityAttribute(null)).isNull()
        assertThat(detailConverter.convertToEntityAttribute("   ")).isNull()
        assertThat(detailListConverter.convertToEntityAttribute("\n\t")).isNull()
    }
}
