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
    @Column(columnDefinition = "BINARY(16)")
    val adminId: UUID? = null,
    @Column
    val adminAnswer: String? = null,
    @Column
    val answeredAt: LocalDateTime? = null,
    @Column
    val deletedAt: LocalDateTime? = null,
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
}
