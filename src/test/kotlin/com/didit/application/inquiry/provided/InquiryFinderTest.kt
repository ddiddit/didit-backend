package com.didit.application.inquiry.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryFinderTest {
    @Mock
    lateinit var inquiryFinder: InquiryFinder

    @Test
    fun `findAll(userId)`() {
        val userId = UUID.randomUUID()
        inquiryFinder.findAll(userId)
        verify(inquiryFinder).findAll(userId)
    }

    @Test
    fun `findById(userId)`() {
        val id = UUID.randomUUID()
        inquiryFinder.findById(id)
        verify(inquiryFinder).findById(id)
    }

    @Test
    fun `findAll()`() {
        inquiryFinder.findAll()
        verify(inquiryFinder).findAll()
    }
}
