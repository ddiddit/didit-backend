package com.didit.application.retrospect

import com.didit.application.retrospect.exception.SpeechEmptyFileException
import com.didit.application.retrospect.exception.SpeechEmptyResultException
import com.didit.application.retrospect.exception.SpeechUnsupportedFileException
import com.didit.application.retrospect.required.SpeechClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile

@ExtendWith(MockitoExtension::class)
class SpeechServiceTest {
    @Mock
    lateinit var speechClient: SpeechClient

    private lateinit var speechService: SpeechService

    @BeforeEach
    fun setUp() {
        speechService = SpeechService(speechClient)
    }

    private fun wavFile(content: ByteArray = ByteArray(100) { 1 }) = MockMultipartFile("file", "voice.wav", "audio/wav", content)

    @Test
    fun `transcribe - wav 파일을 텍스트로 변환한다`() {
        val file = wavFile()
        whenever(speechClient.transcribe(any(), any())).thenReturn("음성 변환된 텍스트")

        val result = speechService.transcribe(file)

        assertThat(result).isEqualTo("음성 변환된 텍스트")
        verify(speechClient).transcribe(file.bytes, "voice.wav")
    }

    @Test
    fun `transcribe - 파일명이 null이면 예외가 발생한다`() {
        val file = MockMultipartFile("file", null, "audio/wav", ByteArray(100) { 1 })

        assertThrows<SpeechUnsupportedFileException> {
            speechService.transcribe(file)
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `transcribe - 빈 파일이면 예외가 발생한다`() {
        val file = wavFile(ByteArray(0))

        assertThrows<SpeechEmptyFileException> {
            speechService.transcribe(file)
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `transcribe - wav가 아닌 파일이면 예외가 발생한다`() {
        val file = MockMultipartFile("file", "voice.mp3", "audio/mpeg", ByteArray(100) { 1 })

        assertThrows<SpeechUnsupportedFileException> {
            speechService.transcribe(file)
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `transcribe - 음성 인식 결과가 비어있으면 예외가 발생한다`() {
        val file = wavFile()
        whenever(speechClient.transcribe(any(), any())).thenReturn("   ")

        assertThrows<SpeechEmptyResultException> {
            speechService.transcribe(file)
        }
    }
}
