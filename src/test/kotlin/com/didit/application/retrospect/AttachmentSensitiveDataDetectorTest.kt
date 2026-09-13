package com.didit.application.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AttachmentSensitiveDataDetectorTest {
    private val detector = AttachmentSensitiveDataDetector()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "담당자 이메일은 worker@example.com 입니다.",
            "연락처: 010-1234-5678",
            "주민등록번호 900101-1234567",
            "api_key=sk-abcdefghijklmnopqrstuvwxyz123456",
            "비밀번호: secret-password",
        ],
    )
    fun `개인정보나 인증정보가 포함된 텍스트를 감지한다`(content: String) {
        assertThat(detector.containsSensitiveData(content)).isTrue()
    }

    @Test
    fun `일반 업무 내용은 민감정보로 판단하지 않는다`() {
        assertThat(detector.containsSensitiveData("배포 체크리스트를 작성하고 QA를 완료했습니다.")).isFalse()
    }

    @Test
    fun `감지된 민감정보 값은 원문을 남기지 않고 마스킹한다`() {
        assertThat(detector.redact("담당자 worker@example.com, 연락처 010-1234-5678"))
            .isEqualTo("담당자 [민감정보], 연락처 [민감정보]")
    }
}
