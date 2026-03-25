package com.didit.application.auth.provided

import com.didit.domain.auth.Job
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
}
