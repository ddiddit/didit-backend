package com.didit.adapter.retrospect.out.persistence

import com.didit.adapter.retrospect.out.persistence.mapper.RetrospectivePersistenceMapper
import com.didit.adapter.retrospect.out.persistence.repository.RetrospectiveJpaRepository
import com.didit.application.retrospect.port.out.RetrospectiveCommandPort
import com.didit.application.retrospect.port.out.RetrospectiveQueryPort
import com.didit.domain.retrospect.entity.Retrospective
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.util.UUID

@Primary
@Component
class RetrospectivePersistenceAdapter(
    private val retrospectiveJpaRepository: RetrospectiveJpaRepository,
    private val retrospectivePersistenceMapper: RetrospectivePersistenceMapper,
) : RetrospectiveCommandPort,
    RetrospectiveQueryPort {
    override fun save(retrospective: Retrospective): Retrospective {
        val entity = retrospectivePersistenceMapper.toJpaEntity(retrospective)
        val savedEntity = retrospectiveJpaRepository.save(entity)
        return retrospectivePersistenceMapper.toDomain(savedEntity)
    }

    override fun findById(retrospectiveId: UUID): Retrospective? {
        val entity =
            retrospectiveJpaRepository.findWithChatMessagesById(retrospectiveId)
                ?: return null

        return retrospectivePersistenceMapper.toDomain(entity)
    }
}
