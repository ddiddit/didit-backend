package com.didit.application.inquiry.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryInfoFinderTest {
    @Mock
    lateinit var inquiryInfoFinder: InquiryInfoFinder

    @Test
    fun `findEmail`() {
        val userId = UUID.randomUUID()
        val expectedEmail = "test@example.com"

        whenever(inquiryInfoFinder.findEmail(userId)).thenReturn(expectedEmail)

        val email = inquiryInfoFinder.findEmail(userId)
        verify(inquiryInfoFinder).findEmail(userId)
        assert(email == expectedEmail)
    }
}
