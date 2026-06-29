package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.exception.InvalidPopupTypeException
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.UserMission
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserMissionServiceTest {
    @Mock
    lateinit var userMissionRepository: UserMissionRepository

    private lateinit var userMissionService: UserMissionService

    private val userId = UUID.randomUUID()
    private val missionId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userMissionService = UserMissionService(userMissionRepository)
    }

    @Test
    fun `레벨업 팝업 확인을 처리한다`() {
        val userMission = UserMission(userId = userId, missionId = missionId)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(userMissionRepository.save(any())).thenReturn(userMission)

        userMissionService.confirmPopup(userId, "LEVEL_UP")

        assertThat(userMission.isLevelUpPopupShown()).isTrue()
        verify(userMissionRepository).save(userMission)
    }

    @Test
    fun `실패 팝업 확인을 처리한다`() {
        val userMission = UserMission(userId = userId, missionId = missionId)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)
        whenever(userMissionRepository.save(any())).thenReturn(userMission)

        userMissionService.confirmPopup(userId, "FAILURE")

        assertThat(userMission.isFailurePopupShown()).isTrue()
        verify(userMissionRepository).save(userMission)
    }

    @Test
    fun `현재 미션이 없으면 CurrentMissionNotFoundException을 발생시킨다`() {
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(null)

        assertThatThrownBy { userMissionService.confirmPopup(userId, "LEVEL_UP") }
            .isInstanceOf(CurrentMissionNotFoundException::class.java)
    }

    @Test
    fun `유효하지 않은 팝업 타입이면 InvalidPopupTypeException을 발생시킨다`() {
        val userMission = UserMission(userId = userId, missionId = missionId)
        whenever(userMissionRepository.findCurrentMissionByUserId(userId)).thenReturn(userMission)

        assertThatThrownBy { userMissionService.confirmPopup(userId, "INVALID_TYPE") }
            .isInstanceOf(InvalidPopupTypeException::class.java)
    }
}
