package com.didit.application.retrospect.provided

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile

@ExtendWith(MockitoExtension::class)
class SpeechTranscriberTest {
    @Mock
    lateinit var speechTranscriber: SpeechTranscriber

    private fun wavFile() = MockMultipartFile("file", "voice.wav", "audio/wav", ByteArray(100) { 1 })

    @Test
    fun `transcribe - wav 파일을 텍스트로 변환한다`() {
        val file = wavFile()
        whenever(speechTranscriber.transcribe(file)).thenReturn("음성 변환된 텍스트")

        val result = speechTranscriber.transcribe(file)

        verify(speechTranscriber).transcribe(file)
        assertThat(result).isEqualTo("음성 변환된 텍스트")
    }
}
