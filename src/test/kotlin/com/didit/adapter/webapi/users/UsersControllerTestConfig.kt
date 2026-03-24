package com.didit.adapter.webapi.users

import com.didit.application.users.provided.WithdrawUseCase
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class UsersControllerTestConfig {
    @Bean
    fun withdrawUseCase(): WithdrawUseCase = mock(WithdrawUseCase::class.java)
}
