package com.didit.domain.inquiry

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "inquiries")
@Entity
class Inquiry(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val userId: UUID,
    @Column(nullable = false)
    val email: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: InquiryType,
    @Column
    val typeEtc: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(nullable = false)
    val isAgreed: Boolean = false,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: InquiryStatus = InquiryStatus.PENDING,
    @Column(columnDefinition = "BINARY(16)")
    var adminId: UUID? = null,
    @Column
    var adminAnswer: String? = null,
    @Column
    var answeredAt: LocalDateTime? = null,
    @Column
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isAnswered(): Boolean = adminAnswer != null

    init {
        require(isAgreed) { "개인정보 수집 동의는 필수입니다." }
        if (type == InquiryType.ETC) {
            require(!typeEtc.isNullOrBlank()) { "기타 유형은 추가 입력이 필요합니다." }
        }
    }

    companion object {
        fun register(
            request: InquiryRegisterRequest,
            userId: UUID,
            email: String?,
        ): Inquiry =
            Inquiry(
                id = UUID.randomUUID(),
                userId = userId,
                email = email!!,
                type = request.type,
                typeEtc = request.typeEtc,
                content = request.content,
                isAgreed = request.isAgreed,
            )
    }

    fun answer(
        adminId: UUID,
        answer: String,
    ) {
        require(status != InquiryStatus.ANSWERED) { "이미 답변이 완료된 문의입니다." }

        this.adminId = adminId
        this.adminAnswer = answer
        this.status = InquiryStatus.ANSWERED
        this.answeredAt = LocalDateTime.now()
    }

    fun updateAnswer(
        answer: String,
        adminId: UUID,
    ) {
        require(status == InquiryStatus.ANSWERED) { "답변이 미완료된 문의는 답변 수정이 불가능합니다." }
        require(this.adminId == adminId) { "해당 문의 답변에 권한이 없습니다." }

        this.adminAnswer = answer
        this.answeredAt = LocalDateTime.now()
    }
}
