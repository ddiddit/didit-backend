package com.didit.support

import com.didit.adapter.config.JpaAuditingConfig
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(JpaAuditingConfig::class)
abstract class RepositoryTestSupport
