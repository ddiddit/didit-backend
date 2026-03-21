package com.didit.adapter.retrospect.out.persistence.entity

import com.didit.domain.retrospect.enums.RetroStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "retrospective")
class RetrospectiveJpaEntity(

    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,

    @Column(nullable = false)
    var userId: UUID,

    @Column
    var projectId: UUID? = null,

    @Column(length = 255)
    var title: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RetroStatus = RetroStatus.IN_PROGRESS,

    @Column(nullable = false)
    var inputTokens: Int = 0,

    @Column(nullable = false)
    var outputTokens: Int = 0,

    @Column(columnDefinition = "TEXT")
    var doneWork: String? = null,

    @Column(columnDefinition = "TEXT")
    var blockedPoint: String? = null,

    @Column(columnDefinition = "TEXT")
    var solutionProcess: String? = null,

    @Column(columnDefinition = "TEXT")
    var lessonLearned: String? = null,

    @Column(columnDefinition = "TEXT")
    var insight: String? = null,

    @Column(columnDefinition = "TEXT")
    var improvementDirection: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @ElementCollection
    @CollectionTable(
        name = "retrospective_tag",
        joinColumns = [JoinColumn(name = "retrospective_id")]
    )
    @Column(name = "tag_id", nullable = false)
    var tagIds: MutableList<UUID> = mutableListOf()

    @OneToMany(
        mappedBy = "retrospective",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var chatMessages: MutableList<ChatMessageJpaEntity> = mutableListOf()

    constructor() : this(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID()
    )

    fun replaceChatMessages(messages: List<ChatMessageJpaEntity>) {
        chatMessages.clear()
        chatMessages.addAll(messages)
    }
}