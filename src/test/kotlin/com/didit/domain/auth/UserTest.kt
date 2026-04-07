package com.didit.domain.auth

import com.didit.domain.shared.Job
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
        assertThat(user.email).isEqualTo("test@kakao.com")
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
    fun `completeOnboarding - invalid nickname throws exception`() {
        val user = UserFixture.create()

        assertThatThrownBy { user.completeOnboarding(nickname = "a", job = Job.DEVELOPER) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("닉네임은 2~10자 한글, 영문, 숫자만 가능합니다.")
    }

    @Test
    fun `completeOnboarding - nickname with special characters throws exception`() {
        val user = UserFixture.create()

        assertThatThrownBy { user.completeOnboarding(nickname = "닉네임!", job = Job.DEVELOPER) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("닉네임은 2~10자 한글, 영문, 숫자만 가능합니다.")
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
        assertThat(user.consent).isNull()
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
    fun `updateProfile - invalid nickname throws exception`() {
        val user = UserFixture.createOnboarded()

        assertThatThrownBy { user.updateProfile(nickname = "a", job = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("닉네임은 2~10자 한글, 영문, 숫자만 가능합니다.")
    }

    @Test
    fun `updateProfile - nickname with special characters throws exception`() {
        val user = UserFixture.createOnboarded()

        assertThatThrownBy { user.updateProfile(nickname = "닉네임!", job = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("닉네임은 2~10자 한글, 영문, 숫자만 가능합니다.")
    }

    @Test
    fun `updateProfile - job can be null`() {
        val user = UserFixture.createOnboarded()

        user.updateProfile(nickname = "닉네임", job = null)

        assertThat(user.job).isNull()
    }

    @Test
    fun `createConsent`() {
        val user = UserFixture.create()

        user.createConsent(marketingAgreed = true)

        assertThat(user.consent).isNotNull
        assertThat(user.marketingAgreed).isTrue()
    }

    @Test
    fun `createConsent - already exists throws exception`() {
        val user = UserFixture.create()
        user.createConsent(marketingAgreed = true)

        assertThatThrownBy { user.createConsent(marketingAgreed = false) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("이미 동의 정보가 존재합니다.")
    }

    @Test
    fun `updateMarketingConsent`() {
        val user = UserFixture.create()
        user.createConsent(marketingAgreed = true)

        user.updateMarketingConsent(agreed = false)

        assertThat(user.marketingAgreed).isFalse()
    }

    @Test
    fun `updateMarketingConsent - consent not exists throws exception`() {
        val user = UserFixture.create()

        assertThatThrownBy { user.updateMarketingConsent(agreed = true) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("동의 정보가 존재하지 않습니다.")
    }

    @Test
    fun `marketingAgreed - consent not exists throws exception`() {
        val user = UserFixture.create()

        assertThatThrownBy { user.marketingAgreed }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("동의 정보가 존재하지 않습니다.")
    }
}
