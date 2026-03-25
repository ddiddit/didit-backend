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
        assertThat(user.providerId).isEqualTo("kakao-0325")
        assertThat(user.nickname).isNull()
        assertThat(user.job).isNull()
        assertThat(user.isDeleted).isFalse()
        assertThat(user.isOnboardingCompleted).isFalse()
    }

    @Test
    fun `completeOnboarding`() {
        val user = UserFixture.create()
        val now = LocalDateTime.now()

        user.completeOnboarding(nickname = "디딧유저", job = Job.DEVELOPER, now = now)

        assertThat(user.nickname).isEqualTo("디딧유저")
        assertThat(user.job).isEqualTo(Job.DEVELOPER)
        assertThat(user.isOnboardingCompleted).isTrue()
        assertThat(user.onboardingCompletedAt).isEqualTo(now)
    }

    @Test
    fun `completeOnboarding - already completed throws exception`() {
        val user = UserFixture.createOnboarded()

        assertThatThrownBy { user.completeOnboarding(nickname = "디딧유저", job = Job.PLANNER) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("이미 온보딩이 완료된 회원입니다.")
    }

    @Test
    fun `completeOnboarding - blank nickname throws exception`() {
        val user = UserFixture.create()

        assertThatThrownBy { user.completeOnboarding(nickname = " ", job = Job.DEVELOPER) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("닉네임은 비어있을 수 없습니다.")
    }

    @Test
    fun `rejoin`() {
        val user = UserFixture.createOnboarded()
        user.withdraw()

        user.rejoin()

        assertThat(user.isDeleted).isFalse()
        assertThat(user.isOnboardingCompleted).isFalse()
        assertThat(user.nickname).isNull()
        assertThat(user.job).isNull()
    }

    @Test
    fun `rejoin - not withdrawn throws exception`() {
        val user = UserFixture.create()

        assertThatThrownBy { user.rejoin() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("탈퇴한 회원이 아닙니다.")
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
        val user = UserFixture.createOnboarded()

        user.updateProfile(nickname = "새닉네임", job = Job.DESIGNER)

        assertThat(user.nickname).isEqualTo("새닉네임")
        assertThat(user.job).isEqualTo(Job.DESIGNER)
    }

    @Test
    fun `updateProfile - blank nickname throws exception`() {
        val user = UserFixture.createOnboarded()

        assertThatThrownBy { user.updateProfile(nickname = " ", job = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("닉네임은 비어있을 수 없습니다.")
    }

    @Test
    fun `updateProfile - job can be null`() {
        val user = UserFixture.createOnboarded()

        user.updateProfile(nickname = "닉네임", job = null)

        assertThat(user.job).isNull()
    }
}
