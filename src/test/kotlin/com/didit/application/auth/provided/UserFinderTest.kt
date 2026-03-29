package com.didit.application.auth.provided

import com.didit.application.auth.exception.UserNotFoundException
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserFinderTest {
    @Mock
    lateinit var userFinder: UserFinder

    @Test
    fun `findByIdOrThrow`() {
        val user = UserFixture.createOnboarded()
        whenever(userFinder.findByIdOrThrow(user.id)).thenReturn(user)

        val found = userFinder.findByIdOrThrow(user.id)

        verify(userFinder).findByIdOrThrow(user.id)
        assertThat(found.id).isEqualTo(user.id)
    }

    @Test
    fun `findByIdOrThrow - not found`() {
        val userId = UUID.randomUUID()
        whenever(userFinder.findByIdOrThrow(userId)).thenThrow(UserNotFoundException(userId))

        assertThatThrownBy { userFinder.findByIdOrThrow(userId) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `existsByNickname - exists`() {
        whenever(userFinder.existsByNickname("디딧유저")).thenReturn(true)

        val exists = userFinder.existsByNickname("디딧유저")

        verify(userFinder).existsByNickname("디딧유저")
        assertThat(exists).isTrue()
    }

    @Test
    fun `existsByNickname - not exists`() {
        whenever(userFinder.existsByNickname("없는닉네임")).thenReturn(false)

        val exists = userFinder.existsByNickname("없는닉네임")

        verify(userFinder).existsByNickname("없는닉네임")
        assertThat(exists).isFalse()
    }

    @Test
    fun `getJobByUserId - 유저의 직무를 반환한다`() {
        val user = UserFixture.createOnboarded()
        whenever(userFinder.getJobByUserId(user.id)).thenReturn(user.job)

        val job = userFinder.getJobByUserId(user.id)

        verify(userFinder).getJobByUserId(user.id)
        assertThat(job).isEqualTo(user.job)
    }

    @Test
    fun `getJobByUserId - 유저가 없으면 예외가 발생한다`() {
        val userId = UUID.randomUUID()
        whenever(userFinder.getJobByUserId(userId)).thenThrow(UserNotFoundException(userId))

        assertThatThrownBy { userFinder.getJobByUserId(userId) }
            .isInstanceOf(UserNotFoundException::class.java)
    }
}
