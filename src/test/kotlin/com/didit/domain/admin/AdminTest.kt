package com.didit.domain.admin

import com.didit.support.AdminFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AdminTest {
    @Test
    fun `createSuperAdmin`() {
        val admin = AdminFixture.createSuperAdmin()

        assertThat(admin.email).isEqualTo("super@didit.com")
        assertThat(admin.role).isEqualTo(AdminRole.SUPER_ADMIN)
        assertThat(admin.position).isNull()
    }

    @Test
    fun `register`() {
        val admin = AdminFixture.createAdmin(position = AdminPosition.DEVELOPER)

        assertThat(admin.email).isEqualTo("admin@didit.com")
        assertThat(admin.role).isEqualTo(AdminRole.ADMIN)
        assertThat(admin.position).isEqualTo(AdminPosition.DEVELOPER)
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
