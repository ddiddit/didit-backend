package com.didit.domain.auth

import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserTest {
    @Test
    fun `register`() {
        val user = UserFixture.create()

        assertThat(user.provider).isEqualTo(Provider.KAKAO)
        assertThat(user.providerId).isEqualTo("kakao-123")
        assertThat(user.nickname).isEqualTo("디딧유저")
        assertThat(user.job).isNull()
        assertThat(user.isDeleted).isFalse()
    }

    @Test
    fun `withdraw`() {
        val user = UserFixture.create()
        val now = LocalDateTime.now()

        user.withdraw(now)

        assertThat(user.isDeleted).isTrue()
        assertThat(user.deletedAt).isEqualTo(now)
    }

    @Test
    fun `withdraw - already withdrawn throws exception`() {
        val user = UserFixture.create()
        user.withdraw()

        assertThatThrownBy { user.withdraw() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("이미 탈퇴한 회원입니다.")
    }

    @Test
    fun `updateProfile`() {
        val user = UserFixture.create()

        user.updateProfile(nickname = "새닉네임", job = "개발자")

        assertThat(user.nickname).isEqualTo("새닉네임")
        assertThat(user.job).isEqualTo("개발자")
    }

    @Test
    fun `updateProfile - blank nickname throws exception`() {
        val user = UserFixture.create()

        assertThatThrownBy { user.updateProfile(nickname = " ", job = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("닉네임은 비어있을 수 없습니다.")
    }

    @Test
    fun `updateProfile - job can be null`() {
        val user = UserFixture.create()
        user.updateProfile(nickname = "닉네임", job = "개발자")

        user.updateProfile(nickname = "닉네임", job = null)

        assertThat(user.job).isNull()
    }
}
