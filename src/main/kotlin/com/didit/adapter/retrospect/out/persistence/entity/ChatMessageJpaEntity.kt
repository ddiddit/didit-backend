package com.didit.adapter.retrospect.out.persistence.entity

import com.didit.domain.retrospect.enums.QuestionType
import com.didit.domain.retrospect.enums.Sender
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_message")
class ChatMessageJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var sender: Sender,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var questionType: QuestionType,

    @Column(nullable = false)
    var isSkipped: Boolean = false,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrospective_id", nullable = false)
    var retrospective: RetrospectiveJpaEntity
) {
    protected constructor() : this(
        sender = Sender.AI,
        content = "",
        questionType = QuestionType.Q1,
        isSkipped = false,
        createdAt = LocalDateTime.now(),
        retrospective = RetrospectiveJpaEntity()
    )
}