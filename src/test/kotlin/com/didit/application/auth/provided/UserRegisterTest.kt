package com.didit.application.auth.provided

import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserRegisterTest {
    @Mock
    lateinit var userRegister: UserRegister

    @Test
    fun `register`() {
        val userId = UUID.randomUUID()

        userRegister.register(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            marketingAgreed = true,
            nightPushAgreed = false,
        )

        verify(userRegister).register(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            marketingAgreed = true,
            nightPushAgreed = false,
        )
    }

    @Test
    fun `register - marketing not agreed`() {
        val userId = UUID.randomUUID()

        userRegister.register(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            marketingAgreed = false,
            nightPushAgreed = false,
        )

        verify(userRegister).register(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            marketingAgreed = false,
            nightPushAgreed = false,
        )
    }

    @Test
    fun `register - night push agreed`() {
        val userId = UUID.randomUUID()

        userRegister.register(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            marketingAgreed = false,
            nightPushAgreed = true,
        )

        verify(userRegister).register(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            marketingAgreed = false,
            nightPushAgreed = true,
        )
    }

    @Test
    fun `updateProfile`() {
        val userId = UUID.randomUUID()

        userRegister.updateProfile(
            userId = userId,
            nickname = "새닉네임",
            job = Job.DESIGNER,
        )

        verify(userRegister).updateProfile(
            userId = userId,
            nickname = "새닉네임",
            job = Job.DESIGNER,
        )
    }

    @Test
    fun `registerV2 - age와 experience 포함`() {
        val userId = UUID.randomUUID()

        userRegister.registerV2(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            age = UserAge.AGE_30,
            experience = UserExperience.YEARS_3_TO_5,
            marketingAgreed = true,
            nightPushAgreed = false,
        )

        verify(userRegister).registerV2(
            userId = userId,
            nickname = "디딧유저",
            job = Job.DEVELOPER,
            age = UserAge.AGE_30,
            experience = UserExperience.YEARS_3_TO_5,
            marketingAgreed = true,
            nightPushAgreed = false,
        )
    }

    @Test
    fun `updateProfileV2 - age와 experience 모두 포함`() {
        val userId = UUID.randomUUID()

        userRegister.updateProfileV2(
            userId = userId,
            nickname = "수정된닉네임",
            job = Job.PLANNER,
            age = UserAge.AGE_40_PLUS,
            experience = UserExperience.YEARS_10_PLUS,
        )

        verify(userRegister).updateProfileV2(
            userId = userId,
            nickname = "수정된닉네임",
            job = Job.PLANNER,
            age = UserAge.AGE_40_PLUS,
            experience = UserExperience.YEARS_10_PLUS,
        )
    }

    @Test
    fun `updateProfileV2 - age와 experience 없이`() {
        val userId = UUID.randomUUID()

        userRegister.updateProfileV2(
            userId = userId,
            nickname = "수정된닉네임",
            job = Job.DESIGNER,
        )

        verify(userRegister).updateProfileV2(
            userId = userId,
            nickname = "수정된닉네임",
            job = Job.DESIGNER,
            age = null,
            experience = null,
        )
    }
}
