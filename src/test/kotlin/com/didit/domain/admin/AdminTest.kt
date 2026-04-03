package com.didit.domain.admin

import com.didit.support.AdminFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AdminTest {
    @Test
    fun `createSuperAdmin`() {
        val admin = AdminFixture.createSuperAdmin()

        assertThat(admin.email).isEqualTo("super@didit.com")
        assertThat(admin.role).isEqualTo(AdminRole.SUPER_ADMIN)
        assertThat(admin.position).isNull()
        assertThat(admin.status).isEqualTo(AdminStatus.ACTIVE)
    }

    @Test
    fun `register`() {
        val admin = AdminFixture.createAdmin(position = AdminPosition.DEVELOPER)

        assertThat(admin.email).isEqualTo("admin@didit.com")
        assertThat(admin.role).isEqualTo(AdminRole.ADMIN)
        assertThat(admin.position).isEqualTo(AdminPosition.DEVELOPER)
        assertThat(admin.status).isEqualTo(AdminStatus.PENDING)
    }

    @Test
    fun `approve`() {
        val admin = AdminFixture.createAdmin()

        admin.approve()

        assertThat(admin.status).isEqualTo(AdminStatus.ACTIVE)
        assertThat(admin.isActive).isTrue()
    }

    @Test
    fun `approve - not pending throws exception`() {
        val admin = AdminFixture.createAdmin()
        admin.approve()

        assertThatThrownBy { admin.approve() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("대기 중인 어드민이 아닙니다.")
    }

    @Test
    fun `reject`() {
        val admin = AdminFixture.createAdmin()

        admin.reject()

        assertThat(admin.status).isEqualTo(AdminStatus.REJECTED)
        assertThat(admin.isActive).isFalse()
    }

    @Test
    fun `reject - not pending throws exception`() {
        val admin = AdminFixture.createAdmin()
        admin.reject()

        assertThatThrownBy { admin.reject() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("대기 중인 어드민이 아닙니다.")
    }

    @Test
    fun `register - planner`() {
        val admin = AdminFixture.createAdmin(position = AdminPosition.PLANNER)

        assertThat(admin.position).isEqualTo(AdminPosition.PLANNER)
    }

    @Test
    fun `register - designer`() {
        val admin = AdminFixture.createAdmin(position = AdminPosition.DESIGNER)

        assertThat(admin.position).isEqualTo(AdminPosition.DESIGNER)
    }
}
