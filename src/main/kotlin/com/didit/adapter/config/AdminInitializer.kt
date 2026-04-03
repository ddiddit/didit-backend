package com.didit.adapter.config

import com.didit.application.admin.required.AdminRepository
import com.didit.application.admin.required.PasswordEncryptor
import com.didit.domain.admin.Admin
import com.didit.domain.admin.AdminRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AdminInitializer(
    private val adminRepository: AdminRepository,
    private val passwordEncryptor: PasswordEncryptor,
    @param:Value("\${admin.initial.username}") private val username: String,
    @param:Value("\${admin.initial.password}") private val password: String,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (adminRepository.existsByRole(AdminRole.SUPER_ADMIN)) return

        adminRepository.save(
            Admin.createSuperAdmin(
                email = username,
                encodedPassword = passwordEncryptor.encode(password),
            ),
        )
    }
}
