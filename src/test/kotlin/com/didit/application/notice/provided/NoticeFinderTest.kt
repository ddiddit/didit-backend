package com.didit.application.notice.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NoticeFinderTest {
    @Mock
    lateinit var noticeFinder: NoticeFinder

    @Test
    fun `findAll()`() {
        noticeFinder.findAll()
        verify(noticeFinder).findAll()
    }

    @Test
    fun `findById()`() {
        val noticeId = UUID.randomUUID()

        noticeFinder.findById(noticeId)

        verify(noticeFinder).findById(noticeId)
    }

    @Test
    fun `findAllForAdmin()`() {
        noticeFinder.findAllForAdmin()
        verify(noticeFinder).findAllForAdmin()
    }

    @Test
    fun `findByIdForAdmin()`() {
        val noticeId = UUID.randomUUID()

        noticeFinder.findByIdForAdmin(noticeId)

        verify(noticeFinder).findByIdForAdmin(noticeId)
    }
}
