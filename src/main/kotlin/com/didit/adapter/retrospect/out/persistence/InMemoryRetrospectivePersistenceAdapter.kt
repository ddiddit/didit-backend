package com.didit.adapter.retrospect.out.persistence

import com.didit.application.retrospect.port.out.RetrospectiveCommandPort
import com.didit.application.retrospect.port.out.RetrospectiveQueryPort
import com.didit.domain.retrospect.entity.Retrospective
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryRetrospectivePersistenceAdapter :
    RetrospectiveCommandPort,
    RetrospectiveQueryPort {
    private val storage = ConcurrentHashMap<UUID, Retrospective>()

    override fun save(retrospective: Retrospective): Retrospective {
        storage[retrospective.id] = retrospective
        return retrospective
    }

    override fun findById(retrospectiveId: UUID): Retrospective? = storage[retrospectiveId]
}
