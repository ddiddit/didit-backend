package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
)
@Entity
class User(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(length = 50)
    var nickname: String? = null,
    @Column
    var email: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var job: Job? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: Provider,
    @Column(nullable = false)
    val providerId: String,
    @Column(name = "onboarding_completed_at")
    var onboardingCompletedAt: LocalDateTime? = null,
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    var consent: UserConsent? = null

    val isDeleted: Boolean get() = deletedAt != null
    val isOnboardingCompleted: Boolean get() = onboardingCompletedAt != null
    val marketingAgreed: Boolean get() = checkNotNull(consent) { "동의 정보가 존재하지 않습니다." }.marketingAgreed

    fun withdraw(now: LocalDateTime = LocalDateTime.now()) {
        check(!isDeleted) { "이미 탈퇴한 회원입니다." }
        deletedAt = now
    }

    fun completeOnboarding(
        nickname: String,
        job: Job,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        check(!isOnboardingCompleted) { "이미 온보딩이 완료된 회원입니다." }
        require(nickname.isValidNickname()) { "닉네임은 2~10자 한글, 영문, 숫자만 가능합니다." }

        this.nickname = nickname
        this.job = job
        this.onboardingCompletedAt = now
    }

    fun updateProfile(
        nickname: String,
        job: Job?,
    ) {
        require(nickname.isValidNickname()) { "닉네임은 2~10자 한글, 영문, 숫자만 가능합니다." }

        this.nickname = nickname
        this.job = job
    }

    fun rejoin(now: LocalDateTime = LocalDateTime.now()) {
        check(isDeleted) { "탈퇴한 회원이 아닙니다." }

        deletedAt = null
        onboardingCompletedAt = null
        nickname = null
        job = null
    }

    fun createConsent(marketingAgreed: Boolean) {
        check(consent == null) { "이미 동의 정보가 존재합니다." }
        consent = UserConsent.create(userId = id, marketingAgreed = marketingAgreed)
    }

    fun updateMarketingConsent(agreed: Boolean) {
        checkNotNull(consent) { "동의 정보가 존재하지 않습니다." }
        consent!!.updateMarketing(agreed)
    }

    private fun String.isValidNickname(): Boolean = matches(Regex("^[가-힣a-zA-Z0-9]{2,10}$"))

    companion object {
        fun register(request: UserRegisterRequest) =
            User(
                provider = request.provider,
                providerId = request.providerId,
                email = request.email,
            )
    }
}
