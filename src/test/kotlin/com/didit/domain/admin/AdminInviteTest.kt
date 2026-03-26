package com.didit.domain.admin

import com.didit.support.AdminInviteFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AdminInviteTest {
    @Test
    fun `create`() {
        val invite = AdminInviteFixture.create()

        assertThat(invite.email).isEqualTo("invite@didit.com")
        assertThat(invite.position).isEqualTo(AdminPosition.DEVELOPER)
        assertThat(invite.isUsed).isFalse()
        assertThat(invite.isExpired()).isFalse()
    }

    @Test
    fun `use`() {
        val invite = AdminInviteFixture.create()
        val now = LocalDateTime.now()

        invite.use(now)

        assertThat(invite.isUsed).isTrue()
        assertThat(invite.usedAt).isEqualTo(now)
    }

    @Test
    fun `use - already used throws exception`() {
        val invite = AdminInviteFixture.create()
        invite.use()

        assertThatThrownBy { invite.use() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("이미 사용된 초대입니다.")
    }

    @Test
    fun `use - expired throws exception`() {
        val invite = AdminInviteFixture.create(expiredAt = LocalDateTime.now().minusDays(1))

        assertThatThrownBy { invite.use() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("만료된 초대입니다.")
    }

    @Test
    fun `isExpired - not expired`() {
        val invite = AdminInviteFixture.create(expiredAt = LocalDateTime.now().plusDays(1))
        val now = LocalDateTime.now()

        assertThat(invite.isExpired(now)).isFalse()
    }

    @Test
    fun `isExpired - expired`() {
        val invite = AdminInviteFixture.create(expiredAt = LocalDateTime.now().minusDays(1))
        val now = LocalDateTime.now()

        assertThat(invite.isExpired(now)).isTrue()
    }
}
